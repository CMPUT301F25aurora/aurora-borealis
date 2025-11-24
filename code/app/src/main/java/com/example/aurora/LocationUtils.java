package com.example.aurora;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationUtils {

    public static boolean isLocationPermissionGranted(Context ctx) {
        return ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Context ctx) {
        if (ctx instanceof Activity) {
            ActivityCompat.requestPermissions(
                    (Activity) ctx,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    101
            );
        }
    }

    public interface OnLocationResult {
        void onLocation(double lat, double lng);
    }

    public static void getUserLocation(Context ctx, OnLocationResult callback) {
        FusedLocationProviderClient fused = LocationServices.getFusedLocationProviderClient(ctx);

        if (!isLocationPermissionGranted(ctx)) {
            callback.onLocation(Double.NaN, Double.NaN);
            return;
        }

        fused.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        callback.onLocation(Double.NaN, Double.NaN);
                    }
                });
    }
}
