/*
 * Copyright © 2014 Victor Vucicevich.
 */

package com.taplytics.sdk.utils.promises;

/**
 * Our listener for promises. The most important stuff, right here. So, this is what a promise can listen for.
 * 
 * @author V
 * 
 * @param <T>
 **/
public abstract class PromiseListener<T> {

	// If something fucks up
	public void failed() {

	}

	// If something fucks up and we wanna talk about it
	public void failed(Exception reason) {

	}

	// If someone decided to pull the plug
	public void cancelled() {

	}

	// At this point we're just lazy, maybe I should just call this 'ended'
	public void failedOrCancelled(Exception reason) {

	}

	public void failedOrCancelled() {

	}

	// Woo everything looks good.
	public void succeeded(T result) {

	}

	public void succeeded() {

	}

	// Different than succeeded. Basically this is so the listener will know its safe to get rid of this.
	public void completed() {

	}

}
