package com.example.formregistrasi;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etNama, etNik;
    private String nama, nik;
    private final String URL = "http://10.10.2.2/pendaftaranPerumdam/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        nama = nik = "";
        etNama = findViewById(R.id.etNama);
        etNik = findViewById(R.id.etNik);
    }

    public void btnKembali(View view) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void login(View view) {
        nama = etNama.getText().toString().trim();
        nik = etNik.getText().toString().trim();
        if(!nama.equals("") && !nik.equals("")){
            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (response.equals("success")) {
                        Intent intent = new Intent(LoginActivity.this, Profil.class);
                        startActivity(intent);
                    } else if (response.equals("failure")) {
                        Toast.makeText(LoginActivity.this, "Nama atau NIK Anda salah", Toast.LENGTH_SHORT).show();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(LoginActivity.this, error.toString().trim(), Toast.LENGTH_SHORT).show();
                }
            }
            ){
                @Nullable
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> data = new HashMap<>();
                    data.put("nama", nama);
                    data.put("nik", nik);
                    return data;
                }
            };
            RequestQueue requestQueue = Volley.newRequestQueue((getApplicationContext()));
            requestQueue.add(stringRequest);
        }else{
            Toast.makeText(this, "Data tidak ditemukan!", Toast.LENGTH_SHORT).show();
        }
    }
}