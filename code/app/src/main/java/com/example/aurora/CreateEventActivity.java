package com.example.aurora;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * CreateEventActivity
 *
 * Covers organizer stories on the creation side:
 * - US 02.01.01: create event + generate unique QR that links to event in app.
 * - US 02.01.04: set registration period.
 * - US 02.02.03: toggle geolocation requirement for event.
 * - US 02.03.01: optionally limit waiting list size (maxSpots).
 * - US 02.04.01 / 02.04.02: upload/update event poster.
 * - US 02.05.02: specify how many entrants to sample for invitations.
 */
public class CreateEventActivity extends AppCompatActivity {

    private EditText editTitle, editDescription, editLocation, editCategory;
    private EditText editStartDate, editEndDate, editRegStart, editRegEnd;
    private EditText editMaxSpots, editLotterySampleSize;
    private CheckBox checkGeoRequired;
    private Button btnChoosePoster, btnCreateEvent;
    private ImageView imgPosterPreview;

    private Uri selectedPosterUri = null;

    private FirebaseFirestore db;
    private StorageReference posterStorageRef;

    private ActivityResultLauncher<Intent> posterPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        posterStorageRef = FirebaseStorage.getInstance().getReference("event_posters");

        bindViews();
        setupPosterPicker();

        btnChoosePoster.setOnClickListener(v -> openPosterPicker());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void bindViews() {
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        editCategory = findViewById(R.id.editCategory);

        editStartDate = findViewById(R.id.editStartDate);
        editEndDate = findViewById(R.id.editEndDate);
        editRegStart = findViewById(R.id.editRegStart);
        editRegEnd = findViewById(R.id.editRegEnd);

        editMaxSpots = findViewById(R.id.editMaxSpots);
        editLotterySampleSize = findViewById(R.id.editLotterySampleSize);

        checkGeoRequired = findViewById(R.id.checkGeoRequired);
        btnChoosePoster = findViewById(R.id.btnChoosePoster);
        imgPosterPreview = findViewById(R.id.imgPosterPreview);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
    }

    private void setupPosterPicker() {
        posterPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedPosterUri = result.getData().getData();
                        if (selectedPosterUri != null) {
                            imgPosterPreview.setVisibility(View.VISIBLE);
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                        getContentResolver(), selectedPosterUri);
                                imgPosterPreview.setImageBitmap(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        posterPickerLauncher.launch(Intent.createChooser(intent, "Select event poster"));
    }

    private void createEvent() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String category = editCategory.getText().toString().trim();

        String startDate = editStartDate.getText().toString().trim();
        String endDate = editEndDate.getText().toString().trim();
        String regStart = editRegStart.getText().toString().trim();
        String regEnd = editRegEnd.getText().toString().trim();

        String maxSpotsStr = editMaxSpots.getText().toString().trim();
        String lotterySizeStr = editLotterySampleSize.getText().toString().trim();

        boolean geoRequired = checkGeoRequired.isChecked();

        if (title.isEmpty() || description.isEmpty() || location.isEmpty() || startDate.isEmpty()) {
            Toast.makeText(this, "Please fill in title, description, location, and start date.", Toast.LENGTH_SHORT).show();
            return;
        }

        Long maxSpots = null;
        if (!maxSpotsStr.isEmpty()) {
            try {
                maxSpots = Long.parseLong(maxSpotsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Maximum entrants must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Long lotterySampleSize = null;
        if (!lotterySizeStr.isEmpty()) {
            try {
                lotterySampleSize = Long.parseLong(lotterySizeStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Lottery sample size must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (selectedPosterUri != null) {
            uploadPosterAndCreateEvent(selectedPosterUri, title, description, location, category,
                    startDate, endDate, regStart, regEnd,
                    maxSpots, lotterySampleSize, geoRequired);
        } else {
            createEventInFirestore(null, title, description, location, category,
                    startDate, endDate, regStart, regEnd,
                    maxSpots, lotterySampleSize, geoRequired);
        }
    }

    private void uploadPosterAndCreateEvent(
            Uri posterUri,
            String title,
            String description,
            String location,
            String category,
            String startDate,
            String endDate,
            String regStart,
            String regEnd,
            Long maxSpots,
            Long lotterySampleSize,
            boolean geoRequired
    ) {
        String fileName = "poster_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = posterStorageRef.child(fileName);
        UploadTask uploadTask = ref.putFile(posterUri);

        uploadTask
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    createEventInFirestore(url, title, description, location, category,
                            startDate, endDate, regStart, regEnd,
                            maxSpots, lotterySampleSize, geoRequired);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to upload poster: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createEventInFirestore(
            String posterUrl,
            String title,
            String description,
            String location,
            String category,
            String startDate,
            String endDate,
            String regStart,
            String regEnd,
            Long maxSpots,
            Long lotterySampleSize,
            boolean geoRequired
    ) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("description", description);
        event.put("location", location);
        event.put("category", category);

        // Event timing
        event.put("startDate", startDate);
        event.put("endDate", endDate);
        // For older parts of the app that expect a "date" string
        event.put("date", startDate);

        // Registration window (US 02.01.04)
        event.put("registrationStart", regStart);
        event.put("registrationEnd", regEnd);

        // Capacity / lottery (US 02.03.01, 02.05.02)
        event.put("maxSpots", maxSpots);
        event.put("lotterySampleSize", lotterySampleSize);

        // Geo requirement (US 02.02.03)
        event.put("geoRequired", geoRequired);

        // Poster (US 02.04.01 / 02.04.02)
        event.put("posterUrl", posterUrl);

        // Organizer that created this (IMPORTANT for organiser dashboard)
        String organizerEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);
        event.put("organizerEmail", organizerEmail);

        // Lists for later organizer features:
        event.put("waitingList", new ArrayList<String>());
        event.put("selectedEntrants", new ArrayList<String>());
        event.put("finalEntrants", new ArrayList<String>());
        event.put("cancelledEntrants", new ArrayList<String>());

        // Metadata
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> {
                    String deepLink = "aurora://event/" + ref.getId();
                    ref.update("deepLink", deepLink);
                    showQrDialogAndReturnHome(deepLink);
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Show QR code then return to OrganizerActivity.
     */
    private void showQrDialogAndReturnHome(String deepLink) {
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
                    .setPositiveButton("Done", (dialog, which) -> {
                        dialog.dismiss();
                        goBackToOrganizerHome();
                    })
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            goBackToOrganizerHome();
        }
    }

    private void goBackToOrganizerHome() {
        Intent intent = new Intent(CreateEventActivity.this, OrganizerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
