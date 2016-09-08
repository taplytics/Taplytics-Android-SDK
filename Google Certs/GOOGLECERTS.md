# Google Push Certificates

#### Upload your Google Push certificates so you can send push notifications using Taplytics.

Before you can get started using Taplytics push notifications, we need to upload your Google Push Notification credentials.

1. Getting your Google Credentials
2. Upload your credentials to Taplytics

### Getting your Google Credentials

First, head over to the [Firebase Console](https://console.firebase.google.com). If your project is already in firebase, simply enter that project. Otherwise, click `CREATE NEW PROJECT`. Then navigate to your project's settings:

![image](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/Google%20Certs/settings.png?raw=true)

From here, switch to the Cloud Messaging tab. You will see two keys. The first, your server key, and the second, your Sender ID.

![image](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/Google%20Certs/cloudmessaging.png?raw=true)

Keep this browser tab open and open up a Taplytics tab.

### Uploading your Google Credentials

Now, on your Taplytics project, navigate to your project Settings tab. On the left, you wll see a smaller tab for Push Notification Settings. Click that, then go down to the 'Google Cloud Messaging' section. In the input fields for 'Sender ID' and 'GCM API Key', enter the keys in the appropriate fields. Finally, click 'Save Credentials' and you're on your way to sending Push Notifications to Android!

![image](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/Google%20Certs/upload.png?raw=true)
