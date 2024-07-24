package com.example.formregistrasi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ImageView logo;
    private TextView txt_masuk, daftarText;
    private Button btn_masuk;
    private EditText et_userAkun, et_passwordAkun;

    private static final String URL_LOGIN = "http://192.168.230.124/pendaftaranPerumdam/masukAkun.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.logo);
        txt_masuk = findViewById(R.id.txt_masuk);
        btn_masuk = findViewById(R.id.btn_masuk);
        daftarText = findViewById(R.id.daftarText);
        et_userAkun = findViewById(R.id.userName);
        et_passwordAkun = findViewById(R.id.password);

        daftarText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BuatUser.class);
                startActivity(intent);
            }
        });

        btn_masuk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInputValid()) {
                    login();
                }
            }
        });
    }

    private boolean isInputValid() {
        String userAkun = et_userAkun.getText().toString().trim();
        String passwordAkun = et_passwordAkun.getText().toString().trim();

        if (userAkun.isEmpty() || passwordAkun.isEmpty()) {
            Toast.makeText(this, "Mohon melengkapi semua data", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void login() {
        final String userAkun = et_userAkun.getText().toString().trim();
        final String passwordAkun = et_passwordAkun.getText().toString().trim();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_LOGIN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String status = jsonObject.getString("status");

                            if(status.equals("success")){
                                String username = jsonObject.getString("username");
                                Toast.makeText(MainActivity.this, "Masuk akun berhasil", Toast.LENGTH_SHORT).show();

                                // Simpan username ke SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("username", username);
                                editor.apply();

                                Intent intent = new Intent(MainActivity.this, IndexPendaftaranLogin.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonObject.getString("message");
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "JSON Error: " + e.toString());
                            Toast.makeText(MainActivity.this, "Terjadi kesalahan. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley Error: " + error.toString());
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null && networkResponse.data != null) {
                            String jsonError = new String(networkResponse.data);
                            Log.e(TAG, "Error Response: " + jsonError);
                        }
                        Toast.makeText(MainActivity.this, "Gagal terhubung ke server. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userAkun", userAkun);
                params.put("passwordAkun", passwordAkun);
                return params;
            }
        };

        // Menambahkan retry policy
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}