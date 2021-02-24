/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import android.support.annotation.NonNull;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by clvcooke on 9/22/15.
 */
//This is really just a wrapper on top of a singleThread executor
public class TLExecutor implements ExecutorService {


    //does all the actual work
    ExecutorService executorSlave;

    //create a TLExecutor feeding it in a slave to do all the actual work of a singleThread executor
    public TLExecutor(ExecutorService slave) {
        executorSlave = slave;
    }

    @Override
    public void shutdown() {
        executorSlave.shutdown();
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        return executorSlave.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorSlave.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorSlave.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorSlave.awaitTermination(timeout, unit);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorSlave.submit(task);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executorSlave.submit(task, result);
    }

    @NonNull
    @Override
    public Future<?> submit(Runnable task) {
        try {
            //actually execute the runnable
            if (task != null) {

                Class eventRunnable = task.getClass();
                Field eventField = eventRunnable.getDeclaredField("event");
                eventField.setAccessible(true);
                Object event = eventField.get(task);
                eventField.setAccessible(false);

                Method getEventData = event.getClass().getDeclaredMethod("getEventData");
                Object eventDataObj = getEventData.invoke(event);

                if (eventDataObj instanceof Map) {
                    Map<String, Object> eventData = (Map<String, Object>) eventDataObj;

                    String eventName = (String) eventData.get("action");
                    Map<?, ?> contextData = (Map<?, ?>) eventData.get("contextdata");

                    if (eventName != null) {
                        JSONObject contextDataJson = contextData == null ? null : new JSONObject(contextData);

                        TLManager.getInstance()
                                .getTlAnalytics()
                                .trackSourceEvent("adobe", eventName, null, contextDataJson);
                    }

                }
            }
        } catch (Throwable e) {
            TLLog.error("Error executing Adobe command", e);
        }

        return executorSlave.submit(task);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorSlave.invokeAll(tasks);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executorSlave.invokeAll(tasks, timeout, unit);
    }

    @NonNull
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorSlave.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorSlave.invokeAny(tasks, timeout, unit);
    }


    //Adobe has a logging thread, which is really just a single threaded executor. When an event is tracked with adobe they post it to the single thread executor to handle the actual logging of it.
    //So in order to detect the events we replace their executor with our own. This way every time they log an event we log it normally, then RIGHT after we retrieve it from the database
    @Override
    public void execute(Runnable command) {
        try {
            //actually execute the runnable
            if (command != null) {
                executorSlave.execute(command);

                Map m = new HashMap();
                String eventName = null;

                if (command.getClass().getDeclaredFields().length >= 2) {
                    for (Field f : command.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType().isAssignableFrom(String.class)) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("action") || name.contains("state")) {
                                eventName = (String) f.get(command);
                            }
                        } else if (f.getType().isAssignableFrom(HashMap.class)) {
                            m = (Map) f.get(command);
                        }
                    }
                    if (eventName != null) {
                        TLManager.getInstance().getTlAnalytics().trackSourceEvent("adobe", eventName, null, m == null ? null : new JSONObject(m));
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error executing Adobe command", e);
        }
    }
}
