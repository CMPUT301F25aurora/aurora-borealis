/*
 * Sources / citations for CreateEventActivity.
 *
 * source: Android docs - "Get a result from an activity".
 * url: https://developer.android.com/training/basics/intents/result
 * note: Used as a reference for using ActivityResultLauncher and
 *       registerForActivityResult instead of the deprecated startActivityForResult
 *       when picking a poster image.
 *
 * source: Stack Overflow user - "android pick images from gallery (now startActivityForResult is deprecated)".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/66908673/android-pick-images-from-gallery-now-startactivityforresult-is-depreciated
 * note: Example of replacing startActivityForResult with the Activity Result API
 *       when opening the image picker.
 *
 * source: Stack Overflow user - "android pick images from gallery".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/5309190/android-pick-images-from-gallery
 * note: Used for the basic idea of launching an intent with type "image/*"
 *       so the user can choose an image from the gallery.
 *
 * source: Stack Overflow user - "Deprecated getBitmap with API 29. Any alternative codes?".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/56651444/deprecated-getbitmap-with-api-29-any-alternative-codes
 * note: Background for why MediaStore.Images.Media.getBitmap is deprecated and what
 *       the ImageDecoder alternative looks like; this activity still uses getBitmap
 *       only to show a quick preview.
 *
 * source: Firebase docs - "Upload files with Cloud Storage on Android".
 * url: https://firebase.google.com/docs/storage/android/upload-files
 * note: Used as a reference for StorageReference.putFile, handling the UploadTask,
 *       and then calling getDownloadUrl() to obtain the poster URL.
 *
 * source: Firebase docs - "Add data to Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/manage-data/add-data
 * note: Used for building a Map<String,Object> and calling
 *       db.collection("events").add(event) to create the event document with fields
 *       like title, description, dates, and posterUrl.
 *
 * source: Firebase developers article - "The secrets of Firestore's FieldValue.serverTimestamp()".
 * url: https://medium.com/firebase-developers/the-secrets-of-firestores-fieldvalue-servertimestamp-revealed-29dd7a38a82b
 * note: Explains why createdAt uses FieldValue.serverTimestamp() so event creation
 *       time is based on the server instead of the device clock.
 *
 * source: "Firestore Server Timestamp" example article.
 * url: https://code.luasoftware.com/tutorials/google-cloud-firestore/firestore-server-timestamp
 * note: Extra background on storing and later reading server timestamps from Firestore.
 *
 * source: Stack Overflow user - "How to generate a QR Code for an Android application?".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/8800919/how-to-generate-a-qr-code-for-an-android-application
 * note: Used for the idea of using the ZXing library's QRCodeWriter and BitMatrix
 *       to generate a QR code bitmap.
 *
 * source: Stack Overflow / ZXing examples linked from "Generate list of QR codes using RecyclerView, ZXing, RxJava in Android".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/a/25283174
 * note: Reinforced the pattern of looping over BitMatrix pixels and writing them
 *       into a Bitmap for display in an ImageView.
 *
 * source: Stack Overflow user - "Android AlertDialog with embedded EditText" and similar custom dialog examples.
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/2795300/android-alertdialog-with-embedded-edittext
 * note: Used as a reference for building a custom AlertDialog and setting a view
 *       (here an ImageView with the QR bitmap) plus a positive button.
 *
 * source: Firebase docs - "Download files with Cloud Storage on Android".
 * url: https://firebase.google.com/docs/storage/android/download-files
 * note: Confirms that getDownloadUrl() returns a stable HTTPS URL that can be stored
 *       in Firestore as posterUrl for later use.
 *
 * source: Stack Overflow user - "How do you use Intent.FLAG_ACTIVITY_CLEAR_TOP to clear the activity stack".
 * author: Stack Overflow user
 * url: https://stackoverflow.com/questions/4342761/how-do-you-use-intent-flag-activity-clear-top-to-clear-the-activity-stack
 * note: Used as a reference for adding FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_NEW_TASK
 *       when returning to OrganizerActivity in goBackToOrganizerHome().
 *
 * source: Android docs - "Tasks and the back stack".
 * url: https://developer.android.com/guide/components/activities/tasks-and-back-stack
 * note: Explains how FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_NEW_TASK affect navigation
 *       so the user ends up back at the organizer home screen instead of building
 *       a deep back stack.
 *
 * source: ChatGPT (OpenAI assistant).
 * note: Helped with naming helper methods, clarifying validation messages, and
 *       writing these citation notes, not with Firebase, ZXing, or navigation APIs.
 */


