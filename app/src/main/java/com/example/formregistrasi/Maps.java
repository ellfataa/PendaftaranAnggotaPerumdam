package com.example.formregistrasi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Maps extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap gMap;
    private Marker currentLocationMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private Button btnPilihLokasi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.idMap);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnPilihLokasi = findViewById(R.id.btn_pilihLokasi);
        btnPilihLokasi.setOnClickListener(v -> onLocationSelected());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        setupMap();
    }

    private void setupMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            gMap.setMyLocationEnabled(true);
            gMap.getUiSettings().setMyLocationButtonEnabled(true);
            gMap.getUiSettings().setZoomControlsEnabled(true);
            gMap.getUiSettings().setZoomGesturesEnabled(true);
            gMap.getUiSettings().setScrollGesturesEnabled(true);
            gMap.getUiSettings().setRotateGesturesEnabled(true);
            gMap.getUiSettings().setTiltGesturesEnabled(true);

            getCurrentLocation();
        }

        gMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                currentLocationMarker = marker;
            }
        });

        gMap.setOnMapClickListener(latLng -> {
            if (currentLocationMarker != null) {
                currentLocationMarker.setPosition(latLng);
            } else {
                currentLocationMarker = gMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Lokasi yang Dipilih")
                        .draggable(true));
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        currentLocationMarker = gMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Lokasi yang Dipilih")
                                .draggable(true));
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onLocationSelected() {
        if (currentLocationMarker != null) {
            LatLng selectedLocation = currentLocationMarker.getPosition();
            String latitude = String.format("%.6f", selectedLocation.latitude);
            String longitude = String.format("%.6f", selectedLocation.longitude);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_LATITUDE", latitude);
            resultIntent.putExtra("SELECTED_LONGITUDE", longitude);
            setResult(RESULT_OK, resultIntent);
            finish(); // Ini akan menutup Maps dan kembali ke activity sebelumnya (RegistrasiActivity)
        } else {
            Toast.makeText(this, "Silakan pilih lokasi pada peta terlebih dahulu", Toast.LENGTH_SHORT).show();
        }
    }
}