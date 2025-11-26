package com.example.aurora.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class EntrantEventHistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout historyContainer;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_history);
        TextView backBtn = findViewById(R.id.backButtonHistory);
        backBtn.setOnClickListener(v -> finish());


        db = FirebaseFirestore.getInstance();
        historyContainer = findViewById(R.id.historyContainer);

        // get email from SharedPreferences
        userEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        if (userEmail == null) {
            Toast.makeText(this, "No user email found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventHistory();
    }

    private void loadEventHistory() {
        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {

                    historyContainer.removeAllViews();

                    if (snap.isEmpty()) {
                        addCard("No events found", "", "");
                        return;
                    }

                    boolean found = false;

                    for (QueryDocumentSnapshot doc : snap) {

                        // Safely read all lists
                        List<?> waiting = (List<?>) doc.get("waitingList");
                        List<?> selected = (List<?>) doc.get("selectedEntrants");
                        List<?> cancelled = (List<?>) doc.get("cancelledEntrants");
                        List<?> finalEntrants = (List<?>) doc.get("finalEntrants");
                        List<?> losers = (List<?>) doc.get("losersEntrants"); // ⭐ NEW

                        // Determine membership
                        boolean inWaiting = listContainsUser(waiting, userEmail);
                        boolean inSelected = listContainsUser(selected, userEmail);
                        boolean inCancelled = listContainsUser(cancelled, userEmail);
                        boolean inFinal = listContainsUser(finalEntrants, userEmail);
                        boolean inLosers = listContainsUser(losers, userEmail);

                        // If the user never interacted with this event, skip
                        if (!inWaiting && !inSelected && !inCancelled && !inFinal && !inLosers)
                            continue;

                        found = true;

                        String title = doc.getString("title");
                        if (title == null) title = "Untitled Event";

                        String date = doc.getString("date");
                        if (date == null) date = "";

                        // Determine status
                        String status;

                        if (inLosers) {
                            status = "Not Selected This Round";
                        } else if (inFinal) {
                            status = "Accepted!";
                        } else if (inSelected) {
                            status = "Selected, Awaiting Response";
                        } else if (inCancelled) {
                            status = "Declined";
                        } else if (inWaiting) {
                            status = "In Waiting List";
                        } else {
                            status = "";
                        }

                        addCard(title, date, status);
                    }

                    if (!found) {
                        addCard("No event history yet", "", "");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading history: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private boolean listContainsUser(List<?> list, String email) {
        if (list == null) return false;
        for (Object o : list) {
            if (o != null && o.toString().equalsIgnoreCase(email))
                return true;
        }
        return false;
    }

    private void addCard(String title, String date, String status) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_history_card, historyContainer, false);

        ((TextView) view.findViewById(R.id.historyTitle)).setText(title);
        ((TextView) view.findViewById(R.id.historyDate)).setText(date);
        ((TextView) view.findViewById(R.id.historyStatus)).setText(status);

        historyContainer.addView(view);
    }
}
