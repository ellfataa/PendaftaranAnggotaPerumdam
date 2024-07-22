package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IndexPendaftaranLogin extends AppCompatActivity {

    private Button btn_login, btn_registrasi;
    private TextView akun, txtIndexPelanggan, username;
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

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        // Ambil username dari SharedPreferences
        String username = sharedPreferences.getString("username", "");

        if (!username.isEmpty()) {
            akun.setText("Akun: " + username);
            btn_login.setVisibility(View.GONE);
            btn_registrasi.setVisibility(View.GONE);
            txtIndexPelanggan.setVisibility(View.VISIBLE);
        } else {
            akun.setText("Akun: Belum Login");
            btn_login.setVisibility(View.VISIBLE);
            btn_registrasi.setVisibility(View.VISIBLE);
            txtIndexPelanggan.setVisibility(View.GONE);
        }

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Periksa kembali status login setiap kali activity di-resume
        String username = sharedPreferences.getString("username", "");
        if (!username.isEmpty()) {
            akun.setText("Akun: " + username);
            btn_login.setVisibility(View.GONE);
            btn_registrasi.setVisibility(View.GONE);
            txtIndexPelanggan.setVisibility(View.VISIBLE);
        } else {
            akun.setText("Akun: Belum Login");
            btn_login.setVisibility(View.VISIBLE);
            btn_registrasi.setVisibility(View.VISIBLE);
            txtIndexPelanggan.setVisibility(View.GONE);
        }
    }
}