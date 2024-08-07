package com.example.formregistrasi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrasiActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.230.84/registrasi-pelanggan/public/api/";
    private static final int REQUEST_CODE_MAP = 1001;
    private static final int REQUEST_LOCATION = 1002;
    private static final int REQUEST_IMAGE_KTP = 1;
    private static final int REQUEST_IMAGE_RUMAH = 2;
    private static final int REQUEST_IMAGE_CAPTURE_KTP = 3;
    private static final int REQUEST_IMAGE_CAPTURE_RUMAH = 4;
    private Uri photoUriKTP;
    private Uri photoUriRumah;

    private EditText etNama, etEmail, etNik, etAlamat, etRT, etRW, etNoTelp, etKodePos, etJumlahPenghuni, etLatitude, etLongitude;
    private AutoCompleteTextView idPekerjaan, idKelurahan, idKecamatan;
    private ImageView fotoKTP, fotoRumah;
    private String fotoKTPBase64, fotoRumahBase64;
    private Button btnKembali, btnDaftar, btnPickImgKTP, btnPickImgRumah, btnPeta;
    private TextView txtUserEmail;

    private Map<String, JSONArray> kelurahanByKecamatan = new HashMap<>();
    private String userName;
    private String userEmail;

    static final String PREFS_NAME = "UserInfo";
    private static final String HAS_REGISTERED_KEY = "hasRegistered";

    // Method ini dipanggil ketika activity dibuat
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cek apakah user udah pernah daftar sebelumnya
        if (hasUserAlreadyRegistered()) {
            Toast.makeText(this, "Anda sudah melakukan registrasi sebelumnya.", Toast.LENGTH_LONG).show();
            finish(); // Tutup activity ini
            return;
        }
        setContentView(R.layout.activity_registrasi);

        try {
            setContentView(R.layout.activity_registrasi);
        } catch (Exception e) {
            Log.e("RegistrasiActivity", "Error in onCreate", e);
            Toast.makeText(this, "Terjadi kesalahan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        initializeViews();
        setListeners();
        fetchDropdownData();

        // Ambil data user dari intent
        userName = getIntent().getStringExtra("userName");
        userEmail = getIntent().getStringExtra("userEmail");

        // Set nama dan email
        if (userName != null && !userName.isEmpty()) {
            etNama.setText(userName);
        } else {
            // Kalo userName ga ada, coba ambil dari SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
            String name = sharedPreferences.getString("name", "");
            if (!name.isEmpty()) {
                etNama.setText(name);
            }
        }

        // Ambil email dari SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userEmail = sharedPreferences.getString("email", "");
        if (!userEmail.isEmpty()) {
            txtUserEmail.setText(userEmail);
            txtUserEmail.setVisibility(View.GONE); // Hide the email TextView
            etEmail.setVisibility(View.GONE); // Hide the email EditText
        }

        // Cek lagi apakah user udah pernah daftar
        if (hasUserAlreadyRegistered()) {
            Toast.makeText(this, "Anda sudah melakukan registrasi sebelumnya.", Toast.LENGTH_LONG).show();
            finish(); // Tutup activity ini
            return;
        }

        btnPeta.setOnClickListener(v -> openMap());
    }

    // Method buat inisialisasi semua view yang ada di layout
    private void initializeViews() {
        etNama = findViewById(R.id.etNama);
        etNama.setFilters(new InputFilter[]{getTextOnlyFilter()});
        txtUserEmail = findViewById(R.id.txtUserEmail);
        etEmail = findViewById(R.id.etEmail);
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
        fotoKTP.setVisibility(View.GONE);
        fotoRumah.setVisibility(View.GONE);

        btnKembali = findViewById(R.id.btnKembali);
        btnDaftar = findViewById(R.id.btnDaftar);
        btnPickImgKTP = findViewById(R.id.btnPickImgKTP);
        btnPickImgRumah = findViewById(R.id.btnPickImgRumah);
        btnPeta = findViewById(R.id.btnPeta);
    }

    // Method buat set listener ke semua tombol
    private void setListeners() {
        btnPickImgKTP.setOnClickListener(v -> pickImage(REQUEST_IMAGE_KTP));
        btnPickImgRumah.setOnClickListener(v -> pickImage(REQUEST_IMAGE_RUMAH));
        btnDaftar.setOnClickListener(v -> registerUser());
        btnKembali.setOnClickListener(this::btnKembali);
        btnPeta.setOnClickListener(v -> checkLocationServiceAndOpenMap());
    }

    // Method buat cek apakah layanan lokasi aktif dan buka peta
    private void checkLocationServiceAndOpenMap() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationSettingsDialog();
        } else {
            openMap();
        }
    }

    // Method buat nampilin dialog pengaturan lokasi
    private void showLocationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lokasi tidak aktif");
        builder.setMessage("Untuk menggunakan fitur ini, mohon aktifkan layanan lokasi pada perangkat Anda.");
        builder.setPositiveButton("Pengaturan", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_LOCATION);
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Method buat buka activity peta
    private void openMap() {
        Intent intent = new Intent(this, Maps.class);
        startActivityForResult(intent, REQUEST_CODE_MAP);
    }

    // Method buat milih gambar dari galeri
    private void pickImage(int requestCode) {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        startActivityForResult(galleryIntent, requestCode);
    }

    // Method buat ambil foto pake kamera
    private void dispatchTakePictureIntent(boolean isKTP) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(isKTP);
            } catch (IOException ex) {
                Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.formregistrasi.fileprovider",
                        photoFile);
                if (isKTP) {
                    photoUriKTP = photoURI;
                } else {
                    photoUriRumah = photoURI;
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, isKTP ? REQUEST_IMAGE_CAPTURE_KTP : REQUEST_IMAGE_CAPTURE_RUMAH);
            }
        }
    }

    // Method buat bikin file gambar
    private File createImageFile(boolean isKTP) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    // Method ini dipanggil ketika ada hasil dari activity lain
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_KTP:
                case REQUEST_IMAGE_RUMAH:
                    if (data != null && data.getData() != null) {
                        Uri imageUri = data.getData();
                        processImage(imageUri, requestCode == REQUEST_IMAGE_KTP);
                    }
                    break;
                case REQUEST_CODE_MAP:
                    if (data != null) {
                        String latitude = data.getStringExtra("SELECTED_LATITUDE");
                        String longitude = data.getStringExtra("SELECTED_LONGITUDE");
                        if (latitude != null && longitude != null) {
                            etLatitude.setText(latitude);
                            etLongitude.setText(longitude);
                        }
                    }
                    break;
                case REQUEST_LOCATION:
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        openMap();
                    } else {
                        Toast.makeText(this, "Layanan lokasi masih tidak aktif", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    // Method buat dapetin URI dari bitmap
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    // Method buat ambil data dropdown dari server
    private void fetchDropdownData() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchKecamatan();
        fetchPekerjaan();
    }

    // Method buat ambil data kecamatan dari server
    private void fetchKecamatan() {
        String url = BASE_URL + "getKecamatan";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("RegistrasiActivity", "Kecamatan Raw Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Log.d("RegistrasiActivity", "Kecamatan Parsed JSON: " + jsonObject.toString());

                        if (jsonObject.has("data")) {
                            JSONArray kecamatanArray = jsonObject.getJSONArray("data");
                            populateDropdown(kecamatanArray, idKecamatan, "kecamatan");
                        } else {
                            Log.e("RegistrasiActivity", "Kecamatan data not found in response");
                            Toast.makeText(getApplicationContext(), "Data kecamatan tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("RegistrasiActivity", "JSON Parsing Error for Kecamatan: " + e.getMessage());
                        Toast.makeText(getApplicationContext(), "Error parsing kecamatan data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    String errorMessage = "Error fetching Kecamatan data: ";
                    if (error.networkResponse != null) {
                        errorMessage += "Status Code: " + error.networkResponse.statusCode;
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e("RegistrasiActivity", "Error response body: " + responseBody);
                            errorMessage += "\nResponse: " + responseBody;
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else if (error.getMessage() != null) {
                        errorMessage += error.getMessage();
                    } else {
                        errorMessage += "Unknown error occurred";
                    }
                    Log.e("RegistrasiActivity", errorMessage);
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                return new HashMap<>();
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Method buat ambil data kelurahan berdasarkan kecamatan dari server
    private void fetchKelurahanByKecamatan(String idKecamatan) {
        String url = BASE_URL + "getKelurahanByKecamatan";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("RegistrasiActivity", "Kelurahan Raw Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Log.d("RegistrasiActivity", "Kelurahan Parsed JSON: " + jsonObject.toString());

                        if (jsonObject.has("data")) {
                            JSONArray kelurahanArray = jsonObject.getJSONArray("data");
                            populateDropdown(kelurahanArray, idKelurahan, "kelurahan");
                        } else {
                            Log.e("RegistrasiActivity", "Kelurahan data not found in response");
                            Toast.makeText(getApplicationContext(), "Data kelurahan tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("RegistrasiActivity", "JSON Parsing Error for Kelurahan: " + e.getMessage());
                        Toast.makeText(getApplicationContext(), "Error parsing kelurahan data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    String errorMessage = "Error fetching Kelurahan data: ";
                    if (error.networkResponse != null) {
                        errorMessage += "Status Code: " + error.networkResponse.statusCode;
                    } else if (error.getMessage() != null) {
                        errorMessage += error.getMessage();
                    } else {
                        errorMessage += "Unknown error occurred";
                    }
                    Log.e("RegistrasiActivity", errorMessage);
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id_kecamatan", idKecamatan);
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Method buat ambil data pekerjaan dari server
    private void fetchPekerjaan() {
        String url = BASE_URL + "getPekerjaan";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("RegistrasiActivity", "Pekerjaan Raw Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Log.d("RegistrasiActivity", "Pekerjaan Parsed JSON: " + jsonObject.toString());

                        if (jsonObject.has("data")) {
                            JSONArray pekerjaanArray = jsonObject.getJSONArray("data");
                            populateDropdown(pekerjaanArray, idPekerjaan, "pekerjaan");
                        } else {
                            Log.e("RegistrasiActivity", "Pekerjaan data not found in response");
                            Toast.makeText(getApplicationContext(), "Data pekerjaan tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("RegistrasiActivity", "JSON Parsing Error for Pekerjaan: " + e.getMessage());
                        Toast.makeText(getApplicationContext(), "Error parsing pekerjaan data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    String errorMessage = "Error fetching Pekerjaan data: ";
                    if (error.networkResponse != null) {
                        errorMessage += "Status Code: " + error.networkResponse.statusCode;
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e("RegistrasiActivity", "Error response body: " + responseBody);
                            errorMessage += "\nResponse: " + responseBody;
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else if (error.getMessage() != null) {
                        errorMessage += error.getMessage();
                    } else {
                        errorMessage += "Unknown error occurred";
                    }
                    Log.e("RegistrasiActivity", errorMessage);
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                return new HashMap<>(); // Add any required parameters here
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Method buat isi dropdown dengan data dari server
    private void populateDropdown(JSONArray jsonArray, AutoCompleteTextView autoCompleteTextView, String type) {
        try {
            Map<String, String> itemMap = new HashMap<>();
            List<String> itemList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                String id = item.getString("id_" + type);
                String nama = item.getString("nama_" + type);
                itemList.add(nama);
                itemMap.put(nama, id);
            }

            Collections.sort(itemList, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });

            String[] items = itemList.toArray(new String[0]);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
            autoCompleteTextView.setAdapter(adapter);

            autoCompleteTextView.setTag(itemMap);

            if (type.equals("kecamatan")) {
                autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedName = (String) parent.getItemAtPosition(position);
                    String selectedId = itemMap.get(selectedName);
                    Log.d("RegistrasiActivity", type + " selected: " + selectedName + " (ID: " + selectedId + ")");
                    fetchKelurahanByKecamatan(selectedId);
                });
            } else {
                autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedName = (String) parent.getItemAtPosition(position);
                    String selectedId = itemMap.get(selectedName);
                    Log.d("RegistrasiActivity", type + " selected: " + selectedName + " (ID: " + selectedId + ")");
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("RegistrasiActivity", "Error populating " + type + " dropdown: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Error populating " + type + " dropdown: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method buat cek apakah user udah pernah daftar sebelumnya
    private boolean hasUserAlreadyRegistered() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(HAS_REGISTERED_KEY + "_" + userEmail, false);
    }

    // Method buat daftarin user ke server
    private void registerUser() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateFields()) {
            Toast.makeText(this, "Harap isi semua data", Toast.LENGTH_SHORT).show();
            return;
        }

        String nomorKtp = etNik.getText().toString();
        if (isNomorKtpAlreadyRegistered(nomorKtp)) {
            Toast.makeText(this, "Nomor KTP sudah terdaftar. Silakan gunakan Nomor KTP lain.", Toast.LENGTH_LONG).show();
            return;
        }

        String url = BASE_URL + "register";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("RegistrasiActivity", "Registration Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String status = jsonObject.getString("status");
                        String message = jsonObject.getString("message");

                        if (status.equals("OK")) {
                            Toast.makeText(getApplicationContext(), "Registrasi Anda berhasil!", Toast.LENGTH_SHORT).show();
                            onRegistrationSuccess();
                        } else {
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "Error during registration: ";
                    if (error.networkResponse != null) {
                        errorMessage += "Status Code: " + error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                JSONObject jsonObject = new JSONObject(responseBody);
                                errorMessage += "\nMessage: " + jsonObject.optString("message", "Unknown error");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (error.getMessage() != null) {
                        errorMessage += error.getMessage();
                    } else {
                        errorMessage += "Unknown error occurred";
                    }
                    Log.e("RegistrasiActivity", errorMessage);
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("nama", etNama.getText().toString());
                params.put("email", userEmail); // Use the hidden email
                params.put("email", userEmail.isEmpty() ? etEmail.getText().toString() : userEmail);
                params.put("nomor_ktp", etNik.getText().toString());
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
                params.put("foto_lokasi", "data:image/jpeg;base64," + fotoRumahBase64);
                return params;
            }
        };

        // Set retry policy
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(stringRequest);
        onRegistrationSuccess();
    }

    // Method buat cek apakah NIK udah pernah didaftarin sebelumnya
    private boolean isNomorKtpAlreadyRegistered(String nomorKtp) {
        SharedPreferences prefs = getSharedPreferences("AllRegisteredNomorKTP", MODE_PRIVATE);
        return prefs.contains(nomorKtp);
    }

    // Method buat validasi semua field
    private boolean validateFields() {
        if (etNama.getText().toString().isEmpty() ||
                etNik.getText().toString().isEmpty() ||
                etNik.getText().toString().length() != 16 || // Membatasi agar nomor nik diisi sejumlah 16 digit
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
            Toast.makeText(this, "Harap isi semua field dengan benar", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Method buat proses gambar yang dipilih
    private void processImage(Uri imageUri, boolean isKTP) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            String encodedImage = encodeImageToBase64(selectedImage);
            if (isKTP) {
                fotoKTPBase64 = encodedImage;
                fotoKTP.setImageBitmap(selectedImage);
                fotoKTP.setVisibility(View.VISIBLE);
            } else {
                fotoRumahBase64 = encodedImage;
                fotoRumah.setImageBitmap(selectedImage);
                fotoRumah.setVisibility(View.VISIBLE);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Gambar tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method buat ngubah gambar jadi string base64
    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Method buat cek koneksi internet
    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Method buat handle tombol kembali
    public void btnKembali(View view) {
        Intent intent = new Intent(RegistrasiActivity.this, IndexPendaftaranLogin.class);
        startActivity(intent);
    }

    // Method yang dipanggil setelah registrasi berhasil
    private void onRegistrationSuccess() {
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();

        String userEmail = userPrefs.getString("email", "");

        // Tambahkan nomor KTP ke daftar yang sudah terdaftar
        String nomorKtp = etNik.getText().toString();
        SharedPreferences allNomorKTP = getSharedPreferences("AllRegisteredNomorKTP", MODE_PRIVATE);
        SharedPreferences.Editor ktpEditor = allNomorKTP.edit();
        ktpEditor.putString(nomorKtp, userEmail);
        ktpEditor.apply();


        // Simpan status registrasi berdasarkan email
        editor.putBoolean(HAS_REGISTERED_KEY + "_" + userEmail, true);

        // Simpan semua data registrasi dengan prefix email
        editor.putString("nama_" + userEmail, etNama.getText().toString());
        editor.putString("nomor_ktp_" + userEmail, etNik.getText().toString());
        editor.putString("alamat_" + userEmail, etAlamat.getText().toString());
        editor.putString("rt_" + userEmail, etRT.getText().toString());
        editor.putString("rw_" + userEmail, etRW.getText().toString());
        editor.putString("telp_hp_" + userEmail, etNoTelp.getText().toString());
        editor.putString("kode_pos_" + userEmail, etKodePos.getText().toString());
        editor.putString("jumlah_penghuni_" + userEmail, etJumlahPenghuni.getText().toString());
        editor.putString("latitude_" + userEmail, etLatitude.getText().toString());
        editor.putString("longitude_" + userEmail, etLongitude.getText().toString());

        // Simpan data dropdown
        editor.putString("pekerjaan_" + userEmail, idPekerjaan.getText().toString());
        editor.putString("kelurahan_" + userEmail, idKelurahan.getText().toString());
        editor.putString("kecamatan_" + userEmail, idKecamatan.getText().toString());

        // Simpan ID dari dropdown (jika diperlukan)
        Map<String, String> pekerjaanMap = (Map<String, String>) idPekerjaan.getTag();
        Map<String, String> kelurahanMap = (Map<String, String>) idKelurahan.getTag();
        Map<String, String> kecamatanMap = (Map<String, String>) idKecamatan.getTag();

        editor.putString("id_pekerjaan_" + userEmail, pekerjaanMap.get(idPekerjaan.getText().toString()));
        editor.putString("id_kelurahan_" + userEmail, kelurahanMap.get(idKelurahan.getText().toString()));
        editor.putString("id_kecamatan_" + userEmail, kecamatanMap.get(idKecamatan.getText().toString()));

        // Simpan base64 dari foto KTP dan foto rumah
        editor.putString("foto_ktp_" + userEmail, fotoKTPBase64);
        editor.putString("foto_rumah_" + userEmail, fotoRumahBase64);

        // Terapkan perubahan
        editor.apply();

        // Tampilkan pesan sukses
        Toast.makeText(this, "Registrasi Anda berhasil!", Toast.LENGTH_LONG).show();

        // Siapkan intent untuk pindah ke halaman Status
        Intent intent = new Intent(RegistrasiActivity.this, Status.class);
        intent.putExtra("NIK", etNik.getText().toString());
        intent.putExtra("REGISTERED", true);

        // Tambahkan flag untuk membersihkan stack activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Pindah ke halaman Status
        startActivity(intent);
        finish();
    }

    // Method buat bikin filter yang cuma nerima huruf dan spasi
    private InputFilter getTextOnlyFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetter(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
    }


}