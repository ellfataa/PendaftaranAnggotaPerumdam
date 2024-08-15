package com.example.formregistrasi;

import static com.example.formregistrasi.RegistrasiActivity.PREFS_NAME;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_TOKEN_TIMESTAMP = "token_timestamp";
    private static final long TOKEN_EXPIRATION_TIME = 3600000; // 1 jam dalam milidetik

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public boolean isLoggedInWithGoogle() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("is_google_login", false);
    }

    // Konstruktor untuk inisialisasi SessionManager
    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Menyimpan token JWT ke SharedPreferences
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    // Mengambil token JWT dari SharedPreferences
    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    // Memeriksa apakah token JWT sudah kadaluarsa
    public boolean isTokenExpired() {
        long tokenTimestamp = sharedPreferences.getLong(KEY_TOKEN_TIMESTAMP, 0);
        return System.currentTimeMillis() - tokenTimestamp > TOKEN_EXPIRATION_TIME;
    }

    // Memperbaharui token JWT
    public String refreshToken() {
        // Implementasi logika untuk memperbaharui token
        // Ini mungkin melibatkan permintaan jaringan ke server Anda
        // Untuk saat ini, kita hanya mengembalikan token yang ada
        return getToken();
    }

    // Menghapus token JWT dari SharedPreferences
    public void clearToken() {
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_TOKEN_TIMESTAMP);
        editor.apply();
    }

    // Menambahkan metode logout
    public void logout() {
        editor.clear();
        editor.apply();
    }
}