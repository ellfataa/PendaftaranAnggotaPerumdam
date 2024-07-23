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
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class IndexPendaftaranLogin extends AppCompatActivity {

    private Button btn_login, btn_registrasi;
    private TextView akun, txtIndexPelanggan;
    private SharedPreferences sharedPreferences;

    private static final String REGISTER_URL = "http://192.168.230.122/pendaftaranPerumdam/indexPelangganLogin.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index_pendaftaran_login);

        btn_login = findViewById(R.id.btn_login);
        btn_registrasi = findViewById(R.id.btn_registrasi);
        akun = findViewById(R.id.akun);
        txtIndexPelanggan = findViewById(R.id.txtIndexPelanggan);

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(IndexPendaftaranLogin.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btn_registrasi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(IndexPendaftaranLogin.this, RegistrasiActivity.class);
                startActivity(intent);
            }
        });

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        String username = sharedPreferences.getString("username", "");
        Log.d("SharedPreferences", "Username: " + username);
        if (!username.isEmpty()) {
            getUserAkun(username);
        } else {
            akun.setText("Akun: Belum Login");
        }
        // Semua elemen UI tetap ditampilkan
        btn_login.setVisibility(View.VISIBLE);
        btn_registrasi.setVisibility(View.VISIBLE);
        txtIndexPelanggan.setVisibility(View.VISIBLE);
        // txtIndexPelanggan tidak berubah
    }

    private void getUserAkun(String username) {
        String url = REGISTER_URL + "?username=" + username;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("ServerResponse", response.toString());
                        try {
                            String status = response.getString("status");
                            if (status.equals("success")) {
                                String userAkun = response.getString("userAkun");
                                akun.setText("" + userAkun);
                                Log.d("UserAkun", "UserAkun set to: " + userAkun);
                            } else {
                                String message = response.getString("message");
                                Log.e("ServerError", "Error: " + message);
                                Toast.makeText(IndexPendaftaranLogin.this, "Gagal mengambil data akun: " + message, Toast.LENGTH_SHORT).show();
                                akun.setText("Akun: Gagal mengambil data");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("JSONError", "Error parsing JSON: " + e.getMessage());
                            Toast.makeText(IndexPendaftaranLogin.this, "Error parsing JSON", Toast.LENGTH_SHORT).show();
                            akun.setText("Akun: Error");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VolleyError", "Error: " + error.getMessage());
                        Toast.makeText(IndexPendaftaranLogin.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        akun.setText("Akun: Error koneksi");
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }
}