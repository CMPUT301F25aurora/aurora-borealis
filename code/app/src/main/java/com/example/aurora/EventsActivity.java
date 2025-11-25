package com.example.aurora;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 * Entrant home screen (main screen after login)
 *
 * Shows events + receives real-time notifications from organizers.
 */
public class EventsActivity extends AppCompatActivity {

    // 🔔 Notification listener
    private ListenerRegistration notifListener;

    private EditText searchEvents;
    private Button logoutButton;
    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;

    private Button btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology;
    private Button navEvents, navProfile, navAlerts;
    private Button btnScanQr;
    private Button btnFilter;

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
        setContentView(R.layout.activity_events);

        searchEvents = findViewById(R.id.searchEvents);
        logoutButton = findViewById(R.id.logoutButton);
        recyclerEvents = findViewById(R.id.recyclerEvents);

        btnAll = findViewById(R.id.btnAll);
        btnMusic = findViewById(R.id.btnMusic);
        btnSports = findViewById(R.id.btnSports);
        btnEducation = findViewById(R.id.btnEducation);
        btnArts = findViewById(R.id.btnArts);
        btnTechnology = findViewById(R.id.btnTechnology);

        navEvents = findViewById(R.id.navEvents);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);

        btnScanQr = findViewById(R.id.btnScanQr);
        btnFilter = findViewById(R.id.btnFilter);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventsAdapter(this, eventList);
        recyclerEvents.setAdapter(adapter);

        logoutButton.setOnClickListener(v -> logoutUser());

        btnAll.setOnClickListener(v -> loadEvents(null));
        btnMusic.setOnClickListener(v -> loadEvents("Music"));
        btnSports.setOnClickListener(v -> loadEvents("Sports"));
        btnEducation.setOnClickListener(v -> loadEvents("Education"));
        btnArts.setOnClickListener(v -> loadEvents("Arts"));
        btnTechnology.setOnClickListener(v -> loadEvents("Technology"));

        navEvents.setOnClickListener(v -> {});
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        navAlerts.setOnClickListener(v ->
                startActivity(new Intent(this, AlertsActivity.class)));

        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> startQrScan());
        }

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

        // Load events
        loadEvents(null);

        // 🔔 START NOTIFICATION LISTENER FOR ENTRANTS
        listenForNotifications();
    }

    // ------------------------------------------------------------
    // 🔔 REALTIME NOTIFICATION LISTENER
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // 🔔 SHOW ANDROID NOTIFICATION
    // ------------------------------------------------------------

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifListener.remove();
    }

    // ------------------------------------------------------------
    // EXISTING CODE — EVENTS, FILTERS, QR SCAN ETC (UNCHANGED)
    // ------------------------------------------------------------

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

    private void startQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan Aurora event QR");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

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
}
