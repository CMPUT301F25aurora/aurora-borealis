/*
 * source: Firebase docs — "Perform simple and compound queries in Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/query-data/queries
 * note: Used for the logic distinguishing between email queries and document ID queries (FieldPath.documentId()).
 *
 * source: Firebase docs — "Add data to Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/manage-data/add-data
 * note: Used for db.collection("notifications").add(nm) to write new alerts to the database.
 *
 * source: Android Developers — "Log".
 * url: https://developer.android.com/reference/android/util/Log
 * note: Used for standard Android logging (Log.d, Log.e) to track the success or failure of notification writes.
 *
 * source: Firebase docs — "Get data with Cloud Firestore".
 * url: https://firebase.google.com/docs/firestore/query-data/get-data
 * note: Used for checking if a user exists and if their notifications are enabled before sending.
 */


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

    public static void sendIfAllowed(FirebaseFirestore db, String email, NotificationModel nm) {

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) return;

                    DocumentSnapshot userDoc = snapshot.getDocuments().get(0);

                    Boolean enabled = userDoc.getBoolean("entrant_notifications_enabled");

                    if (enabled == null || enabled) {
                        db.collection("notifications").add(nm);
                    }
                });
    }

    /**
     * Sends a notification to a user informing them that they are on the waiting list.
     * Also logs the action in "notificationLogs".
     *
     * @param db              Firestore instance
     * @param userIdentifier  email or Firestore UID
     * @param eventName       readable event title
     * @param eventId         Firestore event document ID
     * @param organizerEmail  organizer performing the action
     */
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

            sendIfAllowed(db, email, nm);

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

    /**
     * Notifies a user that they have been selected for an event.
     *
     * @param db              Firestore instance
     * @param userIdentifier  email or document ID
     * @param eventName       event name
     * @param eventId         event ID
     * @param organizerEmail  organizer performing the action
     */
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

    /**
     * Notifies a user that their status for an event has been cancelled.
     *
     * @param db              Firestore instance
     * @param userIdentifier  email or document ID
     * @param eventName       event name
     * @param eventId         event ID
     * @param organizerEmail  organizer performing the action
     */
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

    /**
     * Sends a custom text message notification to a specific user.
     *
     * @param db              Firestore instance
     * @param userIdentifier  email or document ID
     * @param eventName       event name
     * @param eventId         event ID
     * @param message         message body to send
     * @param organizerEmail  organizer performing the action
     */
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

            logNotification(
                    db,
                    organizerEmail,
                    eventId,
                    eventName,
                    email,
                    message,
                    "custom_message"
            );
        });
    }

    /**
     * Saves a record of every notification organizers send to users.
     *
     * @param db              Firestore instance
     * @param organizerEmail  organizer/admin performing the action
     * @param eventId         ID of event involved (nullable)
     * @param eventName       name of event
     * @param recipientEmail  user receiving the notification
     * @param message         message content
     * @param type            notification type
     */
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
    /**
     * Sends a user-facing notification + admin log when organizer access is revoked.
     */
    public static void sendOrganizerRevokedNotification(FirebaseFirestore db, String email) {


        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "organizer_revoked");
        notif.put("title", "Organizer Access Revoked");
        notif.put("message", "An admin has removed your organizer privileges.");
        notif.put("eventId", null);
        notif.put("userId", email);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("status", "unread");
        db.collection("notifications").add(notif);


        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("sentByOrganizerEmail", "admin");
        log.put("eventId", null);
        log.put("eventName", "Admin Action");
        log.put("toUserEmail", email);
        log.put("message", "Organizer privileges revoked");
        log.put("notificationType", "organizer_revoked");
        db.collection("notificationLogs").add(log);
    }

    /**
     * Sends a notification + log when organizer access is restored.
     */
    public static void sendOrganizerEnabledNotification(FirebaseFirestore db, String email) {

        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "organizer_enabled");
        notif.put("title", "Organizer Access Restored");
        notif.put("message", "Your organizer privileges have been restored by an admin.");
        notif.put("eventId", null);
        notif.put("userId", email);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("status", "unread");
        db.collection("notifications").add(notif);

        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("sentByOrganizerEmail", "admin");
        log.put("eventId", null);
        log.put("eventName", "Admin Action");
        log.put("toUserEmail", email);
        log.put("message", "Organizer privileges restored");
        log.put("notificationType", "organizer_enabled");
        db.collection("notificationLogs").add(log);
    }


}
