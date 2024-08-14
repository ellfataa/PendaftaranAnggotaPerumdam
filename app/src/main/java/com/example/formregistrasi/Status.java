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
    private static final String TOKEN_KEY = "token";
    private static final String EMAIL_KEY = "email";

    private SessionManager sessionManager;
    private TextView tvNama, tvNik, tvPekerjaan, tvTelp, tvAlamat, tvRt, tvRw, tvKelurahan, tvKecamatan,
            tvKodePos, tvJumlahPenghuni, tvLatitude, tvLongitude, tvBiaya, tvStatus, tvToken, tvStatusBiaya;
    private Button btnKembali, btnBuktiBayar;
    private ProgressBar progressBar;
    private boolean justRegistered = false;
    private boolean justUploaded = false;
    private boolean shouldRefreshData = false;

    // Method ini dipanggil saat activity dibuat
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        sessionManager = new SessionManager(this);

        initializeViews();

        justRegistered = getIntent().getBooleanExtra("JUST_REGISTERED", false);
        justUploaded = getIntent().getBooleanExtra("JUST_UPLOADED", false);
        shouldRefreshData = getIntent().getBooleanExtra("REFRESH_DATA", false);

        String nomorKtp = getIntent().getStringExtra("NOMOR_KTP");
        if (nomorKtp != null && !nomorKtp.isEmpty()) {
            fetchRegistrationDetails(nomorKtp);
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String email = prefs.getString(EMAIL_KEY, "");
            if (!email.isEmpty()) {
                fetchRegistrationDetailsByEmail(email);
            } else {
                Toast.makeText(this, "Nomor KTP atau email tidak ditemukan", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        if (justRegistered || justUploaded) {
            refreshData();
        }
    }

    // Method ini digunakan untuk menginisialisasi semua view yang ada di layout
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

    // Method ini dipanggil untuk mengambil detail registrasi berdasarkan nomor KTP
    private void fetchRegistrationDetails(String nomorKtp) {
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final String token = prefs.getString(TOKEN_KEY, "");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, BASE_URL,
                this::handleApiResponse,
                error -> handleApiError(error)) {
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

        // Disable caching for this request
        stringRequest.setShouldCache(false);

        addToRequestQueue(stringRequest);
    }

    // Method ini dipanggil untuk mengambil detail registrasi berdasarkan email
    private void fetchRegistrationDetailsByEmail(String email) {
        progressBar.setVisibility(View.VISIBLE);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, BASE_URL,
                this::handleApiResponse,
                error -> handleApiError(error)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };

        addToRequestQueue(stringRequest);
    }

    // Method ini menangani respon dari API
    private void handleApiResponse(String response) {
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
    }

    // Method ini menangani error dari API
    private void handleApiError(Exception error) {
        progressBar.setVisibility(View.GONE);
        Log.e(TAG, "Error: " + error.toString());
        String errorMessage = "Error fetching data";
        if (error instanceof com.android.volley.NetworkError) {
            errorMessage += " (Network Error)";
        } else if (error instanceof com.android.volley.ServerError) {
            errorMessage += " (Server Error)";
        } else if (error instanceof com.android.volley.AuthFailureError) {
            errorMessage += " (Authentication Failure)";
        } else if (error instanceof com.android.volley.ParseError) {
            errorMessage += " (Parse Error)";
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMessage += " (No Connection)";
        } else if (error instanceof com.android.volley.TimeoutError) {
            errorMessage += " (Timeout)";
        }
        Toast.makeText(Status.this, errorMessage, Toast.LENGTH_SHORT).show();
        finish();
    }

    // Method ini menambahkan request ke dalam queue
    private void addToRequestQueue(StringRequest stringRequest) {
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // Method ini memperbarui UI untuk status "ditinjau"
    private void updateUIForReview() {
        tvStatusBiaya.setText("Status Pembayaran: Pembayaran masih ditinjau");
        btnBuktiBayar.setVisibility(View.GONE);
    }

    // Method ini dipanggil saat activity dilanjutkan
    @Override
    protected void onResume() {
        super.onResume();
        boolean justUploaded = getIntent().getBooleanExtra("JUST_UPLOADED", false);
        boolean refreshData = getIntent().getBooleanExtra("REFRESH_DATA", false);
        String newStatus = getIntent().getStringExtra("NEW_STATUS");

        if (justUploaded || refreshData || newStatus != null) {
            refreshData();
        }
    }

    private void refreshData() {
        String nomorKtp = getIntent().getStringExtra("NOMOR_KTP");
        if (nomorKtp != null && !nomorKtp.isEmpty()) {
            fetchRegistrationDetails(nomorKtp);
        }
    }

    // Method ini memperbarui UI dengan data dari API
    private void updateUI(JSONObject data) {
        try {
            JSONObject registrasi = data.getJSONObject("registrasi");

            // Update basic info
            updateBasicInfo(registrasi);

            // Handle payment status
            String savedStatus = getIntent().getStringExtra("SAVED_STATUS");
            String newStatus = getIntent().getStringExtra("NEW_STATUS");
            if (newStatus != null) {
                handlePaymentStatus(newStatus);
            } else if (savedStatus != null && !savedStatus.isEmpty()) {
                handlePaymentStatus(savedStatus);
            } else {
                handlePaymentStatus(registrasi.optString("status_pembayaran", ""));
            }

            // Handle token visibility
            handleTokenVisibility(registrasi);

            // Handle registration status
            handleRegistrationStatus(data);

            Log.d(TAG, "Data updated successfully");

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON in updateUI: " + e.getMessage());
            e.printStackTrace();
            setDefaultValues();
            Toast.makeText(this, "Error mengupdate data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method ini memperbarui informasi dasar pada UI
    private void updateBasicInfo(JSONObject registrasi) throws JSONException {
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
    }

    // Method ini menangani status pembayaran
    private void handlePaymentStatus(String statusPembayaran) {
        switch (statusPembayaran.toLowerCase()) {
            case "ditinjau":
                tvStatusBiaya.setText("Status Pembayaran: Pembayaran masih ditinjau");
                btnBuktiBayar.setVisibility(View.GONE);
                break;
            case "belum dibayar":
            case "":
                tvStatusBiaya.setText("Status Pembayaran: Pembayaran Belum dilakukan");
                btnBuktiBayar.setVisibility(View.VISIBLE);
                break;
            case "lunas":
                tvStatusBiaya.setText("Status Pembayaran: Lunas");
                btnBuktiBayar.setVisibility(View.GONE);
                break;
            default:
                tvStatusBiaya.setText("Status Pembayaran: " + statusPembayaran);
                btnBuktiBayar.setVisibility(View.GONE);
                break;
        }
        savePaymentStatus(statusPembayaran);
    }

    private void savePaymentStatus(String status) {
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("payment_status_" + tvNik.getText().toString().replace("NIK: ", ""), status);
        editor.apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        justUploaded = intent.getBooleanExtra("JUST_UPLOADED", false);
        String nomorKtp = intent.getStringExtra("NOMOR_KTP");
        if (nomorKtp != null && !nomorKtp.isEmpty()) {
            fetchRegistrationDetails(nomorKtp);
        }
    }

    // Method ini menangani visibilitas token
    private void handleTokenVisibility(JSONObject registrasi) throws JSONException {
        String statusPembayaran = registrasi.optString("status_pembayaran", "").toLowerCase();
        if (statusPembayaran.equals("lunas")) {
            String token = registrasi.optString("token", "N/A");
            tvToken.setText("Token: " + token);
            tvToken.setVisibility(View.VISIBLE);
        } else {
            tvToken.setVisibility(View.GONE);
        }
    }

    // Method ini menangani status registrasi
    private void handleRegistrationStatus(JSONObject data) throws JSONException {
        JSONArray statusHistory = data.getJSONArray("status_history");
        if (statusHistory.length() > 0) {
            JSONObject latestStatus = statusHistory.getJSONObject(0);
            tvStatus.setText("Status: " + latestStatus.optString("status", "N/A"));
        } else {
            tvStatus.setText("Status: N/A");
        }
    }

    // Method ini mengatur nilai default untuk semua field jika terjadi error
    private void setDefaultValues() {
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
        btnBuktiBayar.setVisibility(View.GONE);
    }

    // Method ini dipanggil saat tombol kembali ditekan
    public void btnKembali(View view) {
        Intent intent = new Intent(Status.this, IndexPendaftaranLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}