/*
 * References for this screen:
 *
 * 1) author: Stack Overflow user — "How to generate a QR Code for an Android application?"
 *    https://stackoverflow.com/questions/8800919/how-to-generate-a-qr-code-for-an-android-application
 *    Used for the QRCodeWriter + BitMatrix pattern to draw a QR code Bitmap.
 *
 * 2) source: Firebase docs — "Get data with Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/get-data
 *    Used for reading a single event document and its fields from Firestore.
 *
 * 3) source: Firebase docs — "Perform simple and compound queries in Cloud Firestore"
 *    https://firebase.google.com/docs/firestore/query-data/queries
 *    Used for loading related data such as entrants or logs tied to the event.
 *
 * 4) source: Android Developers — "Dialogs"
 *    https://developer.android.com/develop/ui/views/components/dialogs
 *    Used for showing an AlertDialog with a custom view around the QR code or actions.
 *
 * 5) source: Android Developers — "Settings.Secure"
 *    https://developer.android.com/reference/android/provider/Settings.Secure
 *    Used when reading a device identifier like ANDROID_ID to tag actions from this screen.
 *
 * 6) source: ChatGPT (OpenAI assistant)
 *    Used only to help tidy up wording in the Javadoc and choose some helper method names.
 */


package com.example.aurora.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.map.JoinLocation;
import com.example.aurora.utils.LocationUtils;
import com.example.aurora.R;
import com.example.aurora.utils.DeepLinkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Shows details for a single event.
 * As an entrant you can:
 *  - See title, time, location, description, stats.
 *  - Join / leave the waiting list.
 *  - View selection criteria (dialog).
 *  - View the event's QR code (no scanning here).
 *
 * Waiting list entries are stored as the entrant's EMAIL when possible,
 * falling back to device ID only if no email is available.
 */


public class EventDetailsActivity extends AppCompatActivity {

    private ImageView imgBanner;
    private TextView txtTitle,  txtTime,
            txtLocation, txtAbout, txtStats, txtRegWindow;//txtSubtitle
    private Button btnCriteria;
    private Button btnShowQr;
    private ImageButton btnBackEvent;
    private Button btnSignUp;


    private FirebaseFirestore db;
    private String eventId;
    /** Identifier stored in waitingList (email preferred, else device id). */
    private String userId;
    private boolean isJoined = false;
    private String currentDeepLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        userId = resolveCurrentUserKey();   // <- EMAIL (with fallback)

        imgBanner = findViewById(R.id.imgBanner);
        txtTitle = findViewById(R.id.txtTitle);
        //txtSubtitle = findViewById(R.id.txtSubtitle);
        txtTime = findViewById(R.id.txtTime);
        txtLocation = findViewById(R.id.txtLocation);
        txtAbout = findViewById(R.id.txtAbout);
        txtStats = findViewById(R.id.txtStats);
        txtRegWindow = findViewById(R.id.txtRegWindow);
        btnCriteria = findViewById(R.id.btnCriteria);
        btnShowQr = findViewById(R.id.btnShowQr);
        btnBackEvent = findViewById(R.id.btnBackEvent);

        // Back arrow
        btnBackEvent.setOnClickListener(v -> onBackPressed());
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> signUpForEvent(eventId));


        // Get event ID from intent extra or deep link
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            eventId = DeepLinkUtil.extractEventIdFromIntent(getIntent());
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        String role = sp.getString("user_role", null);
        if (role == null || role.isEmpty()) {
            // User not logged in — save event and redirect
            getSharedPreferences("aurora", MODE_PRIVATE)
                    .edit()
                    .putString("pending_event", eventId)
                    .apply();

            Intent i = new Intent(this, WelcomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }


        btnCriteria.setOnClickListener(v -> showCriteriaDialog());

        btnShowQr.setOnClickListener(v -> {
            if (currentDeepLink == null || currentDeepLink.isEmpty()) {
                Toast.makeText(this, "No QR link saved for this event", Toast.LENGTH_SHORT).show();
            } else {
                showQrPopup(currentDeepLink);
            }
        });

        loadEventDetails();
    }

    /**
     * Prefer to use the entrant's EMAIL as the waitingList key.
     * Fallbacks:
     *  - SharedPreferences "user_email" (if your login stored it)
     *  - FirebaseAuth currentUser email
     *  - ANDROID_ID (old behaviour)
     */
    private String resolveCurrentUserKey() {
        // 1) SharedPreferences (if you store email there)
        String email = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        // 2) FirebaseAuth email
        if ((email == null || email.isEmpty())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        // 3) Fallback to device ID to preserve old behaviour
        if (email == null || email.isEmpty()) {
            email = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        }
        return email;
    }

    // Loading + binding event
    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindEvent(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            // (Comment out finish() when doing certain UI tests if needed)
            finish();
            return;
        }

        String title = doc.getString("title");
        if (title == null) title = doc.getString("name");

        String description = doc.getString("description");
        String date = doc.getString("date");
        if (date == null) date = doc.getString("startDate");

        String location = doc.getString("location");
        String regStart = doc.getString("registrationStart");
        String regEnd = doc.getString("registrationEnd");
        // ⭐ Load poster image
        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgBanner);
        } else {
            imgBanner.setImageResource(R.drawable.ic_launcher_background);
        }

        Long capacity = doc.getLong("maxSpots");
        if (capacity == null) {
            capacity = 0L;
        }

        List<String> waiting = (List<String>) doc.get("waitingList");
        int joinedCount = waiting == null ? 0 : waiting.size();

        List<String> accepted = (List<String>) doc.get("acceptedEntrants");
        boolean isAccepted = accepted != null && accepted.contains(userId);

        List<String> finalEntrants = (List<String>) doc.get("finalEntrants");
        boolean isFinal = finalEntrants != null && finalEntrants.contains(userId);

