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

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.HashMap;
import java.util.Map;

public class IndexPendaftaranLogin extends AppCompatActivity {

    private static final String TAG = "IndexPendaftaranLogin";
    private Button btn_status, btn_registrasi, btn_keluar;
    private TextView txtIndexPelanggan, txtUserName;
    private SharedPreferences sharedPreferences;

    private static final String INDEX_URL = "http://192.168.230.122/pendaftaranPerumdam/indexPelangganLogin.php";
    private static final String LOGOUT_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/logout";

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";
    private boolean hasRegistered;

    // Method ini dipanggil ketika activity dibuat
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        initializeViews();
        setupSharedPreferences();
        setupGoogleSignIn();
        checkRegistrationStatus();
        handleIntentData();
        setupButtonListeners();
        checkLoginStatus();
    }

    // Method untuk menginisialisasi semua view
    private void initializeViews() {
        btn_status = findViewById(R.id.btn_status);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        btn_keluar = findViewById(R.id.btnLogout);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);
        txtUserName = findViewById(R.id.txtUserName);
    }

    // Method untuk mengatur visibility tombol berdasarkan status registrasi
    private void updateButtonVisibility(boolean hasRegistered) {
        if (btn_registrasi != null) {
            btn_registrasi.setVisibility(hasRegistered ? View.GONE : View.VISIBLE);
        }
        if (btn_status != null) {
            btn_status.setVisibility(View.VISIBLE);
        }
    }

    // Method untuk menyiapkan SharedPreferences
    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
    }

    // Method untuk menyiapkan Google Sign In
    private void setupGoogleSignIn() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    // Method untuk menangani data dari intent
    private void handleIntentData() {
        Intent intent = getIntent();
        boolean hasRegistered = intent.getBooleanExtra("hasRegistered", false);
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(HAS_REGISTERED_KEY, hasRegistered);
        editor.apply();

        updateButtonVisibility(hasRegistered);

        if (intent.getBooleanExtra("REGISTERED", false)) {
            String nik = intent.getStringExtra("NIK");
            updateSharedPreferences(nik);
            updateUI(true);
        } else {
            checkRegistrationStatus();
        }
    }

    // Method untuk menyiapkan listener untuk tombol-tombol
    private void setupButtonListeners() {
        btn_status.setOnClickListener(v -> checkStatusAccess());
        btn_registrasi.setOnClickListener(v -> checkRegistrationAccess());
        btn_keluar.setOnClickListener(v -> logout());
    }

    // Method untuk mengupdate UI berdasarkan status registrasi
    private void updateUI(boolean isRegistered) {
        if (isRegistered) {
            btn_registrasi.setVisibility(View.GONE);
            btn_status.setVisibility(View.VISIBLE);
        } else {
            btn_registrasi.setVisibility(View.VISIBLE);
            btn_status.setVisibility(View.GONE);
        }
    }

    // Method untuk memeriksa akses ke halaman status
    private void checkStatusAccess() {
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String savedNik = userPrefs.getString("nomor_ktp", "");

        if (!savedNik.isEmpty()) {
            Intent intent = new Intent(IndexPendaftaranLogin.this, Status.class);
            intent.putExtra("NIK", savedNik);
            startActivity(intent);
        } else {
            showAlert("Anda belum melakukan registrasi. Silakan registrasi terlebih dahulu.");
        }
    }

    // Method untuk memeriksa akses ke halaman registrasi
    private void checkRegistrationAccess() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY, false);

        if (hasRegistered) {
            showAlert("Anda sudah melakukan registrasi sebelumnya.");
        } else {
            navigateToRegistration();
        }
    }

    // Method untuk menampilkan alert dialog
    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Informasi")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Method untuk memeriksa status registrasi
    private void checkRegistrationStatus() {
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String loggedInEmail = userPrefs.getString("email", "");
        boolean hasRegistered = userPrefs.getBoolean("hasRegistered_" + loggedInEmail, false);

        updateButtonStates(hasRegistered);
    }

    // Method untuk mengupdate SharedPreferences dengan NIK
    private void updateSharedPreferences(String nik) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HAS_REGISTERED_KEY, true);
        editor.putString("nomor_ktp", nik);
        editor.apply();
    }

    // Method untuk memeriksa status login
    private void checkLoginStatus() {
        String name = sharedPreferences.getString("name", "");
        String token = sharedPreferences.getString("token", "");

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        if (!name.isEmpty() && !token.isEmpty()) {
            updateUIForLoggedInUser(name);
        } else if (acct != null) {
            String googleName = acct.getDisplayName();
            updateUIForLoggedInUser(googleName != null ? googleName : "Google User");
        } else {
            updateUIForLoggedOutUser();
        }
    }

    // Method untuk mengupdate UI untuk user yang sudah login
    private void updateUIForLoggedInUser(String name) {
        txtUserName.setText(name);
        txtUserName.setVisibility(View.VISIBLE);
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY, false);
        btn_registrasi.setVisibility(hasRegistered ? View.GONE : View.VISIBLE);
    }

    // Method untuk mengupdate UI untuk user yang belum login
    private void updateUIForLoggedOutUser() {
        txtUserName.setText("Akun: Belum Login");
        txtUserName.setVisibility(View.VISIBLE);
        btn_keluar.setVisibility(View.GONE);
        btn_status.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(View.VISIBLE);
    }

    // Method untuk melakukan logout
    private void logout() {
        String token = sharedPreferences.getString("token", "");

        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.clear();
        editor.apply();

        if (!token.isEmpty()) {
            logoutRegular(token);
        } else {
            logoutGoogle();
        }
    }

    // Method untuk logout dari akun regular
    private void logoutRegular(String token) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGOUT_URL,
                response -> {
                    clearUserInfo();
                    navigateToMainActivity();
                    Toast.makeText(IndexPendaftaranLogin.this, "Logout berhasil", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(IndexPendaftaranLogin.this, "Logout gagal", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Method untuk logout dari akun Google
    private void logoutGoogle() {
        gsc.signOut().addOnCompleteListener(this, task -> {
            clearUserInfo();
            navigateToMainActivity();
            Toast.makeText(IndexPendaftaranLogin.this, "Google logout berhasil", Toast.LENGTH_SHORT).show();
        });
    }

    // Method untuk menghapus informasi user
    private void clearUserInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    // Method untuk kembali ke MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Method untuk berpindah ke halaman registrasi
    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);

        String userName = txtUserName.getText().toString();
        if (!userName.equals("Akun: Belum Login")) {
            intent.putExtra("userName", userName);
        }

        String userEmail = sharedPreferences.getString("email", "");
        if (!userEmail.isEmpty()) {
            intent.putExtra("userEmail", userEmail);
        }

        startActivityForResult(intent, 1);
    }

    // Method untuk menangani hasil dari activity lain
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            SharedPreferences registrationPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = registrationPrefs.edit();
            editor.putBoolean(HAS_REGISTERED_KEY, true);
            if (data != null && data.hasExtra("NIK")) {
                editor.putString("nomor_ktp", data.getStringExtra("NIK"));
            }
            editor.apply();
            updateButtonStates(true);
        }
    }

    // Method yang dipanggil ketika activity kembali menjadi fokus
    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
        checkRegistrationStatus();
    }

    // Method untuk mengupdate status tombol
    private void updateButtonStates(boolean hasRegistered) {
        if (btn_registrasi != null) {
            btn_registrasi.setEnabled(!hasRegistered);
            btn_registrasi.setVisibility(hasRegistered ? View.GONE : View.VISIBLE);
        }
        if (btn_status != null) {
            btn_status.setEnabled(true);
            btn_status.setVisibility(View.VISIBLE);
        }
    }
}