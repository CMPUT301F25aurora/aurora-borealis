/*
 * References for this screen:
 *
 * 1) source: Google Maps SDK — "Maps SDK for Android"
 *    https://developers.google.com/maps/documentation/android-sdk
 *    Used for implementing GoogleMap, markers, and tap listeners.
 *
 * 2) source: Android Developers — "Request App Permissions"
 *    https://developer.android.com/training/permissions/requesting
 *    Used for runtime ACCESS_FINE_LOCATION permission flow.
 *
 */
package com.example.aurora.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.aurora.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * MapPickerActivity
 *
 * This screen allows the user to visually select an event location on
 * a Google Map by tapping anywhere on the map.
 *
 * Features:
 *  Displays a Google Map with the user's current location (if permitted).
 *  Lets the user tap anywhere to drop a marker.
 *  Sends the selected latitude/longitude back to the caller activity via setResult().
 *  Handles runtime location permissions cleanly.
 */

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Marker currentMarker;
    private LatLng selectedLatLng;

    private FusedLocationProviderClient fusedLocationClient;
    private Button btnConfirm;
    private static final int REQUEST_LOCATION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnConfirm = findViewById(R.id.btnConfirmLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Tap on the map to choose a location", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent result = new Intent();
            result.putExtra("lat", selectedLatLng.latitude);
            result.putExtra("lng", selectedLatLng.longitude);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    /**
     * Called when the GoogleMap is fully ready.
     * Sets tap listener and enables user location display.
     *
     * @param googleMap the fully loaded Google Map instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable tap listener to drop marker
        mMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;

            if (currentMarker != null) {
                currentMarker.remove();
            }

            currentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Event Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        });

        enableUserLocation();
    }

    /**
     * Requests location permission if not granted.
     * If granted, enables blue "my location" dot and centers the camera.
     */
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION
            );
            return;
        }

        mMap.setMyLocationEnabled(true);
        // Center map on user's current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 14f));
                    }
                });
    }

    /**
     * Handles the result of a permission request.
     * If location is granted, enable location display; otherwise show a warning.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                enableUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
