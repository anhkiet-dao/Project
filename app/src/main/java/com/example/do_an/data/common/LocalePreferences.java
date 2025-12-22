package com.example.do_an.data.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.do_an.core.constant.Language;

public class LocalePreferences {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    private final SharedPreferences prefs;

    public LocalePreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setLanguage(Language language) {
        prefs.edit().putString(KEY_LANGUAGE, language.getCode()).apply();
    }

    public Language getLanguage() {
        String code = prefs.getString(KEY_LANGUAGE, Language.VI.getCode());
        return Language.fromCode(code);
    }

    public void clearLanguage() {
        prefs.edit().remove(KEY_LANGUAGE).apply();
    }
}
