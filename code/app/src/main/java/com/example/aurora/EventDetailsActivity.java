package com.example.aurora;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EventDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String eventId;
    private String uid;

    private ImageView banner;
    private TextView title, subtitle, timeView, about, regWindow, joinedBadge, stats, location;
    private Button btnJoinLeave;

    private final List<String> currentWaitingList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventId = getIntent().getStringExtra("eventId");
        uid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db = FirebaseFirestore.getInstance();

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

        loadEvent();
        btnJoinLeave.setOnClickListener(v -> toggleWaitlist());
    }

    private void loadEvent() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(this::bindEvent);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void bindEvent(DocumentSnapshot d) {
        if (d == null || !d.exists()) return;

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