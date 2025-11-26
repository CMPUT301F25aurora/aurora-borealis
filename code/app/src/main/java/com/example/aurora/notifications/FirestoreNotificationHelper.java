package com.example.aurora.notifications;

import android.util.Log;

import com.example.aurora.models.NotificationModel;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for sending notifications AND logging them.
 */
public class FirestoreNotificationHelper {

    // 🔹 Sends only if user allows notifications
    public static void sendIfAllowed(FirebaseFirestore db, String email, NotificationModel nm) {

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) return;

                    DocumentSnapshot userDoc = snapshot.getDocuments().get(0);

                    Boolean enabled = userDoc.getBoolean("entrant_notifications_enabled");

                    // Default = true if missing
                    if (enabled == null || enabled) {
                        db.collection("notifications").add(nm);
                    }
                });
    }

    // ----------------------------------------------------------
    // 🔵 WAITING LIST NOTIFICATION
    // ----------------------------------------------------------
    public static void sendWaitingListNotification(FirebaseFirestore db,
                                                   String userIdentifier,
                                                   String eventName,
                                                   String eventId,
                                                   String organizerEmail) {

        CollectionReference users = db.collection("users");

        Query query = userIdentifier.contains("@")
                ? users.whereEqualTo("email", userIdentifier)
                : users.whereEqualTo(FieldPath.documentId(), userIdentifier);

        query.get().addOnSuccessListener(snapshot -> {

            if (snapshot.isEmpty()) return;

            String email = snapshot.getDocuments().get(0).getString("email");

            NotificationModel nm = new NotificationModel(
                    "waiting_list_info",
                    "Waiting List Update",
                    "You are currently on the waiting list for " + eventName,
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            // Send notification if allowed
            sendIfAllowed(db, email, nm);

            // LOGGING
            logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    eventName,
                    email,
                    "Waiting list update sent",
                    "waiting_list_info"
            );

        });
    }

    // ----------------------------------------------------------
    // 🔵 SELECTED LIST — WINNER NOTIFICATION
    // ----------------------------------------------------------
    public static void sendSelectedListNotification(FirebaseFirestore db,
                                                    String userIdentifier,
                                                    String eventName,
                                                    String eventId,
                                                    String organizerEmail) {

        CollectionReference users = db.collection("users");

        Query query = userIdentifier.contains("@")
                ? users.whereEqualTo("email", userIdentifier)
                : users.whereEqualTo(FieldPath.documentId(), userIdentifier);

        query.get().addOnSuccessListener(snapshot -> {

            if (snapshot.isEmpty()) return;

            String email = snapshot.getDocuments().get(0).getString("email");

            NotificationModel nm = new NotificationModel(
                    "selected_list_info",
                    "Selected Entrant Update",
                    "You have been selected for " + eventName,
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            sendIfAllowed(db, email, nm);

            logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    eventName,
                    email,
                    "Selected entrant notification sent",
                    "selected_list_info"
            );
        });
    }

    // ----------------------------------------------------------
    // 🔵 CANCELLED FROM EVENT
    // ----------------------------------------------------------
    public static void sendCancelledNotification(FirebaseFirestore db,
                                                 String userIdentifier,
                                                 String eventName,
                                                 String eventId,
                                                 String organizerEmail) {

        CollectionReference users = db.collection("users");

        Query query = userIdentifier.contains("@")
                ? users.whereEqualTo("email", userIdentifier)
                : users.whereEqualTo(FieldPath.documentId(), userIdentifier);

        query.get().addOnSuccessListener(snapshot -> {

            if (snapshot.isEmpty()) return;

            String email = snapshot.getDocuments().get(0).getString("email");

            NotificationModel nm = new NotificationModel(
                    "cancelled_list_info",
                    "Cancelled Entrant Update",
                    "Your waiting list status for " + eventName + " has changed.",
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            sendIfAllowed(db, email, nm);

            logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    eventName,
                    email,
                    "Cancelled notification sent",
                    "cancelled_list_info"
            );
        });
    }

    // ----------------------------------------------------------
    // 🔵 CUSTOM MESSAGE
    // ----------------------------------------------------------
    public static void sendCustomNotification(FirebaseFirestore db,
                                              String userIdentifier,
                                              String eventName,
                                              String eventId,
                                              String message,
                                              String organizerEmail) {

        CollectionReference users = db.collection("users");

        Query query = userIdentifier.contains("@")
                ? users.whereEqualTo("email", userIdentifier)
                : users.whereEqualTo(FieldPath.documentId(), userIdentifier);

        query.get().addOnSuccessListener(snapshot -> {

            if (snapshot.isEmpty()) return;

            String email = snapshot.getDocuments().get(0).getString("email");

            NotificationModel nm = new NotificationModel(
                    "custom_message",
                    eventName,
                    message,
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            sendIfAllowed(db, email, nm);

            // 🔥 FIXED: Log the real message
            logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    eventName,
                    email,
                    message,           // <-- Correct
                    "custom_message"
            );
        });
    }
    // ----------------------------------------------------------
// 🔵 LOGGING FUNCTION (REQUIRED BY ALL NOTIFICATION TYPES)
// ----------------------------------------------------------
    public static void logNotification(
            FirebaseFirestore db,
            String organizerEmail,
            String eventId,
            String eventName,
            String recipientEmail,
            String message,
            String type
    ) {

        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("sentByOrganizerEmail", organizerEmail);
        log.put("eventId", eventId);
        log.put("eventName", eventName);
        log.put("toUserEmail", recipientEmail);
        log.put("message", message);
        log.put("notificationType", type);

        db.collection("notificationLogs")
                .add(log)
                .addOnSuccessListener(doc ->
                        Log.d("LOGS", "Notification log saved")
                )
                .addOnFailureListener(e ->
                        Log.e("LOGS", "Failed to save log", e)
                );
    }

}