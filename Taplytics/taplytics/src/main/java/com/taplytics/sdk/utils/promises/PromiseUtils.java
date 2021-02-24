/*
 * Copyright © 2014 Victor Vucicevich.
 */

package com.taplytics.sdk.utils.promises;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PromiseUtils {

	private static final ScheduledExecutorService _worker = Executors.newSingleThreadScheduledExecutor();

	public static ScheduledExecutorService getWorkerService() {
		return _worker;
	}
}
