package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BuatUser extends AppCompatActivity {

    private ImageView logo;
    private TextView txt_buat, masukText;
    private EditText namaLengkap, userName, emailAkun, password;
    private Button btn_buat;

    private static final String REGISTER_URL = "http://192.168.230.122/pendaftaranPerumdam/buatAkun.php";
    private RequestQueue requestQueue;

    /**
     * Metode ini dipanggil saat aktivitas pertama kali dibuat.
     * Ini menginisialisasi UI, menyiapkan listener, dan mengatur filter input.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buat_user);

        requestQueue = Volley.newRequestQueue(this);

        logo = findViewById(R.id.logo);
        txt_buat = findViewById(R.id.txt_buat);
        namaLengkap = findViewById(R.id.namaLengkap);
        userName = findViewById(R.id.userName);
        emailAkun = findViewById(R.id.emailAkun);
        password = findViewById(R.id.password);
        btn_buat = findViewById(R.id.btn_buat);
        masukText = findViewById(R.id.masukText);

        namaLengkap.setFilters(new InputFilter[] {
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (!Character.isLetter(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                                return "";
                            }
                        }
                        return null;
                    }
                }
        });

        masukText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToMainActivity();
            }
        });

        btn_buat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    registerUser();
                }
            }
        });
    }

    /**
     * Memvalidasi input pengguna sebelum melakukan pendaftaran.
     * @return true jika semua input valid, false jika tidak.
     */
    private boolean validateInput() {
        String namaAkun = namaLengkap.getText().toString().trim();
        String userAkun = userName.getText().toString().trim();
        String email = emailAkun.getText().toString().trim();
        String passwordAkun = password.getText().toString().trim();

        if (namaAkun.isEmpty() || userAkun.isEmpty() || email.isEmpty() || passwordAkun.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailAkun.setError("Format email tidak valid");
            emailAkun.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Melakukan pendaftaran pengguna dengan mengirimkan data ke server.
     * Menampilkan dialog progress selama proses berlangsung.
     */
    private void registerUser() {
        final String namaAkun = namaLengkap.getText().toString().trim();
        final String userAkun = userName.getText().toString().trim();
        final String email = emailAkun.getText().toString().trim();
        final String passwordAkun = password.getText().toString().trim();

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mendaftarkan...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject serverResponse = jsonObject.getJSONObject("server_response");
                            String status = serverResponse.getString("status");
                            if (status.equals("OK")) {
                                Toast.makeText(BuatUser.this, "Buat akun berhasil", Toast.LENGTH_SHORT).show();
                                goToMainActivity();
                            } else {
                                Toast.makeText(BuatUser.this, "Buat akun gagal", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(BuatUser.this, "Terjadi kesalahan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Toast.makeText(BuatUser.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("namaAkun", namaAkun);
                params.put("userAkun", userAkun);
                params.put("emailAkun", email);
                params.put("passwordAkun", passwordAkun);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(BuatUser.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}