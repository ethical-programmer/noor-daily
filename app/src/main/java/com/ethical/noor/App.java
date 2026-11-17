package com.ethical.noor;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.WindowManager;

/** Locks the app again whenever it goes to the background. */
public class App extends Application implements Application.ActivityLifecycleCallbacks {
    public static boolean unlocked = false;
    int started = 0;

    @Override public void onCreate() { super.onCreate(); registerActivityLifecycleCallbacks(this); Notifs.channels(this); Alarms.ensure(this); }

    @Override public void onActivityCreated(Activity a, Bundle b) {
        if (new Data(a).getBool("secure", true)) a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    @Override public void onActivityStarted(Activity a) { started++; }
    @Override public void onActivityStopped(Activity a) { started--; if (started <= 0 && !a.isChangingConfigurations()) unlocked = false; }
    @Override public void onActivityResumed(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
