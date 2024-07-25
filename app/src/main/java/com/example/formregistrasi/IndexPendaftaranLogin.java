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
    private Button btn_status, btn_registrasi, btn_keluar;
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
        updateUI();
    }

    private void initializeViews() {
        btn_status = findViewById(R.id.btn_status);
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
        btn_status.setOnClickListener(v -> navigateToStatus());
        btn_registrasi.setOnClickListener(v -> navigateToRegistration());
        btn_keluar.setOnClickListener(v -> signOut());
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
        String nik = sharedPreferences.getString("NIK", "");
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        if (acct != null) {
            updateUIForGoogleAccount(acct, nik);
        } else if (!username.isEmpty()) {
            updateUIForRegularAccount(username, nik);
        } else {
            updateUIForNoLogin();
        }

        txtIndexPelanggan.setVisibility(View.VISIBLE);
        updateStatusButtonListener(nik);
    }

    private void updateUIForGoogleAccount(GoogleSignInAccount acct, String nik) {
        String personName = acct.getDisplayName();
        akun.setText(personName != null ? personName : "Nama tidak tersedia");
        updateButtonVisibility(true, cekStatusRegistrasi(nik));
    }

    private void updateUIForRegularAccount(String username, String nik) {
        getUserAkun(username);
        updateButtonVisibility(true, cekStatusRegistrasi(nik));
    }

    private void updateUIForNoLogin() {
        akun.setText("Akun: Belum Login");
        updateButtonVisibility(false, false);
    }

    private void updateStatusButtonListener(final String nik) {
        btn_status.setOnClickListener(v -> navigateToStatus(nik));
    }

    private void updateButtonVisibility(boolean isLoggedIn, boolean hasRegistered) {
        btn_keluar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        btn_status.setVisibility(isLoggedIn && hasRegistered ? View.VISIBLE : View.GONE);
        btn_registrasi.setVisibility(isLoggedIn && !hasRegistered ? View.VISIBLE : View.GONE);
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
        gsc.signOut().addOnCompleteListener(task -> {
            clearLoginData();
            Toast.makeText(IndexPendaftaranLogin.this, "Logout berhasil", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        });
    }

    private void clearLoginData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToStatus() {
        String nik = sharedPreferences.getString("NIK", "");
        navigateToStatus(nik);
    }

    private void navigateToStatus(String nik) {
        Intent intent = new Intent(IndexPendaftaranLogin.this, Status.class);
        intent.putExtra("NIK", nik);
        startActivity(intent);
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
        startActivity(intent);
    }
}