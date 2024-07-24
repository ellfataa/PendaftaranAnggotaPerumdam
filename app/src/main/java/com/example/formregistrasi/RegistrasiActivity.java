package com.example.formregistrasi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private EditText etNama, etNik, etAlamat, etRT, etRW, etNoTelp, etKodePos, etJumlahPenghuni, etLatitude, etLongitude;
    private AutoCompleteTextView idPekerjaan, idKelurahan, idKecamatan;
    private ImageView fotoKTP, fotoRumah;
    private String fotoKTPBase64, fotoRumahBase64;
    private Button btnKembali, btnDaftar, btnTemukanLokasi, btnPickImgKTP, btnPickImgRumah;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrasi);

        initializeViews();
        setListeners();
        fetchDropdownData();
    }

    private void initializeViews() {
        etNama = findViewById(R.id.etNama);
        etNik = findViewById(R.id.etNik);
        etAlamat = findViewById(R.id.etAlamat);
        etRT = findViewById(R.id.etRT);
        etRW = findViewById(R.id.etRW);
        etNoTelp = findViewById(R.id.etNoTelp);
        etKodePos = findViewById(R.id.etKodePos);
        etJumlahPenghuni = findViewById(R.id.etJumlahPenghuni);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongtitude);

        idPekerjaan = findViewById(R.id.idPekerjaan);
        idKelurahan = findViewById(R.id.idKelurahan);
        idKecamatan = findViewById(R.id.idKecamatan);

        fotoKTP = findViewById(R.id.fotoKTP);
        fotoRumah = findViewById(R.id.fotoRumah);

        btnKembali = findViewById(R.id.btnKembali);
        btnDaftar = findViewById(R.id.btnDaftar);
        btnTemukanLokasi = findViewById(R.id.btn_temukan);
        btnPickImgKTP = findViewById(R.id.btnPickImgKTP);
        btnPickImgRumah = findViewById(R.id.btnPickImgRumah);
    }

    private void setListeners() {
        btnPickImgKTP.setOnClickListener(v -> pickImage(REQUEST_IMAGE_KTP));
        btnPickImgRumah.setOnClickListener(v -> pickImage(REQUEST_IMAGE_RUMAH));
        btnTemukanLokasi.setOnClickListener(v -> getLocation());
        btnDaftar.setOnClickListener(v -> registerUser());
        btnKembali.setOnClickListener(this::btnKembali);
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (requestCode == REQUEST_IMAGE_KTP) {
                processImage(imageUri, true);
            } else if (requestCode == REQUEST_IMAGE_RUMAH) {
                processImage(imageUri, false);
            }
        }
    }

    private void fetchDropdownData() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://192.168.230.122/pendaftaranPerumdam/registrasi.php";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("RegistrasiActivity", "Raw Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        if (jsonObject.has("kecamatan")) {
                            JSONArray kecamatanArray = jsonObject.getJSONArray("kecamatan");
                            populateDropdown(kecamatanArray, idKecamatan, "kecamatan");
                        }

                        if (jsonObject.has("kelurahan")) {
                            JSONArray kelurahanArray = jsonObject.getJSONArray("kelurahan");
                            populateDropdown(kelurahanArray, idKelurahan, "kelurahan");
                        }

                        if (jsonObject.has("pekerjaan")) {
                            JSONArray pekerjaanArray = jsonObject.getJSONArray("pekerjaan");
                            populateDropdown(pekerjaanArray, idPekerjaan, "pekerjaan");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error parsing JSON data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("RegistrasiActivity", "Error: " + error.toString());
                    Toast.makeText(getApplicationContext(), "Error fetching data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(stringRequest);
    }

    private void populateDropdown(JSONArray jsonArray, AutoCompleteTextView autoCompleteTextView, String type) {
        try {
            Map<String, String> itemMap = new HashMap<>();
            String[] items = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                String id = item.getString("id");
                String nama = item.has("nama") ? item.getString("nama") :
                        (item.has("nama_" + type) ? item.getString("nama_" + type) : "Unknown");
                items[i] = nama;
                itemMap.put(nama, id);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
            autoCompleteTextView.setAdapter(adapter);

            autoCompleteTextView.setTag(itemMap);

            autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);
                String selectedId = itemMap.get(selectedName);
                Log.d("RegistrasiActivity", type + " selected: " + selectedName + " (ID: " + selectedId + ")");
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("RegistrasiActivity", "Error populating " + type + " dropdown: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Error populating " + type + " dropdown: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateFields()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://192.168.230.122/pendaftaranPerumdam/registrasi.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("RegistrasiActivity", "Registration Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String status = jsonObject.getString("status");
                        String message = jsonObject.getString("message");

                        if (status.equals("OK")) {
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                            // Handle successful registration (e.g., clear form, navigate to another activity)
                        } else {
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getApplicationContext(), "Error during registration: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("nama", etNama.getText().toString());
                params.put("nik", etNik.getText().toString());
                params.put("alamat", etAlamat.getText().toString());
                params.put("rt", etRT.getText().toString());
                params.put("rw", etRW.getText().toString());
                params.put("telp_hp", etNoTelp.getText().toString());
                params.put("kode_pos", etKodePos.getText().toString());
                params.put("jumlah_penghuni", etJumlahPenghuni.getText().toString());
                params.put("latitude", etLatitude.getText().toString());
                params.put("longitude", etLongitude.getText().toString());

                Map<String, String> pekerjaanMap = (Map<String, String>) idPekerjaan.getTag();
                Map<String, String> kelurahanMap = (Map<String, String>) idKelurahan.getTag();
                Map<String, String> kecamatanMap = (Map<String, String>) idKecamatan.getTag();

                params.put("id_pekerjaan", pekerjaanMap.get(idPekerjaan.getText().toString()));
                params.put("id_kelurahan", kelurahanMap.get(idKelurahan.getText().toString()));
                params.put("id_kecamatan", kecamatanMap.get(idKecamatan.getText().toString()));

                params.put("foto_ktp", "data:image/jpeg;base64," + fotoKTPBase64);
                params.put("foto_rumah", "data:image/jpeg;base64," + fotoRumahBase64);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(stringRequest);
    }

    private boolean validateFields() {
        // Add validation for all required fields
        if (etNama.getText().toString().isEmpty() ||
                etNik.getText().toString().isEmpty() ||
                etAlamat.getText().toString().isEmpty() ||
                etRT.getText().toString().isEmpty() ||
                etRW.getText().toString().isEmpty() ||
                etNoTelp.getText().toString().isEmpty() ||
                etKodePos.getText().toString().isEmpty() ||
                etJumlahPenghuni.getText().toString().isEmpty() ||
                etLatitude.getText().toString().isEmpty() ||
                etLongitude.getText().toString().isEmpty() ||
                idPekerjaan.getText().toString().isEmpty() ||
                idKelurahan.getText().toString().isEmpty() ||
                idKecamatan.getText().toString().isEmpty() ||
                fotoKTPBase64 == null ||
                fotoRumahBase64 == null) {
            return false;
        }
        return true;
    }

    private void processImage(Uri imageUri, boolean isKTP) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            String encodedImage = encodeImageToBase64(selectedImage);
            if (isKTP) {
                fotoKTPBase64 = encodedImage;
                fotoKTP.setImageBitmap(selectedImage);
            } else {
                fotoRumahBase64 = encodedImage;
                fotoRumah.setImageBitmap(selectedImage);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Image not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
    }

        private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Fetch location logic here
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public void btnKembali(View view) {
        Intent intent = new Intent(RegistrasiActivity.this, IndexPendaftaranLogin.class);
        startActivity(intent);

    }
}