package com.example.formregistrasi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView logo;
    private TextView txt_register;
    private Button btn_register, btn_login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.logo);
        txt_register = findViewById(R.id.txt_register);
        btn_register = findViewById(R.id.btn_register);
        btn_login = findViewById(R.id.btn_login);

    }

    public void btn_register(View view) {
        Intent intent = new Intent(MainActivity.this, RegistrasiActivity.class);
        startActivity(intent);
    }

    public void btn_login(View view) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}