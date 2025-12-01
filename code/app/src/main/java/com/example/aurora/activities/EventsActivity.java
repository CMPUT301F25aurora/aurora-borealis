package com.example.aurora.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aurora.adapters.EventsAdapter;
import com.example.aurora.notifications.NotificationHelper;
import com.example.aurora.models.NotificationModel;
import com.example.aurora.R;
import com.example.aurora.models.Event;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * EventsActivity
 *
 * Main entrant home screen.
 * Shows events, supports category filters, availability filters,
 * searching, QR scanning, and listens for real-time notifications.
 */
public class EventsActivity extends AppCompatActivity {

    private ListenerRegistration notifListener;

    private EditText searchEvents;
    private ImageButton logoutButton;
    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;
    private LinearLayout btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology, btnCommunity;
    private ImageView iconMusic, iconSports, iconEducation, iconArts, iconTech, iconCommunity;
    private TextView textAll, textMusic, textSports, textEducation, textArts, textTech, textCommunity;
    private LinearLayout navEvents, navProfile, navAlerts;
    private ImageButton btnFilter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final List<Event> baseEvents = new ArrayList<>();
    private final List<Event> eventList = new ArrayList<>();
    private String currentCategory = null;
    private final boolean[] daySelected = new boolean[7];
    private boolean slotMorning = false;
    private boolean slotAfternoon = false;
    private boolean slotEvening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_events);

        searchEvents = findViewById(R.id.searchEvents);
        logoutButton = findViewById(R.id.logoutButton);
        recyclerEvents = findViewById(R.id.recyclerEvents);

        btnAll = findViewById(R.id.btnAll);
        textAll = findViewById(R.id.textAll);

        navEvents = findViewById(R.id.navEvents);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);

        btnFilter = findViewById(R.id.btnFilter);

        btnMusic = findViewById(R.id.btnMusic);
        btnSports = findViewById(R.id.btnSports);
        btnEducation = findViewById(R.id.btnEducation);
        btnArts = findViewById(R.id.btnArts);
        btnTechnology = findViewById(R.id.btnTechnology);
        btnCommunity = findViewById(R.id.btnCommunity);

        iconMusic = findViewById(R.id.iconMusic);
        iconSports = findViewById(R.id.iconSports);
        iconEducation = findViewById(R.id.iconEducation);
        iconArts = findViewById(R.id.iconArts);
        iconTech = findViewById(R.id.iconTech);
        iconCommunity = findViewById(R.id.iconCommunity);

        textMusic = findViewById(R.id.textMusic);
        textSports = findViewById(R.id.textSports);
        textEducation = findViewById(R.id.textEducation);
        textArts = findViewById(R.id.textArts);
        textTech = findViewById(R.id.textTech);
        textCommunity = findViewById(R.id.textCommunity);

        btnAll.setOnClickListener(v -> {
            highlightSelectedNoIcon(btnAll, textAll);
            loadEvents(null);
        });

        btnMusic.setOnClickListener(v -> {
            highlightSelected(btnMusic, iconMusic, textMusic);
            loadEvents("Music");
        });

        btnSports.setOnClickListener(v -> {
            highlightSelected(btnSports, iconSports, textSports);
            loadEvents("Sports");
        });

        btnEducation.setOnClickListener(v -> {
            highlightSelected(btnEducation, iconEducation, textEducation);
            loadEvents("Education");
        });

        btnArts.setOnClickListener(v -> {
            highlightSelected(btnArts, iconArts, textArts);
            loadEvents("Arts");
        });

        btnTechnology.setOnClickListener(v -> {
            highlightSelected(btnTechnology, iconTech, textTech);
            loadEvents("Technology");
        });

        btnCommunity.setOnClickListener(v -> {
            highlightSelected(btnCommunity, iconCommunity, textCommunity);
            loadEvents("Community");
        });


        highlightSelectedNoIcon(btnAll, textAll);


        ImageButton iconQr = findViewById(R.id.iconQr);

        iconQr.setOnClickListener(v -> startQrScan());

        ExtendedFloatingActionButton fab = findViewById(R.id.roleSwitchFab);

        String userDocId = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_doc_id", null);

        fab.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userDocId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        Boolean allowed = doc.getBoolean("organizer_allowed");

                        if (allowed == null || !allowed) {
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Access Denied")
                                    .setMessage("Organizer privileges have been revoked.")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_last_mode", "organizer")
                                .apply();

                        startActivity(new Intent(this, OrganizerActivity.class));
                        finish();
                    });
        });


        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventsAdapter(this, eventList);
        recyclerEvents.setAdapter(adapter);

        logoutButton.setOnClickListener(v -> logoutUser());

        navEvents.setOnClickListener(v -> {});
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        navAlerts.setOnClickListener(v ->
                startActivity(new Intent(this, AlertsActivity.class)));


        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        searchEvents.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndRefresh();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadEvents(null);

        listenForNotifications();
    }

    /** Listens for Firestore notifications targeted at this entrant. */
    private void listenForNotifications() {

        String email = getSharedPreferences("aurora_prefs", MODE_PRIVATE)
                .getString("user_email", null);

        Log.d("DEBUG_EMAIL", "Entrant email from prefs: " + email);

        if (email == null || email.isEmpty()) return;

        notifListener = db.collection("notifications")
                .whereEqualTo("userId", email)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, err) -> {

                    if (err != null || snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {

                        if (dc.getType() == DocumentChange.Type.ADDED) {

                            NotificationModel notif =
                                    dc.getDocument().toObject(NotificationModel.class);

                            showLocalNotification(notif);
                        }
                    }
                });
    }

    /** Displays a local Android notification triggering AlertsActivity. */
    private void showLocalNotification(NotificationModel notif) {

        NotificationHelper helper = new NotificationHelper(this);

        Intent intent = new Intent(this, AlertsActivity.class);
        intent.putExtra("eventId", notif.getEventId());

        PendingIntent pIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        helper.getManager().notify(
                (int) System.currentTimeMillis(),
                helper.getNotification(notif.getTitle(), notif.getMessage(), pIntent).build()
        );
    }

    /** Removes Firestore listener to prevent leaks. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifListener.remove();
    }

    /** Signs out user and clears SharedPreferences. */
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        SharedPreferences sp = getSharedPreferences("aurora_prefs", MODE_PRIVATE);
        sp.edit().clear().apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Loads events from Firestore.
     * If category != null → loads filtered category.
     */
    private void loadEvents(@Nullable String category) {
        currentCategory = category;
        Query q = db.collection("events");
        if (category != null) {
            q = q.whereEqualTo("category", category);
        }
        q.get()
                .addOnSuccessListener(query -> {
                    baseEvents.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        baseEvents.add(event);
                    }
                    applyFiltersAndRefresh();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show());
    }

    /** Shows availability filter dialog (days + morning/afternoon/evening). */
    private void showFilterDialog() {
        android.view.View view = getLayoutInflater()
                .inflate(R.layout.dialog_filter_availability, null);

        final android.widget.CheckBox cbMon = view.findViewById(R.id.cbMon);
        final android.widget.CheckBox cbTue = view.findViewById(R.id.cbTue);
        final android.widget.CheckBox cbWed = view.findViewById(R.id.cbWed);
        final android.widget.CheckBox cbThu = view.findViewById(R.id.cbThu);
        final android.widget.CheckBox cbFri = view.findViewById(R.id.cbFri);
        final android.widget.CheckBox cbSat = view.findViewById(R.id.cbSat);
        final android.widget.CheckBox cbSun = view.findViewById(R.id.cbSun);

        final android.widget.CheckBox cbMorning = view.findViewById(R.id.cbMorning);
        final android.widget.CheckBox cbAfternoon = view.findViewById(R.id.cbAfternoon);
        final android.widget.CheckBox cbEvening = view.findViewById(R.id.cbEvening);

        cbMon.setChecked(daySelected[0]);
        cbTue.setChecked(daySelected[1]);
        cbWed.setChecked(daySelected[2]);
        cbThu.setChecked(daySelected[3]);
        cbFri.setChecked(daySelected[4]);
        cbSat.setChecked(daySelected[5]);
        cbSun.setChecked(daySelected[6]);

        cbMorning.setChecked(slotMorning);
        cbAfternoon.setChecked(slotAfternoon);
        cbEvening.setChecked(slotEvening);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        Button btnClear = view.findViewById(R.id.btnClearFilters);
        Button btnApply = view.findViewById(R.id.btnApplyFilters);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClear.setOnClickListener(v -> {
            for (int i = 0; i < daySelected.length; i++) daySelected[i] = false;
            slotMorning = slotAfternoon = slotEvening = false;

            cbMon.setChecked(false);
            cbTue.setChecked(false);
            cbWed.setChecked(false);
            cbThu.setChecked(false);
            cbFri.setChecked(false);
            cbSat.setChecked(false);
            cbSun.setChecked(false);

            cbMorning.setChecked(false);
            cbAfternoon.setChecked(false);
            cbEvening.setChecked(false);

            dialog.dismiss();
            dialog.show();
            applyFiltersAndRefresh();
        });

        btnApply.setOnClickListener(v -> {
            daySelected[0] = cbMon.isChecked();
            daySelected[1] = cbTue.isChecked();
            daySelected[2] = cbWed.isChecked();
            daySelected[3] = cbThu.isChecked();
            daySelected[4] = cbFri.isChecked();
            daySelected[5] = cbSat.isChecked();
            daySelected[6] = cbSun.isChecked();

            slotMorning = cbMorning.isChecked();
            slotAfternoon = cbAfternoon.isChecked();
            slotEvening = cbEvening.isChecked();

            dialog.dismiss();
            applyFiltersAndRefresh();
        });

        dialog.show();
    }

    /**
     * Applies search + availability filters
     * and refreshes the RecyclerView list.
     */
    private void applyFiltersAndRefresh() {
        String q = searchEvents.getText() == null
                ? ""
                : searchEvents.getText().toString().trim().toLowerCase(Locale.getDefault());

        eventList.clear();
        for (Event e : baseEvents) {
            if (!matchesSearch(e, q)) continue;
            if (!matchesAvailability(e)) continue;
            eventList.add(e);
        }
        adapter.notifyDataSetChanged();
    }

    /** Returns true if event matches search text. */
    private boolean matchesSearch(Event e, String query) {
        if (query.isEmpty()) return true;

        String title = e.getTitle() == null ? "" : e.getTitle();
        String location = e.getLocation() == null ? "" : e.getLocation();
        String description = e.getDescription() == null ? "" : e.getDescription();

        title = title.toLowerCase(Locale.getDefault());
        location = location.toLowerCase(Locale.getDefault());
        description = description.toLowerCase(Locale.getDefault());

        return title.contains(query) || location.contains(query) || description.contains(query);
    }

    /** Returns true if event fits availability filters. */
    private boolean matchesAvailability(Event e) {
        if (!isAvailabilityFilterActive()) return true;

        String dateStr = e.getDate();
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return true;
        }

        Date date = parseDateBestEffort(dateStr.trim());
        if (date == null) return true;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int calDay = cal.get(Calendar.DAY_OF_WEEK);
        int idx = dayIndexFromCalendar(calDay);

        boolean anyDay = anyDaySelected();
        boolean dayOk = !anyDay || (idx >= 0 && daySelected[idx]);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean inMorning = hour >= 6 && hour < 12;
        boolean inAfternoon = hour >= 12 && hour < 18;
        boolean inEvening = hour >= 18 || hour < 6;

        boolean anySlot = slotMorning || slotAfternoon || slotEvening;
        boolean slotOk = !anySlot
                || (slotMorning && inMorning)
                || (slotAfternoon && inAfternoon)
                || (slotEvening && inEvening);

        return dayOk && slotOk;
    }
    private boolean isAvailabilityFilterActive() {
        return anyDaySelected() || slotMorning || slotAfternoon || slotEvening;
    }
    private boolean anyDaySelected() {
        for (boolean b : daySelected) {
            if (b) return true;
        }
        return false;
    }
    private int dayIndexFromCalendar(int calDay) {
        switch (calDay) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return -1;
        }
    }

    private Date parseDateBestEffort(String s) {
        String[] patterns = new String[]{
                "MMMM d, yyyy • h:mm a",
                "MMMM d, yyyy h:mm a",
                "MMMM d, yyyy",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd"
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.getDefault());
                sdf.setLenient(true);
                Date d = sdf.parse(s);
                if (d != null) return d;
            } catch (ParseException ignored) {}
        }
        return null;
    }

    /** Starts QR code scanner for entering/joining events. */
    private void startQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan Aurora event QR");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    /** Handles QR scan result → opens EventDetailsActivity. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                handleScannedText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** Parses scanned QR text to detect Aurora event links. */
    private void handleScannedText(String text) {
        try {
            Uri uri = Uri.parse(text);
            if ("aurora".equalsIgnoreCase(uri.getScheme())
                    && "event".equalsIgnoreCase(uri.getHost())) {

                String eventId = null;
                if (uri.getPath() != null && uri.getPath().length() > 1) {
                    eventId = uri.getPath().substring(1);
                } else {
                    eventId = uri.getQueryParameter("id");
                }

                if (eventId != null && !eventId.isEmpty()) {
                    Intent i = new Intent(this, EventDetailsActivity.class);
                    i.putExtra("eventId", eventId);
                    startActivity(i);
                } else {
                    Toast.makeText(this, "Invalid Aurora event QR", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Not an Aurora event QR", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to handle QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightSelected(LinearLayout layout, ImageView icon, TextView text) {
        resetAllCategoryHighlights();
        layout.setBackgroundResource(R.drawable.bg_category_selected);
        icon.setColorFilter(Color.parseColor("#233D4D")); // dark text
        text.setTextColor(Color.parseColor("#233D4D"));
    }

    /**
     * Resets a category button that includes an icon.
     * Applies the unselected background, orange icon tint, and orange text color.
     *
     * @param layout The category layout container.
     * @param icon   The ImageView icon inside the category button.
     * @param text   The TextView label of the category button.
     */
    private void resetHighlight(LinearLayout layout, ImageView icon, TextView text) {
        layout.setBackgroundResource(R.drawable.bg_category_unselected);
        icon.setColorFilter(Color.parseColor("#fe7f2d")); // orange instead of white
        text.setTextColor(Color.parseColor("#fe7f2d"));
    }

    /**
     * Highlights the selected category button that does NOT have an icon.
     * Clears all other highlights first, then applies the selected background
     * and dark text color for readability.
     *
     * @param layout The category layout container.
     * @param text   The TextView label of the selected category.
     */
    private void highlightSelectedNoIcon(LinearLayout layout, TextView text) {
        resetAllCategoryHighlights();
        layout.setBackgroundResource(R.drawable.bg_category_selected);
        text.setTextColor(Color.parseColor("#233D4D")); // dark text
    }

    /**
     * Resets a category button that does NOT have an icon.
     * Applies the unselected background and orange text color.
     *
     * @param layout The category layout container.
     * @param text   The TextView label of the category button.
     */
    private void resetHighlightNoIcon(LinearLayout layout, TextView text) {
        layout.setBackgroundResource(R.drawable.bg_category_unselected);
        text.setTextColor(Color.parseColor("#fe7f2d"));
    }

    /**
     * Resets the highlight state of ALL category buttons.
     * Ensures only one category can appear selected at a time.
     * Applies the proper reset method depending on whether the
     * category button contains an icon or not.
     */
    private void resetAllCategoryHighlights() {
        resetHighlightNoIcon(btnAll, textAll);
        resetHighlight(btnMusic, iconMusic, textMusic);
        resetHighlight(btnSports, iconSports, textSports);
        resetHighlight(btnEducation, iconEducation, textEducation);
        resetHighlight(btnArts, iconArts, textArts);
        resetHighlight(btnTechnology, iconTech, textTech);
        resetHighlight(btnCommunity, iconCommunity, textCommunity);
    }


}
