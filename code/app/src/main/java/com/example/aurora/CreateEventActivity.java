/**
 * CreateEventActivity.java
 *
 * This activity allows organizers to create new events in the Aurora app.
 * It provides input fields for event details such as name, description, dates,
 * registration period, and maximum capacity. Users can also upload a poster image.
 *
 * When the "Create Event" button is clicked, the entered event information
 * is validated and then uploaded to the Firestore database under the "events" collection.
 * Successful uploads show a confirmation message, while errors display a toast with details.
 */


package com.example.aurora;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText eventName, eventDescription, eventStart, eventEnd, registrationStart, registrationEnd, maxCapacity, maxEntrantsEditText;
    private Button choosePosterButton, createEventButton;
    private ImageView imagePreview;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        eventStart = findViewById(R.id.eventStartDate);
        eventEnd = findViewById(R.id.eventEndDate);
        registrationStart = findViewById(R.id.registrationStart);
        registrationEnd = findViewById(R.id.registrationEnd);
        maxCapacity = findViewById(R.id.maxCapacity);
        choosePosterButton = findViewById(R.id.choosePosterButton);
        imagePreview = findViewById(R.id.imagePreview);
        createEventButton = findViewById(R.id.createEventButton);
        maxEntrantsEditText = findViewById(R.id.maxEntrantsEditText);

        createEventButton.setOnClickListener(v -> uploadEvent());
    }

    private void uploadEvent() {
        String name = eventName.getText().toString().trim();
        String description = eventDescription.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please enter event name and description", Toast.LENGTH_SHORT).show();
            return;
        }

        saveEventData();
    }

    private void saveEventData() {
        Map<String, Object> event = new HashMap<>();
        event.put("name", eventName.getText().toString());
        event.put("description", eventDescription.getText().toString());
        event.put("startDate", eventStart.getText().toString());
        event.put("endDate", eventEnd.getText().toString());
        event.put("registrationStart", registrationStart.getText().toString());
        event.put("registrationEnd", registrationEnd.getText().toString());
        event.put("capacity", maxCapacity.getText().toString());

        String limitText = maxEntrantsEditText.getText().toString().trim();
        Long maxEntrants = null;
        if (!limitText.isEmpty()) {
            try {
                maxEntrants = Long.parseLong(limitText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number for maximum entrants", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        event.put("maxEntrants", maxEntrants);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();

                    // Generate deep link and store in Firestore
                    String eventId = ref.getId();
                    String deepLink = "aurora://event/" + eventId;

                    Map<String, Object> linkData = new HashMap<>();
                    linkData.put("deepLink", deepLink);

                    db.collection("events").document(eventId)
                            .set(linkData, SetOptions.merge());

                    // Show QR code popup
                    showQrPopup(deepLink);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Generates QR bitmap and shows it in a popup dialog
    private void showQrPopup(String deepLink) {
        if (deepLink == null || deepLink.isEmpty()) {
            Toast.makeText(this, "No QR code available for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, 800, 800);
            Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565);
            for (int x = 0; x < 800; x++) {
                for (int y = 0; y < 800; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(bitmap);
            qrView.setPadding(40, 40, 40, 40);

            new AlertDialog.Builder(this)
                    .setTitle("🎟️ Event QR Code")
                    .setView(qrView)
                    .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

}
