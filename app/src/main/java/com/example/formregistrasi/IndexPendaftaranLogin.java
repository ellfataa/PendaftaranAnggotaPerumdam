package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.HashMap;
import java.util.Map;

public class IndexPendaftaranLogin extends AppCompatActivity {

    private static final String TAG = "IndexPendaftaranLogin";
    private Button btn_status, btn_registrasi, btn_keluar;
    private TextView txtIndexPelanggan, txtUserName, txtUserEmail;
    private SharedPreferences sharedPreferences;

    private static final String LOGOUT_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/logout";

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    private static final String PREFS_NAME = "UserInfo";
    private static final String AUTH_TOKEN_KEY = "token";
    private static final String USER_EMAIL_KEY = "email";
    private static final String NAME_KEY = "nama";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";

    // Method ini dijalankan saat activity dibuat
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        initializeViews();
        setupSharedPreferences();
        setupGoogleSignIn();
        setupButtonListeners();
        checkLoginStatus();
    }

    // Method ini dijalankan saat activity di-resume
    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    // Inisialisasi semua view yang ada di layout
    private void initializeViews() {
        btn_status = findViewById(R.id.btn_status);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        btn_keluar = findViewById(R.id.btnLogout);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
    }

    // Setup SharedPreferences untuk menyimpan data user
    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    // Setup Google Sign In
    private void setupGoogleSignIn() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    // Menambahkan listener ke semua tombol
    private void setupButtonListeners() {
        btn_status.setOnClickListener(v -> checkStatusAccess());
        btn_registrasi.setOnClickListener(v -> checkRegistrationAccess());
        btn_keluar.setOnClickListener(v -> logout());
    }

    // Memeriksa status login user
    private void checkLoginStatus() {
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        String authToken = sharedPreferences.getString(AUTH_TOKEN_KEY, "");

        if (!userEmail.isEmpty() && !authToken.isEmpty()) {
            updateUIForLoggedInUser(userEmail);
        } else {
            updateUIForLoggedOutUser();
        }
    }

    // Memperbarui tampilan untuk user yang sudah login
    private void updateUIForLoggedInUser(String email) {
        String userName = sharedPreferences.getString(NAME_KEY, "");

        txtUserName.setText(!userName.isEmpty() ? userName : email);
        txtUserName.setVisibility(View.VISIBLE);
        txtUserEmail.setText(email);
        txtUserEmail.setVisibility(View.VISIBLE);
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);

        boolean hasRegistered = sharedPreferences.getBoolean(HAS_REGISTERED_KEY + "_" + email, false);
        btn_registrasi.setVisibility(hasRegistered ? View.GONE : View.VISIBLE);
    }

    // Memperbarui tampilan untuk user yang belum login
    private void updateUIForLoggedOutUser() {
        txtUserName.setText("Akun: Belum Login");
        txtUserName.setVisibility(View.VISIBLE);
        txtUserEmail.setText("");
        txtUserEmail.setVisibility(View.GONE);
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(View.VISIBLE);
    }

    // Memeriksa akses ke halaman status
    private void checkStatusAccess() {
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        if (!userEmail.isEmpty()) {
            boolean hasRegistered = sharedPreferences.getBoolean(HAS_REGISTERED_KEY + "_" + userEmail, false);
            if (hasRegistered) {
                String nomorKtp = sharedPreferences.getString("nomor_ktp_" + userEmail, "");
                if (!nomorKtp.isEmpty()) {
                    navigateToStatus(nomorKtp);
                } else {
                    showAlert("Nomor KTP tidak ditemukan");
                }
            } else {
                showAlert("Anda belum melakukan registrasi");
            }
        } else {
            showAlert("Anda belum melakukan login. Silakan login terlebih dahulu.");
        }
    }

    private void navigateToStatus(String nomorKtp) {
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String savedStatus = prefs.getString("payment_status_" + nomorKtp, "");

        Intent intent = new Intent(IndexPendaftaranLogin.this, Status.class);
        intent.putExtra("NOMOR_KTP", nomorKtp);
        intent.putExtra("SAVED_STATUS", savedStatus);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // Memeriksa akses ke halaman registrasi
    private void checkRegistrationAccess() {
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        if (!userEmail.isEmpty()) {
            boolean hasRegistered = sharedPreferences.getBoolean(HAS_REGISTERED_KEY + "_" + userEmail, false);
            if (hasRegistered) {
                showAlert("Anda sudah melakukan registrasi sebelumnya.");
            } else {
                navigateToRegistration();
            }
        } else {
            showAlert("Anda belum melakukan login. Silakan login terlebih dahulu.");
        }
    }

    // Navigasi ke halaman registrasi
    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        if (!userEmail.isEmpty()) {
            intent.putExtra("userEmail", userEmail);
        }
        startActivity(intent);
    }

    // Menampilkan pesan alert
    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Informasi")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Proses logout
    private void logout() {
        boolean isGoogleLogin = sharedPreferences.getBoolean("is_google_login", false);
        if (isGoogleLogin) {
            logoutGoogle();
        } else {
            String authToken = sharedPreferences.getString(AUTH_TOKEN_KEY, "");
            if (!authToken.isEmpty()) {
                logoutRegular(authToken);
            } else {
                // Handle case where there's no auth token (shouldn't normally happen)
                clearAuthData();
                navigateToMainActivity();
            }
        }
    }

    // Logout dari akun biasa
    private void logoutRegular(String token) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGOUT_URL,
                response -> {
                    handleLogoutSuccess();
                },
                error -> {
                    handleLogoutError((com.android.volley.VolleyError) error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Menangani logout yang berhasil
    private void handleLogoutSuccess() {
        clearAuthData();
        navigateToMainActivity();
        Toast.makeText(IndexPendaftaranLogin.this, "Logout berhasil", Toast.LENGTH_SHORT).show();
    }

    // Menangani error saat logout
    private void handleLogoutError(com.android.volley.VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.statusCode == 500) {
            Toast.makeText(IndexPendaftaranLogin.this, "Server error saat logout. Mohon coba lagi nanti.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(IndexPendaftaranLogin.this, "Logout gagal: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        }
        clearAuthData();
        navigateToMainActivity();
    }
//
    // Logout dari akun Google
    private void logoutGoogle() {
        gsc.signOut().addOnCompleteListener(this, task -> {
            clearAuthData();
            navigateToMainActivity();
            Toast.makeText(IndexPendaftaranLogin.this, "Google logout berhasil", Toast.LENGTH_SHORT).show();
        });
    }

    // Menghapus data autentikasi dari SharedPreferences
    private void clearAuthData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(AUTH_TOKEN_KEY);
        editor.remove(USER_EMAIL_KEY);
        editor.remove(NAME_KEY);
        editor.remove("is_google_login");
        editor.apply();
    }

    // Navigasi ke MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}