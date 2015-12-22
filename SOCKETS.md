###Sockets

####Usage
Sockets are used in the Taplytics SDK to establish a two way connection to Taplytics' servers. This connection is used for editing experiments, both for visual experiments and dynamic variables. However it is only used in the development versions of the app, or devices which have been explicitly paired with Taplytics using a pairing email or text message. 

####Inclusion in release builds - as of 1.7.0

Due to experiment editing for the most part being done on internal dev builds sockets are not necessary on release versions of the app. In order to account for this Taplytics does not require the socket dependency for release builds. So instead of using the standard `compile` directive when adding the socket dependency the `debugCompile` directive can be used instead.

**Please note that this will not allow pairing of live versions of the app using text message or email pairing.**

```
debugCompile ('io.socket:socket.io-client:+') {
        // excluding org.json which is provided by Android
        exclude group: 'org.json', module: 'json'
}
```

####OkHttp Build Errors

Currently, this socket lib is on OkHttp-ws 2.5.0, but as you may know, the most recent version is 2.7.0

If you are seeing an error, you can simply exclude okhttp from the dependency. 

Keep track of the progress of this here: https://github.com/socketio/engine.io-client-java/issues/41


####I'm getting a popup saying update my sockets!

There was a recent change (1.7.0) in the package name of the socket library used by Taplytics. Given that the library has continued to improve over time Taplytics makes use of this new library. Unfortunately this means that the dependency that was being used before is no longer valid.

There is a quick change that will fix this issue, simple change:
```
 compile("com.github.nkzawa:socket.io-client:+") {
            exclude group: 'org.json'
}
compile("com.github.nkzawa:engine.io-client:+") {
            exclude group: 'org.json'
}
```

to:

```
compile('io.socket:socket.io-client:+') {
        // excluding org.json which is provided by Android
        exclude group: 'org.json', module: 'json'
}
```

as mentioned above you can use `debugCompile` instead of `compile` if you don't want to include `socket-io` in the release version of your app.
