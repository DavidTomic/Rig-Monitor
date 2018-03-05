package davtomic.hr.rigstatusapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dtomic on 23/02/2018.
 */

public class CheckService extends Service {

    public static int SERVICE_ID = 1111;
    private static final String TAG = "CheckService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        HttpGetRequest task = new HttpGetRequest(this);
        task.execute();
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showNotification(String msg) {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                i, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Rig status")
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentText(msg);
        mBuilder.setContentIntent(contentIntent);

        int NOTIFICATION_ID = 1;

        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }


    static class HttpGetRequest extends AsyncTask<Void, Void, String> {
        private static final String TAG = "HttpGetRequest";

        private static final String REQUEST_METHOD = "GET";
        private static final int READ_TIMEOUT = 15000;
        private static final int CONNECTION_TIMEOUT = 15000;

        private WeakReference<CheckService> checkServiceReference;

        HttpGetRequest(CheckService service) {
            this.checkServiceReference = new WeakReference<>(service);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = null;
            String inputLine;
            try {
                URL myUrl = new URL("http://herceg.ethosdistro.com/?json=yes");

                HttpURLConnection connection =(HttpURLConnection)
                        myUrl.openConnection();
                connection.setRequestMethod(REQUEST_METHOD);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.connect();

                //Create a new InputStreamReader
                InputStreamReader streamReader = new
                        InputStreamReader(connection.getInputStream());
                //Create a new buffered reader and String Builder
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                //Check if the line we are reading is not null
                while((inputLine = reader.readLine()) != null){
                    stringBuilder.append(inputLine);
                }
                //Close our InputStream and Buffered reader
                reader.close();
                streamReader.close();
                //Set our result equal to our stringBuilder
                result = stringBuilder.toString();

                Log.i(TAG, "response " + result);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                result = null;
            }


            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                JSONObject jsonObject = new JSONObject(s);
                int aliveGpus = jsonObject.getInt("alive_gpus");
                Log.i(TAG, "aliveGpus " + aliveGpus);
                if (aliveGpus != 18) {
                    Log.i(TAG, "SEND");
                    String msg = "herceg rig: broj aktivnih kartica je " + aliveGpus;
                    CheckService checkService = checkServiceReference.get();
                    checkService.showNotification(msg);
                    checkService.sendSMS("+385994794931", msg);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}


