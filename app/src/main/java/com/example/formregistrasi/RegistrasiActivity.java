package com.example.formregistrasi;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RegistrasiActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_KTP = 1;
    private static final int REQUEST_IMAGE_RUMAH = 2;

    Button btnPickImgKTP, btnPickImgRumah, btnDaftar;
    ImageView imageViewKTP, imageViewRumah;
    Bitmap bitmap, bitmapRumah;
    String encodedImageKTP, encodedImageRumah;

    private TextView Latitude, Longitude;
    private Button btnTemukan;
    private FusedLocationProviderClient LocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrasi);

        btnPickImgKTP = findViewById(R.id.btnPickImgKTP);
        btnPickImgRumah = findViewById(R.id.btnPickImgRumah);
        btnDaftar = findViewById(R.id.btnDaftar);
        imageViewKTP = findViewById(R.id.fotoKTP);
        imageViewRumah = findViewById(R.id.fotoRumah);

        btnPickImgKTP.setOnClickListener(view -> pickImage(REQUEST_IMAGE_KTP));
        btnPickImgRumah.setOnClickListener(view -> pickImage(REQUEST_IMAGE_RUMAH));

        btnDaftar.setOnClickListener(view -> uploadImages());

        Latitude = findViewById(R.id.etLatitude);
        Longitude = findViewById(R.id.etLongtitude);
        btnTemukan = findViewById(R.id.btn_temukan);

        LocationProviderClient = LocationServices.getFusedLocationProviderClient(RegistrasiActivity.this);
        btnTemukan.setOnClickListener(v -> getLocation());
    }

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

    private void uploadImages() {
        StringRequest request = new StringRequest(Request.Method.POST, "http://your-actual-url.com/api-endpoint",
                response -> Toast.makeText(RegistrasiActivity.this, response, Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(RegistrasiActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("imageKTP", encodedImageKTP);
                params.put("imageRumah", encodedImageRumah);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(RegistrasiActivity.this);
        requestQueue.add(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri filePath = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(filePath);
                if (requestCode == REQUEST_IMAGE_KTP) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    imageViewKTP.setImageBitmap(bitmap);
                    imageStore(bitmap, true);
                } else if (requestCode == REQUEST_IMAGE_RUMAH) {
                    bitmapRumah = BitmapFactory.decodeStream(inputStream);
                    imageViewRumah.setImageBitmap(bitmapRumah);
                    imageStore(bitmapRumah, false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void imageStore(Bitmap bitmap, boolean isKTP) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageBytes = stream.toByteArray();
        String encodedImage = android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
        if (isKTP) {
            encodedImageKTP = encodedImage;
        } else {
            encodedImageRumah = encodedImage;
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, 10);
        } else {
            LocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    Latitude.setText(String.valueOf(location.getLatitude()));
                    Longitude.setText(String.valueOf(location.getLongitude()));
                } else {
                    Toast.makeText(getApplicationContext(), "Lokasi anda tidak aktif!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(getApplicationContext(), "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void btnKembali(View view) {
        Intent intent = new Intent(RegistrasiActivity.this, MainActivity.class);
        startActivity(intent);
    }
}