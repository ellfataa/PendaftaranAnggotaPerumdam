package com.example.formregistrasi;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final long TOKEN_EXPIRATION_TIME = 3600000; // 1 hour in milliseconds
    private static final String KEY_TOKEN_TIMESTAMP = "token_timestamp";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public boolean isTokenExpired() {
        long tokenTimestamp = sharedPreferences.getLong(KEY_TOKEN_TIMESTAMP, 0);
        return System.currentTimeMillis() - tokenTimestamp > TOKEN_EXPIRATION_TIME;
    }


    public String refreshToken() {
        // Implement the logic to refresh the token
        // This might involve making a network request to your server
        // For now, we'll just return the current token
        return getToken();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    public void clearToken() {
        editor.remove(KEY_TOKEN);
        editor.apply();
    }
}