package com.example.aurora;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.database.Cursor;              // for file size check
import android.graphics.ImageDecoder;        // for modern preview
import android.os.Build;                     // to check API level
import android.provider.OpenableColumns;     // to read file size
import android.provider.MediaStore;


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

    // UI Components
    private EditText editTitle, editDescription, editLocation;
    private Spinner spinnerCategory;
    private EditText editMaxSpots, editLotterySampleSize;
    private CheckBox checkGeoRequired;
    private Button btnChoosePoster, btnCreateEvent;
    private ImageView imgPosterPreview;

    // Date/Time Pickers
    private Button btnPickStartDate, btnPickStartTime;
    private Button btnPickEndDate, btnPickEndTime;
    private Button btnPickRegStartDate, btnPickRegStartTime;
    private Button btnPickRegEndDate, btnPickRegEndTime;

    // Date/Time Display TextViews
    private TextView txtStartDateTime, txtEndDateTime;
    private TextView txtRegStartDateTime, txtRegEndDateTime;

    // Calendar instances to store selected dates/times
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private Calendar regStartCalendar = Calendar.getInstance();
    private Calendar regEndCalendar = Calendar.getInstance();

    // Poster
    private Uri selectedPosterUri = null;

    // Firebase
    private FirebaseFirestore db;
    private StorageReference posterStorageRef;
    private ActivityResultLauncher<Intent> posterPickerLauncher;

    // Date format for display
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        posterStorageRef = FirebaseStorage.getInstance().getReference("event_posters");

        bindViews();
        setupCategorySpinner();
        setupDateTimePickers();
        setupPosterPicker();

        btnChoosePoster.setOnClickListener(v -> openPosterPicker());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void bindViews() {
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        editMaxSpots = findViewById(R.id.editMaxSpots);
        editLotterySampleSize = findViewById(R.id.editLotterySampleSize);
        checkGeoRequired = findViewById(R.id.checkGeoRequired);

        btnChoosePoster = findViewById(R.id.btnChoosePoster);
        imgPosterPreview = findViewById(R.id.imgPosterPreview);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        // Date/Time Buttons
        btnPickStartDate = findViewById(R.id.btnPickStartDate);
        btnPickStartTime = findViewById(R.id.btnPickStartTime);
        btnPickEndDate = findViewById(R.id.btnPickEndDate);
        btnPickEndTime = findViewById(R.id.btnPickEndTime);
        btnPickRegStartDate = findViewById(R.id.btnPickRegStartDate);
        btnPickRegStartTime = findViewById(R.id.btnPickRegStartTime);
        btnPickRegEndDate = findViewById(R.id.btnPickRegEndDate);
        btnPickRegEndTime = findViewById(R.id.btnPickRegEndTime);

        // Date/Time Display TextViews
        txtStartDateTime = findViewById(R.id.txtStartDateTime);
        txtEndDateTime = findViewById(R.id.txtEndDateTime);
        txtRegStartDateTime = findViewById(R.id.txtRegStartDateTime);
        txtRegEndDateTime = findViewById(R.id.txtRegEndDateTime);
    }

    // ================= CATEGORY SPINNER SETUP =================

    private void setupCategorySpinner() {
        // Categories from user stories: Music, Sports, Education, Arts, Technology
        String[] categories = {
                "Select a category",
                "Music",
                "Sports",
                "Education",
                "Arts",
                "Technology"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    // ================= DATE/TIME PICKERS SETUP =================

    private void setupDateTimePickers() {
        // Start Date/Time
        btnPickStartDate.setOnClickListener(v -> showDatePicker(startCalendar, () -> updateDateTimeDisplay(txtStartDateTime, startCalendar)));
        btnPickStartTime.setOnClickListener(v -> showTimePicker(startCalendar, () -> updateDateTimeDisplay(txtStartDateTime, startCalendar)));

        // End Date/Time
        btnPickEndDate.setOnClickListener(v -> showDatePicker(endCalendar, () -> updateDateTimeDisplay(txtEndDateTime, endCalendar)));
        btnPickEndTime.setOnClickListener(v -> showTimePicker(endCalendar, () -> updateDateTimeDisplay(txtEndDateTime, endCalendar)));

        // Registration Start Date/Time
        btnPickRegStartDate.setOnClickListener(v -> showDatePicker(regStartCalendar, () -> updateDateTimeDisplay(txtRegStartDateTime, regStartCalendar)));
        btnPickRegStartTime.setOnClickListener(v -> showTimePicker(regStartCalendar, () -> updateDateTimeDisplay(txtRegStartDateTime, regStartCalendar)));

        // Registration End Date/Time
        btnPickRegEndDate.setOnClickListener(v -> showDatePicker(regEndCalendar, () -> updateDateTimeDisplay(txtRegEndDateTime, regEndCalendar)));
        btnPickRegEndTime.setOnClickListener(v -> showTimePicker(regEndCalendar, () -> updateDateTimeDisplay(txtRegEndDateTime, regEndCalendar)));
    }

    private void showDatePicker(Calendar calendar, Runnable onDateSet) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    onDateSet.run();
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePicker(Calendar calendar, Runnable onTimeSet) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    onTimeSet.run();
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void updateDateTimeDisplay(TextView textView, Calendar calendar) {
        String formatted = dateTimeFormat.format(calendar.getTime());
        textView.setText(formatted);
        textView.setTextColor(Color.parseColor("#212121")); // Make it dark when set
    }

    // ================= POSTER PICKER SETUP =================

    private void setupPosterPicker() {
        posterPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedPosterUri = result.getData().getData();
                        if (selectedPosterUri != null) {
                            imgPosterPreview.setVisibility(View.VISIBLE);
                            loadPosterPreview(selectedPosterUri);   // use helper
                        }
                    }
                }
        );
    }

    private void loadPosterPreview(Uri uri) {
        try {
            Bitmap bitmap;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(src);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }

            imgPosterPreview.setImageBitmap(bitmap);

        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        posterPickerLauncher.launch(Intent.createChooser(intent, "Select event poster"));
    }

    // ================= CREATE EVENT =================

    private void createEvent() {
        // Get basic info
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String location = editLocation.getText().toString().trim();

        // Get category from spinner
        int categoryPosition = spinnerCategory.getSelectedItemPosition();
        if (categoryPosition == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        String category = spinnerCategory.getSelectedItem().toString();

        // Validate required fields
        if (title.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill in title, description, and location", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if start date/time was set
        if (txtStartDateTime.getText().toString().equals("No date/time selected")) {
            Toast.makeText(this, "Please set a start date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get date/time strings
        String startDate = dateTimeFormat.format(startCalendar.getTime());
        String endDate = txtEndDateTime.getText().toString().equals("No date/time selected")
                ? null
                : dateTimeFormat.format(endCalendar.getTime());
        String regStart = txtRegStartDateTime.getText().toString().equals("No date/time selected")
                ? null
                : dateTimeFormat.format(regStartCalendar.getTime());
        String regEnd = txtRegEndDateTime.getText().toString().equals("No date/time selected")
                ? null
                : dateTimeFormat.format(regEndCalendar.getTime());

        // Get capacity/lottery options
        String maxSpotsStr = editMaxSpots.getText().toString().trim();
        String lotterySizeStr = editLotterySampleSize.getText().toString().trim();

        Long maxSpots = null;
        if (!maxSpotsStr.isEmpty()) {
            try {
                maxSpots = Long.parseLong(maxSpotsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Maximum entrants must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Long lotterySampleSize = null;
        if (!lotterySizeStr.isEmpty()) {
            try {
                lotterySampleSize = Long.parseLong(lotterySizeStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Lottery sample size must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        boolean geoRequired = checkGeoRequired.isChecked();
        // ⭐ FIX — image size validation OK
        if (selectedPosterUri != null) {
            Cursor cursor = getContentResolver().query(selectedPosterUri, null, null, null, null);
            if (cursor != null) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                cursor.moveToFirst();
                long fileSize = cursor.getLong(idx);
                cursor.close();
                if (fileSize > 5 * 1024 * 1024) {
                    Toast.makeText(this, "Image too large (max 5MB)", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // ⭐ FIX — this CALL was missing
        createEventInFirestore(
                selectedPosterUri,
                title, description, location, category,
                startDate, endDate, regStart, regEnd,
                maxSpots, lotterySampleSize, geoRequired
        );
    }

    // ⭐ FIX — moved OUTSIDE createEvent() (was illegally nested)
    private void createEventInFirestore(
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

        // Registration window
        event.put("registrationStart", regStart);
        event.put("registrationEnd", regEnd);

        // Capacity / lottery
        event.put("maxSpots", maxSpots);
        event.put("lotterySampleSize", lotterySampleSize);

        // Geo requirement
        event.put("geoRequired", geoRequired);

        // Poster
        // ⭐ CHANGE — at creation, file doesn't exist yet
        event.put("posterUrl", null);


        // Organizer
        String organizerEmail = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);
        event.put("organizerEmail", organizerEmail);

        // Lists for lottery management
        event.put("waitingList", new ArrayList<String>());
        event.put("selectedEntrants", new ArrayList<String>());
        event.put("finalEntrants", new ArrayList<String>());
        event.put("cancelledEntrants", new ArrayList<String>());

        // Metadata
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> {
                    String eventId = ref.getId();
                    String deepLink = "aurora://event/" + eventId;
                    ref.update("deepLink", deepLink);

                    ActivityLogger.logEventCreated(eventId, title);

                    // ⭐ CHANGE — upload poster BEFORE showing QR / navigating home
                    if (posterUri != null) {
                        uploadPosterAndAttachToEvent(eventId, posterUri, deepLink); // ⭐ FIX added deepLink param
                    } else {
                        showQrDialogAndReturnHome(deepLink);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    // ⭐ CHANGE — clean Option A upload method
    private void uploadPosterAndAttachToEvent(String eventId, Uri posterUri, String deepLink) {
        StorageReference ref = posterStorageRef.child(eventId + ".jpg");

        ref.putFile(posterUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    db.collection("events")
                            .document(eventId)
                            .update("posterUrl", downloadUri.toString())
                            .addOnSuccessListener(unused -> showQrDialogAndReturnHome(deepLink)); // ⭐ FIX
                })
                .addOnFailureListener(err -> {
                    Toast.makeText(this, "Poster upload failed", Toast.LENGTH_SHORT).show();
                    showQrDialogAndReturnHome(deepLink); // still show QR
                });
    }


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