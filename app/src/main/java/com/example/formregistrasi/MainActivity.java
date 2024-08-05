package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.android.volley.Response;
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
    private ImageView logo, google_btn;
    private TextView txt_masuk, daftarText;
    private Button btn_masuk;
    private EditText et_emailAkun, et_passwordAkun;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";

    private static final String URL_LOGIN = "http://192.168.230.84/registrasi-pelanggan/public/api/login";

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.logo);
        txt_masuk = findViewById(R.id.txt_masuk);
        btn_masuk = findViewById(R.id.btn_masuk);
        daftarText = findViewById(R.id.daftarText);
        et_emailAkun = findViewById(R.id.emailAkun);
        et_passwordAkun = findViewById(R.id.password);
        google_btn = findViewById(R.id.google_btn);

        // Cek apakah ada data registrasi dari BuatUser
        Intent intent = getIntent();
        if (intent.hasExtra("email") && intent.hasExtra("password")) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            et_emailAkun.setText(email);
            et_passwordAkun.setText(password);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang masuk...");
        progressDialog.setCancelable(false);

        daftarText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BuatUser.class);
                startActivity(intent);
            }
        });

        btn_masuk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInputValid()) {
                    login();
                }
            }
        });

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        google_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private boolean isInputValid() {
        String emailAkun = et_emailAkun.getText().toString().trim();
        String passwordAkun = et_passwordAkun.getText().toString().trim();

        if (emailAkun.isEmpty() || passwordAkun.isEmpty()) {
            Toast.makeText(this, "Mohon melengkapi semua data", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void login() {
        final String emailAkun = et_emailAkun.getText().toString().trim();
        final String passwordAkun = et_passwordAkun.getText().toString().trim();

        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_LOGIN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                                // Login berhasil
                                JSONObject userObject = jsonObject.getJSONObject("user");
                                String email = userObject.getString("email");
                                String name = userObject.getString("name");
                                String token = jsonObject.getString("token");

                                // Simpan token dan informasi user
                                saveUserInfo(email, name, token);

                                // Tampilkan dialog sukses
                                showSuccessDialog(name);
                            } else {
                                // Login gagal
                                String message = jsonObject.optString("message", "Login gagal. Silakan coba lagi.");
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "JSON Error: " + e.toString());
                            Toast.makeText(MainActivity.this, "Terjadi kesalahan dalam memproses data. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Volley Error: " + error.toString());
                        String errorMessage = "Gagal terhubung ke server. ";
                        if (error.networkResponse != null) {
                            errorMessage += "Status code: " + error.networkResponse.statusCode;
                        }
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", emailAkun);
                params.put("password", passwordAkun);
                return params;
            }
        };

        // Tetapkan retry policy
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Tambahkan request ke antrian
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void saveUserInfo(String email, String name, String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("name", name);
        editor.putString("token", token);
        editor.apply();
    }

    private void showSuccessDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle("Login Berhasil")
                .setMessage("Selamat datang, " + name + "!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cek status registrasi
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        boolean hasRegistered = prefs.getBoolean(HAS_REGISTERED_KEY, false);

                        // Pindah ke halaman berikutnya
                        Intent intent = new Intent(MainActivity.this, IndexPendaftaranLogin.class);
                        intent.putExtra("hasRegistered", hasRegistered);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    void signIn(){
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
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

    private void handleSignInResult(GoogleSignInAccount account) {
        if (account != null) {
            String personEmail = account.getEmail();
            if (personEmail == null) personEmail = "Email tidak tersedia";
            showSuccessDialog(personEmail);
        }
    }
}