// If already final → hide button
        if (isFinal) {
            btnSignUp.setVisibility(View.GONE);
        }
// If accepted → show Sign Up button
        else if (isAccepted) {
            btnSignUp.setVisibility(View.VISIBLE);
        }
// Otherwise → hide button
        else {
            btnSignUp.setVisibility(View.GONE);
        }

        isJoined = waiting != null && waiting.contains(userId);


        currentDeepLink = doc.getString("deepLink");

        txtTitle.setText(title == null ? "Event" : title);
        //txtSubtitle.setText(location == null ? "" : location);
        txtTime.setText(date == null ? "" : date);
        txtLocation.setText(location == null ? "" : location);
        txtAbout.setText(description == null ? "" : description);
        txtStats.setText("Spots: " + capacity + " • Joined: " + joinedCount);

        if (regStart != null || regEnd != null) {
            String rs = regStart == null ? "?" : regStart;
            String re = regEnd == null ? "?" : regEnd;
            txtRegWindow.setText("Registration: " + rs + " – " + re);
        } else {
            txtRegWindow.setText("");
        }

    }

    // Join / leave waiting list  (stores EMAIL or fallback key and lat/lng)
    // Join / leave waiting list  (stores EMAIL or fallback key and lat/lng)
    private void toggleJoin() {
        if (eventId == null) return;

        final String finalEventId = eventId;
        final String finalUserId = userId;

        db.collection("events")
                .document(finalEventId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ---- read geoRequired safely ----
                    Boolean tmpGeo = doc.getBoolean("geoRequired");
                    final boolean geoRequiredFinal = (tmpGeo != null && tmpGeo);

                    // You can also recompute join here if you prefer:
                    // List<String> waiting = (List<String>) doc.get("waitingList");
                    // isJoined = waiting != null && waiting.contains(finalUserId);

                    // --------------------------
                    // LEAVE WAITING LIST
                    // --------------------------
                    if (isJoined) {
                        db.collection("events")
                                .document(finalEventId)
                                .update("waitingList", FieldValue.arrayRemove(finalUserId))
                                .addOnSuccessListener(v -> {
                                    // Only matters if we ever stored a location
                                    removeUserLocation(finalEventId, finalUserId);
                                    isJoined = false;
                                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                                });
                        return;
                    }

                    // --------------------------
                    // JOIN WAITING LIST
                    // --------------------------

                    // ✅ CASE 1: geoRequired == false
                    // → NO location permission, NO GPS, NO getUserLocation()
                    if (!geoRequiredFinal) {

                        db.collection("events")
                                .document(finalEventId)
                                .update("waitingList", FieldValue.arrayUnion(finalUserId))
                                .addOnSuccessListener(v -> {
                                    isJoined = true;
                                    Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                });

                        return;
                    }

                    // ✅ CASE 2: geoRequired == true
                    // → enforce permission + GPS + real coordinates

                    // 1) Permission check
                    if (!LocationUtils.isLocationPermissionGranted(this)) {
                        Toast.makeText(this,
                                "This event requires your location to join.",
                                Toast.LENGTH_LONG).show();
                        LocationUtils.requestLocationPermission(this);
                        return;
                    }

                    // 2) GPS setting check
                    if (!LocationUtils.isGpsEnabled(this)) {
                        Toast.makeText(this,
                                "Please enable GPS to join this event.",
                                Toast.LENGTH_LONG).show();
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        return;
                    }

                    // 3) Fetch actual location ONLY when geoRequired = true
                    LocationUtils.getUserLocation(this, (lat, lng) -> {

                        if (Double.isNaN(lat) || Double.isNaN(lng)) {
                            Toast.makeText(this,
                                    "Unable to get your location. Make sure GPS is enabled.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // 4) Save location for entrant
                        db.collection("events")
                                .document(finalEventId)
                                .collection("waitingLocations")
                                .add(new JoinLocation(finalUserId, lat, lng))
                                .addOnSuccessListener(s -> {

                                    // 5) Add to waiting list
                                    db.collection("events")
                                            .document(finalEventId)
                                            .update("waitingList", FieldValue.arrayUnion(finalUserId))
                                            .addOnSuccessListener(v -> {
                                                isJoined = true;
                                                Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show();
                                            });
                                });
                    });

                });
    }




    private void removeUserLocation(String eventId, String userId) {
        db.collection("events")
                .document(eventId)
                .collection("waitingLocations")
                .whereEqualTo("userKey", userId)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }

    private void showCriteriaDialog() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_criteria, null, false);

        Button btnGotIt = view.findViewById(R.id.btnGotIt);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        // --- THE FIX STARTS HERE ---
        // This removes the default white square background from the dialog window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }


        btnGotIt.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void showQrPopup(String deepLink) {
        try {
            int size = 800;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(bitmap);
            qrView.setPadding(40, 40, 40, 40);

            new AlertDialog.Builder(this)
                    .setTitle("Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void signUpForEvent(String eventId) {

        db.collection("events").document(eventId)
                .update(
                        "finalEntrants", FieldValue.arrayUnion(userId),
                        "acceptedEntrants", FieldValue.arrayRemove(userId)
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "You are signed up!", Toast.LENGTH_SHORT).show();
                    btnSignUp.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to sign up", Toast.LENGTH_SHORT).show();
                });
    }


}
