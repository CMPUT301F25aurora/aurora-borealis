
package com.example.aurora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
/**
 * Fragment that displays a scrollable list of events for entrants.
 *
 * Shows events loaded from Firestore and lets users:
 * - Filter by category (All, Music, Sports, Education, Arts, Technology)
 * - Scan QR codes to open event details
 * - Log out and return to the login screen
 *
 * Note: The search bar is included in the layout but not yet functional.
 */


public class EventsFragment extends Fragment {

    private EditText searchEvents;
    private Button logoutButton;
    private RecyclerView recyclerEvents;
    private EventsAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Button btnAll, btnMusic, btnSports, btnEducation, btnArts, btnTechnology;
    private Button btnScanQr;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchEvents = view.findViewById(R.id.searchEvents);
        logoutButton = view.findViewById(R.id.logoutButton);
        recyclerEvents = view.findViewById(R.id.recyclerEvents);

        btnAll = view.findViewById(R.id.btnAll);
        btnMusic = view.findViewById(R.id.btnMusic);
        btnSports = view.findViewById(R.id.btnSports);
        btnEducation = view.findViewById(R.id.btnEducation);
        btnArts = view.findViewById(R.id.btnArts);
        btnTechnology = view.findViewById(R.id.btnTechnology);
        btnScanQr = view.findViewById(R.id.btnScanQr);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventsAdapter(requireContext(), eventList);
        recyclerEvents.setAdapter(adapter);

        logoutButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        btnAll.setOnClickListener(v -> loadEvents(null));
        btnMusic.setOnClickListener(v -> loadEvents("Music"));
        btnSports.setOnClickListener(v -> loadEvents("Sports"));
        btnEducation.setOnClickListener(v -> loadEvents("Education"));
        btnArts.setOnClickListener(v -> loadEvents("Arts"));
        btnTechnology.setOnClickListener(v -> loadEvents("Technology"));

        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> startQrScan());
        }

        // Initial load
        loadEvents(null);
    }

    private void loadEvents(@Nullable String category) {
        Query q = db.collection("events");
        if (!TextUtils.isEmpty(category)) {
            q = q.whereEqualTo("category", category);
        }

        q.get()
                .addOnSuccessListener(query -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show());
    }

    // QR scanning

    private void startQrScan() {
        // IMPORTANT: use forSupportFragment for fragments
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan Aurora event QR");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
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
                    eventId = uri.getPath().substring(1); // /<id>
                } else {
                    eventId = uri.getQueryParameter("id");
                }

                if (eventId != null && !eventId.isEmpty()) {
                    Intent i = new Intent(getContext(), EventDetailsActivity.class);
                    i.putExtra("eventId", eventId);
                    startActivity(i);
                } else {
                    Toast.makeText(getContext(), "Invalid Aurora event QR", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Not an Aurora event QR", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to handle QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
