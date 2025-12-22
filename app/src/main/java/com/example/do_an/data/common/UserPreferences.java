package com.example.do_an.data.common;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {

    private static final String PREFS_NAME = "USER_PREF";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    private final SharedPreferences prefs;

    public UserPreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(String email, String password) {
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    public void clearUser() {
        prefs.edit()
                .remove(KEY_EMAIL)
                .remove(KEY_PASSWORD)
                .apply();
    }
}
