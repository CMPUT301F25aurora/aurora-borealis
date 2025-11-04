package com.example.aurora;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Small helper to write admin/ system logs to Firestore.
 * Collection: "logs"
 *
 * Each document: {
 *   type: "event_created" | "user_registered" | "event_removed" | "profile_removed" | ...
 *   title: "Event Created"
 *   message: "Event \"City Marathon\" was created."
 *   timestamp: server timestamp
 * }
 */
public class ActivityLogger {

    private static final String TAG = "ActivityLogger";

    private static void log(String type, String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("title", title);
        data.put("message", message);
        data.put("timestamp", FieldValue.serverTimestamp());

        db.collection("logs")
                .add(data)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to log activity: " + e.getMessage()));
    }

    public static void logEventCreated(String eventName) {
        log("event_created", "Event Created",
                "Event \"" + eventName + "\" was created.");
    }

    public static void logUserRegistered(String email) {
        log("user_registered", "User Registered",
                "User \"" + email + "\" registered.");
    }

    public static void logEventRemoved(String eventName) {
        log("event_removed", "Event Removed",
                "Event \"" + eventName + "\" was removed by admin.");
    }

    public static void logProfileRemoved(String email) {
        log("profile_removed", "Profile Removed",
                "Profile \"" + email + "\" was removed by admin.");
    }
}
