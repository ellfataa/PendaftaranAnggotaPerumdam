package com.example.formregistrasi;

import static com.example.formregistrasi.RegistrasiActivity.PREFS_NAME;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Status extends AppCompatActivity {

    private static final String PREFS_NAME = "UserPrefs" ;
    private TextView tvStatus, tvNama, tvNik, tvAlamat, tvRt, tvRw, tvTelp, tvKodePos, tvJumlahPenghuni;
    private TextView tvPekerjaan, tvKelurahan, tvKecamatan, tvLatitude, tvLongitude;
    private Button btnKembali;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        initializeViews();

        // Get NIK from intent
        Intent intent = getIntent();
        String nik = intent.getStringExtra("NIK");

        if (nik != null && !nik.isEmpty()) {
            fetchDataByNik(nik);
        } else {
            // If no NIK in intent, try to get from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
            String savedNik = prefs.getString("nomor_ktp", "");
            if (!savedNik.isEmpty()) {
                fetchDataByNik(savedNik);
            } else {
                tvStatus.setText("Status: Data tidak ditemukan");
                clearFields();
            }
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

    // Fungsi buat mendapatkan id semua view yang digunakan
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

    private void fetchDataByNik(String nik) {
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String savedNik = userPrefs.getString("nomor_ktp", "");

        if (nik.equals(savedNik)) {
            SharedPreferences registrationPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            displayDataFromPrefs(registrationPrefs);
        } else {
            tvStatus.setText("Status: Data tidak ditemukan");
            clearFields();
        }
    }

    private void displayDataFromPrefs(SharedPreferences prefs) {
        tvStatus.setText("Status: Data masih tahap review");
        tvNama.setText("Nama: " + prefs.getString("nama", ""));
        tvNik.setText("NIK: " + prefs.getString("nomor_ktp", ""));
        tvAlamat.setText("Alamat: " + prefs.getString("alamat", ""));
        tvRt.setText("RT: " + prefs.getString("rt", ""));
        tvRw.setText("RW: " + prefs.getString("rw", ""));
        tvTelp.setText("No. Telp: " + prefs.getString("telp_hp", ""));
        tvKodePos.setText("Kode Pos: " + prefs.getString("kode_pos", ""));
        tvJumlahPenghuni.setText("Jumlah Penghuni: " + prefs.getString("jumlah_penghuni", ""));
        tvPekerjaan.setText("Pekerjaan: " + prefs.getString("pekerjaan", ""));
        tvKelurahan.setText("Kelurahan: " + prefs.getString("kelurahan", ""));
        tvKecamatan.setText("Kecamatan: " + prefs.getString("kecamatan", ""));
        tvLatitude.setText("Latitude: " + prefs.getString("latitude", ""));
        tvLongitude.setText("Longitude: " + prefs.getString("longitude", ""));
    }

    private void clearFields() {
        tvNama.setText("Nama: -");
        tvNik.setText("NIK: -");
        tvAlamat.setText("Alamat: -");
        tvRt.setText("RT: -");
        tvRw.setText("RW: -");
        tvTelp.setText("No. Telp: -");
        tvKodePos.setText("Kode Pos: -");
        tvJumlahPenghuni.setText("Jumlah Penghuni: -");
        tvPekerjaan.setText("Pekerjaan: -");
        tvKelurahan.setText("Kelurahan: -");
        tvKecamatan.setText("Kecamatan: -");
        tvLatitude.setText("Latitude: -");
        tvLongitude.setText("Longitude: -");
    }
}