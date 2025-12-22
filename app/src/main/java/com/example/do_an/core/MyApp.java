package com.example.do_an.core;

import android.app.Application;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LocaleManager.applySavedLocale(this);
    }


}
