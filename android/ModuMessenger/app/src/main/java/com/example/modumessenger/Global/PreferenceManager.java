package com.example.modumessenger.Global;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    public static final String PREFERENCES_NAME = "modu_preference";
    private static final String DEFAULT_VALUE_STRING = "";
    private static final boolean DEFAULT_VALUE_BOOLEAN = false;
    private static final int DEFAULT_VALUE_INT = -1;
    private static final long DEFAULT_VALUE_LONG = -1L;
    private static final float DEFAULT_VALUE_FLOAT = -1F;

    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE );
        editor = prefs.edit();
    }

    /**
     * String 값 저장
     * param : key
     * param : value
     */
    public static void setString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * boolean 값 저장
     * param : key
     * param : value
     */
    public static void setBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * int 값 저장
     * param : key
     * param : value
     */
    public static void setInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * long 값 저장
     * param : key
     * param : value
     */
    public static void setLong(String key, long value) {
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * float 값 저장
     * param : key
     * param : value
     */
    public static void setFloat(String key, float value) {
        editor.putFloat(key, value);
        editor.apply();
    }

    /**
     * String 값 로드
     * param : key
     * param : value
     */
    public static String getString(String key) {
        return prefs.getString(key, DEFAULT_VALUE_STRING);
    }

    /**
     * boolean 값 로드
     * param : key
     * return : boolean
     */
    public static boolean getBoolean(String key) {
        return prefs.getBoolean(key, DEFAULT_VALUE_BOOLEAN);
    }

    /**
     * int 값 로드
     * param : key
     * return : int
     */
    public static int getInt(String key) {
        return prefs.getInt(key, DEFAULT_VALUE_INT);
    }

    /**
     * long 값 로드
     * param : key
     * return : long
     */
    public static long getLong(String key) {
        return prefs.getLong(key, DEFAULT_VALUE_LONG);

    }

    /**
     * float 값 로드
     * param : key
     * return : float
     */
    public static float getFloat(String key) {
        return prefs.getFloat(key, DEFAULT_VALUE_FLOAT);
    }

    /**
     * 키 값 삭제
     * param : key
     */
    public static void removeKey(String key) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(key);
        edit.apply();
    }

    /**
     * 모든 저장 데이터 삭제
     */
    public static void clear() {
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.apply();
    }
}