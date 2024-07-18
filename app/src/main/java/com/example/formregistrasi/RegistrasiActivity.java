package com.example.formregistrasi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrasiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrasi);

    }

    public void btnKembali(View view) {
        Intent intent = new Intent(RegistrasiActivity.this, MainActivity.class);
        startActivity(intent);
    }
}