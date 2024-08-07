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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeViews();
        setupSharedPreferences();
        setupGoogleSignIn();
        setupButtonListeners();
        checkLoginStatus();
    }

    private void initializeViews() {
        btn_status = findViewById(R.id.btn_status);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        btn_keluar = findViewById(R.id.btnLogout);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
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
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        if (!userEmail.isEmpty()) {
            boolean hasRegistered = sharedPreferences.getBoolean(HAS_REGISTERED_KEY + "_" + userEmail, false);
            if (hasRegistered) {
                Intent intent = new Intent(IndexPendaftaranLogin.this, Status.class);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            } else {
                showAlert("Anda belum melakukan registrasi");
            }
        } else {
            showAlert("Anda belum melakukan login. Silakan login terlebih dahulu.");
        }
    }

    private void checkRegistrationAccess() {
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        if (!userEmail.isEmpty()) {
            boolean hasRegistered = sharedPreferences.getBoolean("hasRegistered_" + userEmail, false);
            if (hasRegistered) {
                showAlert("Anda sudah melakukan registrasi sebelumnya.");
            } else {
                navigateToRegistration();
            }
        } else {
            showAlert("Anda belum melakukan login. Silakan login terlebih dahulu.");
        }
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Informasi")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkLoginStatus() {
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        String authToken = sharedPreferences.getString(AUTH_TOKEN_KEY, "");

        if (!userEmail.isEmpty()) {
            updateUIForLoggedInUser(userEmail);
        } else {
            updateUIForLoggedOutUser();
        }
    }

    private void updateUIForLoggedInUser(String email) {
        String userName = sharedPreferences.getString(NAME_KEY, "");

        if (!userName.isEmpty()) {
            txtUserName.setText(userName);
        } else {
            txtUserName.setText(email);
        }

        txtUserName.setVisibility(View.VISIBLE);
        txtUserEmail.setVisibility(View.GONE); // Hide the email TextView
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);

        // Check if user has registered
        boolean hasRegistered = sharedPreferences.getBoolean(HAS_REGISTERED_KEY + "_" + email, false);
        btn_registrasi.setVisibility(hasRegistered ? View.GONE : View.VISIBLE);
    }

    private void updateUIForLoggedOutUser() {
        txtUserName.setText("Akun: Belum Login");
        txtUserName.setVisibility(View.VISIBLE);
        txtUserEmail.setText("");
        txtUserEmail.setVisibility(View.GONE);
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(View.VISIBLE);
    }

    private void logout() {
        String authToken = sharedPreferences.getString(AUTH_TOKEN_KEY, "");
        if (!authToken.isEmpty()) {
            logoutRegular(authToken);
        } else {
            logoutGoogle();
        }
    }

    private void logoutRegular(String token) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGOUT_URL,
                response -> {
                    clearAuthData();
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
            clearAuthData();
            navigateToMainActivity();
            Toast.makeText(IndexPendaftaranLogin.this, "Google logout berhasil", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearAuthData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(AUTH_TOKEN_KEY);

        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "");
        if (!userEmail.isEmpty()) {
            intent.putExtra("userEmail", userEmail);
        }
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
    }
}