package com.example.user.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.example.user.myapplication.R;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.concurrent.ExecutionException;


public class Settings extends PreferenceActivity {

    public static final String KEY_PREF_SYNC_CONN = "settings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        API api = new API();
        ListPreference listPref = (ListPreference) findPreference("peg_id");

        api.execute("ids");
        JSONArray ids = null;
        try {
            ids = (JSONArray) api.get().get("array");

        Object[] array = ids.toArray();
        String[] stringArray = new String[array.length];
        for(int i = 0; i<array.length; i++) {
            stringArray[i] = String.valueOf(((JSONObject) array[i]).get("id"));
        }
        listPref.setEntries(stringArray);
        listPref.setEntryValues(stringArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_PREF_SYNC_CONN)) {
            Preference connectionPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            connectionPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }

}
