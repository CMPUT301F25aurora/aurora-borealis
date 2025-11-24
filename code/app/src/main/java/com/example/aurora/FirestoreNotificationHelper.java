package com.example.aurora;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class FirestoreNotificationHelper {

    public static void sendWaitingListNotification(FirebaseFirestore db,
                                                   String userIdentifier,
                                                   String eventName,
                                                   String eventId) {

        CollectionReference users = db.collection("users");
        Query query;

        if (userIdentifier.contains("@")) {
            query = users.whereEqualTo("email", userIdentifier);
        } else {
            query = users.whereEqualTo(FieldPath.documentId(), userIdentifier);
        }

        query.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) return;

            String email = snapshot.getDocuments().get(0).getString("email");

            // Build consistent NotificationModel
            NotificationModel nm = new NotificationModel(
                    "waiting_list_info",
                    "Waiting List Update",
                    "You are currently on the waiting list for " + eventName,
                    eventId,
                    email,                // <-- this maps to userId
                    System.currentTimeMillis()  // <-- maps to createdAt
            );

            // Write to Firestore
            db.collection("notifications").add(nm);
        });
    }

    // ⭐ NEW — Selected list notification
    public static void sendSelectedListNotification(FirebaseFirestore db,
                                                    String userIdentifier,
                                                    String eventName,
                                                    String eventId) {

        CollectionReference users = db.collection("users");
        Query query;

        if (userIdentifier.contains("@")) {
            query = users.whereEqualTo("email", userIdentifier);
        } else {
            query = users.whereEqualTo(FieldPath.documentId(), userIdentifier);
        }

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

            db.collection("notifications").add(nm);
        });
    }

    // ⭐ NEW — Send notification to CANCELLED entrants
    public static void sendCancelledNotification(
            FirebaseFirestore db,
            String userIdentifier,
            String eventName,
            String eventId
    ) {

        CollectionReference users = db.collection("users");
        Query query;

        if (userIdentifier.contains("@")) {
            query = users.whereEqualTo("email", userIdentifier);
        } else {
            query = users.whereEqualTo(FieldPath.documentId(), userIdentifier);
        }

        query.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) return;

            String email = snapshot.getDocuments().get(0).getString("email");

            // ⭐ FIX: use the correct notification type
            NotificationModel nm = new NotificationModel(
                    "cancelled_list_info",        // ⭐ CHANGED
                    "Cancelled Entrant Update",
                    "You are currently on the cancelled list for " + eventName,
                    eventId,
                    email,
                    System.currentTimeMillis()
            );

            db.collection("notifications").add(nm);
        });
    }
}