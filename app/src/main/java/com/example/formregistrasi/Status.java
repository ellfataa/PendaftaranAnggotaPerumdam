package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Status extends AppCompatActivity {

    private static final String PREFS_NAME = "UserInfo";
    private TextView tvStatus, tvNama, tvNik, tvAlamat, tvRt, tvRw, tvTelp, tvKodePos, tvJumlahPenghuni;
    private TextView tvPekerjaan, tvKelurahan, tvKecamatan, tvLatitude, tvLongitude, txtUserEmail;
    private Button btnKembali;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        initializeViews();

        // Get email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");

        if (!userEmail.isEmpty()) {
            txtUserEmail.setText(userEmail);
            txtUserEmail.setVisibility(View.GONE); // Hide the email TextView
            fetchDataByEmail(userEmail);
        } else {
            Toast.makeText(this, "Email pengguna tidak ditemukan", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Status: Data tidak ditemukan");
            clearFields();
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
        txtUserEmail = findViewById(R.id.txtUserEmail);
        btnKembali = findViewById(R.id.btnKembali);
    }

    private void fetchDataByEmail(String email) {
        SharedPreferences userPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasRegistered = userPrefs.getBoolean("hasRegistered_" + email, false);

        if (hasRegistered) {
            displayDataFromPrefs(userPrefs, email);
        } else {
            tvStatus.setText("Status: Belum melakukan registrasi");
            clearFields();
        }
    }

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