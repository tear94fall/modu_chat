package com.example.modumessenger.Global;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreStr;

import android.annotation.SuppressLint;
import android.app.appsearch.AppSearchResult;
import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import com.example.modumessenger.entity.Member;
import com.google.gson.Gson;

import java.io.File;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class DataStoreUtils {

    private static volatile DataStoreUtils instance;
    private static RxDataStore<Preferences> dataStore;
    private String filePath;

    public static DataStoreUtils getInstance() {
        if (null == instance) {
            synchronized (DataStoreUtils.class) {
                if (instance == null) {
                    instance = new DataStoreUtils();
                }
            }
        }
        return instance;
    }

    public void getPreferences(Context context, String dataFileName) {
        this.filePath = context.getFilesDir().getAbsolutePath();
        RxPreferenceDataStoreBuilder builder = new RxPreferenceDataStoreBuilder(context, dataFileName);
        dataStore = builder.build();
    }

    /***
     * Clear all DataStore files
     */
    public void clearAll() {
        File file = new File(filePath);
        File[] files = file.listFiles();
        assert files != null;
        if (files.length == 0) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File name = files[i];
            if(name.delete()) {
                i--;
            }
        }
    }

    /***
     * Write DataStore value
     * param strKey
     * param value
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public void writeValue(String strKey, Object value) {
        if (Integer.class.equals(value.getClass())) {
            Preferences.Key<Integer> intKey = PreferencesKeys.intKey(strKey);
            dataStore.updateDataAsync(preferences -> {
                MutablePreferences mutablePreferences = preferences.toMutablePreferences();
                mutablePreferences.set(intKey, (Integer) value);
                return Single.just(mutablePreferences);
            });
        } else if (String.class.equals(value.getClass())) {
            Preferences.Key<String> stringKey = PreferencesKeys.stringKey(strKey);
            dataStore.updateDataAsync(preferences -> {
                MutablePreferences mutablePreferences = preferences.toMutablePreferences();
                mutablePreferences.set(stringKey, (String) value);
                return Single.just(mutablePreferences);
            });
        } else if (Boolean.class.equals(value.getClass())) {
            Preferences.Key<Boolean> booleanKey = PreferencesKeys.booleanKey(strKey);
            dataStore.updateDataAsync(preferences -> {
                MutablePreferences mutablePreferences = preferences.toMutablePreferences();
                mutablePreferences.set(booleanKey, (Boolean) value);
                return Single.just(mutablePreferences);
            });
        } else if (Double.class.equals(value.getClass())) {
            Preferences.Key<Double> doubleKey = PreferencesKeys.doubleKey(strKey);
            dataStore.updateDataAsync(preferences -> {
                MutablePreferences mutablePreferences = preferences.toMutablePreferences();
                mutablePreferences.set(doubleKey, (Double) value);
                return Single.just(mutablePreferences);
            });
        } else if (Float.class.equals(value.getClass())) {
            Preferences.Key<Float> floatKey = PreferencesKeys.floatKey(strKey);
            dataStore.updateDataAsync(preferences -> {
                MutablePreferences mutablePreferences = preferences.toMutablePreferences();
                mutablePreferences.set(floatKey, (Float) value);
                return Single.just(mutablePreferences);
            });
        } else if (Long.class.equals(value.getClass())) {
            Preferences.Key<Long> longKey = PreferencesKeys.longKey(strKey);
            dataStore.updateDataAsync(preferences -> {
                MutablePreferences mutablePreferences = preferences.toMutablePreferences();
                mutablePreferences.set(longKey, (Long) value);
                return Single.just(mutablePreferences);
            });
        } else {
            throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    /**
     * Read DataStore value
     * param strKey
     * return Returns a value of type String
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public String readValueBackStr(String strKey) {
        Preferences.Key<String> stringKey = PreferencesKeys.stringKey(strKey);
        // RxJava 의 map 은 null 반환을 금지한다. 저장된 키가 없으면 preferences.get 이
        // null 을 돌려주고, 그 NPE 가 스케줄러 스레드에서 undeliverable 로 터져 앱이 죽는다.
        Flowable<String> flowable = dataStore.data().map(preferences -> {
            String value = preferences.get(stringKey);
            return value == null ? "" : value;
        });
        return flowable.blockingFirst();
    }

    /**
     * param strKey
     * return Returns a value of type Integer
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public Integer readValueBackInteger(String strKey) {
        Preferences.Key<Integer> key = PreferencesKeys.intKey(strKey);
        // RxJava 의 map 은 null 반환을 금지한다. 저장된 키가 없으면 preferences.get 이
        // null 을 돌려주고, 그 NPE 가 스케줄러 스레드에서 undeliverable 로 터져 앱이 죽는다.
        Flowable<Integer> flowable = dataStore.data().map(preferences -> {
            Integer value = preferences.get(key);
            return value == null ? 0 : value;
        });
        return flowable.blockingFirst();
    }

    /***
     * param strKey
     * return Returns a value of type Boolean
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public Boolean readValueBackBoolean(String strKey) {
        Preferences.Key<Boolean> key = PreferencesKeys.booleanKey(strKey);
        // RxJava 의 map 은 null 반환을 금지한다. 저장된 키가 없으면 preferences.get 이
        // null 을 돌려주고, 그 NPE 가 스케줄러 스레드에서 undeliverable 로 터져 앱이 죽는다.
        Flowable<Boolean> booleanFlowable = dataStore.data().map(preferences -> {
            Boolean value = preferences.get(key);
            return value == null ? Boolean.FALSE : value;
        });
        return booleanFlowable.blockingFirst();
    }

    /***
     * param strKey
     * return Returns a value of type Float
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public Float readValueBackFloat(String strKey) {
        Preferences.Key<Float> keys = PreferencesKeys.floatKey(strKey);
        // RxJava 의 map 은 null 반환을 금지한다. 저장된 키가 없으면 preferences.get 이
        // null 을 돌려주고, 그 NPE 가 스케줄러 스레드에서 undeliverable 로 터져 앱이 죽는다.
        Flowable<Float> floatFlowable = dataStore.data().map(preferences -> {
            Float value = preferences.get(keys);
            return value == null ? 0f : value;
        });
        return floatFlowable.blockingFirst();
    }

    /***
     * param strKey
     * return Returns a value of type Double
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public Double readValueBackDouble(String strKey) {
        Preferences.Key<Double> key = PreferencesKeys.doubleKey(strKey);
        // RxJava 의 map 은 null 반환을 금지한다. 저장된 키가 없으면 preferences.get 이
        // null 을 돌려주고, 그 NPE 가 스케줄러 스레드에서 undeliverable 로 터져 앱이 죽는다.
        Flowable<Double> flowable = dataStore.data().map(preferences -> {
            Double value = preferences.get(key);
            return value == null ? 0d : value;
        });
        return flowable.blockingFirst();
    }

    /***
     * param strKey
     * return Returns a Long type value
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public Long readValueBackLong(String strKey) {
        Preferences.Key<Long> key = PreferencesKeys.longKey(strKey);
        // RxJava 의 map 은 null 반환을 금지한다. 저장된 키가 없으면 preferences.get 이
        // null 을 돌려주고, 그 NPE 가 스케줄러 스레드에서 undeliverable 로 터져 앱이 죽는다.
        Flowable<Long> flowable = dataStore.data().map(preferences -> {
            Long value = preferences.get(key);
            return value == null ? 0L : value;
        });
        return flowable.blockingFirst();
    }

    /**
     * Delete DataStore value
     * param strKey
     */
    @SuppressLint("UnsafeOptInUsageWarning")
    public void deleteValue(String strKey) {
        Preferences.Key<String> stringKey = PreferencesKeys.stringKey(strKey);
        dataStore.updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.remove(stringKey);
            return Single.just(mutablePreferences);
        });
    }

    @SuppressLint("UnsafeOptInUsageWarning")
    public Boolean checkKey(String strKey) {
        Preferences.Key<Long> key = PreferencesKeys.longKey(strKey);
        Flowable<Boolean> flowable = dataStore.data().map(preferences -> preferences.contains(key));
        return flowable.blockingFirst();
    }
}
