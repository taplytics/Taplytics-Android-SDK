/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import com.taplytics.sdk.CodeBlockListener;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

/**
 * Created by VicV on 6/3/15.
 * <p/>
 * Little manager for CodeBlocks.
 */
public class TLCodeBlockManager {

    /**
     * Instance of {@link TLCodeBlockManager}
     */
    private static TLCodeBlockManager instance;

    /**
     * @return {@link #instance} of {@link TLCodeBlockManager}
     */
    public static TLCodeBlockManager getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new TLCodeBlockManager();
        }
        return instance;
    }

    /**
     * Run a code block synchronously. Basically, if its not there in TLProperties, don't run it.
     *
     * @param name     name of the code block.
     * @param listener a {@link CodeBlockListener} that gets its {@link CodeBlockListener#run() run}
     *                 function called if necessary.
     */
    public void runCodeBlockSync(String name, CodeBlockListener listener) {
        simpleRun(name, listener, true);
    }

    /**
     * Basic runner for the code block by name. Grab name from tlproperties. Run listener if it exists.
     *
     * @param name      name of the code block.
     * @param listener  a {@link CodeBlockListener} that gets its {@link CodeBlockListener#run() run}
     *                  function called if necessary.
     * @param allowPush whether or not we want to push to the socket if we don't have the data.
     * @return whether or not the value was received from TLProperties.
     */
    private boolean simpleRun(String name, CodeBlockListener listener, boolean allowPush) {
        try {
            //Grab our dynamic variables.
            TLProperties props = TLManager.getInstance().getTlProperties();
            if (props != null) {
                JSONObject vars = props.getDynamicVars();
                //See if vars exist and if our var exists.
                if (vars != null && vars.has(name)) {
                    JSONObject var = vars.getJSONObject(name);
                    //Check that the type is a codeBlock
                    if (var.has("variableType")) {
                        String type = var.getString("variableType");
                        if (type.equals("Code Block")) {
                            //If the type is a code block, we're good, run it.
                            listener.run();
                            return true;
                        }
                    }
                } else {
                    //We don't want to push twice, so only push if we want to.
                    if (allowPush) {
                        pushToSocket(name);
                    }
                }
            } else {
                if (allowPush) {
                    pushToSocket(name);
                }
            }
        } catch (Exception e) {
            TLLog.error("Problem running code block sync", e);
        }
        return false;
    }

    /**
     * Run a code block asynchronously. Basically, if TL properties doesn't exist, re-call
     * {@link TLManager#getPropertiesFromServer(Map, Promise) getPropertiesFromServer} and check
     * once more.
     */
    public void runCodeBlock(String name, CodeBlockListener listener) {
        //If simpleRun returns false, it means we used the default value and
        //the variable was not in tlproperties.
        if (!simpleRun(name, listener, true)) {
            checkServerThenRun(name, listener);
        }
    }

    /**
     * Force a re-check of TLProperties with a promise. Then just do our
     * {@link #runCodeBlockSync(String, CodeBlockListener) runCodeBlock} things.
     *
     * @param name     name of the code block.
     * @param listener a {@link CodeBlockListener} that gets its {@link CodeBlockListener#run() run}
     *                 function called if necessary.
     */
    private void checkServerThenRun(final String name, final CodeBlockListener listener) {
        try {
            //Grab the promise and check when we're done.
            PromiseListener codeBlockPromise = new PromiseListener() {
                @Override
                public void succeeded() {
                    super.completed();
                    simpleRun(name, listener, false);
                }

                @Override
                public void failedOrCancelled() {
                    super.failedOrCancelled();
                }
            };
            TLManager.getInstance().getTlPropertiesPromise().add(codeBlockPromise);
        } catch (Exception e) {
            TLLog.error("Problem running code block sync", e);
        }
    }

    /**
     * Push this up to the socket.
     *
     * @param name name of the code block.
     */
    private void pushToSocket(final String name) {
        if (TLManager.getInstance().isLiveUpdate()) {
            TLSocketManager.getInstance().getSocketRoomPromise().add(new PromiseListener() {
                @Override
                public void succeeded() {
                    super.succeeded();
                    try {
                        if (TLManager.getInstance().getTrackingEnabled() && TLManager.getInstance().isLiveUpdate()) {
                            JSONObject inner = new JSONObject();
                            inner.put("name", name);
                            //At the end of the day a codeBlock is just a dynamic variable with a method as its type.
                            inner.put("variableType", "Code Block");
                            inner.put("created_at", new Date());
                            TLSocketManager.getInstance().emitSocketEvent("newCodeBlock", inner);
                        }
                    } catch (Exception e) {
                        TLLog.error("error pushing codeBlock to server", e);
                    }
                }
            });
        }
    }


}
