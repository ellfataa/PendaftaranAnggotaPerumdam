package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Status extends AppCompatActivity {

    private TextView tvStatus, tvNama, tvNik, tvAlamat, tvRt, tvRw, tvTelp, tvKodePos, tvJumlahPenghuni;
    private TextView tvPekerjaan, tvKelurahan, tvKecamatan, tvLatitude, tvLongitude;
    private Button btnKembali;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        initializeViews();

        // Get data from intent
        Intent intent = getIntent();
        if (intent.hasExtra("nomor_ktp")) {
            displayData(intent);
        } else {
            // If no data in intent, try to get from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
            String nik = prefs.getString("nomor_ktp", "");
            if (!nik.isEmpty()) {
                displayDataFromPrefs(prefs);
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

    private void displayData(Intent intent) {
        tvStatus.setText("Status: Data masih tahap review");
        tvNama.setText("Nama: " + intent.getStringExtra("nama"));
        tvNik.setText("NIK: " + intent.getStringExtra("nomor_ktp"));
        tvAlamat.setText("Alamat: " + intent.getStringExtra("alamat"));
        tvRt.setText("RT: " + intent.getStringExtra("rt"));
        tvRw.setText("RW: " + intent.getStringExtra("rw"));
        tvTelp.setText("No. Telp: " + intent.getStringExtra("telp_hp"));
        tvKodePos.setText("Kode Pos: " + intent.getStringExtra("kode_pos"));
        tvJumlahPenghuni.setText("Jumlah Penghuni: " + intent.getStringExtra("jumlah_penghuni"));
        tvPekerjaan.setText("Pekerjaan: " + intent.getStringExtra("pekerjaan"));
        tvKelurahan.setText("Kelurahan: " + intent.getStringExtra("kelurahan"));
        tvKecamatan.setText("Kecamatan: " + intent.getStringExtra("kecamatan"));
        tvLatitude.setText("Latitude: " + intent.getStringExtra("latitude"));
        tvLongitude.setText("Longitude: " + intent.getStringExtra("longitude"));

        // Save data to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nik", intent.getStringExtra("nomor_ktp"));
        editor.putString("nama", intent.getStringExtra("nama"));
        editor.putString("alamat", intent.getStringExtra("alamat"));
        editor.putString("rt", intent.getStringExtra("rt"));
        editor.putString("rw", intent.getStringExtra("rw"));
        editor.putString("telp_hp", intent.getStringExtra("telp_hp"));
        editor.putString("kode_pos", intent.getStringExtra("kode_pos"));
        editor.putString("jumlah_penghuni", intent.getStringExtra("jumlah_penghuni"));
        editor.putString("pekerjaan", intent.getStringExtra("pekerjaan"));
        editor.putString("kelurahan", intent.getStringExtra("kelurahan"));
        editor.putString("kecamatan", intent.getStringExtra("kecamatan"));
        editor.putString("latitude", intent.getStringExtra("latitude"));
        editor.putString("longitude", intent.getStringExtra("longitude"));
        editor.apply();
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
}