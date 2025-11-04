package com.example.aurora;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;

public class EventDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String eventId;
    private String uid;

    private ImageView banner;
    private TextView title, subtitle, about, regWindow, joinedBadge, stats, location;
    private Button btnJoinLeave;

    private List<String> currentWaitingList = new ArrayList<>();

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
        db.collection("events").document(eventId).get().addOnSuccessListener(this::bindEvent);
    }

    private void bindEvent(DocumentSnapshot d) {
        Event e = d.toObject(Event.class);
        if (e == null) return;

        currentWaitingList = e.getWaitingList() == null ? new ArrayList<>() : e.getWaitingList();

        title.setText(e.getTitle() == null ? "" : e.getTitle());
        subtitle.setText(e.getDate() == null ? "" : e.getDate());
        location.setText(e.getLocation() == null ? "" : e.getLocation());
        about.setText(e.getDescription() == null ? "" : e.getDescription());
        regWindow.setText(e.getStartDate() == null && e.getEndDate() == null ? "" : ("Registration: " + (e.getStartDate() == null ? "" : e.getStartDate()) + " — " + (e.getEndDate() == null ? "" : e.getEndDate())));
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
