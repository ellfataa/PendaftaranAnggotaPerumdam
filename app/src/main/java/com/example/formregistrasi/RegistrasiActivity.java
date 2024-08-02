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

    private Map<String, JSONArray> kelurahanByKecamatan = new HashMap<>();
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_registrasi);
        } catch (Exception e) {
            Log.e("RegistrasiActivity", "Error in onCreate", e);
            Toast.makeText(this, "Terjadi kesalahan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        initializeViews();
        setListeners();
        fetchDropdownData();

        // Get user data from intent
        String userName = getIntent().getStringExtra("userName");
        userEmail = getIntent().getStringExtra("userEmail");

        // Set name and email
        if (userName != null && !userName.isEmpty()) {
            etNama.setText(userName);
            // etNama.setEnabled(false);
        }
        if (userEmail != null && !userEmail.isEmpty()) {
            etEmail.setText(userEmail);
            etEmail.setVisibility(View.GONE); // Hide the email field
        }

        // Check if user has already registered
        if (hasUserAlreadyRegistered()) {
            Toast.makeText(this, "Anda sudah melakukan registrasi sebelumnya.", Toast.LENGTH_LONG).show();
            finish(); // Close the activity
            return;
        }

        btnPeta.setOnClickListener(v -> openMap());
    }

    // Fungsi buat set semua view yang ada di layout
    private void initializeViews() {
        etNama = findViewById(R.id.etNama);
        etNama.setFilters(new InputFilter[]{getTextOnlyFilter()});
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
        // Menyembunyikan tampilan imageView terlebih dahulu
        fotoKTP.setVisibility(View.GONE);
        fotoRumah.setVisibility(View.GONE);

        btnKembali = findViewById(R.id.btnKembali);
        btnDaftar = findViewById(R.id.btnDaftar);
        btnPickImgKTP = findViewById(R.id.btnPickImgKTP);
        btnPickImgRumah = findViewById(R.id.btnPickImgRumah);
        btnPickImgKTP.setOnClickListener(v -> showImageSourceDialog(true));
        btnPickImgRumah.setOnClickListener(v -> showImageSourceDialog(false));
        btnPeta = findViewById(R.id.btnPeta);
    }

    // Fungsi buat ngasih aksi ke tombol-tombol
    private void setListeners() {
        btnPickImgKTP.setOnClickListener(v -> pickImage(REQUEST_IMAGE_KTP));
        btnPickImgRumah.setOnClickListener(v -> pickImage(REQUEST_IMAGE_RUMAH));
        btnDaftar.setOnClickListener(v -> registerUser());
        btnKembali.setOnClickListener(this::btnKembali);
        btnPeta.setOnClickListener(v -> openMap());
        btnPeta.setOnClickListener(v -> checkLocationServiceAndOpenMap());
    }

    // Fungsi buat ngecek layanan lokasi dan buka peta
    private void checkLocationServiceAndOpenMap() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationSettingsDialog();
        } else {
            openMap();
        }
    }

    // Fungsi buat nampilin dialog pengaturan lokasi
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

    // Fungsi buat milih gambar dari galeri
    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    // Fungsi buat buka halaman Maps buat pilih lokasi
    private void openMap() {
        Intent intent = new Intent(this, Maps.class);
        startActivityForResult(intent, REQUEST_CODE_MAP);
    }

    // Fungsi buat nampilin dialog pilihan sumber gambar (BELUM BISA DIGUNAKAN)
    private void showImageSourceDialog(boolean isKTP) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Sumber Gambar");
        String[] options = {"Kamera", "Galeri"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                dispatchTakePictureIntent(isKTP);
            } else {
                pickImage(isKTP ? REQUEST_IMAGE_KTP : REQUEST_IMAGE_RUMAH);
            }
        });
        builder.show();
    }

    // Fungsi buat ngambil gambar pake kamera (BELUM BISA DIGUNAKAN)
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

    // Fungsi buat bikin file gambar
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

    // Fungsi buat ngatur hasil dari aktivitas lain (milih gambar atau lokasi)
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
                case REQUEST_IMAGE_CAPTURE_KTP:
                    if (photoUriKTP != null) {
                        processImage(photoUriKTP, true);
                    }
                    break;
                case REQUEST_IMAGE_CAPTURE_RUMAH:
                    if (photoUriRumah != null) {
                        processImage(photoUriRumah, false);
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

    // Fungsi buat ngambil data buat dropdown dari server
    private void fetchDropdownData() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchKecamatan();
        fetchPekerjaan();
    }

    // Fungsi buat ngambil data kecamatan dari server
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

    // Fungsi buat ngambil data kelurahan berdasarkan kecamatan dari server
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

    // Fungsi buat ngambil data pekerjaan dari server
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

    // Fungsi buat ngisi dropdown dengan data dari server
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

    private boolean hasUserAlreadyRegistered() {
        SharedPreferences prefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
        return prefs.getBoolean(userEmail + "_registered", false);
    }

    // Fungsi buat ngedaftarin data inputan user ke server database
    private void registerUser() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateFields()) {
            Toast.makeText(this, "Harap isi semua data", Toast.LENGTH_SHORT).show();
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
                error -> Toast.makeText(getApplicationContext(), "Error during registration: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("nama", etNama.getText().toString());
                params.put("email", userEmail); // Use the hidden email
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

        Volley.newRequestQueue(this).add(stringRequest);
    }


    // Fungsi buat ngecek apakah semua field udah diisi dengan bener atau belum
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

    // Fungsi buat memproses gambar yang dipilih user
    private void processImage(Uri imageUri, boolean isKTP) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            String encodedImage = encodeImageToBase64(selectedImage);
            if (isKTP) {
                fotoKTPBase64 = encodedImage;
                fotoKTP.setImageBitmap(selectedImage);
                fotoKTP.setVisibility(View.VISIBLE); // Memunculkan ImageView
            } else {
                fotoRumahBase64 = encodedImage;
                fotoRumah.setImageBitmap(selectedImage);
                fotoRumah.setVisibility(View.VISIBLE); // Memunculkan ImageView
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Gambar tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }
    }

    // Fungsi buat ubah gambar jadi format base64
    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Fungsi buat ngecek koneksi internet
    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Fungsi buat aksi tombol kembali ke IndexPendaftaranLogin
    public void btnKembali(View view) {
        Intent intent = new Intent(RegistrasiActivity.this, IndexPendaftaranLogin.class);
        startActivity(intent);
    }

    // Fungsi yang dijalanin kalo user berhasil registrasi
    private void onRegistrationSuccess() {
        // Save registration status
        SharedPreferences registrationPrefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
        SharedPreferences.Editor registrationEditor = registrationPrefs.edit();
        registrationEditor.putBoolean(userEmail + "_registered", true);
        registrationEditor.apply();

        Log.d("RegistrasiActivity", "Registration success for email: " + userEmail);

        Toast.makeText(this, "Registrasi Anda berhasil!", Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);

        // Redirect to LoginActivity or appropriate next screen
        Intent intent = new Intent(RegistrasiActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Fungsi buat memfilter input cuma bisa teks doang
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