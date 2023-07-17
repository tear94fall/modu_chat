package com.example.modumessenger.Global;

import android.content.Context;

public class DataStoreHelper {

    public static void initDataStore(Context context, String fileName) {
        DataStoreUtils.getInstance().getPreferences(context, fileName);
    }

    public static void clearDataStore() {
        DataStoreUtils.getInstance().clearAll();
    }

    public static void setDataStoreObject(String key, Object value) {
        DataStoreUtils.getInstance().writeValue(key, value);
    }

    public static Boolean getDataStoreBoolean(String key) {
        return DataStoreUtils.getInstance().readValueBackBoolean(key);
    }

    public static Integer getDataStoreInt(String key) {
        return DataStoreUtils.getInstance().readValueBackInteger(key);
    }

    public static Float getDataStoreFloat(String key) {
        return DataStoreUtils.getInstance().readValueBackFloat(key);
    }

    public static Double getDataStoreDouble(String key) {
        return DataStoreUtils.getInstance().readValueBackDouble(key);
    }

    public static Long getDataStoreLong(String key) {
        return DataStoreUtils.getInstance().readValueBackLong(key);
    }

    public static String getDataStoreStr(String key) {
        return DataStoreUtils.getInstance().readValueBackStr(key);
    }

    public static void delDataStoreObject(String key) {
        DataStoreUtils.getInstance().deleteValue(key);
    }
}
