/*
 * Copyright © 2014 Victor Vucicevich.
 */

package com.taplytics.sdk.utils.promises;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Promise<T> {

    public enum State {
        finished, failed, cancelled, notDone
    }

    // We're gonna strap listeners to every promise we make. So we can keep
    // track of them here. So many threads!
    private final List<PromiseListener<T>> _allListeners = new ArrayList<>();

    private final Object _completionLock = new Object();

    // The thing we give back!
    private T _result;

    // State yo
    private State _state = State.notDone;

    public boolean isComplete() {
        return _complete;
    }

    // Whether or not this promise is done
    private boolean _complete = false;

    // Why you gotta break, man?
    private Exception _exception;

    // Clear up the promisess!
    private void finishPromise(State state, T returnable, Exception exception) {

        List<PromiseListener<T>> listeners = new ArrayList(_allListeners);

        synchronized (_completionLock) {
            if (!_complete) {
                _allListeners.clear();
                _state = state;
                _exception = exception;
                _complete = true;
                _result = returnable;
            } else {
                return;
            }
        }

        for (PromiseListener<T> listener : listeners) {
            reactToListener(listener, state);
        }

    }

    // Something happened! Yay!
    // TODO: Refactor. This is ugly.
    private void reactToListener(PromiseListener<T> currentListener, State state) {
        switch (state) {
            case finished:
                currentListener.succeeded(_result);
                currentListener.succeeded();
                break;
            case failed:
                currentListener.failed(_exception);
                currentListener.failed();
                currentListener.failedOrCancelled(_exception);
                currentListener.failedOrCancelled();
                break;
            case cancelled:
                currentListener.cancelled();
                currentListener.failedOrCancelled(_exception);
                currentListener.failedOrCancelled();
                break;
            default:
                break;
        }
    }

    public boolean hasListeners() {
        return _allListeners.size() > 0;
    }

    public Exception getException() {
        return _exception;
    }

    public T getResult() {
        return _result;
    }

    // Okay so now what we WANT To happen, is that when we strap a listener to
    // promise, its going to wait for a call. But if the promise has already
    // completed? Then we go through the finish process.
    public <TypedPromiseListener extends PromiseListener<T>> TypedPromiseListener add(TypedPromiseListener newListener) {
        if (_complete) {
            // This is neato:
            // If the promise is done, and we try to listen to it, we'll return
            // the final results
            reactToListener(newListener, _state);
        } else {
            _allListeners.add(newListener);
        }

        return newListener;
    }

    // Resolving them
    public void finish(T result) {
        finishPromise(State.finished, result, null);
    }

    public void finish() {
        finishPromise(State.finished, null, null);
    }

    public void fail(Exception e) {
        finishPromise(State.failed, null, e);
    }

    public void cancel() {
        finishPromise(State.cancelled, null, null);
    }

    // Tracking them
    public boolean isFinished() {
        return _state == State.finished;
    }

    public boolean isCancelled() {
        return _state == State.cancelled;
    }

    public boolean hasFailed() {
        return _state == State.failed;
    }

    public void timeout(int millis) {

        final ScheduledFuture<?> futureTask = PromiseUtils.getWorkerService().schedule(new Runnable() {

            @Override
            public void run() {
                getPromise().fail(new TimeoutException());
            }
        }, millis, TimeUnit.MILLISECONDS);

        add(new PromiseListener<T>() {
            @Override
            public void completed() {
                futureTask.cancel(true);
            }
        });

    }

    public State getState() {
        return _state;
    }

    private Promise<T> getPromise() {
        return this;
    }

}
