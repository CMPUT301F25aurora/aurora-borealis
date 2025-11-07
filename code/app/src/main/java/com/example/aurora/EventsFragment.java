package com.example.aurora;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class EventsFragment extends Fragment {

    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> shownEvents = new ArrayList<>();

    private FirebaseFirestore db;

    private EditText searchEvents;
    private Button btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology;
    private Button btnAvailabilityFilter;
    private Button logoutButton;

    private final Set<Integer> selectedDays = new HashSet<>();
    private final EnumSet<TimeSlot> selectedSlots = EnumSet.noneOf(TimeSlot.class);

    private enum TimeSlot { MORNING, AFTERNOON, EVENING }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_events, container, false);

        db = FirebaseFirestore.getInstance();

        searchEvents = v.findViewById(R.id.searchEvents);
        logoutButton = v.findViewById(R.id.logoutButton);
        recyclerEvents = v.findViewById(R.id.recyclerEvents);
        btnAll = v.findViewById(R.id.btnAll);
        btnMusic = v.findViewById(R.id.btnMusic);
        btnSports = v.findViewById(R.id.btnSports);
        btnEducation = v.findViewById(R.id.btnEducation);
        btnArts = v.findViewById(R.id.btnArts);
        btnTechnology = v.findViewById(R.id.btnTechnology);
        btnAvailabilityFilter = v.findViewById(R.id.btnAvailabilityFilter);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventsAdapter(requireContext(), shownEvents);
        recyclerEvents.setAdapter(adapter);

        btnAll.setOnClickListener(x -> loadEvents(null));
        btnMusic.setOnClickListener(x -> loadEvents("Music"));
        btnSports.setOnClickListener(x -> loadEvents("Sports"));
        btnEducation.setOnClickListener(x -> loadEvents("Education"));
        btnArts.setOnClickListener(x -> loadEvents("Arts"));
        btnTechnology.setOnClickListener(x -> loadEvents("Technology"));

        if (searchEvents != null) {
            searchEvents.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyAllFilters(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnAvailabilityFilter != null) {
            btnAvailabilityFilter.setOnClickListener(x -> showAvailabilityDialog());
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(x -> {
                Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(getContext(), LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                requireActivity().finish();
            });
        }

        loadEvents(null);
        return v;
    }

    private void showAvailabilityDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_availability, null, false);

        CheckBox chkMon = dialogView.findViewById(R.id.chkMon);
        CheckBox chkTue = dialogView.findViewById(R.id.chkTue);
        CheckBox chkWed = dialogView.findViewById(R.id.chkWed);
        CheckBox chkThu = dialogView.findViewById(R.id.chkThu);
        CheckBox chkFri = dialogView.findViewById(R.id.chkFri);
        CheckBox chkSat = dialogView.findViewById(R.id.chkSat);
        CheckBox chkSun = dialogView.findViewById(R.id.chkSun);

        CheckBox chkMorning = dialogView.findViewById(R.id.chkMorning);
        CheckBox chkAfternoon = dialogView.findViewById(R.id.chkAfternoon);
        CheckBox chkEvening = dialogView.findViewById(R.id.chkEvening);

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

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnClear).setOnClickListener(v -> {
            selectedDays.clear();
            selectedSlots.clear();
            applyAllFilters();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnApply).setOnClickListener(v -> {
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
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show());
    }

    private void applyAllFilters() {
        String search = searchEvents != null ? searchEvents.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
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

    private boolean slotMatch(int hour24) {
        boolean morning = selectedSlots.contains(TimeSlot.MORNING) && (hour24 >= 6 && hour24 < 12);
        boolean afternoon = selectedSlots.contains(TimeSlot.AFTERNOON) && (hour24 >= 12 && hour24 < 18);
        boolean evening = selectedSlots.contains(TimeSlot.EVENING) && (hour24 >= 18 && hour24 <= 23);
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
