package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    EditText etNama, etNik;
    Button btnLogin;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNama = (EditText) findViewById(R.id.etNama);
        etNik = (EditText) findViewById(R.id.etNik);
        btnLogin = findViewById(R.id.btnLogin);
        progressDialog = new ProgressDialog(LoginActivity.this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String setNama = etNama.getText().toString();
                String setNik = etNik.getText().toString();

                CheckLogin(setNama, setNik);
            }
        });
    }

    public void CheckLogin(final String etNama, final  String etNik){
        if (checkNetworkConnection()){
            progressDialog.show();
            StringRequest stringRequest = new StringRequest(Request.Method.POST, DbContract.SERVER_LOGIN_URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String resp = jsonObject.getString("server_response");
                        if (resp.equals("[{\"status\":\"OK\"}]")) {
                            Toast.makeText(getApplicationContext(), "Anda Berhasil Login", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(getApplicationContext(), resp, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws  AuthFailureError{
                    Map<String, String> params = new HashMap<>();
                    params.put("nama", etNama);
                    params.put("NIK", etNik);

                    return params;
                }
            };

            VolleyConnection.getInstance(LoginActivity.this).addToRequestQue(stringRequest);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                }
            }, 2000);
        }else {
            Toast.makeText(getApplicationContext(), "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netwokInfo = connectivityManager.getActiveNetworkInfo();
        return (netwokInfo != null && netwokInfo.isConnected());
    }


//    TOMBOL KEMBALI
    public void btnKembali(View view) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}