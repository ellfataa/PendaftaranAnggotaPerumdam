package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Status extends AppCompatActivity {

    private static final String TAG = "Status";
    private static final String BASE_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/register-detail";
    private static final String PREFS_NAME = "UserInfo";
    private static final String AUTH_TOKEN_KEY = "token";
    private static final String USER_EMAIL_KEY = "email";
    private static final String NAME_KEY = "name";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";

    private SessionManager sessionManager;
    private TextView tvNama, tvNik, tvPekerjaan, tvTelp, tvAlamat, tvRt, tvRw, tvKelurahan, tvKecamatan,
            tvKodePos, tvJumlahPenghuni, tvLatitude, tvLongitude, tvBiaya, tvStatus, tvToken, tvStatusBiaya;
    private Button btnKembali, btnBuktiBayar;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        sessionManager = new SessionManager(this);

        initializeViews();

        String nomorKtp = getIntent().getStringExtra("NOMOR_KTP");
        if (nomorKtp != null && !nomorKtp.isEmpty()) {
            fetchRegistrationDetails(nomorKtp);
        } else {
            Toast.makeText(this, "Nomor KTP tidak ditemukan", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        tvNama = findViewById(R.id.tvNama);
        tvNik = findViewById(R.id.tvNik);
        tvPekerjaan = findViewById(R.id.tvPekerjaan);
        tvTelp = findViewById(R.id.tvTelp);
        tvAlamat = findViewById(R.id.tvAlamat);
        tvRt = findViewById(R.id.tvRt);
        tvRw = findViewById(R.id.tvRw);
        tvKelurahan = findViewById(R.id.tvKelurahan);
        tvKecamatan = findViewById(R.id.tvKecamatan);
        tvKodePos = findViewById(R.id.tvKodePos);
        tvJumlahPenghuni = findViewById(R.id.tvJumlahPenghuni);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvBiaya = findViewById(R.id.tvBiaya);
        tvToken = findViewById(R.id.tvToken);
        tvStatusBiaya = findViewById(R.id.tvStatusBiaya);
        tvStatus = findViewById(R.id.tvStatus);
        btnBuktiBayar = findViewById(R.id.btnBuktiBayar);
        btnKembali = findViewById(R.id.btnKembali);
        progressBar = findViewById(R.id.progressBar);

        btnBuktiBayar.setOnClickListener(v -> {
            Intent intent = new Intent(Status.this, UploadBuktiPembayaran.class);
            intent.putExtra("NOMOR_KTP", tvNik.getText().toString().replace("NIK: ", ""));
            startActivity(intent);
        });

        btnKembali.setOnClickListener(v -> {
            Intent intent = new Intent(Status.this, IndexPendaftaranLogin.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchRegistrationDetails(String nomorKtp) {
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final String token = prefs.getString(AUTH_TOKEN_KEY, "");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, BASE_URL,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Full API Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            updateUI(data);
                        } else {
                            String message = jsonResponse.optString("message", "Failed to fetch data");
                            Toast.makeText(Status.this, message, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(Status.this, "Error parsing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error: " + error.toString());
                    String errorMessage = "Error fetching data";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status " + error.networkResponse.statusCode + ")";
                    }
                    Toast.makeText(Status.this, errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                if (!token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Accept", "application/json");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("nik", nomorKtp);
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

    @Override
    protected void onResume() {
        super.onResume();
        String nomorKtp = getIntent().getStringExtra("NOMOR_KTP");
        String newStatus = getIntent().getStringExtra("STATUS_PEMBAYARAN");

        if (nomorKtp != null && !nomorKtp.isEmpty()) {
            if (newStatus != null && newStatus.equals("ditinjau")) {
                updateUIForReview();
            }
            // Always fetch the latest details to ensure we have the most up-to-date information
            fetchRegistrationDetails(nomorKtp);
        }
    }

    private void updateUIForReview() {
        tvStatus.setText("Status: upload bukti bayar");
        tvStatusBiaya.setText("Status Pembayaran: Pembayaran masih ditinjau/dicek");
        btnBuktiBayar.setVisibility(View.GONE);
    }

    private void updateUI(JSONObject data) {
        try {
            JSONObject registrasi = data.getJSONObject("registrasi");

            tvNama.setText("Nama: " + registrasi.optString("nama", "N/A"));
            tvNik.setText("NIK: " + registrasi.optString("nomor_ktp", "N/A"));
            tvPekerjaan.setText("Pekerjaan: " + registrasi.optString("pekerjaan", "N/A"));
            tvTelp.setText("No. Telp: " + registrasi.optString("telp_hp", "N/A"));
            tvAlamat.setText("Alamat: " + registrasi.optString("alamat", "N/A"));
            tvRt.setText("RT: " + registrasi.optString("rt", "N/A"));
            tvRw.setText("RW: " + registrasi.optString("rw", "N/A"));
            tvKelurahan.setText("Kelurahan: " + registrasi.optString("kelurahan", "N/A"));
            tvKecamatan.setText("Kecamatan: " + registrasi.optString("kecamatan", "N/A"));
            tvKodePos.setText("Kode Pos: " + registrasi.optString("kode_pos", "N/A"));
            tvJumlahPenghuni.setText("Jumlah Penghuni: " + registrasi.optString("jumlah_penghuni", "N/A"));
            tvLatitude.setText("Latitude: " + registrasi.optString("latitude", "N/A"));
            tvLongitude.setText("Longitude: " + registrasi.optString("longitude", "N/A"));
            tvBiaya.setText("Biaya Registrasi: Rp " + registrasi.optString("biaya_registrasi", "N/A"));

            String statusPembayaran = registrasi.optString("status_pembayaran", "").toLowerCase();
            if (statusPembayaran.equals("ditinjau")) {
                updateUIForReview();
            } else if (statusPembayaran.isEmpty() || statusPembayaran.equals("belum dibayar")) {
                tvStatusBiaya.setText("Status Pembayaran: Pembayaran Belum dilakukan");
                btnBuktiBayar.setVisibility(View.VISIBLE);
            } else {
                tvStatusBiaya.setText("Status Pembayaran: " + statusPembayaran);
                btnBuktiBayar.setVisibility(View.GONE);
            }

            if (statusPembayaran.equals("lunas")) {
                String tokenPembayaran = registrasi.optString("token_pembayaran", "N/A");
                tvToken.setText("Token Pembayaran: " + tokenPembayaran);
                tvToken.setVisibility(View.VISIBLE);
            } else {
                tvToken.setVisibility(View.GONE);
            }

            JSONArray statusHistory = data.getJSONArray("status_history");
            if (statusHistory.length() > 0) {
                JSONObject latestStatus = statusHistory.getJSONObject(0);
                tvStatus.setText("Status: " + latestStatus.optString("status", "N/A"));
            } else {
                tvStatus.setText("Status: N/A");
            }

            Log.d(TAG, "Data updated successfully");

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON in updateUI: " + e.getMessage());
            e.printStackTrace();

            // Set all fields to N/A in case of error
            tvNama.setText("Nama: N/A");
            tvNik.setText("NIK: N/A");
            tvPekerjaan.setText("Pekerjaan: N/A");
            tvTelp.setText("No. Telp: N/A");
            tvAlamat.setText("Alamat: N/A");
            tvRt.setText("RT: N/A");
            tvRw.setText("RW: N/A");
            tvKelurahan.setText("Kelurahan: N/A");
            tvKecamatan.setText("Kecamatan: N/A");
            tvKodePos.setText("Kode Pos: N/A");
            tvJumlahPenghuni.setText("Jumlah Penghuni: N/A");
            tvLatitude.setText("Latitude: N/A");
            tvLongitude.setText("Longitude: N/A");
            tvBiaya.setText("Biaya Registrasi: N/A");
            tvStatusBiaya.setText("Status Pembayaran: N/A");
            tvStatus.setText("Status: N/A");
            tvToken.setVisibility(View.GONE);

            Toast.makeText(this, "Error mengupdate data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void btnKembali(View view) {
        Intent intent = new Intent(Status.this, IndexPendaftaranLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}