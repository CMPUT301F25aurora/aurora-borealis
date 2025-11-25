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
                    if (snap.isEmpty()) {
                        addCard("No events found", "", "");
                        return;
                    }

                    boolean found = false;

                    for (QueryDocumentSnapshot doc : snap) {
                        List<String> waiting = (List<String>) doc.get("waitingList");

                        if (waiting != null && waiting.contains(userEmail)) {
                            found = true;

                            String title = doc.getString("title");
                            String date = doc.getString("date");
                            List<String> selected = (List<String>) doc.get("selectedList");

                            boolean isWinner = selected != null && selected.contains(userEmail);
                            String status = isWinner ? "Selected ✅" : "Not Selected ❌";
                            addCard(title, date, status);
                        }
                    }

                    if (!found) {
                        addCard("No event history yet", "", "");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show());
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
