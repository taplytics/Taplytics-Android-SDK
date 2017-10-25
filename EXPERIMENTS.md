Creating experiments is easy using Taplytics. You can either use our visual editor or create code-based experiments. You can find documentation on how to do this below.

| Table of Contents |
| ----------------- |
| [Dynamic Variables & Code Blocks](#dynamic-variables--code-blocks)|
| [Code Experiments](#code-experiments-deprecated) |
| [Testing Specific Experiments](#testing-specific-experiments) |
| [Visual Editing](#visual-editing) |
| [First-view Experiments](#delay-load) |
| [List Running Experiments](#running-experiments) |

## Dynamic Variables & Code Blocks

**To see and modify these variables or blocks on the dashboard, the app must be launched and this code containing the variable or block must be navigated to at least once.**

The code below is used to send the information of the variable or block to Taplytics, so it will appear on the dashboard.

### Dynamic Variables

Taplytics variables are values in your app that are controlled by experiments. Changing the values can update the content or functionality of your app. Variables are reusable between experiments and operate in one of two modes: synchronous or asynchronous.

#### Synchronous

Synchronous variables are guaranteed to have the same value for the entire session and will have that value immediately after construction.

Due to the synchronous nature of the variable, if it is used before the experiments have been loaded, its value will be the default value rather than the value set for that experiment. This could taint the results of the experiment. In order to prevent this you can ensure that the experiments are loaded before using the variable. This can be done with either the `delayLoad` functionality, the `TaplyticsExperimentsLoadedListener` parameter in your `startTaplytics` call, or the `getRunningExperimentsAndVariations` call.

Synchronous variables take two parameters in its constructor:

1. Variable name (String)
2. Default Value

The type of the variable is defined in the first diamond brackets, and can be a `String`, `Number`, `Boolean` or `JSON`.

For example, using a variable of type `String`:
```java
TaplyticsVar<String> stringVar = new TaplyticsVar<String>("some name", "default value");
```

Then when you wish to get the value for the variable, simply call `get()` on the Taplytics variable:
```java
String value = stringVar.get();
```

#### Asynchronous

Asynchronous variables take care of insuring that the experiments have been loaded before returning a value. This removes any danger of tainting the results of your experiment with bad data. What comes with the insurance of using the correct value is the possibility that the value will not be set immediately. If the variable is constructed *before* the experiments are loaded, you won't have the correct value until the experiments have finished loading. If the experiments fail to load, then you will be given the default value, as specified in the variables constructor. For the best results with asynchronous varaibles make sure that they are constructed after the StartTaplytics call in your app's lifecycle.

Asynchronous variables take three parameters in its constructor:

1. Variable name (String)
2. Default Value
3. TaplyticsVarListener

Just as for synchronous variables the type of the variable is defined in the first diamond brackets, and can be a `String`, `Number`, `Boolean` or `JSON`.

For example, using a variable of type `Number`:

```java
TaplyticsVar<Number> var = new TaplyticsVar<>("name", 5, new TaplyticsVarListener() {
    @Override
    public void variableUpdated(Object value) {
        // Do something with the value
    }
});
```

When the variable's value has been updated, the listener will be called with that updated value. You can specify what you want to do with the variable inside the `variableUpdated` method.

**Note: Default values for dynamic variables cannot be NULL. NULL values may cause default to trigger in all scenarios**

----------

#### Testing Dynamic Variables

When testing dynamic variables in live update mode you can change the values on the fly via the Taplytics interface and you can switch variations with the shake menu on the device.

**Important Note:** When testing synchronous dynamic variables you *must* call the constructor again to see the new value, as there are no callbacks which occur when the variable is updated with a new value.

This can be achieved by using a experiments updated listener. Here is an example for updating a TextView:


``` java

Taplytics.setTaplyticsExperimentsUpdatedListener(new TaplyticsExperimentsUpdatedListener() {
    @Override
    public void onExperimentUpdate() {
        final TaplyticsVar<String> stringVar = new TaplyticsVar<String>("stringVar", "defaultValue");
        updateText(stringVar.get());
    }
});
```
### Code Blocks

Similar to Dynamic Variables, Taplytics has an option for 'Code Blocks'. Code blocks are linked to Experiments through the Taplytics website very much the same way that Dynamic Variables are, and will be executed based on the configuration of the experiment through the Taplytics website. A Code Block is a callback that can be enabled or disabled depending on the variation. If enabled, the code within the callback will be executed. If disabled, the variation will not get the callback.

A Code Block can be used alongside as many other Code Blocks as you would like to determine a combination that yields the best results. Perhaps there are three different Code Blocks on one activity. This means there could be 8 different combinations of Code Blocks being enabled / disabled on that activity if you'd like.

For example:

```java
 Taplytics.runCodeBlock("name", new CodeBlockListener() {
    @Override
    public void run() {
        // Put your code here!
    }
});
```

By default, a code block will _not_ run unless enabled on the Taplytics Dashboard. It must be enabled for a Variation before it will run.

## Testing Specific Experiments

To test/QA specific experiment and variation combinations, add a map to the Taplytics start options containing the experiments and variations you wish to test. The keys should be the experiment names, and values of variation names (or baseline).

For example:


```
	HashMap<String, Object> startOptions = new HashMap<>();
	HashMap testExperiments = new HashMap();
	testExperiments.put("Big Experiment", "Variation 2");
	startOptions.put("testExperiments", testExperiments);

	Taplytics.startTaplytics(this, APIKEY, startOptions);
```
## Visual Editing

**NOTE: Not currently supported for Android TV / Fire TV**

You don't have to do anything else! You can use the Taplytics dashboard to make all your visual changes. See the docs on visual editing [here](https://taplytics.com/docs/guides/visual-experiments).

#### Dialogs

Taplytics supports editing elements on **dialogFragments** (not dialogs). To do this properly, you must use a fragmentTransaction to add the fragment to the backstack. The tag used here should be the same as the tag used to show the fragment, like so:

```java
FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
fragmentTransaction.show(someDialog);
fragmentTransaction.addToBackStack("fragment_some_dialog");
someDialog.show(fragmentTransaction, "fragment_some_dialog");
```

Taplytics tracks the appearance/disappearance of the dialog via the backstack manager, which is why it needs to be sent there. The tag is necessary to confirm that the visual edits are being applied to the correct fragment.

This only works with dialogFragments as normal Dialogs do not have any unique identifying tags.

**NOTE:**

dialogFragments exist on an entirely different view hierarchy than traditional view elements. They exist within their own `window` and have an entirely different viewRoot than the rest of your application. This makes changes on dialogs very difficult, and this feature is not 100% guaranteed to work for all dialogs.

---

## Delay Load

Taplytics has the option to delay the loading of your main activity while Taplytics gets initial ***view*** changes ready. Keep in mind that this initial load will only take some time the very first time, after that, these changes will be saved to disk and will likely not need a delay.

There are two methods to do this, **use both at the start of your ```java onCreate()``` after ```java setContentView()```**:

#### Delay Load With Image
In this instance, Taplytics takes care of the loading for you. Taplytics creates a splash screen with the provided image. The image will fade automatically after the given time, or when Taplytics has successfully loaded visual changes on the provided activity.

Method: ```Taplytics.delayLoad(Activity activity, Drawable image, int maxTime) ```

and ```Taplytics.delayLoad(Activity activity, Drawable image, int maxTime, int minTime) ```


**Activity**: the activity (typically main activity) that will be covered in a splash image.

**Image**: A Drawable image that will be the splash screen.

**maxTime**:  Regardless of the results of Taplytics, the image will fade after this time. Milliseconds.

**minTime**: Sometimes Taplytics loads things really fast, and this might make the image show only for a short amount of time. To keep this from happening, there is an optional minimum time option. Regardless of Taplytics loading experiments, the ```delayLoad``` won't finish until after this minimum time. Milliseconds.

**Examples**:

```java
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        Taplytics.delayLoad(this, getResources().getDrawable(R.drawable.image5), 2000);
        ...
```

**With a 1 second minimum time**

```java
Taplytics.delayLoad(this, getResources().getDrawable(R.drawable.image5), 2000, 1000);
...
```

#### Delay Load with Callbacks
In this instance, Taplytics provides callbacks when the delay load should begin, and when the delay load ends. The callback will also return after the provided timeout time has been reached. This provides you the ability to show a splashscreen that is more than just a simple image.

Method: ```Taplytics.delayLoad(int maxTime, TaplyticsDelayLoadListener listener) ```

and ```Taplytics.delayLoad(int maxTime, int minTime, TaplyticsDelayLoadListener listener) ```



**maxTime**: Regardless of the results of Taplytics, this callback will be triggered if this time is reached.

**minTime**: Sometimes Taplytics loads things really fast, and this might make the behavior of the callback undesirable. To keep this from happening, there is an optional minimum time option. Regardless of Taplytics loading experiments, the ```delayLoad``` won't finish until after this minimum time.

**Listener**: This listener will provide the necessary callbacks.

**Examples**:

```java

@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        Taplytics.delayLoad(2000, new TaplyticsDelayLoadListener() {
                @Override
                public void startDelay() {
                        //Start delaying!
                }

                @Override
                public void delayComplete() {
                        //Loading completed, or the given time has been reached. Insert your code here.
                }
        });
        ...

```

**With a 1 second minimum time:**

```java
        Taplytics.delayLoad(2000, 1000, ...

```

---

## Running Experiments

If you would like to see which variations and experiments are running on a given device, there exists a `getRunningExperimentsAndVariations(TaplyticsRunningExperimentsListener listener)` function which provides a callback with a map of the current experiments and their running variation. An example:

```java
Taplytics.getRunningExperimentsAndVariations(new TaplyticsRunningExperimentsListener() {
	@Override
	public void runningExperimentsAndVariation(Map<String, String> experimentsAndVariations) {
		// TODO: Do something with the map.
	}
});
```

NOTE: This function runs asynchronously, as it waits for the updated properties to load from Taplytics' servers before returning the running experiments.

If you want to see when the experiments have been loaded by Taplytics, you can add a `TaplyticsExperimentLoadedListener` to your `startTaplytics` call. For example

```  
Taplytics.startTaplytics(this, "YOUR API KEY", null, new TaplyticsExperimentsLoadedListener() {
	@Override
	public void loaded() {
		//TODO: Do something now that experiments are loaded
	}
});
```

## Sessions

By default, Taplytics defines a session as when a user is using the app with less than 10 minutes of inactivity. If the app has been backgrounded for 10 minutes, the next time the user opens the app it will be considered a new session. Similarly, if the app is entirely force closed, the next time the app is opened, it will be considered a new session.

### StartNewSession

To manually force a new user session (ex: A user has logged in / out), there exists ```Taplytics.startNewSession```

If there is an internet connection, a new session will be created, and new experiments/variations will be fetched from Taplytics if they exist.

It can be used as follows:

```java
Taplytics.startNewSession(new TaplyticsNewSessionListener() {
            @Override
            public void onNewSession() {
                // New session here! Only returns if successful.
            }
        });
```

### Session Listener

To keep track of when Taplytics defines a new session, use a `TaplyticsNewSessionListener` as follows.

```  
Taplytics.setTaplyticsNewSessionListener(new TaplyticsNewSessionListener() {
     @Override
      public void onNewSession() {
		//We are in a new session
      }
 });
```

**Note that this is NOT called the first time Taplytics loads, only on subsequent sessions**
