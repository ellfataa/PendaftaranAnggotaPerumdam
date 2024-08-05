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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        initializeViews();
        setupSharedPreferences();
        setupGoogleSignIn();
        checkRegistrationStatus();

        // Ambil status registrasi dari Intent
        boolean hasRegistered = getIntent().getBooleanExtra("hasRegistered", false);

        // Simpan status registrasi ke SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(HAS_REGISTERED_KEY, hasRegistered);
        editor.apply();

        // Atur status tombol berdasarkan status registrasi
        if (btn_registrasi != null) {
            btn_registrasi.setEnabled(!hasRegistered);
        }
        if (btn_status != null) {
            btn_status.setEnabled(true);
        }

        setupButtonListeners();
        checkRegistrationStatus();
        handleRegistrationStatus();
        checkLoginStatus();
    }

    private void initializeViews() {
        btn_status = findViewById(R.id.btn_status);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        btn_keluar = findViewById(R.id.btnLogout);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);
        txtUserName = findViewById(R.id.txtUserName);
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
    }

    private void setupGoogleSignIn() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    private void setupButtonListeners() {
        btn_status.setOnClickListener(v -> checkStatusAccess());
        btn_registrasi.setOnClickListener(v -> checkRegistrationAccess());
        btn_keluar.setOnClickListener(v -> logout());
    }

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

    private void checkRegistrationAccess() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY, false);

        if (hasRegistered) {
            showAlert("Anda sudah melakukan registrasi sebelumnya.");
        } else {
            navigateToRegistration();
        }
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Informasi")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkRegistrationStatus() {
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String savedNik = userPrefs.getString("nomor_ktp", "");
        boolean hasRegistered = !savedNik.isEmpty();

        if (btn_registrasi != null) {
            btn_registrasi.setEnabled(!hasRegistered);
        }
        if (btn_status != null) {
            btn_status.setEnabled(hasRegistered);
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(HAS_REGISTERED_KEY, hasRegistered);
        editor.apply();
    }

    private void handleRegistrationStatus() {
        boolean registeredFromStatus = getIntent().getBooleanExtra("REGISTERED", false);
        if (registeredFromStatus) {
            String nik = getIntent().getStringExtra("NIK");
            updateSharedPreferences(nik);
        }
    }

    private void updateSharedPreferences(String nik) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("hasRegistered", true);
        editor.putString("NIK", nik);
        editor.apply();
    }

    private void checkLoginStatus() {
        String name = sharedPreferences.getString("name", "");
        String token = sharedPreferences.getString("token", "");

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        if (!name.isEmpty() && !token.isEmpty()) {
            updateUIForLoggedInUser(name);
            checkRegistrationStatus();
        } else if (acct != null) {
            String googleName = acct.getDisplayName();
            updateUIForLoggedInUser(googleName != null ? googleName : "Google User");
            checkRegistrationStatus();
        } else {
            updateUIForLoggedOutUser();
        }
    }

    private void updateUIForLoggedInUser(String name) {
        txtUserName.setText(name);
        txtUserName.setVisibility(View.VISIBLE);
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY, false);
        btn_registrasi.setVisibility(hasRegistered ? View.GONE : View.VISIBLE);
    }

    private void updateUIForLoggedOutUser() {
        txtUserName.setText("Akun: Belum Login");
        txtUserName.setVisibility(View.VISIBLE);
        btn_keluar.setVisibility(View.GONE);
        btn_status.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(View.VISIBLE);
    }

    private void logout() {
        String token = sharedPreferences.getString("token", "");

        if (!token.isEmpty()) {
            logoutRegular(token);
        } else {
            logoutGoogle();
        }
    }

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

    private void logoutGoogle() {
        gsc.signOut().addOnCompleteListener(this, task -> {
            clearUserInfo();
            navigateToMainActivity();
            Toast.makeText(IndexPendaftaranLogin.this, "Google logout berhasil", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearUserInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

//    private void navigateToLogin() {
//        Intent intent = new Intent(IndexPendaftaranLogin.this, LoginActivity.class);
//        startActivity(intent);
//    }

    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);

        // Tambahkan nama pengguna ke intent
        String userName = txtUserName.getText().toString();
        if (!userName.equals("Akun: Belum Login")) {
            intent.putExtra("userName", userName);
        }

        // Tambahkan email pengguna
        String userEmail = sharedPreferences.getString("email", "");
        if (!userEmail.isEmpty()) {
            intent.putExtra("userEmail", userEmail);
        }

        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                SharedPreferences registrationPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = registrationPrefs.edit();
                editor.putBoolean(HAS_REGISTERED_KEY, true);
                if (data != null && data.hasExtra("NIK")) {
                    editor.putString("nomor_ktp", data.getStringExtra("NIK"));
                }
                editor.apply();
                updateButtonStates();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
        checkRegistrationStatus();
        updateButtonStates();
    }

    private void updateButtonStates() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY, false);

        if (btn_registrasi != null) {
            btn_registrasi.setEnabled(!hasRegistered);
        }
        if (btn_status != null) {
            btn_status.setEnabled(true);
        }
    }
}