package com.example.formregistrasi;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UploadBuktiPembayaran extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_IMAGE_BUKTI = 1;

    private ImageView fotoBukti;
    private Button btnPickImgBukti, btnKirim, btnKembali;
    private TextView txtUserEmail;
    private String nomorKtp;
    private Uri photoUriBukti;
    private String fotoBuktiPath;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_bukti_pembayaran);

        sessionManager = new SessionManager(this);

        initializeViews();
        setListeners();

        nomorKtp = getIntent().getStringExtra("NOMOR_KTP");

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    private void initializeViews() {
        fotoBukti = findViewById(R.id.fotoBukti);
        btnPickImgBukti = findViewById(R.id.btnPickImgBukti);
        btnKirim = findViewById(R.id.btnKirim);
        btnKembali = findViewById(R.id.btnKembali);
        txtUserEmail = findViewById(R.id.txtUserEmail);
    }

    private void setListeners() {
        btnPickImgBukti.setOnClickListener(v -> pickImage());
        btnKirim.setOnClickListener(v -> kirim());
        btnKembali.setOnClickListener(v -> navigateToStatus(null));
    }

    private void pickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            launchImagePicker();
        }
    }

    private void launchImagePicker() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.formregistrasi.fileprovider",
                    photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            photoUriBukti = photoURI;
        }

        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooserIntent, REQUEST_IMAGE_BUKTI);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_BUKTI) {
            Uri imageUri = (data != null && data.getData() != null) ? data.getData() : photoUriBukti;
            processImage(imageUri);
        }
    }

    private void processImage(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            String fileName = "bukti_" + System.currentTimeMillis() + ".jpg";
            fotoBuktiPath = saveImageToInternalStorage(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), fileName);

            fotoBukti.setImageBitmap(selectedImage);
            fotoBukti.setVisibility(ImageView.VISIBLE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Image not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("images", Context.MODE_PRIVATE);
        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    private void kirim() {
        if (fotoBuktiPath == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getToken();
        if (token.isEmpty()) {
            Toast.makeText(this, "Authentication token is missing. Please log in again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        if (sessionManager.isTokenExpired()) {
            refreshTokenAndRetry();
            return;
        }

        String url = "http://192.168.230.84/registrasi-pelanggan/public/api/upload-bukti-bayar";
        Log.d("Upload", "Starting upload to URL: " + url);

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url,
                this::handleUploadResponse,
                this::handleUploadError) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("nomor_ktp", nomorKtp);
                Log.d("Upload", "Params: " + params.toString());
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imageName = System.currentTimeMillis();
                params.put("foto_bukti_bayar", new DataPart(imageName + ".jpg", getFileDataFromPath(fotoBuktiPath)));
                Log.d("Upload", "ByteData: foto_bukti_bayar added");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                String token = sessionManager.getToken();
                if (!token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    Log.d("Upload", "Authorization header: Bearer " + token);
                } else {
                    Log.d("Upload", "No token available for Authorization header");
                }
                return headers;
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Log.d("Upload", "Adding request to queue");
        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private void handleUploadResponse(NetworkResponse response) {
        String responseString = new String(response.data);
        try {
            JSONObject jsonResponse = new JSONObject(responseString);
            boolean success = jsonResponse.getBoolean("success");
            String message = jsonResponse.getString("message");

            if (success) {
                Toast.makeText(UploadBuktiPembayaran.this, "Bukti pembayaran terkirim", Toast.LENGTH_SHORT).show();
                navigateToStatus("ditinjau");
            } else {
                Toast.makeText(UploadBuktiPembayaran.this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(UploadBuktiPembayaran.this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToStatus(String status) {
        Intent intent = new Intent(UploadBuktiPembayaran.this, Status.class);
        intent.putExtra("NOMOR_KTP", nomorKtp);
        intent.putExtra("STATUS_PEMBAYARAN", status);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void handleUploadError(VolleyError error) {
        String errorMessage = "Unknown error occurred";
        if (error instanceof AuthFailureError || (error.networkResponse != null && error.networkResponse.statusCode == 401)) {
            errorMessage = "Authentication error: Token might be invalid or expired";
            refreshTokenAndRetry();
        } else if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            errorMessage = "Error: HTTP " + statusCode;
            if (error.networkResponse.data != null) {
                try {
                    String responseBody = new String(error.networkResponse.data, "utf-8");
                    Log.e("UploadError", "Error response body: " + responseBody);
                    errorMessage += " - " + responseBody;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (error.getMessage() != null) {
            errorMessage = error.getMessage();
        }

        Log.e("UploadError", "Detailed error: " + errorMessage);
        Toast.makeText(UploadBuktiPembayaran.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void refreshTokenAndRetry() {
        String newToken = sessionManager.refreshToken();
        if (newToken != null && !newToken.isEmpty()) {
            sessionManager.saveToken(newToken);
            kirim();
        } else {
            Toast.makeText(this, "Failed to refresh token. Please log in again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private byte[] getFileDataFromPath(String path) {
        File file = new File(path);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUriBukti != null) outState.putString("photoUriBukti", photoUriBukti.toString());
        outState.putString("fotoBuktiPath", fotoBuktiPath);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        String uriBukti = savedInstanceState.getString("photoUriBukti");
        if (uriBukti != null) photoUriBukti = Uri.parse(uriBukti);
        fotoBuktiPath = savedInstanceState.getString("fotoBuktiPath");

        if (fotoBuktiPath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(fotoBuktiPath);
            fotoBukti.setImageBitmap(bitmap);
            fotoBukti.setVisibility(ImageView.VISIBLE);
        }
    }
}