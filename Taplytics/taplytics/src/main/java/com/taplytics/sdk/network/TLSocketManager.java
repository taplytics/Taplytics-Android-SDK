/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLThreadManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.resources.TLHighlightShape;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;


public class TLSocketManager {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static TLSocketManager instance = null;

    public static TLSocketManager getInstance() {
        if (instance == null) {
            instance = new TLSocketManager();
        }
        return instance;
    }

    private Socket socketIO = null;

    private boolean socketConnectOnMainThread = false;
    private String socketRoom = null;
    public boolean isConnected = false;
    private boolean isConnectedToSocketRoom = false;
    private JSONObject pairDeviceMap;
    private boolean pairedFromLink = false;
    private int pairingAttempts = 0;

    private static final int MAX_SOCKET_RECONNECTS = 20;
    private int connectionAttempts;

    public Promise getSocketRoomPromise() {
        return socketRoomPromise;
    }

    private Promise socketRoomPromise = new Promise();

    public void setSocketConnectOnMainThread(boolean socketConnectOnMainThread) {
        this.socketConnectOnMainThread = socketConnectOnMainThread;
    }

    /**
     * Handler used for flushing the event queue in the background. *
     */
    private TLSocketManager() {
        try {
            //Twinprime hijacks our network calls on the event thread. If it exists, we dont want to be on that thread.
            Class.forName("com.twinprime.TwinPrimeSDK.TwinPrimeSDK");
            socketConnectOnMainThread = true;
        } catch (Throwable t) {

        }
    }

    public interface TLConnectSocketListener {
        void onResponse(Boolean connected);
    }

    private boolean connecting = false;

