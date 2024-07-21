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
    Button btnLogin;
    ProgressDialog progressDialog;

    /**
     * Fungsi ini dipanggil saat activity dibuat.
     * Menginisialisasi tampilan dan menetapkan listener untuk tombol login.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNama = findViewById(R.id.etNama);
        etNik = findViewById(R.id.etNik);
        btnLogin = findViewById(R.id.btnLogin);
        progressDialog = new ProgressDialog(LoginActivity.this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
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
    }

    /**
     * Fungsi untuk melakukan proses login.
     * Mengirim permintaan ke server dan menangani respons.
     * @param nama Nama pengguna
     * @param nik Nomor Induk Kependudukan pengguna
     */
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
                                    Intent intent = new Intent(LoginActivity.this, Profil.class);
                                    intent.putExtra("nama", nama);
                                    intent.putExtra("nik", nik);
                                    startActivity(intent);
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

    /**
     * Fungsi untuk memeriksa koneksi jaringan.
     * @return true jika terhubung ke internet, false jika tidak
     */
    public boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Fungsi untuk kembali ke activity sebelumnya.
     * Dipanggil saat tombol kembali ditekan.
     * @param view View yang memicu fungsi ini
     */
    public void btnKembali(View view) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}