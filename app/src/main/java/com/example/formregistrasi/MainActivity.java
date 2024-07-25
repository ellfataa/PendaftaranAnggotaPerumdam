package com.example.formregistrasi;

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

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
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
    private EditText et_userAkun, et_passwordAkun;

    private static final String URL_LOGIN = "http://192.168.230.122/pendaftaranPerumdam/masukAkun.php";

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.logo);
        txt_masuk = findViewById(R.id.txt_masuk);
        btn_masuk = findViewById(R.id.btn_masuk);
        daftarText = findViewById(R.id.daftarText);
        et_userAkun = findViewById(R.id.userName);
        et_passwordAkun = findViewById(R.id.password);
        google_btn = findViewById(R.id.google_btn);

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
                .requestId()
                .requestProfile()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            handleSignInResult(account);
        }

        google_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private void checkRegistrationStatus(String username, String nik) {
        SharedPreferences registrationPrefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
        boolean isRegistered = registrationPrefs.getBoolean(nik + "_registered", false);

        Intent intent = new Intent(MainActivity.this, IndexPendaftaranLogin.class);
        intent.putExtra("REGISTERED", isRegistered);
        intent.putExtra("NIK", nik);
        startActivity(intent);
        finish();
    }

    private boolean isInputValid() {
        String userAkun = et_userAkun.getText().toString().trim();
        String passwordAkun = et_passwordAkun.getText().toString().trim();

        if (userAkun.isEmpty() || passwordAkun.isEmpty()) {
            Toast.makeText(this, "Mohon melengkapi semua data", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void login() {
        final String userAkun = et_userAkun.getText().toString().trim();
        final String passwordAkun = et_passwordAkun.getText().toString().trim();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_LOGIN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String status = jsonObject.optString("status", "");

                            if(status.equals("success")){
                                String username = jsonObject.optString("username", "");
                                String nik = jsonObject.optString("nik", "");

                                // Periksa apakah username atau NIK kosong
                                if (username.isEmpty() && nik.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "Data tidak lengkap dari server", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Incomplete data from server: username and NIK are missing");
                                    return;
                                }

                                // Jika salah satu kosong, gunakan yang lain
                                if (username.isEmpty()) username = userAkun;
                                if (nik.isEmpty()) nik = "DEFAULT_NIK";

                                Toast.makeText(MainActivity.this, "Login berhasil", Toast.LENGTH_SHORT).show();

                                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("username", username);
                                editor.putString("NIK", nik);
                                editor.apply();

                                checkRegistrationStatus(username, nik);
                            } else {
                                String message = jsonObject.optString("message", "Terjadi kesalahan yang tidak diketahui");
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Login failed: " + message);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "JSON Error: " + e.toString());
                            Log.e(TAG, "Response causing error: " + response);
                            Toast.makeText(MainActivity.this, "Terjadi kesalahan dalam memproses data. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley Error: " + error.toString());
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null && networkResponse.data != null) {
                            String jsonError = new String(networkResponse.data);
                            Log.e(TAG, "Error Response: " + jsonError);
                        }
                        Toast.makeText(MainActivity.this, "Gagal terhubung ke server. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userAkun", userAkun);
                params.put("passwordAkun", passwordAkun);
                return params;
            }
        };

        // Tambahkan timeout yang lebih lama
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 detik timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
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

    private String getNikFromServer(String email) {
        // Implementasi untuk mendapatkan NIK dari server berdasarkan email
        // Untuk sementara, kita bisa menggunakan nilai default atau placeholder
        return "DEFAULT_NIK";
    }

    private void handleSignInResult(GoogleSignInAccount account) {
        if (account != null) {
            String personName = account.getDisplayName();
            String personEmail = account.getEmail();

            if (personName == null) personName = "Nama tidak tersedia";
            if (personEmail == null) personEmail = "Email tidak tersedia";

            String nik = getNikFromServer(personEmail);

            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", personName);
            editor.putString("email", personEmail);
            editor.putString("NIK", nik);
            editor.apply();

            Toast.makeText(this, "Berhasil masuk sebagai: " + personName, Toast.LENGTH_SHORT).show();

            checkRegistrationStatus(personName, nik);
        }
    }

    void navigateToSecondActivity(){
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            String nik = getNikFromServer(account.getEmail());
            checkRegistrationStatus(account.getDisplayName(), nik);
        }
    }
}