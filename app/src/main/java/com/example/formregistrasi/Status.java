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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class Status extends AppCompatActivity {

    private TextView tvStatus, tvNama, tvNik, tvAlamat, tvRt, tvRw, tvTelp, tvKodePos, tvJumlahPenghuni;
    private TextView tvPekerjaan, tvKelurahan, tvKecamatan, tvLatitude, tvLongitude;
    private Button btnKembali;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        initializeViews();

        String nik = getIntent().getStringExtra("NIK");
        if (nik != null && !nik.isEmpty()) {
            fetchStatusData(nik);
        } else {
            Toast.makeText(this, "NIK tidak ditemukan", Toast.LENGTH_SHORT).show();
            Log.e("Status", "NIK is null or empty");
        }

        btnKembali.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Status.this, IndexPendaftaranLogin.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initializeViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvNama = findViewById(R.id.tvNama);
        tvNik = findViewById(R.id.tvNik);
        tvAlamat = findViewById(R.id.tvAlamat);
        tvRt = findViewById(R.id.tvRt);
        tvRw = findViewById(R.id.tvRw);
        tvTelp = findViewById(R.id.tvTelp);
        tvKodePos = findViewById(R.id.tvKodePos);
        tvJumlahPenghuni = findViewById(R.id.tvJumlahPenghuni);
        tvPekerjaan = findViewById(R.id.tvPekerjaan);
        tvKelurahan = findViewById(R.id.tvKelurahan);
        tvKecamatan = findViewById(R.id.tvKecamatan);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        btnKembali = findViewById(R.id.btnKembali);
    }

    private void fetchStatusData(String nik) {
        String url = "http://192.168.230.122/pendaftaranPerumdam/status.php?nik=" + nik;
        Log.d("Status", "Fetching data from URL: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d("Status", "Response received: " + response.toString());
                    try {
                        if (response.getString("status").equals("success")) {
                            tvStatus.setText(response.getString("message"));
                            JSONObject data = response.getJSONObject("data");

                            tvNama.setText("Nama: " + data.optString("nama", "Tidak ada data"));
                            tvNik.setText("NIK: " + data.optString("nik", "Tidak ada data"));
                            tvAlamat.setText("Alamat: " + data.optString("alamat", "Tidak ada data"));
                            tvRt.setText("RT: " + data.optString("rt", "Tidak ada data"));
                            tvRw.setText("RW: " + data.optString("rw", "Tidak ada data"));
                            tvTelp.setText("No. Telp: " + data.optString("telp_hp", "Tidak ada data"));
                            tvKodePos.setText("Kode Pos: " + data.optString("kode_pos", "Tidak ada data"));
                            tvJumlahPenghuni.setText("Jumlah Penghuni: " + data.optString("jumlah_penghuni", "Tidak ada data"));
                            tvPekerjaan.setText("Pekerjaan: " + data.optString("pekerjaan", "Tidak ada data"));
                            tvKelurahan.setText("Kelurahan: " + data.optString("kelurahan", "Tidak ada data"));
                            tvKecamatan.setText("Kecamatan: " + data.optString("kecamatan", "Tidak ada data"));
                            tvLatitude.setText("Latitude: " + data.optString("latitude", "Tidak ada data"));
                            tvLongitude.setText("Longitude: " + data.optString("longitude", "Tidak ada data"));

                            // Simpan NIK kembali ke SharedPreferences
                            SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = loginPrefs.edit();
                            editor.putString("NIK", nik);
                            editor.apply();

                            // Tandai bahwa user telah terdaftar
                            SharedPreferences registrationPrefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor regEditor = registrationPrefs.edit();
                            regEditor.putBoolean(nik + "_registered", true);
                            regEditor.apply();

                            Log.d("Status", "Data successfully parsed and displayed");
                        } else {
                            tvStatus.setText(response.getString("message"));
                            Log.d("Status", "Status not success: " + response.getString("message"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("Status", "Error parsing JSON: " + e.getMessage());
                        Log.e("Status", "JSON content: " + response.toString());
                        Toast.makeText(Status.this, "Error parsing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("Status", "Error fetching data: " + error.toString());
                    Toast.makeText(Status.this, "Error fetching data: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });

        // Tambahkan timeout yang lebih lama
        jsonObjectRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, // 30 seconds timeout
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }
}