/*
 * References for this screen:
 *
 * 1) source: Google Developers — "Maps SDK for Android"
 *    https://developers.google.com/maps/documentation/android-sdk/start
 *    Used for setting up GoogleMap, SupportMapFragment, and map callbacks.
 *
 * 2) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for loading event coordinates and waitingLocations from Firestore.
 *
 * 3) author: Stack Overflow user — "Fit GoogleMap camera to all markers"
 *    https://stackoverflow.com/questions/14828217/fit-all-markers-on-google-maps-v2
 *    Used for building LatLngBounds to auto-zoom to all markers.
 */

package com.example.aurora.map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import com.example.aurora.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
/**
 * EventMapActivity
 *
 * Displays a Google Map showing:
 *  The event's location (eventLat, eventLng)
 *  All entrants' join locations from waitingLocations subcollection
 *
 * Features:
 *  Custom event pin icon
 *  Custom entrant pin icon
 *  Auto-zoom and camera bounds to show all markers
 *  Back button to close the map screen
 *
 * This screen is opened when user taps "View Map" inside event details.
 */
public class EventMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String eventId;

    private LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_map);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapEventView);

        mapFragment.getMapAsync(this);
    }

    /**
     * Called when the Google Map is fully initialized and ready.
     * @param googleMap The active GoogleMap instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        loadEventLocation();
    }

    /**
     * Loads the main event coordinates (eventLat, eventLng)
     * Adds an event marker and then loads entrant markers.
     */
    private void loadEventLocation() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Double lat = doc.getDouble("eventLat");
                    Double lng = doc.getDouble("eventLng");

                    if (lat == null || lng == null) return;

                    LatLng eventPos = new LatLng(lat, lng);

                    mMap.addMarker(new MarkerOptions()
                            .position(eventPos)
                            .title("Event Location")
                            .icon(getBitmapDescriptor(R.drawable.event_pin)));

                    boundsBuilder.include(eventPos);

                    loadEntrantLocations();
                });
    }

    /**
     * Loads all entrant join locations from:
     *    events/{eventId}/waitingLocations
     *
     * Each location represents where a user joined the waiting list.
     * Adds markers for all entrants.
     */
    private void loadEntrantLocations() {
        CollectionReference ref = db.collection("events")
                .document(eventId)
                .collection("waitingLocations");

        ref.get().addOnSuccessListener(snap -> {

            for (QueryDocumentSnapshot doc : snap) {
                Double lat = doc.getDouble("lat");
                Double lng = doc.getDouble("lng");

                if (lat == null || lng == null) continue;

                LatLng pos = new LatLng(lat, lng);

                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("Entrant: " + doc.getString("userKey"))
                        .icon(getBitmapDescriptor(R.drawable.entrant_pin)));

                boundsBuilder.include(pos);
            }

            zoomToMarkers();
        });
    }

    /**
     * Automatically zooms the Google Map to show all markers.
     * Uses LatLngBounds built from event + entrant positions.
     */
    private void zoomToMarkers() {
        try {
            LatLngBounds bounds = boundsBuilder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140));
        } catch (Exception e) {
            Log.e("MAP", "Error zooming: " + e.getMessage());
        }
    }

    /**
     * Utility function that scales a PNG drawable into a Google Maps marker-sized BitmapDescriptor.
     * @param resId drawable resource ID
     * @return scaled BitmapDescriptor for Google Maps
     */
    private BitmapDescriptor getBitmapDescriptor(int resId) {
        int height = 110;
        int width = 110;
        Bitmap b = BitmapFactory.decodeResource(getResources(), resId);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(smallMarker);
    }
}
