package com.example.modumessenger.Global;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.modumessenger.entity.Member;
import com.google.gson.Gson;

public class SharedPrefHelper {

    public static void setSharedObject(String key, Object value) {
        PreferenceManager.setString(key, new Gson().toJson(value));
    }

    public static Object getSharedObject(String key) {
        return new Gson().fromJson(PreferenceManager.getString(key), Object.class);
    }

    public static Member getSharedObjectMember() {
        Object json = getSharedObject("member");
        return new Gson().fromJson(json.toString(), Member.class);
    }
}
