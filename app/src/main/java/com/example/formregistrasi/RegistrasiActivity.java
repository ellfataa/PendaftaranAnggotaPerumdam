package com.example.formregistrasi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
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

    private static final int REQUEST_CODE_MAP = 1001;
    private static final int REQUEST_IMAGE_KTP = 1;
    private static final int REQUEST_IMAGE_RUMAH = 2;

    private EditText etNama, etNik, etAlamat, etRT, etRW, etNoTelp, etKodePos, etJumlahPenghuni, etLatitude, etLongitude;
    private AutoCompleteTextView idPekerjaan, idKelurahan, idKecamatan;
    private ImageView fotoKTP, fotoRumah;
    private String fotoKTPBase64, fotoRumahBase64;
    private Button btnKembali, btnDaftar, btnPickImgKTP, btnPickImgRumah, btnPeta;

    private Map<String, JSONArray> kelurahanByKecamatan = new HashMap<>();

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

        btnPeta.setOnClickListener(v -> openMap());
    }

    private void initializeViews() {
        etNama = findViewById(R.id.etNama);
        etNama.setFilters(new InputFilter[]{getTextOnlyFilter()});
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
        btnPickImgKTP = findViewById(R.id.btnPickImgKTP);
        btnPickImgRumah = findViewById(R.id.btnPickImgRumah);
        btnPeta = findViewById(R.id.btnPeta);
    }

    private void setListeners() {
        btnPickImgKTP.setOnClickListener(v -> pickImage(REQUEST_IMAGE_KTP));
        btnPickImgRumah.setOnClickListener(v -> pickImage(REQUEST_IMAGE_RUMAH));
        btnDaftar.setOnClickListener(v -> registerUser());
        btnKembali.setOnClickListener(this::btnKembali);
        btnPeta.setOnClickListener(v -> openMap());
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    private void openMap() {
        Intent intent = new Intent(this, Maps.class);
        startActivityForResult(intent, REQUEST_CODE_MAP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_MAP) {
                String latitude = data.getStringExtra("SELECTED_LATITUDE");
                String longitude = data.getStringExtra("SELECTED_LONGITUDE");

                etLatitude.setText(latitude);
                etLongitude.setText(longitude);
            } else if (requestCode == REQUEST_IMAGE_KTP) {
                Uri imageUri = data.getData();
                processImage(imageUri, true);
            } else if (requestCode == REQUEST_IMAGE_RUMAH) {
                Uri imageUri = data.getData();
                processImage(imageUri, false);
            }
        }
    }

    private void organizeKelurahanByKecamatan(JSONArray kelurahanArray) throws JSONException {
        for (int i = 0; i < kelurahanArray.length(); i++) {
            JSONObject kelurahan = kelurahanArray.getJSONObject(i);
            String idKecamatan = kelurahan.getString("id_kecamatan");
            if (!kelurahanByKecamatan.containsKey(idKecamatan)) {
                kelurahanByKecamatan.put(idKecamatan, new JSONArray());
            }
            kelurahanByKecamatan.get(idKecamatan).put(kelurahan);
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
                        Log.d("RegistrasiActivity", "Parsed JSON: " + jsonObject.toString());

                        if (jsonObject.has("kecamatan")) {
                            JSONArray kecamatanArray = jsonObject.getJSONArray("kecamatan");
                            populateDropdown(kecamatanArray, idKecamatan, "kecamatan");
                        }

                        if (jsonObject.has("kelurahan")) {
                            JSONArray kelurahanArray = jsonObject.getJSONArray("kelurahan");
                            organizeKelurahanByKecamatan(kelurahanArray);
                        }

                        if (jsonObject.has("pekerjaan")) {
                            JSONArray pekerjaanArray = jsonObject.getJSONArray("pekerjaan");
                            populateDropdown(pekerjaanArray, idPekerjaan, "pekerjaan");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("RegistrasiActivity", "JSON Parsing Error: " + e.getMessage());
                    }
                },
                error -> {
                    error.printStackTrace();
                    String errorMessage = "Error fetching data: ";
                    if (error.networkResponse != null) {
                        errorMessage += "Status Code: " + error.networkResponse.statusCode;
                    } else if (error.getMessage() != null) {
                        errorMessage += error.getMessage();
                    } else {
                        errorMessage += "Unknown error occurred";
                    }
                    Log.e("RegistrasiActivity", errorMessage);
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(stringRequest);
    }

    private void populateKelurahanDropdown(String idKecamatan) {
        JSONArray kelurahanArray = kelurahanByKecamatan.get(idKecamatan);
        if (kelurahanArray != null) {
            populateDropdown(kelurahanArray, idKelurahan, "kelurahan");
        } else {
            idKelurahan.setText("");
            idKelurahan.setAdapter(null);
        }
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

            if (type.equals("kecamatan")) {
                autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedName = (String) parent.getItemAtPosition(position);
                    String selectedId = itemMap.get(selectedName);
                    Log.d("RegistrasiActivity", type + " selected: " + selectedName + " (ID: " + selectedId + ")");
                    populateKelurahanDropdown(selectedId);
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
                            Toast.makeText(getApplicationContext(), "Registration successful!", Toast.LENGTH_SHORT).show();
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
        if (etNama.getText().toString().isEmpty() ||
                etNik.getText().toString().isEmpty() ||
                etNik.getText().toString().length() != 16 ||
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
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
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

    private void onRegistrationSuccess() {
        String nik = etNik.getText().toString();

        // Save registration status
        SharedPreferences registrationPrefs = getSharedPreferences("RegistrationPrefs", MODE_PRIVATE);
        SharedPreferences.Editor registrationEditor = registrationPrefs.edit();
        registrationEditor.putBoolean(nik + "_registered", true);
        registrationEditor.apply();

        Log.d("RegistrasiActivity", "Registration success for NIK: " + nik);

        // Show success message
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show();

        // Redirect to LoginActivity
        Intent intent = new Intent(RegistrasiActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close RegistrasiActivity
    }

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