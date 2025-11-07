package com.example.aurora;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.util.Date;

public class AlertsFragment extends Fragment {

    private LinearLayout container;
    private FirebaseFirestore db;
    private ListenerRegistration reg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alerts, parent, false);
        container = v.findViewById(R.id.notificationsContainer);
        db = FirebaseFirestore.getInstance();
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
        startListening();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) reg.remove();
    }

    private void startListening() {
        String androidId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Query q = db.collection("notifications")
                .whereEqualTo("userId", androidId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50);
        reg = q.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            for (DocumentChange dc : snap.getDocumentChanges()) {
                DocumentSnapshot d = dc.getDocument();
                String type = d.getString("type");
                String title = d.getString("title");
                String message = d.getString("message");
                Timestamp ts = d.getTimestamp("createdAt");
                addCard(title, message, ts != null ? ts.toDate() : new Date());
                if ("lottery_win".equals(type)) showSystemNotification(title != null ? title : "You won the lottery!", message != null ? message : "");
                if ("lottery_lose".equals(type)) showSystemNotification(title != null ? title : "Not selected this time", message != null ? message : "");
            }
        });
    }

    private void addCard(String title, String message, Date when) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_notification_card, container, false);
        TextView t = card.findViewById(R.id.notifTitle);
        TextView m = card.findViewById(R.id.notifMessage);
        TextView time = card.findViewById(R.id.notifTime);
        t.setText(title != null ? title : "Notification");
        m.setText(message != null ? message : "");
        time.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(when));
        container.addView(card, 0);
    }

    private void showSystemNotification(String title, String message) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(requireContext(), AuroraApp.CHANNEL_WINNER)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), b.build());
    }
}
