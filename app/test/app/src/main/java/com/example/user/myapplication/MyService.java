package com.example.user.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.simple.JSONObject;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

/**
 * Created by User on 20.01.2018.
 */


public class MyService extends Service {

    private int notificationTime;
    private String pegId;
    private NotificationManager mNotificationManager;
    private boolean hasNotified = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        // START YOUR TASKS

        getPrefs();
        Log.d("service", "started");

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //updateNotificationTime();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String id = "01";
            CharSequence name = "SMAUNDRY";
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
        }


        timer.scheduleAtFixedRate(tt, 300, 60000);


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        // STOP YOUR TASKS
        timer.cancel();
        timer.purge();
        Log.d("service", "destroyed");

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    private Timer timer = new Timer();
    TimerTask tt = new TimerTask() {
        @Override
        public void run() {
            API api = new API();
            api.execute("prediction", pegId);
            JSONObject pred = null;
            try {
                pred = api.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            int minLeft = ((Double) pred.get("prediction")).intValue()/60;

            ///int minLeft = api.getLeftTime();

            if((minLeft <= notificationTime)){
                if(!hasNotified) {
                    hasNotified = true;
                    spnotify(minLeft);
                }
            } else {
                hasNotified = false;
            }
        }
    };



    private void spnotify(int time){
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
    }


    private void getPrefs() {
        Context c = this.getApplicationContext();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);

        pegId = sharedPref.getString("peg_id", "1");

        Map<String, ?> allPrefs = sharedPref.getAll();
        try{
            notificationTime = Integer.parseInt((String) allPrefs.get("pref_notif"));
        } catch (Exception e){
            notificationTime = 0;
        }
    }

}
