package com.example.formregistrasi;

import static com.example.formregistrasi.RegistrasiActivity.PREFS_NAME;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String GOOGLE_TOKEN_KEY = "google_token";
    private static final String IS_GOOGLE_LOGIN = "IsGoogleLogin";
    private static final String GOOGLE_TOKEN = "GoogleToken";
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

    // Metode untuk menyimpan token Google
    public void saveGoogleToken(String token) {
        editor.putString(GOOGLE_TOKEN, token);
        editor.putBoolean(IS_GOOGLE_LOGIN, true);
        editor.apply();
    }

    // Metode untuk mendapatkan token Google
    public String getGoogleToken() {
        return sharedPreferences.getString(GOOGLE_TOKEN, "");
    }

    // Metode untuk mengecek apakah pengguna login menggunakan Google
    public boolean isGoogleLogin() {
        return sharedPreferences.getBoolean(IS_GOOGLE_LOGIN, false);
    }

    public void clearSession() {
        // Hapus semua data dari SharedPreferences
        editor.clear();
        editor.apply();

        // Hapus status login Google dari SharedPreferences terpisah
        SharedPreferences googlePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor googleEditor = googlePrefs.edit();
        googleEditor.remove("is_google_login");
        googleEditor.apply();

        // Hapus token JWT
        clearToken();

        // Hapus token Google
        editor.remove(GOOGLE_TOKEN_KEY);
        editor.apply();

        // Hapus timestamp token
        editor.remove(KEY_TOKEN_TIMESTAMP);
        editor.apply();

        // Jika ada data lain yang perlu dihapus, tambahkan di sini
        // Misalnya, jika Anda menyimpan data user lainnya:
        // editor.remove("user_email");
        // editor.remove("user_name");
        // editor.apply();

        // Log out untuk memastikan semua data telah dihapus
        logout();
    }

    // Menambahkan metode logout
    public void logout() {
        editor.clear();
        editor.apply();
    }
}