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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadBuktiPembayaran extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_IMAGE_BUKTI = 1;

    private ImageView fotoBukti;
    private Button btnPickImgBukti, btnKirim, btnKembali;
    private TextView txtUserEmail;
    private String nomorKtp;
    private Uri photoUriBukti;
    private String fotoBuktiPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_bukti_pembayaran);

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
        btnKembali.setOnClickListener(v -> navigateToStatus());
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

    public void kirim() {
        if (fotoBuktiPath == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://192.168.230.84/registrasi-pelanggan/public/api/upload-bukti-bayar";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Add string params
            buildTextPart(dos, "nomor_ktp", nomorKtp);

            // Add image part
            buildFilePart(dos, "foto_bukti_bayar", fotoBuktiPath);

            // End of multipart/form-data
            dos.writeBytes("--" + boundary + "--" + lineEnd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] multipartBody = bos.toByteArray();

        MultipartRequest multipartRequest = new MultipartRequest(
                url,
                null,
                "multipart/form-data; boundary=" + boundary,
                multipartBody,
                response -> handleUploadResponse(response),
                this::handleUploadError
        );

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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
                navigateToStatus();
            } else {
                Toast.makeText(UploadBuktiPembayaran.this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(UploadBuktiPembayaran.this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUploadError(VolleyError error) {
        Toast.makeText(UploadBuktiPembayaran.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes("--" + boundary + "\r\n");
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"\r\n\r\n");
        dataOutputStream.writeBytes(parameterValue + "\r\n");
    }

    private void buildFilePart(DataOutputStream dataOutputStream, String parameterName, String fileName) throws IOException {
        dataOutputStream.writeBytes("--" + boundary + "\r\n");
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"; filename=\"" + fileName + "\"\r\n");
        dataOutputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n");

        FileInputStream fileInputStream = new FileInputStream(fileName);
        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, 1024 * 1024);
        byte[] buffer = new byte[bufferSize];

        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bytesRead);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, 1024 * 1024);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dataOutputStream.writeBytes("\r\n");
        fileInputStream.close();
    }

    private void navigateToStatus() {
        Intent intent = new Intent(UploadBuktiPembayaran.this, Status.class);
        intent.putExtra("NOMOR_KTP", nomorKtp);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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

    private static final String boundary = "Volley-" + System.currentTimeMillis();
    private static final String lineEnd = "\r\n";
}