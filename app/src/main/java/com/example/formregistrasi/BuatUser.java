package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BuatUser extends AppCompatActivity {

    private static final String TAG = "BuatUser";
    private static final String REGISTER_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/register-user";

    private EditText namaLengkap, emailAkun, password, konfirmasiPassword;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buat_user);

        initializeViews();
        setListeners();
    }

    private void initializeViews() {
        namaLengkap = findViewById(R.id.namaLengkap);
        emailAkun = findViewById(R.id.emailAkun);
        password = findViewById(R.id.password);
        konfirmasiPassword = findViewById(R.id.konfirmasiPassword);
        Button btn_buat = findViewById(R.id.btn_buat);
        TextView masukText = findViewById(R.id.masukText);

        requestQueue = Volley.newRequestQueue(this);

        namaLengkap.setFilters(new InputFilter[]{getNameInputFilter()});

        masukText.setOnClickListener(v -> goToMainActivity());
        btn_buat.setOnClickListener(v -> {
            if (validateInput()) {
                registerUser();
            }
        });
    }

    private void setListeners() {
        // Listener sudah dipindahkan ke initializeViews()
    }

    private InputFilter getNameInputFilter() {
        return (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isLetter(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };
    }

    private boolean validateInput() {
        String namaAkun = namaLengkap.getText().toString().trim();
        String email = emailAkun.getText().toString().trim();
        String passwordAkun = password.getText().toString().trim();
        String konfirmasiPasswordAkun = konfirmasiPassword.getText().toString().trim();

        if (namaAkun.isEmpty() || email.isEmpty() || passwordAkun.isEmpty() || konfirmasiPasswordAkun.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailAkun.setError("Format email tidak valid");
            emailAkun.requestFocus();
            return false;
        }

        if (!passwordAkun.equals(konfirmasiPasswordAkun)) {
            konfirmasiPassword.setError("Password tidak cocok");
            konfirmasiPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void registerUser() {
        final String namaAkun = namaLengkap.getText().toString().trim();
        final String email = emailAkun.getText().toString().trim();
        final String passwordAkun = password.getText().toString().trim();
        final String konfirmasiPasswordAkun = konfirmasiPassword.getText().toString().trim();

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mendaftarkan...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> {
                    progressDialog.dismiss();
                    Log.d(TAG, "Server Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            StringBuilder messageBuilder = new StringBuilder("Registrasi berhasil");
                            if (jsonObject.has("user")) {
                                JSONObject userObject = jsonObject.getJSONObject("user");
                                String name = userObject.optString("name", "");
                                String userEmail = userObject.optString("email", "");
                                if (!name.isEmpty()) {
                                    messageBuilder.append(" untuk ").append(name);
                                }
                                if (!userEmail.isEmpty()) {
                                    messageBuilder.append(" (").append(userEmail).append(")");
                                }
                            }
                            showSuccessDialog(messageBuilder.toString());
                        } else {
                            String message = jsonObject.optString("message", "Registrasi gagal");
                            Toast.makeText(BuatUser.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "JSON Error: " + e.getMessage() + ", Response: " + response);
                        Toast.makeText(BuatUser.this, "Terjadi kesalahan saat memproses respons server", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    handleNetworkError(error);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", namaAkun);
                params.put("email", email);
                params.put("password", passwordAkun);
                params.put("password_confirmation", konfirmasiPasswordAkun);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);
    }

    private void handleNetworkError(VolleyError error) {
        String errorMessage = getErrorMessage(error);
        Toast.makeText(BuatUser.this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Volley Error: " + error.toString());
    }

    private String getErrorMessage(VolleyError error) {
        if (error instanceof NetworkError) return "Network error - please check your internet connection";
        if (error instanceof ServerError) return "Server error - please try again later";
        if (error instanceof AuthFailureError) return "Authentication failure";
        if (error instanceof ParseError) return "Parsing error";
        if (error instanceof NoConnectionError) return "No connection available";
        if (error instanceof TimeoutError) return "Connection timeout";
        return "Unknown error occurred";
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Registrasi Berhasil")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> goToMainActivity())
                .setCancelable(false)
                .show();
    }

    private void goToMainActivity() {
        startActivity(new Intent(BuatUser.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}