package com.example.do_an.application;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "reader_settings";
    private static final String DARK_MODE = "dark_mode";
    private static final String DIRECTION = "direction"; // 0 = vertical, 1 = horizontal
    private static final String PAGE_MODE = "page_mode"; // 1 or 2
    private static final String AUTO_NEXT = "auto_next";
    private static final String AUTO_TIME = "auto_time"; // seconds
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static final String SHOW_PAGE_INDICATOR = "show_page_indicator";

    public SettingsManager(Context ctx) {
        pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setDirection(int v){ editor.putInt(DIRECTION, v).apply(); }
    public int getDirection(){ return pref.getInt(DIRECTION, 0); }

    public void setPageMode(int v){ editor.putInt(PAGE_MODE, v).apply(); }
    public int getPageMode(){ return pref.getInt(PAGE_MODE, 1); }

    public void setAutoNext(boolean v){ editor.putBoolean(AUTO_NEXT, v).apply(); }
    public boolean isAutoNext(){ return pref.getBoolean(AUTO_NEXT, false); }

    public void setAutoTime(int v){ editor.putInt(AUTO_TIME, v).apply(); }
    public int getAutoTime(){ return pref.getInt(AUTO_TIME, 3); }
    public void setShowPageIndicator(boolean v){ editor.putBoolean(SHOW_PAGE_INDICATOR, v).apply(); }
    public boolean isPageIndicatorEnabled(){
        return pref.getBoolean(SHOW_PAGE_INDICATOR, true);
    }
}
