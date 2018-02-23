package davtomic.hr.rigstatusapp;

import android.app.Application;
import android.content.Intent;

/**
 * Created by dtomic on 23/02/2018.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        sendBroadcast(new Intent(this, BootBroadcastReceiver.class));
    }
}
