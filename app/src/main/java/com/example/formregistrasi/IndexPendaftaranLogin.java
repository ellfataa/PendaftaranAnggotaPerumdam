package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class IndexPendaftaranLogin extends AppCompatActivity {

    private static final String TAG = "IndexPendaftaranLogin";
    private Button btn_status, btn_registrasi, btn_keluar;
    private TextView txtIndexPelanggan, txtUserName, txtUserEmail;
    private SharedPreferences sharedPreferences;

    private static final String API_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/login";
    private static final String LOGOUT_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/logout";

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    private static final String PREFS_NAME = "UserInfo";
    private static final String AUTH_TOKEN_KEY = "token";

    private boolean isRegistered = false;
    private String userEmail = "";
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        initializeViews();
        setupSharedPreferences();
        setupGoogleSignIn();
        setupButtonListeners();

        // Ambil data dari intent
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("email");
        isRegistered = intent.getBooleanExtra("isRegistered", false);
        userId = intent.getIntExtra("userId", -1);

        // Selalu fetch data terbaru dari server
        fetchUserData();
    }

    private void loadUserDataFromSharedPreferences() {
        userEmail = sharedPreferences.getString("email", "");
        isRegistered = sharedPreferences.getBoolean("is_registered", false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data user setiap kali activity di-resume
        fetchUserData();
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

    private void fetchUserData() {
        String token = sharedPreferences.getString(AUTH_TOKEN_KEY, "");
        if (token.isEmpty()) {
            updateUIForLoggedOutUser();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, API_URL, null,
                response -> {
                    try {
                        JSONObject userObject = response.getJSONObject("user");
                        userEmail = userObject.getString("email");
                        isRegistered = userObject.getBoolean("is_registered");
                        userId = userObject.getInt("id");

                        updateUI();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        updateUIForLoggedOutUser();
                    }
                },
                error -> {
                    updateUIForLoggedOutUser();
                    // Tampilkan pesan error
                    Toast.makeText(this, "Gagal mengambil data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void updateUI() {
        if (userEmail != null && !userEmail.isEmpty()) {
            txtUserName.setText("Selamat datang!");
            txtUserName.setVisibility(View.VISIBLE);
            txtUserEmail.setText(userEmail);
            txtUserEmail.setVisibility(View.VISIBLE);
            btn_keluar.setVisibility(View.VISIBLE);
            btn_status.setVisibility(View.VISIBLE);
            btn_registrasi.setVisibility(isRegistered ? View.GONE : View.VISIBLE);

            // Tambahkan log untuk userId (opsional, untuk debugging)
            Log.d(TAG, "User ID: " + userId);
        } else {
            updateUIForLoggedOutUser();
        }
    }

    private void updateUIForLoggedInUser(String email, boolean isRegistered) {
        txtUserName.setText("Selamat datang!");
        txtUserName.setVisibility(View.VISIBLE);
        txtUserEmail.setText(email);
        txtUserEmail.setVisibility(View.VISIBLE);
        btn_keluar.setVisibility(View.VISIBLE);
        btn_status.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(isRegistered ? View.GONE : View.VISIBLE);
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

    private void checkStatusAccess() {
        if (!userEmail.isEmpty() && userId != -1) {
            if (isRegistered) {
                navigateToStatus(String.valueOf(userId));
            } else {
                showAlert("Anda belum melakukan registrasi");
            }
        } else {
            showAlert("Anda belum melakukan login. Silakan login terlebih dahulu.");
        }
    }

    private void navigateToStatus(String identifier) {
        Intent intent = new Intent(IndexPendaftaranLogin.this, Status.class);
        intent.putExtra("USER_IDENTIFIER", identifier);
        startActivity(intent);
    }

    private void checkRegistrationAccess() {
        if (!userEmail.isEmpty()) {
            if (isRegistered) {
                showAlert("Anda sudah melakukan registrasi sebelumnya.");
            } else {
                navigateToRegistration();
            }
        } else {
            showAlert("Anda belum melakukan login. Silakan login terlebih dahulu.");
        }
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
        intent.putExtra("userEmail", userEmail);
        startActivity(intent);
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Informasi")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void logout() {
        String token = sharedPreferences.getString(AUTH_TOKEN_KEY, "");
        if (!token.isEmpty()) {
            logoutFromServer(token);
        } else {
            // If no token, just clear local data and return to login screen
            clearAuthData();
            navigateToMainActivity();
        }
    }

    private void logoutFromServer(String token) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGOUT_URL,
                response -> {
                    clearAuthData();
                    navigateToMainActivity();
                    Toast.makeText(IndexPendaftaranLogin.this, "Logout berhasil", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Toast.makeText(IndexPendaftaranLogin.this, "Logout gagal: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    clearAuthData();
                    navigateToMainActivity();
                }
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

    private void clearAuthData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(AUTH_TOKEN_KEY);
        editor.apply();
        userEmail = "";
        isRegistered = false;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}