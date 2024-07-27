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

    // Fungsi ini dipanggil pas activity pertama kali dibuat (inisiasi)
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

    // Fungsi buat menyiapkan semua view yang ada di layout (untuk get id)
    private void initializeViews() {
        btn_login = findViewById(R.id.btn_login);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        btn_keluar = findViewById(R.id.btn_keluar);
        akun = findViewById(R.id.akun);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);
    }

    // Fungsi buat menyiapkan shared preferences
    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
    }

    // Fungsi buat menyiapkan Google Sign In/mendapatkan informasi akun apabila user login dari Google
    private void setupGoogleSignIn() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    // Fungsi buat memberikan aksi ke tombol-tombol
    private void setupButtonListeners() {
        btn_login.setOnClickListener(v -> navigateToLogin());
        btn_registrasi.setOnClickListener(v -> navigateToRegistration());
        btn_keluar.setOnClickListener(v -> signOut());
    }

    // Fungsi buat pindah ke halaman loginActivity
    private void navigateToLogin() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, LoginActivity.class);
        startActivity(intent);
    }

    // Fungsi buat ngurus status registrasiActivity
    private void handleRegistrationStatus() {
        boolean registeredFromStatus = getIntent().getBooleanExtra("REGISTERED", false);
        if (registeredFromStatus) {
            String nik = getIntent().getStringExtra("NIK");
            updateSharedPreferences(nik);
        }
    }

    // Fungsi buat update shared preferences
    private void updateSharedPreferences(String nik) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("hasRegistered", true);
        editor.putString("NIK", nik);
        editor.apply();
    }

    // Fungsi yang dipanggil pas activity balik lagi ke halaman ini, sehingga informasi user yang dari awal tidak hilang/destroy
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    // Fungsi buat update tampilan UI
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

    // Fungsi buat update UI apabila menggunakan akun Google
    private void updateUIForGoogleAccount(GoogleSignInAccount acct) {
        String personName = acct.getDisplayName();
        akun.setText(personName != null ? personName : "Nama tidak tersedia");
        updateButtonVisibility();
    }

    // Fungsi buat update UI apabila menggunakan akun biasa
    private void updateUIForRegularAccount(String username) {
        akun.setText(username);
        updateButtonVisibility();
    }

    // Fungsi buat update UI apabila tidak login
    private void updateUIForNoLogin() {
        akun.setText("Akun: Belum Login");
        updateButtonVisibility();
    }

    // Fungsi buat ngatur tombol mana yang keliatan
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

    // Fungsi buat ngambil data akun user dari server
    private void getUserAkun(String username) {
        String url = REGISTER_URL + "?username=" + username;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                this::handleUserAkunResponse,
                this::handleUserAkunError);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    // Fungsi buat menangani respon dari server pas ngambil data akun
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

    // Fungsi buat menangani error ketika ngambil data akun (versi String)
    private void handleUserAkunError(String errorMessage) {
        Log.e(TAG, "Error: " + errorMessage);
        Toast.makeText(this, "Gagal mengambil data akun: " + errorMessage, Toast.LENGTH_SHORT).show();
        akun.setText("Akun: Gagal mengambil data");
    }

    // Fungsi buat menangani error ketika ngambil data akun (versi VolleyError)
    private void handleUserAkunError(VolleyError error) {
        Log.e(TAG, "Volley Error: " + error.getMessage());
        if (error.networkResponse != null) {
            Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
        }
        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        akun.setText("Akun: Error koneksi");
    }

    // Fungsi buat ngecek apa user udah pernah daftar apa belom
    private boolean cekStatusRegistrasi(String nik) {
        SharedPreferences registrationPrefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
        return registrationPrefs.getBoolean(nik + "_registered", false);
    }

    // Fungsi buat sign out dari akun
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

    // Fungsi buat ngehapus data login dari shared preferences. tujuannya apabila user sign out maka harus login kembali
    private void clearLoginData() {
        SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor loginEditor = loginPrefs.edit();
        loginEditor.clear();
        loginEditor.apply();
    }

    // Fungsi buat balik ke halaman MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Fungsi buat pindah ke halaman registrasiActivity
    private void navigateToRegistration() {
        Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
        startActivity(intent);
    }
}