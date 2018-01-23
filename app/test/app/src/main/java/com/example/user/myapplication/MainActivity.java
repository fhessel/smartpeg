package com.example.user.myapplication;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private TextView lefttime;
    private ProgressBar progress;
    private TextView temperature;
    private TextView humidity;
    private SharedPreferences sharedPref;

    int notificationTime;
    //private NotificationManager mNotificationManager;
    private API api;

    private Double maxConductance = 0.;

    private final Runnable update = new Runnable() {
        public void run() {

            String pegId = sharedPref.getString("peg_id", "1");

            try {
                api = new API();
                api.execute(pegId);
                JSONObject values = api.get();
                int t = ((Double) values.get("temperature")).intValue();
                int h = ((Double) values.get("humidity")).intValue();
                String d = (String) values.get("timestamp");
                Double c = (double) values.get("conductance");
                api.cancel(true);
                api = new API();
                api.execute("prediction", pegId);
                values = api.get();
                int leftMin = ((Double) values.get("prediction")).intValue()/60;

                int p;
                if(c==0) { p = 1;
                }else if(maxConductance <= c) {
                    maxConductance = c;
                    p = 0;
                } else{ p = 1-maxConductance.intValue()/c.intValue();
                }
                updateScreen(p, leftMin, h, t);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void updateScreen(int progress, int minLeft, int hum, int tem) {
        lefttime.setText(String.valueOf(minLeft));
        temperature.setText(String.valueOf(tem)+"°C");
        humidity.setText(String.valueOf(hum)+"%");
        this.progress.setProgress(progress);
    }

    private Timer timer = new Timer();
    TimerTask tt = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(update);

            /*int minLeft = api.getLeftTime();

            if(minLeft <= notificationTime){
                spnotify(minLeft);
            }*/
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPref.edit().clear().commit();

        api = new API();

        lefttime = findViewById(R.id.textView7);
        temperature  = findViewById(R.id.textView2);
        humidity = findViewById(R.id.textView3);
        progress = findViewById(R.id.progressBar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String id = "01";
            CharSequence name = "SmartPeg";
            NotificationChannel mChannel = null;
            mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            mChannel.setDescription("Notification Channel");
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(mChannel);
        }*/


        Intent serviceIntent = new Intent(this, MyService.class);
        if (isMyServiceRunning())
            this.stopService(serviceIntent);
        this.startService(serviceIntent);
        Log.d("main", "should have started service");
        //}
        timer.scheduleAtFixedRate(tt, 300, 60000);
    }



    private void updateNotificationTime() {
        Map<String, ?> allPrefs = sharedPref.getAll();
        try{
            notificationTime = Integer.parseInt((String) allPrefs.get("pref_notif"));
        } catch (Exception e){
            notificationTime = 0;
        }
    }



    /*private void spnotify(int time){
        String text;
        if(time == 0){
            text = "Your laundry is dry now";
        } else {
            text = "Your laundry will be dry in " + time + " minutes";
        }

        NotificationCompat.Builder mBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder = new NotificationCompat.Builder(this, "01");
        } else {
            mBuilder = new NotificationCompat.Builder(this);
        }
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("SmartPeg");
        mBuilder.setContentText(text);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);

        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel(). Chose arbitrary number
        mNotificationManager.notify(15, mBuilder.build());
    }*/




    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

/*    private void stopMyService() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyService.class.getName().equals(service.service.getClassName())) {

            }
        }
    }*/



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        item.expandActionView();

        Intent intent = new Intent(this, id == R.id.action_settings ? Settings.class: Help.class);
        startActivity(intent);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
