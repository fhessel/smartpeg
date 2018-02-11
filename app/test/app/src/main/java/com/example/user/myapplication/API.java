package com.example.user.myapplication;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.*;
import org.json.simple.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class API extends AsyncTask<String, String, JSONObject> {

    int temperature;
    int humidity;
    int leftTime;
    int progress;



    public static JSONObject getMeasurementsHttp(String id) {
        URL url = null;
        try {
            url = new URL("http://smartpeg.fhessel.de/smartpeg/peg/" + id + "/measurement");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String v = readStream(in);
                return getJsonObjectTimeFormat(v);
        } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getPredictionHttp(String id) {
        URL url = null;
        try {
            url = new URL("http://smartpeg.fhessel.de/smartpeg/peg/" + id + "/prediction");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String v = readStream(in);
                return getJsonObject(v);
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static JSONObject getIdsHttp(String formallyNecessary) {
        URL url = null;
        try {
            url = new URL("http://smartpeg.fhessel.de/smartpeg/peg");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String v = readStream(in);
                return getJsonObjectArrayObject(v);
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    @Nullable
    private static JSONObject getJsonObjectTimeFormat(String v) {
        String date = v.substring(v.length()-22, v.length()-1);
        v = v.substring(0, v.length()-35) + "}";

        JSONParser parser = new JSONParser();
        try {
            JSONObject o = (JSONObject) parser.parse(v);
            o.put("timestamp", date);
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JSONObject getJsonObject(String v) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject o = (JSONObject) parser.parse(v);
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JSONObject getJsonObjectArrayObject(String v){
        JSONParser parser = new JSONParser();
        try {
            JSONArray a = (JSONArray) parser.parse(v);
            JSONObject o = new JSONObject();
            Log.d("json", a.toString());
            o.put("array", a);
            return o;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public int getProgress() {
        return progress;
    }

    public int getLeftTime() {
        return leftTime;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getTemperature() {

        return temperature;
    }


    @Override
    protected JSONObject doInBackground(String... string) {
        boolean emul = false;
        if(string[0].equals("ids")){
            if(emul) {
                String v = "[{\"id\":1,\"bat_status\":0}]";
                JSONObject o = getJsonObjectArrayObject(v);
                return o;
            } else {
                return getIdsHttp("");
            }
        } else if (string[0].equals("prediction")){
            if(emul){
                String v = "{\"prediction\":1010.0}";
                JSONObject o = getJsonObject(v);
                return o;
            }else{
                return getPredictionHttp(string[1]);
            }
        }


        if(emul) {
            Log.d("api", String.valueOf(60 - System.nanoTime()/100000000000d));
            String v = "{\"nr\":100634,\"temperature\":20.2,\"humidity\":56.7,\"conductance\":" + String.valueOf(60 - System.nanoTime()/100000000000d) + ",\"timestamp\":2017-12-18 10:29:48.0}";
            JSONObject o = getJsonObjectTimeFormat(v);
            return o;
        }else{
            return getMeasurementsHttp(string[0]);
        }
    }
}
