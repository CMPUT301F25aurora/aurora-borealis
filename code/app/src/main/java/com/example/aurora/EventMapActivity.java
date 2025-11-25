package com.example.aurora;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

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

public class EventMapActivity extends BaseActivity implements OnMapReadyCallback {

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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        loadEventLocation();
    }

    // Load eventLat/eventLng from Firestore
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

    // Load waitingLocations from Firestore
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

    // Fit camera to show all markers
    private void zoomToMarkers() {
        try {
            LatLngBounds bounds = boundsBuilder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140));
        } catch (Exception e) {
            Log.e("MAP", "Error zooming: " + e.getMessage());
        }
    }

    // Utility: scale PNG icons into proper Google Maps marker size
    private BitmapDescriptor getBitmapDescriptor(int resId) {
        int height = 110;
        int width = 110;
        Bitmap b = BitmapFactory.decodeResource(getResources(), resId);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(smallMarker);
    }
}
