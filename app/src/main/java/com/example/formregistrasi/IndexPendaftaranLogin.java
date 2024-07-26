package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class IndexPendaftaranLogin extends AppCompatActivity {

    private static final String TAG = "IndexPendaftaranLogin";
    private Button btn_login, btn_registrasi, btn_keluar;
    private TextView akun, txtIndexPelanggan;
    private SharedPreferences sharedPreferences;

    private static final String REGISTER_URL = "http://192.168.230.122/pendaftaranPerumdam/indexPelangganLogin.php";

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        initializeViews();
        setupSharedPreferences();
        setupGoogleSignIn();
        setupButtonListeners();
        handleRegistrationStatus();

        boolean isRegistered = getIntent().getBooleanExtra("REGISTERED", false);
        String nik = getIntent().getStringExtra("NIK");

        if (isRegistered) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(nik + "_registered", true);
            editor.apply();
        }

        updateUI();
    }

    private void initializeViews() {
        btn_login = findViewById(R.id.btn_login);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        btn_keluar = findViewById(R.id.btn_keluar);
        akun = findViewById(R.id.akun);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
    }

    private void setupGoogleSignIn() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    private void setupButtonListeners() {
        btn_login.setOnClickListener(v -> navigateToLogin());
        btn_registrasi.setOnClickListener(v -> navigateToRegistration());
        btn_keluar.setOnClickListener(v -> signOut());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, LoginActivity.class);
        startActivity(intent);
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

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        String username = sharedPreferences.getString("username", "");
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        if (acct != null) {
            updateUIForGoogleAccount(acct);
        } else if (!username.isEmpty()) {
            updateUIForRegularAccount(username);
        } else {
            updateUIForNoLogin();
        }

        txtIndexPelanggan.setVisibility(View.VISIBLE);
        updateButtonVisibility();
    }

    private void updateUIForGoogleAccount(GoogleSignInAccount acct) {
        String personName = acct.getDisplayName();
        akun.setText(personName != null ? personName : "Nama tidak tersedia");
        updateButtonVisibility();
    }

    private void updateUIForRegularAccount(String username) {
        akun.setText(username);
        updateButtonVisibility();
    }

    private void updateUIForNoLogin() {
        akun.setText("Akun: Belum Login");
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean isLoggedIn = !sharedPreferences.getString("username", "").isEmpty() ||
                GoogleSignIn.getLastSignedInAccount(this) != null;

        btn_keluar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        btn_login.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(View.VISIBLE);

        Log.d("IndexPendaftaranLogin", "Button visibility updated: login " +
                (btn_login.getVisibility() == View.VISIBLE ? "visible" : "gone") +
                ", registrasi " + (btn_registrasi.getVisibility() == View.VISIBLE ? "visible" : "gone"));
    }

    private void getUserAkun(String username) {
        String url = REGISTER_URL + "?username=" + username;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                this::handleUserAkunResponse,
                this::handleUserAkunError);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void handleUserAkunResponse(JSONObject response) {
        Log.d(TAG, "Server Response: " + response.toString());
        try {
            String status = response.getString("status");
            if (status.equals("success")) {
                String userAkun = response.getString("userAkun");
                akun.setText(userAkun);
                Log.d(TAG, "UserAkun set to: " + userAkun);
            } else {
                String message = response.getString("message");
                handleUserAkunError(message);
            }
        } catch (JSONException e) {
            handleUserAkunError(e.getMessage());
        }
    }

    private void handleUserAkunError(String errorMessage) {
        Log.e(TAG, "Error: " + errorMessage);
        Toast.makeText(this, "Gagal mengambil data akun: " + errorMessage, Toast.LENGTH_SHORT).show();
        akun.setText("Akun: Gagal mengambil data");
    }

    private void handleUserAkunError(VolleyError error) {
        Log.e(TAG, "Volley Error: " + error.getMessage());
        if (error.networkResponse != null) {
            Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
        }
        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        akun.setText("Akun: Error koneksi");
    }

    private boolean cekStatusRegistrasi(String nik) {
        SharedPreferences registrationPrefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
        return registrationPrefs.getBoolean(nik + "_registered", false);
    }

    private void signOut() {
        if (gsc != null) {
            gsc.signOut().addOnCompleteListener(task -> {
                clearLoginData();
                Toast.makeText(IndexPendaftaranLogin.this, "Logout berhasil", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            });
        } else {
            clearLoginData();
            Toast.makeText(IndexPendaftaranLogin.this, "Logout berhasil", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        }
    }

    private void clearLoginData() {
        SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor loginEditor = loginPrefs.edit();
        loginEditor.clear();
        loginEditor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
        startActivity(intent);
    }
}