package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "UserInfo";
    private static final String EMAIL_KEY = "email";
    private static final String TOKEN_KEY = "token";
    private static final String NAME_KEY = "name";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";
    private static final String URL_LOGIN = "http://192.168.230.84/registrasi-pelanggan/public/api/login";
    private static final int RC_SIGN_IN = 1000;

    private SessionManager sessionManager;
    private ImageView logo, google_btn;
    private TextView txt_masuk, daftarText;
    private Button btn_masuk;
    private EditText et_emailAkun, et_passwordAkun;

    private GoogleSignInOptions gso;
    private GoogleSignInClient gsc;
    private ProgressDialog progressDialog;

    // Method ini dipanggil waktu activity dibuat
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isLoggedIn()) {
            goToIndexPendaftaranLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        sessionManager = new SessionManager(this);

        initializeViews();
        setupProgressDialog();
        checkForRegistrationData();
        setupClickListeners();
        setupGoogleSignIn();
    }

    // Buat inisialisasi semua view yang ada di layout
    private void initializeViews() {
        logo = findViewById(R.id.logo);
        txt_masuk = findViewById(R.id.txt_masuk);
        btn_masuk = findViewById(R.id.btn_masuk);
        daftarText = findViewById(R.id.daftarText);
        et_emailAkun = findViewById(R.id.emailAkun);
        et_passwordAkun = findViewById(R.id.password);
        google_btn = findViewById(R.id.google_btn);
    }

    // Buat set up dialog loading
    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang masuk...");
        progressDialog.setCancelable(false);
    }

    // Ngecek kalo ada data registrasi yang dikirim dari activity lain
    private void checkForRegistrationData() {
        Intent intent = getIntent();
        if (intent.hasExtra("email") && intent.hasExtra("password")) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            et_emailAkun.setText(email);
            et_passwordAkun.setText(password);
        }
    }

    // Nambahin listener buat tombol-tombol yang bisa diklik
    private void setupClickListeners() {
        daftarText.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BuatUser.class)));
        btn_masuk.setOnClickListener(v -> { if (isInputValid()) login(); });
        google_btn.setOnClickListener(v -> signIn());
    }

    // Buat set up Google Sign In
    private void setupGoogleSignIn() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    // Ngecek apakah input udah bener atau belum
    private boolean isInputValid() {
        String emailAkun = et_emailAkun.getText().toString().trim();
        String passwordAkun = et_passwordAkun.getText().toString().trim();

        if (emailAkun.isEmpty() || passwordAkun.isEmpty()) {
            Toast.makeText(this, "Mohon melengkapi semua data", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Proses login ke server
    private void login() {
        final String emailAkun = et_emailAkun.getText().toString().trim();
        final String passwordAkun = et_passwordAkun.getText().toString().trim();

        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_LOGIN,
                this::handleLoginResponse,
                this::handleLoginError) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", emailAkun);
                params.put("password", passwordAkun);
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void handleLoginResponse(String response) {
        progressDialog.dismiss();
        Log.d(TAG, "Server Response: " + response);
        try {
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                JSONObject userObject = jsonObject.getJSONObject("user");
                String email = userObject.getString("email");
                String name = userObject.getString("name");
                String token = jsonObject.getString("token");

                // Simpan informasi user dan token
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(EMAIL_KEY, email);
                editor.putString(NAME_KEY, name);
                editor.putString(TOKEN_KEY, token);
                editor.apply();

                boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY + "_" + email, false);

                showSuccessDialog(name, hasRegistered);
            } else {
                String message = jsonObject.optString("message", "Login gagal. Silakan coba lagi.");
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON Error: " + e.toString());
            Toast.makeText(MainActivity.this, "Terjadi kesalahan dalam memproses data. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
        }
    }

    // Nangani error waktu login
    private void handleLoginError(VolleyError error) {
        progressDialog.dismiss();
        Log.e(TAG, "Volley Error: " + error.toString());
        String errorMessage;

        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
            errorMessage = "Password salah atau tidak sesuai";
            showErrorDialog(errorMessage);
        } else {
            errorMessage = "Gagal terhubung ke server. ";
            if (error.networkResponse != null) {
                errorMessage += "Status code: " + error.networkResponse.statusCode;
                if (error.networkResponse.data != null) {
                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        JSONObject jsonObject = new JSONObject(responseBody);
                        errorMessage += "\n" + jsonObject.optString("message", "Unknown error occurred");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Nyimpen info user ke SharedPreferences
    private void saveUserInfo(String email, String name, String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(EMAIL_KEY, email);
        editor.putString(NAME_KEY, name);
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

    // Nampilin dialog kalo login berhasil
    private void showSuccessDialog(String name, boolean hasRegistered) {
        new AlertDialog.Builder(this)
                .setTitle("Login Berhasil")
                .setMessage("Selamat datang, " + name + "!")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, IndexPendaftaranLogin.class);
                    intent.putExtra("hasRegistered", hasRegistered);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    // Mulai proses sign in pake Google
    private void signIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Nangani hasil dari Google Sign In
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                handleSignInResult(account);
            } catch (ApiException e) {
                Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
                Toast.makeText(getApplicationContext(), "Sign in gagal: " + e.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Nangani hasil sign in dari Google
    private void handleSignInResult(GoogleSignInAccount account) {
        if (account != null) {
            String personEmail = account.getEmail();
            String personName = account.getDisplayName();
            if (personEmail == null) personEmail = "Email tidak tersedia";
            if (personName == null) personName = "Nama tidak tersedia";

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString(EMAIL_KEY, personEmail);
            editor.putString(NAME_KEY, personName);
            editor.apply();

            saveUserInfo(personEmail, personName, "");  // Token kosong untuk Google Sign In

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY + "_" + personEmail, false);
            showSuccessDialog(personName, hasRegistered);
        }
    }

    // Ngecek apakah user udah login atau belum
    private boolean isLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = sharedPreferences.getString(TOKEN_KEY, "");
        return !token.isEmpty();
    }

    // Pindah ke halaman IndexPendaftaranLogin
    private void goToIndexPendaftaranLogin() {
        Intent intent = new Intent(MainActivity.this, IndexPendaftaranLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}