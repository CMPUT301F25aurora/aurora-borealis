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


package com.example.aurora.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aurora.map.MapPickerActivity;
import com.example.aurora.R;
import com.example.aurora.utils.ActivityLogger;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;

import android.location.Address;
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
    private EditText editTitle, editDescription;
    private AutoCompleteTextView editLocation;
    private Spinner spinnerCategory;
    private EditText editMaxSpots, editLotterySampleSize;
    private CheckBox checkGeoRequired;
    private Button btnChoosePoster, btnCreateEvent;
    private Button btnPickEventLocation;

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

    // Event Location (lat/lng chosen from map)
    private Double eventLat = null;
    private Double eventLng = null;

    // ActivityResultLauncher for map picker
    private ActivityResultLauncher<Intent> mapPickerLauncher;
    // Poster
    private Uri selectedPosterUri = null;

    // Firebase
    private FirebaseFirestore db;
    private StorageReference posterStorageRef;
    private ActivityResultLauncher<Intent> posterPickerLauncher;

    // Date format for display
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private PlacesClient placesClient;
    private ArrayAdapter<String> locationSuggestionsAdapter;
    private List<AutocompletePrediction> currentPredictions = new ArrayList<>();


    /**
     * Called when the activity is first created.
     * Initializes Firebase, binds UI elements, sets up pickers,
     * configures the poster selector, and attaches button listeners.
     *
     * @param savedInstanceState the previously saved state bundle, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);


        Places.initialize(getApplicationContext(), "AIzaSyD-mrZsFbU9NZq7kVC1MI7zloWy2_KSw_U");
        placesClient = Places.createClient(this);

        db = FirebaseFirestore.getInstance();
        posterStorageRef = FirebaseStorage.getInstance().getReference("event_posters");

        bindViews();

        locationSuggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line
        );

        AutoCompleteTextView auto = (AutoCompleteTextView) editLocation;
        auto.setAdapter(locationSuggestionsAdapter);

        setupCategorySpinner();
        setupDateTimePickers();
        setupPosterPicker();
        setupMapPicker(); // ⭐ Add this
        setupLocationAutocomplete();


        btnChoosePoster.setOnClickListener(v -> openPosterPicker());
        btnCreateEvent.setOnClickListener(v -> createEvent());
        btnPickEventLocation.setOnClickListener(v -> openMapPicker());

    }

    private void openMapPicker() {
        Intent intent = new Intent(CreateEventActivity.this, MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    }


    /**
     * Binds all XML layout views to their corresponding fields.
     * Called once during activity initialization.
     */
    private void bindViews() {
            editTitle = findViewById(R.id.editTitle);
            editDescription = findViewById(R.id.editDescription);
            editLocation = findViewById(R.id.editLocation);
            btnPickEventLocation = findViewById(R.id.btnPickEventLocation);

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
        // Inside bindViews() or onCreate():
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }


    /**
     * Sets up a launcher to receive selected event coordinates
     * from MapPickerActivity.
     */
    private void setupMapPicker() {
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                        eventLat = result.getData().getDoubleExtra("lat", 0);
                        eventLng = result.getData().getDoubleExtra("lng", 0);

                        // Auto-fill location field with reverse geocoding
                        if (eventLat != null && eventLng != null) {
                            fillLocationFromCoordinates(eventLat, eventLng);
                        }
                    }
                }
        );
    }

    private void setupLocationAutocomplete() {
        AutoCompleteTextView auto = (AutoCompleteTextView) editLocation;

        auto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 2) return;

                FindAutocompletePredictionsRequest request =
                        FindAutocompletePredictionsRequest.builder()
                                .setQuery(s.toString())
                                .build();

                placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener(response -> {
                            currentPredictions.clear();
                            locationSuggestionsAdapter.clear();

                            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                currentPredictions.add(prediction);
                                locationSuggestionsAdapter.add(prediction.getFullText(null).toString());
                            }

                            locationSuggestionsAdapter.notifyDataSetChanged();
                        });
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        auto.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = currentPredictions.get(position);

            String placeId = prediction.getPlaceId();
            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ID, Place.Field.NAME,
                    Place.Field.ADDRESS, Place.Field.LAT_LNG);

            FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, fields).build();

            placesClient.fetchPlace(request)
                    .addOnSuccessListener(fetchResponse -> {
                        Place place = fetchResponse.getPlace();

                        // Set text
                        editLocation.setText(place.getAddress());

                        // Set lat/lng for event
                        if (place.getLatLng() != null) {
                            eventLat = place.getLatLng().latitude;
                            eventLng = place.getLatLng().longitude;
                        }
                    });
        });
    }


    /**
     * Sets up the event category spinner with predefined category options.
     * These correspond to the organizer user stories for event tagging.
     */
    private void setupCategorySpinner() {
        // Categories from user stories: Music, Sports, Education, Arts, Technology
        String[] categories = {
                "Select a category",
                "Music",
                "Sports",
                "Education",
                "Arts",
                "Technology",
                "Community"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * Initializes all date and time picker buttons and attaches listeners.
     * When a date/time is selected, the appropriate TextView is updated.
     */
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

    /**
     * Displays a date picker dialog for the given calendar instance.
     *
     * @param calendar  the calendar object to update with the user's selection
     * @param onDateSet a callback executed after the user selects a date
     */
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

    /**
     * Displays a time picker dialog for the given calendar instance.
     *
     * @param calendar  the calendar object to update with the selected time
     * @param onTimeSet a callback executed after the user selects a time
     */
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

    /**
     * Updates the provided TextView to display the formatted date/time
     * from the associated calendar instance.
     *
     * @param textView the TextView to update
     * @param calendar the calendar containing the new date/time
     */
    private void updateDateTimeDisplay(TextView textView, Calendar calendar) {
        String formatted = dateTimeFormat.format(calendar.getTime());
        textView.setText(formatted);
        textView.setTextColor(Color.parseColor("#212121")); // Make it dark when set
    }

    /**
     * Configures the ActivityResultLauncher used to pick images from the gallery.
     * When an image is selected, a preview is displayed on the screen.
     */
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

    /**
     * Loads and decodes the selected poster image into a Bitmap preview.
     * Uses ImageDecoder on API 28+ and MediaStore for older devices.
     *
     * @param uri the Uri of the poster image selected by the user
     */
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

    /**
     * Launches an image picker intent to allow the organizer
     * to choose a poster image for the event.
     */
    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        posterPickerLauncher.launch(Intent.createChooser(intent, "Select event poster"));
    }

    /**
     * Validates all event fields and inputs.
     * Ensures required fields are filled, dates are chosen,
     * optional numeric fields are valid, and posters meet size limits.
     * If validation succeeds, begins Firestore event creation.
     */
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

        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a location or pick on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventLat == null || eventLng == null) {
            geocodeTextLocation(location);
            return; // createEventInFirestore will run AFTER geocoding
        }


        createEventInFirestore(
                selectedPosterUri,
                title, description, location, category,
                startDate, endDate, regStart, regEnd,
                maxSpots, lotterySampleSize, geoRequired
        );
    }

    /**
     * Creates the Firestore event document and populates all fields:
     * event metadata, timing, category, organizer email, lists, and poster URL.
     * After creation, uploads the poster if present.
     *
     * @param posterUri          URI of the selected poster image (nullable)
     * @param title              event title
     * @param description        event description
     * @param location           event location
     * @param category           event category
     * @param startDate          starting date/time of the event
     * @param endDate            ending date/time of the event (nullable)
     * @param regStart           registration start timestamp (nullable)
     * @param regEnd             registration end timestamp (nullable)
     * @param maxSpots           optional limit on entrants (nullable)
     * @param lotterySampleSize  optional lottery sample size (nullable)
     * @param geoRequired        whether geolocation check-in is required
     */
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

        event.put("eventLat", eventLat);
        event.put("eventLng", eventLng);


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
                        goBackToOrganizerHome();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Uploads the event poster image to Firebase Storage and saves
     * the resulting download URL in the Firestore event document.
     *
     * @param eventId   unique Firestore event ID
     * @param posterUri URI of the selected poster file
     * @param deepLink  deep link associated with this event
     */
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
                            .addOnSuccessListener(unused -> goBackToOrganizerHome());
                })
                .addOnFailureListener(err -> {
                    Toast.makeText(this, "Poster upload failed", Toast.LENGTH_SHORT).show();
                    goBackToOrganizerHome();
                });
    }

    /**
     * Navigates the organizer back to the OrganizerActivity home screen.
     * Clears the back stack to prevent users from returning to CreateEventActivity.
     */
    private void goBackToOrganizerHome() {
        Intent intent = new Intent(CreateEventActivity.this, OrganizerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void fillLocationFromCoordinates(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);

                String label = addr.getAddressLine(0);
                editLocation.setText(label);
            }
        } catch (Exception e) {
            editLocation.setText(lat + ", " + lng); // fallback
        }
    }

    private void geocodeTextLocation(String textLocation) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> results = geocoder.getFromLocationName(textLocation, 1);

            if (results != null && !results.isEmpty()) {
                Address addr = results.get(0);

                eventLat = addr.getLatitude();
                eventLng = addr.getLongitude();

                // Directly create event — DO NOT call createEvent() again
                createEventInFirestore(
                        selectedPosterUri,
                        editTitle.getText().toString().trim(),
                        editDescription.getText().toString().trim(),
                        editLocation.getText().toString().trim(),
                        spinnerCategory.getSelectedItem().toString(),
                        dateTimeFormat.format(startCalendar.getTime()),
                        txtEndDateTime.getText().toString().equals("No date/time selected")
                                ? null
                                : dateTimeFormat.format(endCalendar.getTime()),
                        txtRegStartDateTime.getText().toString().equals("No date/time selected")
                                ? null
                                : dateTimeFormat.format(regStartCalendar.getTime()),
                        txtRegEndDateTime.getText().toString().equals("No date/time selected")
                                ? null
                                : dateTimeFormat.format(regEndCalendar.getTime()),
                        editMaxSpots.getText().toString().isEmpty() ? null : Long.parseLong(editMaxSpots.getText().toString()),
                        editLotterySampleSize.getText().toString().isEmpty() ? null : Long.parseLong(editLotterySampleSize.getText().toString()),
                        checkGeoRequired.isChecked()
                );

            } else {
                Toast.makeText(this, "Could not find that location", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
        }
    }



}