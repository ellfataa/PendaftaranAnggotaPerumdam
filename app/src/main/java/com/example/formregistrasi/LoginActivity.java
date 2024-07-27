package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    EditText etNama, etNik;
    Button btn_status;
    ProgressDialog progressDialog;

    // Fungsi buat activity pertama kali dibuat (inisiasi awal)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNama = findViewById(R.id.etNama);
        etNik = findViewById(R.id.etNik);
        btn_status = findViewById(R.id.btn_status);
        progressDialog = new ProgressDialog(LoginActivity.this);

        btn_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nama = etNama.getText().toString().trim();
                String nik = etNik.getText().toString().trim();

                if (nama.isEmpty() || nik.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Nama dan NIK harus diisi", Toast.LENGTH_SHORT).show();
                } else {
                    checkLogin(nama, nik);
                }
            }
        });

        // Tambahkan listener untuk btn_status
        btn_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nik = etNik.getText().toString().trim();
                if (!nik.isEmpty()) {
                    navigateToStatus(nik);
                } else {
                    Toast.makeText(getApplicationContext(), "NIK harus diisi untuk melihat status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Fungsi buat ngecek login ke server
    public void checkLogin(final String nama, final String nik) {
        if (checkNetworkConnection()) {
            progressDialog.setMessage("Sedang login...");
            progressDialog.show();
            StringRequest stringRequest = new StringRequest(Request.Method.POST, DbContract.SERVER_LOGIN_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                JSONArray serverResponse = jsonObject.getJSONArray("server_response");
                                JSONObject obj = serverResponse.getJSONObject(0);
                                String status = obj.getString("status");

                                if (status.equals("OK")) {
                                    Toast.makeText(getApplicationContext(), "Login berhasil", Toast.LENGTH_SHORT).show();
                                    navigateToStatus(nik);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Login gagal. Periksa nama dan NIK Anda.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), "Kesalahan JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            progressDialog.dismiss();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Kesalahan: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("nama", nama);
                    params.put("nik", nik);
                    return params;
                }
            };

            VolleyConnection.getInstance(LoginActivity.this).addToRequestQue(stringRequest);
        } else {
            Toast.makeText(getApplicationContext(), "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
        }
    }

    // Fungsi buat pindah ke halaman status apabila login berhasil
    private void navigateToStatus(String nik) {
        Intent intent = new Intent(LoginActivity.this, Status.class);
        intent.putExtra("NIK", nik);
        startActivity(intent);
    }

    // Fungsi buat memeriksa koneksi internet
    public boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    // Fungsi buat balik ke halaman IndexPendaftaranLogin
    public void btnKembali(View view) {
        Intent intent = new Intent(LoginActivity.this, IndexPendaftaranLogin.class);
        startActivity(intent);
    }
}