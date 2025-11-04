package com.example.aurora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView adminRecycler;
    private Button chipEvents, chipProfiles, chipImages, chipLogs;
    private TextView sectionTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        adminRecycler = findViewById(R.id.adminRecycler);
        adminRecycler.setLayoutManager(new LinearLayoutManager(this));

        chipEvents = findViewById(R.id.chipAdminEvents);
        chipProfiles = findViewById(R.id.chipAdminProfiles);
        chipImages = findViewById(R.id.chipAdminImages);
        chipLogs = findViewById(R.id.chipAdminLogs);
        sectionTitle = findViewById(R.id.adminSectionTitle);

        // For now just change the section title.
        // Later we will swap adapters and load from Firestore for each type.
        chipEvents.setOnClickListener(v -> {
            sectionTitle.setText("Events");
            // later: show events list with remove buttons
        });

        chipProfiles.setOnClickListener(v -> {
            sectionTitle.setText("Profiles");
            // later: show profiles list with remove buttons
        });

        chipImages.setOnClickListener(v -> {
            sectionTitle.setText("Images");
            // later: show images list with remove buttons
        });

        chipLogs.setOnClickListener(v -> {
            sectionTitle.setText("Notification logs");
            // later: show logs list
        });
    }
}
