package com.example.do_an.core;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.do_an.core.constant.Language;
import com.example.do_an.data.common.LocalePreferences;

public class LocaleManager {

    private LocaleManager() {
    }

    public static void applySavedLocale(Context context) {
        LocalePreferences prefs = new LocalePreferences(context);
        Language lang = prefs.getLanguage();
        setLocale(lang);
    }

    public static void setLocale(Language language) {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.getCode())
        );
    }

    public static void resetToSystem() {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.getEmptyLocaleList()
        );
    }
}
