package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Status extends AppCompatActivity {

    private static final String PREFS_NAME = "UserPrefs";
    private TextView tvStatus, tvNama, tvNik, tvAlamat, tvRt, tvRw, tvTelp, tvKodePos, tvJumlahPenghuni;
    private TextView tvPekerjaan, tvKelurahan, tvKecamatan, tvLatitude, tvLongitude;
    private Button btnKembali;

    // Method ini dipanggil pas activity dibuat
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        initializeViews();

        // Ambil NIK dari intent
        Intent intent = getIntent();
        String nik = intent.getStringExtra("NIK");

        if (nik != null && !nik.isEmpty()) {
            fetchDataByNik(nik);
        } else {
            // Kalo gak ada NIK di intent, coba ambil dari SharedPreferences
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
                intent.putExtra("REGISTERED", true);
                intent.putExtra("NIK", getIntent().getStringExtra("NIK"));
                startActivity(intent);
                finish();
            }
        });
    }

    // Fungsi buat dapetin id semua view yang dipake
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

    // Fungsi buat ngambil data berdasarkan NIK
    private void fetchDataByNik(String nik) {
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String userEmail = userPrefs.getString("email", "");
        boolean hasRegistered = userPrefs.getBoolean("hasRegistered_" + userEmail, false);

        if (hasRegistered) {
            displayDataFromPrefs(userPrefs, userEmail);
        } else {
            tvStatus.setText("Status: Belum melakukan registrasi");
            clearFields();
        }
    }

    // Fungsi buat nampilin data dari SharedPreferences
    private void displayDataFromPrefs(SharedPreferences prefs, String email) {
        tvStatus.setText("Status: Data masih tahap review");
        tvNama.setText("Nama: " + prefs.getString("nama_" + email, ""));
        tvNik.setText("NIK: " + prefs.getString("nomor_ktp_" + email, ""));
        tvAlamat.setText("Alamat: " + prefs.getString("alamat_" + email, ""));
        tvRt.setText("RT: " + prefs.getString("rt_" + email, ""));
        tvRw.setText("RW: " + prefs.getString("rw_" + email, ""));
        tvTelp.setText("No. Telp: " + prefs.getString("telp_hp_" + email, ""));
        tvKodePos.setText("Kode Pos: " + prefs.getString("kode_pos_" + email, ""));
        tvJumlahPenghuni.setText("Jumlah Penghuni: " + prefs.getString("jumlah_penghuni_" + email, ""));
        tvPekerjaan.setText("Pekerjaan: " + prefs.getString("pekerjaan_" + email, ""));
        tvKelurahan.setText("Kelurahan: " + prefs.getString("kelurahan_" + email, ""));
        tvKecamatan.setText("Kecamatan: " + prefs.getString("kecamatan_" + email, ""));
        tvLatitude.setText("Latitude: " + prefs.getString("latitude_" + email, ""));
        tvLongitude.setText("Longitude: " + prefs.getString("longitude_" + email, ""));
    }

    // Fungsi buat ngehapus semua isian field
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