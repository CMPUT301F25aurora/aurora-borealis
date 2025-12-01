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
            txtLocation, txtAbout, txtStats, txtRegWindow;
    private Button btnCriteria;
    private Button btnShowQr;
    private ImageButton btnBackEvent;
    private Button btnSignUp;


    private FirebaseFirestore db;
    private String eventId;

    private String userId;
    private boolean isJoined = false;
    private String currentDeepLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        userId = resolveCurrentUserKey();

        imgBanner = findViewById(R.id.imgBanner);
        txtTitle = findViewById(R.id.txtTitle);

        txtTime = findViewById(R.id.txtTime);
        txtLocation = findViewById(R.id.txtLocation);
        txtAbout = findViewById(R.id.txtAbout);
        txtStats = findViewById(R.id.txtStats);
        txtRegWindow = findViewById(R.id.txtRegWindow);
        btnCriteria = findViewById(R.id.btnCriteria);
        btnShowQr = findViewById(R.id.btnShowQr);
        btnBackEvent = findViewById(R.id.btnBackEvent);

        btnBackEvent.setOnClickListener(v -> onBackPressed());
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> signUpForEvent(eventId));

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
     * Returns the login identifier for this user.
     * Prefers email, falls back to ANDROID_ID.
     */
    private String resolveCurrentUserKey() {

        String email = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        if ((email == null || email.isEmpty())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        if (email == null || email.isEmpty()) {
            email = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
        }
        return email;
    }

    /** Loads the event document from Firestore. */
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

    /**
     * Binds the event fields into the UI (title, image, info).
     * Also evaluates acceptance / final status.
     */
    private void bindEvent(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
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


        if (isFinal) {
            btnSignUp.setVisibility(View.GONE);
        }
        else if (isAccepted) {
            btnSignUp.setVisibility(View.VISIBLE);
        }
        else {
            btnSignUp.setVisibility(View.GONE);
        }
        isJoined = waiting != null && waiting.contains(userId);

        currentDeepLink = doc.getString("deepLink");

        txtTitle.setText(title == null ? "Event" : title);
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

    /** Shows the selection criteria dialog (custom layout). */
    private void showCriteriaDialog() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_criteria, null, false);

        Button btnGotIt = view.findViewById(R.id.btnGotIt);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnGotIt.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /** Generates a QR code Bitmap and shows it in a popup dialog. */
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

    /**
     * Moves the entrant from acceptedEntrants → finalEntrants.
     * Called when they press "Sign Up".
     */
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
