package com.example.aurora;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EventDetailsActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private String eventId;
    private String uid;

    private ImageView banner;
    private TextView title, subtitle, timeView, about, regWindow, joinedBadge, stats, location;
    private Button btnJoinLeave;
    private Button btnUploadPoster;                        // NEW
    private Uri imageUri;                                  // NEW


    private final List<String> currentWaitingList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventId = getIntent().getStringExtra("eventId");
        uid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        banner = findViewById(R.id.imgBanner);
        title = findViewById(R.id.txtTitle);
        subtitle = findViewById(R.id.txtSubtitle);
        timeView = findViewById(R.id.txtTime);
        about = findViewById(R.id.txtAbout);
        regWindow = findViewById(R.id.txtRegWindow);
        joinedBadge = findViewById(R.id.txtJoinedBadge);
        stats = findViewById(R.id.txtStats);
        location = findViewById(R.id.txtLocation);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnUploadPoster = findViewById(R.id.btnUploadPoster);   // NEW

        loadEvent();
        btnJoinLeave.setOnClickListener(v -> toggleWaitlist());
        btnUploadPoster.setOnClickListener(v -> openFileChooser()); // NEW
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            banner.setImageURI(imageUri); // show locally
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) return;

        StorageReference fileRef = storage.getReference()
                .child("event_posters/" + eventId + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String url = uri.toString();
                            db.collection("events").document(eventId)
                                    .update("posterUrl", url)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(this, "Poster uploaded!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to save URL: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void loadEvent() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void bindEvent(DocumentSnapshot d) {
        if (d == null || !d.exists()) return;

        String posterUrl = d.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            // Glide is a lightweight image-loading library
            // Add this dependency in build.gradle:
            // implementation 'com.github.bumptech.glide:glide:4.16.0'
            com.bumptech.glide.Glide.with(this)
                    .load(posterUrl)
                    .centerCrop()
                    .into(banner);
        }

        // Title / Date
        String titleStr = nz(d.getString("title"));
        if (titleStr.isEmpty()) titleStr = nz(d.getString("name"));

        String dateStr = nz(d.getString("date"));
        if (dateStr.isEmpty()) dateStr = nz(d.getString("dateDisplay"));

        title.setText(titleStr);
        subtitle.setText(dateStr);

        // Location
        String locStr = nz(d.getString("location"));
        if (locStr.isEmpty()) {
            String ln = nz(d.getString("locationName"));
            String la = nz(d.getString("locationAddress"));
            locStr = (ln + (la.isEmpty() ? "" : ", " + la)).trim();
        }
        location.setText(locStr);

        // Description
        String aboutStr = nz(d.getString("description"));
        if (aboutStr.isEmpty()) aboutStr = nz(d.getString("notes"));
        about.setText(aboutStr);

        // Time (startAt/endAt -> "h:mm a MST")
        Timestamp startTs = d.getTimestamp("startAt");
        Timestamp endTs = d.getTimestamp("endAt");
        if (startTs != null && endTs != null) {
            SimpleDateFormat tfmt = new SimpleDateFormat("h:mm a 'MST'", Locale.CANADA);
            tfmt.setTimeZone(TimeZone.getTimeZone("America/Edmonton")); // Mountain Time
            String s = tfmt.format(startTs.toDate());
            String e = tfmt.format(endTs.toDate());
            timeView.setText(s + " – " + e);
            timeView.setVisibility(TextView.VISIBLE);
        } else {
            timeView.setText("");
            timeView.setVisibility(TextView.GONE);
        }

        // Registration window
        Timestamp regOpen = d.getTimestamp("registrationOpensAt");
        Timestamp regClose = d.getTimestamp("registrationClosesAt");
        if (regOpen != null || regClose != null) {
            SimpleDateFormat dfmt = new SimpleDateFormat("MMM d, yyyy h:mm a 'MST'", Locale.CANADA);
            dfmt.setTimeZone(TimeZone.getTimeZone("America/Edmonton"));
            String openS = regOpen == null ? "" : dfmt.format(regOpen.toDate());
            String closeS = regClose == null ? "" : dfmt.format(regClose.toDate());
            regWindow.setText(("Registration: " + openS +
                    (closeS.isEmpty() ? "" : " — " + closeS)).trim());
        } else {
            regWindow.setText("");
        }

        // Waiting list
        List<String> wl = (List<String>) d.get("waitingList");
        currentWaitingList.clear();
        if (wl != null) currentWaitingList.addAll(wl);
        stats.setText("Waiting List: " + currentWaitingList.size());

        boolean joined = currentWaitingList.contains(uid);
        joinedBadge.setText(joined ? "You're on the waiting list" : "");
        btnJoinLeave.setText(joined ? "Leave Waiting List" : "Join Waiting List");
    }

    private void toggleWaitlist() {
        boolean joined = currentWaitingList.contains(uid);
        if (joined) {
            db.collection("events").document(eventId)
                    .update("waitingList", FieldValue.arrayRemove(uid))
                    .addOnSuccessListener(v -> {
                        currentWaitingList.remove(uid);
                        btnJoinLeave.setText("Join Waiting List");
                        joinedBadge.setText("");
                        stats.setText("Waiting List: " + currentWaitingList.size());
                    });
        } else {
            db.collection("events").document(eventId)
                    .update("waitingList", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener(v -> {
                        currentWaitingList.add(uid);
                        btnJoinLeave.setText("Leave Waiting List");
                        joinedBadge.setText("You're on the waiting list");
                        stats.setText("Waiting List: " + currentWaitingList.size());
                    });
        }
    }
}
