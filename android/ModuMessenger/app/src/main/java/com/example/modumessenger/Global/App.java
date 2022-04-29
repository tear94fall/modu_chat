package com.example.modumessenger.Global;

import android.app.Application;

public class App extends Application {

    public static PreferenceManager refs;

    @Override
    public void onCreate(){
        super.onCreate();

        refs = new PreferenceManager(getApplicationContext());
    }
}
