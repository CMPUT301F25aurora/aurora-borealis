/*
 * source: Android Developers — "Request location permissions".
 * url: https://developer.android.com/training/location/permissions
 * note: Used for checking ContextCompat.checkSelfPermission and requesting permissions if needed.
 *
 * source: Android Developers — "Get the last known location".
 * url: https://developer.android.com/develop/sensors-and-location/location/retrieve-current
 * note: Used for FusedLocationProviderClient.getLastLocation() to fetch the user's coordinates.
 *
 * source: Stack Overflow user — "Check if GPS is enabled programmatically".
 * url: https://stackoverflow.com/questions/843675/how-do-i-find-out-if-the-gps-is-enabled
 * note: Logic adapted for isGpsEnabled() using the LocationManager system service.
 */

package com.example.aurora.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationUtils {

    /** Permission check **/
    public static boolean isLocationPermissionGranted(Context ctx) {
        return ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Request fine location permission **/
    public static void requestLocationPermission(Context ctx) {
        if (ctx instanceof Activity) {
            ActivityCompat.requestPermissions(
                    (Activity) ctx,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    101
            );
        }
    }

    /** Check GPS / device location setting **/
    public static boolean isGpsEnabled(Context ctx) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /** Callback interface **/
    public interface OnLocationResult {
        void onLocation(double lat, double lng);
    }

    /**
     * Fetch user location ONLY if:
     * - Permission granted
     * - GPS enabled
     * Otherwise return NaN to indicate invalid.
     */
    public static void getUserLocation(Context ctx, OnLocationResult callback) {

        // 1. Permission check
        if (!isLocationPermissionGranted(ctx)) {
            callback.onLocation(Double.NaN, Double.NaN);
            return;
        }

        // 2. GPS enabled check
        if (!isGpsEnabled(ctx)) {
            callback.onLocation(Double.NaN, Double.NaN);
            return;
        }

        // 3. Try obtaining last known location
        FusedLocationProviderClient fused = LocationServices.getFusedLocationProviderClient(ctx);
        fused.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        // No last known location
                        callback.onLocation(Double.NaN, Double.NaN);
                    }
                });
    }
}