    public void connectSocketIO(final TLConnectSocketListener listener, boolean deepLinkPairing) {
        if (TLManager.getInstance().thereAreSockets) {
            String url = TLManager.getInstance().getTlNetworking().getEnvironment().getSocketPath();
            TLLog.debug("connecting socket");

            try {
                if ((socketIO == null || !socketIO.connected()) && (!TLUtils.isDisabled(Functionality.SOCKETS) || deepLinkPairing || this.pairedFromLink) && !connecting) {
                    // Create socket
                    try {
                        disconnectSocketIO();
                        IO.Options opts = new IO.Options();
                        opts.forceNew = true;
                        opts.reconnection = true;
                        opts.reconnectionDelay = 5000;
                        opts.upgrade = true;
                        opts.timeout = -1;
                        opts.transports = new String[]{WebSocket.NAME};
                        socketIO = IO.socket(url, opts);
                        connecting = true;
                    } catch (URISyntaxException e1) {
                        connecting = false;
                        TLLog.error("Socket URI path error", e1);
                    }

                    isConnectedToSocketRoom = false;

                    // Set socket callbacks
                    socketIO.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("SocketIO Connected!");
                            isConnected = true;
                            connecting = false;
                            pairingAttempts = 0;
                            connectToSocketRoom();
                            if (TLViewManager.getInstance().getBorderMode().equals(ViewUtils.BORDER_MODE.DISCONNECT)) {
                                TLViewManager.getInstance().hideDisconnectBorder();
                            }
                            if (listener != null) listener.onResponse(true);
                        }
                    }).on("connectTestDevice", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                final JSONObject obj = (JSONObject) args[0];
                                TLLog.debug("socketio connectTestDevice socket: " + obj.toString());
                                TLUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showTestDeviceAlert(obj);
                                    }
                                });
                            } catch (Exception e) {
                                TLLog.error("socketio connectTestDevice error", e);
                            }
                        }
                    }).on("foundTestDevice", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                final JSONObject obj = (JSONObject) args[0];
                                TLLog.debug("socketio foundTestDevice socket: " + obj.toString());

                                TLUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideTestDeviceAlert(obj);
                                        TLViewManager.getInstance().removeBorder();
                                    }
                                });
                            } catch (Exception e) {
                                TLLog.error("socketio foundTestDevice error", e);
                            }
                        }
                    }).on("reload", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                TLManager.getInstance().getPropertiesFromServer(null, null);
                            } catch (Throwable e) {
                                //
                            }
                        }
                    }).on("clientShowExperiment", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                TLLog.debug("client show experiment socket");

                                if (args[0] == null || !(args[0] instanceof JSONObject)) {
                                    return;
                                }
                                final JSONObject obj = (JSONObject) args[0];
                                JSONObject experiment = obj.optJSONObject("experiment");
                                final String experimentName = experiment == null ? "" : experiment.optString("name");
                                final String variationName = obj.has("variation_name") ? obj.optString("variation_name") : obj.optString("variation_id");
                                TLLog.debug("Client show experiment: " + experimentName + ". Variation: " + variationName);

                                if (TLManager.getInstance().isLiveUpdate()) {
                                    TLManager.getInstance().setExperimentLoadTimeout(false);
                                }
                                TLUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            TLManager.getInstance().switchVariation(obj.optString("experiment_id"), obj.optString("variation_id"), experimentName, variationName);

                                            TLViewManager.getInstance().setTestingExperiment(true);
                                            TLViewManager.getInstance().hideTapMode();
                                            ViewGroup vg = TLViewManager.getInstance().getCurrentViewGroup();
                                            if (vg != null) {
                                                View border = vg.findViewById(TLHighlightShape.getOverlayId());
                                                if (border != null) {
                                                    vg.removeView(border);
                                                }
                                            }
                                        } catch (Exception e) {
                                            TLLog.error("ClientShowExperiment inner error", e);
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                TLLog.error("ClientShowExperiment error", e);
                            }
                        }
                    }).on("clientHideExperiment", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                TLLog.debug("Client hide experiment");
                                TLUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TLViewManager.getInstance().removeBorder();
                                        TLManager.getInstance().getPropertiesFromServer(null, null);
                                        TLViewManager.getInstance().setTestingExperiment(false);
                                    }
                                });
                            } catch (Exception e) {
                                TLLog.error("clientHideExperiment error", e);
                            }
                        }
                    }).on("clientPickElement", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                if (args[0] == null)
                                    return;
                                final JSONObject obj = (JSONObject) args[0];
                                TLLog.debug("clientPickElement Socket: " + obj.toString());
                                TLUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TLViewManager.getInstance().enterTapMode(obj);
                                    }
                                });
                            } catch (Exception e) {
                                TLLog.error("clientPickElement error", e);
                            }
                        }

                    }).on("highlightTapElement", new Emitter.Listener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void call(final Object... args) {
                            TLLog.debug("highlightTapElement Socket");
                            //use a GradientDrawable with only one color set, to make it a solid color

                            try {
                                final JSONObject tapElement = ((JSONObject) args[0]).optJSONObject("tapElement");
                                final View v = ViewUtils.findViewFromJSON(tapElement.has("anID") ? tapElement : tapElement.optJSONObject("initProperties"));
                                final boolean clear = ((JSONObject) args[0]).optBoolean("clear");
                                TLViewManager.getInstance().getCurrentViewGroup().post(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            final ViewGroup vg = TLViewManager.getInstance().getCurrentViewGroup();
                                            if (vg == null) return;

                                            if (clear) {
                                                View border = vg.findViewById(TLHighlightShape.getOverlayId());
                                                if (border != null) {
                                                    border.clearAnimation();
                                                    vg.removeView(border);
                                                }
                                            } else {
                                                if (v != null) {
                                                    ViewUtils.outlineView(v, tapElement.has("anID"));
                                                } else {
                                                    View border = vg.findViewById(TLHighlightShape.getOverlayId());
                                                    if (border != null) {
                                                        border.clearAnimation();
                                                        vg.removeView(border);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            TLLog.error("Error clearing border", e);
                                        }
                                    }
                                });

                            } catch (Exception e) {
                                ///something
                            }
                        }
                    }).on("experimentUpdated", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                if (args[0] == null)
                                    return;
                                final JSONObject obj = (JSONObject) args[0];

                                final JSONObject experiment = obj.optJSONObject("experiment");
                                final JSONObject variation = obj.optJSONObject("variation");
                                if (experiment == null || variation == null)
                                    return;

                                TLLog.debug("Client updated experiment: " + experiment.optString("name"));
                                TLUtils.runOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TLManager.getInstance().experimentUpdated(obj.optString("experiment_id"), experiment.optString("name"), variation.optString("name"));
                                    }
                                });
                            } catch (Exception e) {
                                TLLog.error("experimentUpdated", e);
                            }
                        }
                    }).on("dataUpdated", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("dataUpdated Socket");
                            final JSONObject data = (JSONObject) args[0];

                            TLUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    TLManager.getInstance().updateDataFromSocket(data);
                                }
                            });
                        }
                    }).on("chooseView", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("chooseView Socket");
                            final JSONObject data = (JSONObject) args[0];
                            TLUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    TLViewManager.getInstance().enterActivityMode(data);
                                }
                            });
                        }
                    }).on("foundView", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("foundView Socket");
                            TLUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    TLViewManager.getInstance().getFoundActivity();
                                }
                            });
                        }
                    }).on("pairTokenSuccessful", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("pairTokenSuccessful Socket");
                            TLUtils.runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    TLManager.getInstance().pairTokenSuccessful();
                                }
                            });
                        }
                    }).on("pairTokenFailed", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("pairTokenFailed Socket");
                        }
                    }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            connecting = false;
                            TLLog.debug("SocketIO Disconnected!");
                            isConnected = false;
                            isConnectedToSocketRoom = false;

                            socketRoom = null;
                            if (listener != null) listener.onResponse(false);
                            if (pairingAttempts == 0 && TLManager.getInstance().isActivityActive() && ViewUtils.isBorderShowing(TLViewManager.getInstance().getCurrentViewGroup())) {
                                TLViewManager.getInstance().applyDisconnectBorder();
                            }
                            if (TLManager.getInstance().isActivityActive() && TLManager.getInstance().isLiveUpdate())
                                retrySocketConnection();
                        }
                    }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            connecting = false;
                            TLLog.error("SocketIO Error", (Exception) args[0]);
                        }
                    }).on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            if (args != null) {
                                TLLog.debug("socketio reconnect: " + args[0]);
                                TLViewManager.getInstance().updateDisconnectBorder((Integer) args[0]);
                            }
                        }
                    }).on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            TLLog.debug("socketio reconnect failed");
                        }
                    }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            connecting = false;
                            if (pairingAttempts == 0 && TLManager.getInstance().isActivityActive() && ViewUtils.isBorderShowing(TLViewManager.getInstance().getCurrentViewGroup())) {
                                TLViewManager.getInstance().applyDisconnectBorder();
                            }
                            TLLog.debug("socket connect error");

                        }
                    }).on("subscribeFinished", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            socketRoomPromise = socketRoomPromise.isFinished() ? new Promise<>() : socketRoomPromise;
                            socketRoomPromise.finish();
                            connectionAttempts = 0;
                            connecting = false;
                            TLLog.debug("socket subscribe worked");
                        }
                    });
                    TLLog.debug("Connect SocketIO with path: " + url);
                    socketIO.connect();
                } else {
                    if (listener != null) listener.onResponse(true);
                }
            } catch (Throwable e) {
                //Put this catch in simply because some guy had a problem deep in the java websockets library.
                TLLog.error("socketio problem", e);
                connecting = false;
            }
        }
    }

    private void retrySocketConnection() {
        if (TLManager.getInstance().thereAreSockets) {
            TLLog.debug("Retrying Connecting SocketIO");
            disconnectSocketIO();
            socketIO = null;

            ++connectionAttempts;
            if (connectionAttempts <= MAX_SOCKET_RECONNECTS) {
                final long delayInMillis = 500 * connectionAttempts; // 500ms per connection attempt
                TLLog.debug("Socket reconnect delay: " + delayInMillis);

                TLThreadManager.getInstance().scheduleOnBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        connectSocketIO(null, false);
                    }
                }, delayInMillis, TimeUnit.MILLISECONDS);
            } else {
                TLLog.warning("Reached max socket reconnects: " + connectionAttempts);
            }
        }
    }

    public void disconnectSocketIO() {
        if (socketIO != null) {
            socketIO.disconnect();
            isConnected = false;
            isConnectedToSocketRoom = false;
            socketIO = null;
        }
    }

    private void connectToSocketRoom() {
        try {
            if (TLManager.getInstance().thereAreSockets) {

                TLManager.getInstance().getTlPropertiesPromise().add(new PromiseListener() {

                    private void connectInThread() {
                        try {
                            Map<String, Object> props = TLManager.getInstance().getTLDeviceInfo().getDeviceProperties();

                            if (socketIO != null && props != null && props.containsKey("pid")) {
                                String room = "project_" + props.get("pid");
                                props.put("lv", true);
                                if (socketRoom == null || !socketRoom.equals(room)) {
                                    props.put("room", room);
                                    socketIO.emit("subscribe", new JSONObject(props));
                                    socketRoom = room;
                                    isConnectedToSocketRoom = true;
                                    TLLog.debug("Connected to Socket Room: " + socketRoom);
                                }
                            }
                        } catch (Throwable t) {
                            TLLog.error("WebSocket room connect issue", t);
                        }
                    }

                    private void innerConnect() {
                        try {
                            socketRoomPromise = socketRoomPromise.isFinished() ? new Promise<>() : socketRoomPromise;
                            // Get a handler that can be used to post to the main thread
                            if (socketConnectOnMainThread) {

                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        connectInThread();
                                    }
                                };
                                mainHandler.postDelayed(myRunnable, 3000);
                            } else {
                                connectInThread();
                            }

                        } catch (Throwable e) {
                            //Honestly just catching because java WebSockets suck on high latency.
                            TLLog.error("WebSocket room connect issue", e);
                        }
                    }

                    @Override
                    public void succeeded() {
                        super.succeeded();
                        innerConnect();
                    }

                    @Override
                    public void failedOrCancelled(Exception e) {
                        super.failedOrCancelled();
                        if (e != null && e.getMessage() != null && e.getMessage().equals("no properties on disk")) {
                            innerConnect();
                        }
                    }
                });
            }
        } catch (Exception e) {
            //Safety
        }
    }


    public void emitSocketEvent(String event, Object jsonObjOrArry) {
        if (TLManager.getInstance().thereAreSockets && socketIO != null && isConnected && isConnectedToSocketRoom) {
            TLLog.debug("Emit Socket Event: " + event + ", data: " + jsonObjOrArry.toString());
            if (jsonObjOrArry != (JSONObject.NULL)) {
                socketIO.emit(event, jsonObjOrArry);
            } else {
                socketIO.emit(event);
            }
        } else {
            disconnectSocketIO();
        }
    }

    private AlertDialog testDeviceAlert = null;

    private void showTestDeviceAlert(final JSONObject msg) {
        try {
            TLManager tlManager = TLManager.getInstance();
            if (tlManager.getTrackingEnabled() && tlManager.getAppContext() != null && msg != null && testDeviceAlert == null
                    && TLManager.getInstance().isActivityActive()) {
                Activity currentActivity = TLManager.getInstance().getCurrentActivity();
                if (currentActivity != null && TLManager.getInstance().isActivityActive()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
                    builder.setTitle("Connect Taplytics Test Device");
                    String userName = null;
                    try {
                        userName = msg.getString("user_name");
                    } catch (JSONException ex) {
                        TLLog.error("Getting Test Device username", ex);
                    }
                    if (userName != null) {
                        builder.setMessage("To User: " + userName);
                    }
                    builder.setCancelable(true);
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            TLManager.getInstance().setExperimentLoadTimeout(false);

                            TLProperties properties = TLManager.getInstance().getTlProperties();
                            if (properties == null) {
                                Promise newPropsPromise = new Promise();
                                TLManager.getInstance().getPropertiesFromServer(null, newPropsPromise);
                                newPropsPromise.add(new PromiseListener() {
                                    @Override
                                    public void succeeded() {
                                        super.succeeded();
                                        sendConnectTestDeviceSocket(msg);
                                    }

                                    @Override
                                    public void failedOrCancelled() {
                                        super.failedOrCancelled();
                                        sendConnectTestDeviceSocket(msg);
                                    }
                                });
                            } else {
                                sendConnectTestDeviceSocket(msg);
                            }
                            dialog.cancel();
                            destroyTestDeviceAlert();
                        }
                    });
                    builder.setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            destroyTestDeviceAlert();
                        }
                    });

                    testDeviceAlert = builder.create();
                    testDeviceAlert.show();
                    testDeviceAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#2196F3"));
                    testDeviceAlert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2196F3"));

                }
            }
        } catch (Exception e) {
            TLLog.error(getClass().toString(), e);
        }
    }

    private void destroyTestDeviceAlert() {
        this.testDeviceAlert = null;
    }

    private void sendConnectTestDeviceSocket(JSONObject msg) {
        if (TLManager.getInstance().thereAreSockets) {

            TLProperties properties = TLManager.getInstance().getTlProperties();
            if (properties != null && properties.getSessionID() != null && msg != null) {
                TLLog.debug("Send Connect Test Device Socket!");
                JSONObject obj;
                try {
                    obj = new JSONObject(msg.toString());
                    obj.put("session_id", properties.getSessionID());
                } catch (JSONException e) {
                    TLLog.error("sendConnectTestDeviceSocket creating JSON Object", e);
                    obj = null;
                }

                if (obj != null) {
                    socketIO.emit("foundTestDevice", obj);
                    TLLog.debug("Found test device: " + obj.toString());
                }
            }
        }
    }

    private void hideTestDeviceAlert(JSONObject msg) {
        if (msg != null && testDeviceAlert != null) {
            TLProperties properties = TLManager.getInstance().getTlProperties();
            String project_id = null;
            String propProject_id = null;
            try {
                project_id = msg.getString("project_id");
                if (properties != null && properties.getProject() != null)
                    propProject_id = properties.getProject().getString("_id");
            } catch (JSONException ex) {
                TLLog.error("hideTestDeviceAlert get project_id", ex);
            }

            if (project_id != null && propProject_id != null && project_id.equals(propProject_id)) {
                testDeviceAlert.dismiss();
                this.testDeviceAlert = null;
            }
        }
    }

    private int count = 0;

    public void pairDeviceFromLink(final String link) {
        if (TLManager.getInstance().thereAreSockets) {

            if (pairedFromLink) {
                return;
            }
            if (link == null) {
                TLLog.debug("Missing link to pair device");
                return;
            }
            if (link.startsWith("tl-")) {
                String sub = link.substring(3); // 3 is "tl-".length
                String deviceToken = sub.substring(sub.indexOf("://") + 3, sub.length());
                TLManager.getInstance().setDeviceToken(deviceToken);
                final TLProperties props = TLManager.getInstance().getTlProperties();
                try {
                    if (!isConnected) {
                        this.connectSocketIO(new TLConnectSocketListener() {
                            @Override
                            public void onResponse(Boolean connected) {
                                if (connected) {
                                    try {
                                        if (pairDeviceMap == null)
                                            pairDeviceMap = new JSONObject();
                                        pairDeviceMap.put("wasConnected", false);
                                        mainHandler
                                                .postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        pairDeviceFromLink(link);
                                                    }
                                                }, 10);
                                    } catch (Exception e) {
                                        TLLog.error("Connecting pairing device socket", e);
                                    }
                                }
                            }
                        }, true);
                    } else if (TLManager.getInstance().getApiKey() != null && props != null
                            && props.getProject() != null && props.getSessionID() != null) {
                        try {
                            if (pairDeviceMap == null)
                                pairDeviceMap = new JSONObject();
                            pairDeviceMap.put("projectToken", TLManager.getInstance().getApiKey());
                            pairDeviceMap.put("projectConnectToken", sub.substring(0, sub.indexOf("://")));
                            pairDeviceMap.put("deviceToken", deviceToken);
                            pairDeviceMap.put("session_id", props.getSessionID());
                            pairedFromLink = true;
                            socketRoomPromise.add(new PromiseListener() {
                                @Override
                                public void succeeded() {
                                    emitSocketEvent("pairDeviceWithToken", pairDeviceMap);
                                    super.succeeded();
                                }
                            });
                        } catch (Exception e) {
                            TLLog.error("Sending pairing device socket", e);
                        }
                        //Attempt to reconnect 15 times over 15 seconds
                    } else if (count <= 15) {
                        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: Figure out how this all actually works and do something better than a count
                                count++;
                                pairDeviceFromLink(link);
                            }
                        }, 1000);
                        //Do not repeat if not in liveUpdate mode. Make them try to pair again.
                    } else if (pairedFromLink || TLManager.getInstance().isLiveUpdate()) {
                        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                count = 0;
                            }
                        }, 1000);
                    }
                } catch (Exception e) {
                    //Safety net
                }
            }
        }
    }

    public void sendHasAppLinking() {
        TLProperties props = TLManager.getInstance().getTlProperties();
        if (!TLManager.getInstance().hasSentAppLink() && props != null && props.getProject() != null
                && isConnected) {
            TLManager.getInstance().getTlAnalytics().trackTLEvent(TLAnalyticsManager.TLAnalyticsEventHasAppLinking);
            try {
                JSONObject obj = new JSONObject();
                if (props.getProject() != null && props.getProject().has("_id"))
                    obj.put("project_id", props.getProject().opt("_id"));
                if (props.getAppUser() != null && props.getAppUser().has("_id"))
                    obj.put("appUser_id", props.getAppUser().opt("_id"));
                if (props.getSessionID() != null)
                    obj.put("session_id", props.getSessionID());
                emitSocketEvent("hasAppLinking", obj);
                TLManager.getInstance().setSentAppLink(true);
            } catch (Exception ex) {
                TLLog.error("Sending hasAppLinking Socket", ex);
            }
        }
    }
}
