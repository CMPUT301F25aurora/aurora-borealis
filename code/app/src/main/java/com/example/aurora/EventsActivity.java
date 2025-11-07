package com.example.aurora;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventsAdapter adapter;
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> shownEvents = new ArrayList<>();

    private FirebaseFirestore db;
    private EditText searchBox;
    private Button btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology;
    private Button btnAvailabilityFilter;
    private Button logoutButton;

    private final Set<Integer> selectedDays = new HashSet<>();
    private final EnumSet<TimeSlot> selectedSlots = EnumSet.noneOf(TimeSlot.class);

    private enum TimeSlot { MORNING, AFTERNOON, EVENING }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventsAdapter(this, shownEvents, false);
        recyclerView.setAdapter(adapter);

        searchBox = findViewById(R.id.searchEvents);
        btnAvailabilityFilter = findViewById(R.id.btnAvailabilityFilter);

        btnAll = findViewById(R.id.btnAll);
        btnMusic = findViewById(R.id.btnMusic);
        btnSports = findViewById(R.id.btnSports);
        btnEducation = findViewById(R.id.btnEducation);
        btnArts = findViewById(R.id.btnArts);
        btnTechnology = findViewById(R.id.btnTechnology);

        btnAll.setOnClickListener(v -> loadEvents(null));
        btnMusic.setOnClickListener(v -> loadEvents("Music"));
        btnSports.setOnClickListener(v -> loadEvents("Sports"));
        btnEducation.setOnClickListener(v -> loadEvents("Education"));
        btnArts.setOnClickListener(v -> loadEvents("Arts"));
        btnTechnology.setOnClickListener(v -> loadEvents("Technology"));

        if (searchBox != null) {
            searchBox.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyAllFilters(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnAvailabilityFilter != null) {
            btnAvailabilityFilter.setOnClickListener(v -> showAvailabilityDialog());
        }

        logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            });
        }

        loadEvents(null);
    }

    private void showAvailabilityDialog() {
        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_filter_availability, null, false);
        builder.setView(view);
        dialog = builder.create();

        CheckBox chkMon = view.findViewById(R.id.chkMon);
        CheckBox chkTue = view.findViewById(R.id.chkTue);
        CheckBox chkWed = view.findViewById(R.id.chkWed);
        CheckBox chkThu = view.findViewById(R.id.chkThu);
        CheckBox chkFri = view.findViewById(R.id.chkFri);
        CheckBox chkSat = view.findViewById(R.id.chkSat);
        CheckBox chkSun = view.findViewById(R.id.chkSun);

        CheckBox chkMorning = view.findViewById(R.id.chkMorning);
        CheckBox chkAfternoon = view.findViewById(R.id.chkAfternoon);
        CheckBox chkEvening = view.findViewById(R.id.chkEvening);

        chkMon.setChecked(selectedDays.contains(Calendar.MONDAY));
        chkTue.setChecked(selectedDays.contains(Calendar.TUESDAY));
        chkWed.setChecked(selectedDays.contains(Calendar.WEDNESDAY));
        chkThu.setChecked(selectedDays.contains(Calendar.THURSDAY));
        chkFri.setChecked(selectedDays.contains(Calendar.FRIDAY));
        chkSat.setChecked(selectedDays.contains(Calendar.SATURDAY));
        chkSun.setChecked(selectedDays.contains(Calendar.SUNDAY));

        chkMorning.setChecked(selectedSlots.contains(TimeSlot.MORNING));
        chkAfternoon.setChecked(selectedSlots.contains(TimeSlot.AFTERNOON));
        chkEvening.setChecked(selectedSlots.contains(TimeSlot.EVENING));

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnClear).setOnClickListener(v -> {
            selectedDays.clear();
            selectedSlots.clear();
            applyAllFilters();
            dialog.dismiss();
        });
        view.findViewById(R.id.btnApply).setOnClickListener(v -> {
            selectedDays.clear();
            if (chkMon.isChecked()) selectedDays.add(Calendar.MONDAY);
            if (chkTue.isChecked()) selectedDays.add(Calendar.TUESDAY);
            if (chkWed.isChecked()) selectedDays.add(Calendar.WEDNESDAY);
            if (chkThu.isChecked()) selectedDays.add(Calendar.THURSDAY);
            if (chkFri.isChecked()) selectedDays.add(Calendar.FRIDAY);
            if (chkSat.isChecked()) selectedDays.add(Calendar.SATURDAY);
            if (chkSun.isChecked()) selectedDays.add(Calendar.SUNDAY);

            selectedSlots.clear();
            if (chkMorning.isChecked()) selectedSlots.add(TimeSlot.MORNING);
            if (chkAfternoon.isChecked()) selectedSlots.add(TimeSlot.AFTERNOON);
            if (chkEvening.isChecked()) selectedSlots.add(TimeSlot.EVENING);

            applyAllFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadEvents(@Nullable String category) {
        Query q = db.collection("events");
        if (category != null) q = q.whereEqualTo("category", category);

        q.get().addOnSuccessListener(query -> {
            allEvents.clear();
            for (QueryDocumentSnapshot doc : query) {
                Event e = doc.toObject(Event.class);
                e.setEventId(doc.getId());
                allEvents.add(e);
            }
            applyAllFilters();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show());
    }

    private void applyAllFilters() {
        String search = searchBox != null ? searchBox.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";

        shownEvents.clear();
        for (Event e : allEvents) {
            if (!matchesSearch(e, search)) continue;
            if (!matchesAvailability(e)) continue;
            shownEvents.add(e);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean matchesSearch(Event e, String search) {
        if (search.isEmpty()) return true;
        String t = safe(e.getTitle());
        String d = safe(e.getDescription());
        String loc = safe(e.getLocation());
        return t.contains(search) || d.contains(search) || loc.contains(search);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.getDefault());
    }

    private boolean matchesAvailability(Event e) {
        if (selectedDays.isEmpty() && selectedSlots.isEmpty()) return true;
        Calendar cal = parseEventStart(e.getStartDate());
        if (cal == null) return true;
        int day = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean dayOk = selectedDays.isEmpty() || selectedDays.contains(day);
        boolean timeOk = selectedSlots.isEmpty() || slotMatch(hour);
        return dayOk && timeOk;
    }

    private boolean slotMatch(int h) {
        boolean morning = selectedSlots.contains(TimeSlot.MORNING) && (h >= 6 && h < 12);
        boolean afternoon = selectedSlots.contains(TimeSlot.AFTERNOON) && (h >= 12 && h < 18);
        boolean evening = selectedSlots.contains(TimeSlot.EVENING) && (h >= 18 && h <= 23);
        return morning || afternoon || evening;
    }

    @Nullable
    private Calendar parseEventStart(@Nullable String start) {
        if (start == null || start.trim().isEmpty()) return null;
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.getDefault());
                Calendar c = Calendar.getInstance();
                c.setTime(sdf.parse(start));
                return c;
            } catch (ParseException ignored) {}
        }
        return null;
    }
}
