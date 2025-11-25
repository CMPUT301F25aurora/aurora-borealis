package com.example.aurora.notifications;

import com.example.aurora.models.NotificationModel;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Utility class for sending different types of notifications
 * into the Firestore "notifications" collection.
 */
public class FirestoreNotificationHelper {

    // 🔥 NEW: Universal method that checks user preference
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

    public static void sendWaitingListNotification(FirebaseFirestore db,
                                                   String userIdentifier,
                                                   String eventName,
                                                   String eventId) {

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

            // 🔥 CHECK PREFERENCE BEFORE SENDING
            sendIfAllowed(db, email, nm);
        });
    }

    public static void sendSelectedListNotification(FirebaseFirestore db,
                                                    String userIdentifier,
                                                    String eventName,
                                                    String eventId) {

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
                    "You are currently on the selected list for " + eventName,
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            // 🔥 CHECK PREFERENCE BEFORE SENDING
            sendIfAllowed(db, email, nm);
        });
    }

    public static void sendCancelledNotification(FirebaseFirestore db,
                                                 String userIdentifier,
                                                 String eventName,
                                                 String eventId) {

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
                    "You are currently on the cancelled list for " + eventName,
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            // 🔥 CHECK PREFERENCE BEFORE SENDING
            sendIfAllowed(db, email, nm);
        });
    }

    public static void sendCustomNotification(FirebaseFirestore db,
                                              String userIdentifier,
                                              String eventName,
                                              String eventId,
                                              String message) {

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

            // 🔥 CHECK PREFERENCE BEFORE SENDING
            sendIfAllowed(db, email, nm);
        });
    }
}
