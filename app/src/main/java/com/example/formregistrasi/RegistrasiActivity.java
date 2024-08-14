package com.example.formregistrasi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Uri photoUriKTP;
    private Uri photoUriRumah;
    private ProgressDialog progressDialog;

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
    private String fotoKTPPath;
    private String fotoRumahPath;
    private String lineEnd;
    private Object response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasUserAlreadyRegistered()) {
            Toast.makeText(this, "Anda sudah melakukan registrasi sebelumnya.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_registrasi);

        initializeViews();
        setListeners();
        fetchDropdownData();

        fotoKTPPath = null;
        fotoRumahPath = null;

        userName = getIntent().getStringExtra("userName");
        userEmail = getIntent().getStringExtra("userEmail");

        if (userName != null && !userName.isEmpty()) {
            etNama.setText(userName);
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
            String name = sharedPreferences.getString("name", "");
            if (!name.isEmpty()) {
                etNama.setText(name);
            }
        }

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userEmail = sharedPreferences.getString("email", "");
        if (!userEmail.isEmpty()) {
            txtUserEmail.setText(userEmail);
            txtUserEmail.setVisibility(View.GONE);
            etEmail.setVisibility(View.GONE);
        }

        if (savedInstanceState != null) {
            String uriKTP = savedInstanceState.getString("photoUriKTP");
            String uriRumah = savedInstanceState.getString("photoUriRumah");
            if (uriKTP != null) photoUriKTP = Uri.parse(uriKTP);
            if (uriRumah != null) photoUriRumah = Uri.parse(uriRumah);
            fotoKTPPath = savedInstanceState.getString("fotoKTPPath");
            fotoRumahPath = savedInstanceState.getString("fotoRumahPath");

            if (fotoKTPPath != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(fotoKTPPath);
                fotoKTP.setImageBitmap(bitmap);
                fotoKTP.setVisibility(View.VISIBLE);
            }
            if (fotoRumahPath != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(fotoRumahPath);
                fotoRumah.setImageBitmap(bitmap);
                fotoRumah.setVisibility(View.VISIBLE);
            }
        }

        btnPeta.setOnClickListener(v -> openMap());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang mendaftarkan...");
        progressDialog.setCancelable(false);
    }

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

    private void setListeners() {
        btnPickImgKTP.setOnClickListener(v -> pickImage(REQUEST_IMAGE_KTP));
        btnPickImgRumah.setOnClickListener(v -> pickImage(REQUEST_IMAGE_RUMAH));
        btnDaftar.setOnClickListener(v -> registerUser());
        btnKembali.setOnClickListener(this::btnKembali);
        btnPeta.setOnClickListener(v -> checkLocationServiceAndOpenMap());
    }

    private void fetchDropdownData() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchKecamatan();
        fetchPekerjaan();
    }

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
                return new HashMap<>();
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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(HAS_REGISTERED_KEY + "_" + userEmail, false);
    }

    private void registerUser() {
        if (!checkNetworkConnection()) {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateFields()) {
            Log.d("RegistrasiActivity", "Validation failed");
            Toast.makeText(this, "Harap isi semua field dengan benar", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("RegistrasiActivity", "All fields validated successfully");
        progressDialog.show();

        String url = BASE_URL + "register";

        try {
            String boundary = "Volley-" + System.currentTimeMillis();
            String mimeType = "multipart/form-data;boundary=" + boundary;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            // Add string params
            Map<String, String> stringParams = new HashMap<>();
            stringParams.put("nama", etNama.getText().toString());
            stringParams.put("email", userEmail.isEmpty() ? etEmail.getText().toString() : userEmail);
            stringParams.put("nomor_ktp", etNik.getText().toString());
            stringParams.put("alamat", etAlamat.getText().toString());
            stringParams.put("rt", etRT.getText().toString());
            stringParams.put("rw", etRW.getText().toString());
            stringParams.put("telp_hp", etNoTelp.getText().toString());
            stringParams.put("kode_pos", etKodePos.getText().toString());
            stringParams.put("jumlah_penghuni", etJumlahPenghuni.getText().toString());
            stringParams.put("latitude", etLatitude.getText().toString());
            stringParams.put("longitude", etLongitude.getText().toString());

            Map<String, String> pekerjaanMap = (Map<String, String>) idPekerjaan.getTag();
            Map<String, String> kelurahanMap = (Map<String, String>) idKelurahan.getTag();
            Map<String, String> kecamatanMap = (Map<String, String>) idKecamatan.getTag();

            stringParams.put("id_pekerjaan", pekerjaanMap.get(idPekerjaan.getText().toString()));
            stringParams.put("id_kelurahan", kelurahanMap.get(idKelurahan.getText().toString()));
            stringParams.put("id_kecamatan", kecamatanMap.get(idKecamatan.getText().toString()));

            for (Map.Entry<String, String> entry : stringParams.entrySet()) {
                buildTextPart(dos, boundary, entry.getKey(), entry.getValue());
            }

            // Add image parts
            buildFilePart(dos, boundary, "foto_ktp", fotoKTPPath);
            buildFilePart(dos, boundary, "foto_lokasi", fotoRumahPath);

            // End of multipart/form-data
            dos.writeBytes("--" + boundary + "--" + lineEnd);

            MultipartRequest multipartRequest = new MultipartRequest(
                    url,
                    null, // headers
                    mimeType,
                    bos.toByteArray(),
                    response -> {
                        progressDialog.dismiss();
                        handleRegistrationResponse(response);
                    },
                    error -> {
                        progressDialog.dismiss();
                        handleRegistrationError(error);
                    }
            );

            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(this).add(multipartRequest);

        } catch (IOException e) {
            e.printStackTrace();
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Error preparing request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRegistrationResponse(NetworkResponse response) {
        try {
            String responseBody = new String(response.data, StandardCharsets.UTF_8);
            Log.d("RegistrasiActivity", "Raw server response: " + responseBody);
            JSONObject jsonObject = new JSONObject(responseBody);

            if (jsonObject.has("success")) {
                boolean success = jsonObject.getBoolean("success");
                String message = jsonObject.optString("message", "No message provided");

                if (success) {
                    Toast.makeText(getApplicationContext(), "Registrasi Anda berhasil!", Toast.LENGTH_SHORT).show();
                    onRegistrationSuccess();
                } else {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Unexpected server response", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRegistrationError(VolleyError error) {
        String errorMessage = "Error during registration: ";
        if (error.networkResponse != null) {
            errorMessage += "Status Code: " + error.networkResponse.statusCode;
            if (error.networkResponse.data != null) {
                try {
                    String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    Log.d("RegistrasiActivity", "Error response: " + responseBody);
                    JSONObject jsonObject = new JSONObject(responseBody);
                    if (jsonObject.has("message")) {
                        errorMessage = jsonObject.getString("message");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorMessage += "\nError parsing error response: " + e.getMessage();
                }
            }
        } else if (error.getMessage() != null) {
            errorMessage += error.getMessage();
        } else {
            errorMessage += "Unknown error occurred";
        }
        Log.e("RegistrasiActivity", errorMessage);
        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private void buildTextPart(DataOutputStream dataOutputStream, String boundary, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes("--" + boundary + "\r\n");
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"\r\n");
        dataOutputStream.writeBytes("\r\n");
        dataOutputStream.writeBytes(parameterValue + "\r\n");
    }

    private void buildFilePart(DataOutputStream dataOutputStream, String boundary, String parameterName, String fileName) throws IOException {
        dataOutputStream.writeBytes("--" + boundary + "\r\n");
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"; filename=\"" + fileName + "\"\r\n");
        dataOutputStream.writeBytes("Content-Type: image/jpeg\r\n");
        dataOutputStream.writeBytes("\r\n");

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

    private boolean validateFields() {
        if (etNama.getText().toString().trim().isEmpty() ||
                etNik.getText().toString().trim().isEmpty() ||
                etNik.getText().toString().trim().length() != 16 ||
                etAlamat.getText().toString().trim().isEmpty() ||
                etRT.getText().toString().trim().isEmpty() ||
                etRW.getText().toString().trim().isEmpty() ||
                etNoTelp.getText().toString().trim().isEmpty() ||
                etKodePos.getText().toString().trim().isEmpty() ||
                etJumlahPenghuni.getText().toString().trim().isEmpty() ||
                etLatitude.getText().toString().trim().isEmpty() ||
                etLongitude.getText().toString().trim().isEmpty() ||
                idPekerjaan.getText().toString().trim().isEmpty() ||
                idKelurahan.getText().toString().trim().isEmpty() ||
                idKecamatan.getText().toString().trim().isEmpty() ||
                fotoKTPPath == null ||
                fotoRumahPath == null) {
            return false;
        }
        return true;
    }

    private void processImage(Uri imageUri, boolean isKTP) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            String fileName = isKTP ? "ktp_" + System.currentTimeMillis() + ".jpg" : "rumah_" + System.currentTimeMillis() + ".jpg";
            String imagePath = saveImageToInternalStorage(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), fileName);

            if (isKTP) {
                fotoKTPPath = imagePath;
                fotoKTP.setImageBitmap(selectedImage);
                fotoKTP.setVisibility(View.VISIBLE);
            } else {
                fotoRumahPath = imagePath;
                fotoRumah.setImageBitmap(selectedImage);
                fotoRumah.setVisibility(View.VISIBLE);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Gambar tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
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
        SharedPreferences userPrefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();

        String userEmail = userPrefs.getString("email", "");
        String nomorKtp = etNik.getText().toString();

        editor.putBoolean(HAS_REGISTERED_KEY + "_" + userEmail, true);

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

        editor.putString("pekerjaan_" + userEmail, idPekerjaan.getText().toString());
        editor.putString("kelurahan_" + userEmail, idKelurahan.getText().toString());
        editor.putString("kecamatan_" + userEmail, idKecamatan.getText().toString());

        Map<String, String> pekerjaanMap = (Map<String, String>) idPekerjaan.getTag();
        Map<String, String> kelurahanMap = (Map<String, String>) idKelurahan.getTag();
        Map<String, String> kecamatanMap = (Map<String, String>) idKecamatan.getTag();

        editor.putString("id_pekerjaan_" + userEmail, pekerjaanMap.get(idPekerjaan.getText().toString()));
        editor.putString("id_kelurahan_" + userEmail, kelurahanMap.get(idKelurahan.getText().toString()));
        editor.putString("id_kecamatan_" + userEmail, kecamatanMap.get(idKecamatan.getText().toString()));

        editor.putString("foto_ktp_" + userEmail, fotoKTPBase64);
        editor.putString("foto_rumah_" + userEmail, fotoRumahBase64);

        editor.apply();

        SharedPreferences allNomorKTP = getSharedPreferences("AllRegisteredNomorKTP", MODE_PRIVATE);
        SharedPreferences.Editor ktpEditor = allNomorKTP.edit();
        ktpEditor.putString(nomorKtp, userEmail);
        ktpEditor.apply();

        Toast.makeText(this, "Registrasi Anda berhasil!", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(RegistrasiActivity.this, Status.class);
        intent.putExtra("NOMOR_KTP", nomorKtp);
        intent.putExtra("JUST_REGISTERED", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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

    private void checkLocationServiceAndOpenMap() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationSettingsDialog();
        } else {
            openMap();
        }
    }

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

    private void openMap() {
        Intent intent = new Intent(this, Maps.class);
        startActivityForResult(intent, REQUEST_CODE_MAP);
    }

    private void pickImage(int requestCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            launchImagePicker(requestCode);
        }
    }

    private void launchImagePicker(int requestCode) {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(requestCode == REQUEST_IMAGE_KTP);
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.formregistrasi.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                if (requestCode == REQUEST_IMAGE_KTP) {
                    photoUriKTP = photoURI;
                } else {
                    photoUriRumah = photoURI;
                }
            }
        }

        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooserIntent, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, lanjutkan dengan membuka pemilih gambar
                launchImagePicker(REQUEST_IMAGE_KTP); // atau REQUEST_IMAGE_RUMAH, tergantung konteks
            } else {
                // Izin ditolak, beri tahu pengguna
                Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_LONG).show();
            }
        }
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_KTP:
                case REQUEST_IMAGE_RUMAH:
                    Uri imageUri;
                    if (data != null && data.getData() != null) {
                        // Gallery result
                        imageUri = data.getData();
                    } else {
                        // Camera result
                        imageUri = (requestCode == REQUEST_IMAGE_KTP) ? photoUriKTP : photoUriRumah;
                    }
                    processImage(imageUri, requestCode == REQUEST_IMAGE_KTP);
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

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private String saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("images", Context.MODE_PRIVATE);
        File file = new File(directory, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUriKTP != null) outState.putString("photoUriKTP", photoUriKTP.toString());
        if (photoUriRumah != null) outState.putString("photoUriRumah", photoUriRumah.toString());
        outState.putString("fotoKTPPath", fotoKTPPath);
        outState.putString("fotoRumahPath", fotoRumahPath);
    }
}