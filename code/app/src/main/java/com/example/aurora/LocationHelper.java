package com.example.aurora;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Helper class for getting user's current location.
 *
 * Uses Google Play Services FusedLocationProviderClient for accurate location.
 */
public class LocationHelper {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Callback interface for location results
     */
    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onLocationFailed(String error);
    }

    /**
     * Check if location permissions are granted
     */
    public static boolean hasLocationPermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location permissions from user
     */
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Get the user's current location
     */
    public static void getCurrentLocation(Activity activity, LocationCallback callback) {
        // Check permissions first
        if (!hasLocationPermission(activity)) {
            callback.onLocationFailed("Location permission not granted");
            return;
        }

        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(activity);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                callback.onLocationReceived(latitude, longitude);
                            } else {
                                callback.onLocationFailed("Location not available. Please ensure location services are enabled.");
                            }
                        }
                    })
                    .addOnFailureListener(activity, e -> {
                        callback.onLocationFailed("Failed to get location: " + e.getMessage());
                    });
        } catch (SecurityException e) {
            callback.onLocationFailed("Location permission denied");
        }
    }

    /**
     * Format location as a readable string
     */
    public static String formatLocation(double latitude, double longitude) {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}