package com.example.modumessenger.Global;

import static com.example.modumessenger.Global.DataStoreHelper.initDataStore;

import android.app.Application;

public class App extends Application {

    @Override
    public void onCreate(){
        super.onCreate();

        initDataStore(getApplicationContext(), "modu-chat");
    }
}
