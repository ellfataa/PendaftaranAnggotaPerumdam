package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.formregistrasi.databinding.ActivityRegistrasiBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RegistrasiActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_KTP = 1;
    private static final int REQUEST_IMAGE_RUMAH = 2;
    private static final String SERVER_REGISTER_URL = "http://192.168.230.122/pendaftaranPerumdam/registrasi.php";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10;

    private ActivityRegistrasiBinding binding;
    private FusedLocationProviderClient locationProviderClient;
    private Bitmap bitmapKTP, bitmapRumah;
    private String encodedImageKTP, encodedImageRumah;

    // Menginisialisasi aktivitas dan menyiapkan UI
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrasiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        setupUIListeners();
    }

    // Menyiapkan pendengar klik untuk elemen UI
    private void setupUIListeners() {
        binding.btnPickImgKTP.setOnClickListener(v -> pickImage(REQUEST_IMAGE_KTP));
        binding.btnPickImgRumah.setOnClickListener(v -> pickImage(REQUEST_IMAGE_RUMAH));
        binding.btnDaftar.setOnClickListener(v -> attemptRegistration());
        binding.btnTemukan.setOnClickListener(v -> getLocation());
    }

    // Mencoba mendaftarkan pengguna jika semua input valid
    private void attemptRegistration() {
        if (validateInputs()) {
            createDataToServer();
        } else {
            Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
        }
    }

    // Memvalidasi semua field input
    private boolean validateInputs() {
        return !binding.etNama.getText().toString().isEmpty()
                && !binding.etNik.getText().toString().isEmpty()
                && !binding.etPekerjaan.getText().toString().isEmpty()
                && !binding.etAlamat.getText().toString().isEmpty()
                && !binding.etRT.getText().toString().isEmpty()
                && !binding.etRW.getText().toString().isEmpty()
                && !binding.etKelurahan.getText().toString().isEmpty()
                && !binding.etKecamatan.getText().toString().isEmpty()
                && !binding.etNoTelp.getText().toString().isEmpty()
                && !binding.etKodePos.getText().toString().isEmpty()
                && !binding.etJumlahPenghuni.getText().toString().isEmpty()
                && !binding.etLatitude.getText().toString().isEmpty()
                && !binding.etLongtitude.getText().toString().isEmpty()
                && bitmapKTP != null
                && bitmapRumah != null;
    }

    // Mengirim data registrasi ke server
    private void createDataToServer() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mendaftarkan...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, SERVER_REGISTER_URL,
                response -> handleServerResponse(response, progressDialog),
                error -> handleServerError(error, progressDialog)) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("nama", binding.etNama.getText().toString());
                params.put("nik", binding.etNik.getText().toString());
                params.put("id_pekerjaan", binding.etPekerjaan.getText().toString());
                params.put("alamat", binding.etAlamat.getText().toString());
                params.put("rt", binding.etRT.getText().toString());
                params.put("rw", binding.etRW.getText().toString());
                params.put("id_kelurahan", binding.etKelurahan.getText().toString());
                params.put("id_kecamatan", binding.etKecamatan.getText().toString());
                params.put("kode_pos", binding.etKodePos.getText().toString());
                params.put("jumlah_penghuni", binding.etJumlahPenghuni.getText().toString());
                params.put("latitude", binding.etLatitude.getText().toString());
                params.put("longitude", binding.etLongtitude.getText().toString());
                params.put("telp_hp", binding.etNoTelp.getText().toString());
                params.put("foto_ktp", encodedImageKTP);
                params.put("foto_rumah", encodedImageRumah);
                return params;
            }
        };

        VolleyConnection.getInstance(this).addToRequestQue(stringRequest);
    }

    // Menangani respons server setelah upaya registrasi
    private void handleServerResponse(String response, ProgressDialog progressDialog) {
        progressDialog.dismiss();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray serverResponse = jsonObject.getJSONArray("server_response");
            JSONObject responseObject = serverResponse.getJSONObject(0);
            String status = responseObject.getString("status");
            String message = responseObject.getString("message");

            if ("OK".equals(status)) {
                Toast.makeText(this, "Registrasi berhasil: " + message, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Registrasi gagal: " + message, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing server response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Menangani kesalahan server selama registrasi
    private void handleServerError(VolleyError error, ProgressDialog progressDialog) {
        progressDialog.dismiss();
        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // Memulai proses pemilihan gambar dari galeri
    private void pickImage(int requestCode) {
        Dexter.withActivity(this)
                .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent, "Pilih foto"), requestCode);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(RegistrasiActivity.this, "Izin diperlukan untuk memilih gambar", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    // Menangani hasil pemilihan gambar
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri filePath = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(filePath);
                if (requestCode == REQUEST_IMAGE_KTP) {
                    bitmapKTP = BitmapFactory.decodeStream(inputStream);
                    binding.fotoKTP.setImageBitmap(bitmapKTP);
                    encodedImageKTP = encodeImage(bitmapKTP);
                } else if (requestCode == REQUEST_IMAGE_RUMAH) {
                    bitmapRumah = BitmapFactory.decodeStream(inputStream);
                    binding.fotoRumah.setImageBitmap(bitmapRumah);
                    encodedImageRumah = encodeImage(bitmapRumah);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // Mengkodekan gambar bitmap ke string Base64
    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return "data:image/jpeg;base64," + encodedImage;
    }

    // Mengambil lokasi saat ini
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            locationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    binding.etLatitude.setText(String.valueOf(location.getLatitude()));
                    binding.etLongtitude.setText(String.valueOf(location.getLongitude()));
                } else {
                    Toast.makeText(getApplicationContext(), "Lokasi anda tidak aktif!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Menangani hasil permintaan izin
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(getApplicationContext(), "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Memeriksa apakah ada koneksi jaringan aktif
    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Menangani klik tombol kembali
    public void btnKembali(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}