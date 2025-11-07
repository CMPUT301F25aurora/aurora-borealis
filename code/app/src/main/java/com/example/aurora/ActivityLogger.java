package com.example.aurora;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class responsible for creating and writing structured logs to the Firestore
 * "logs" collection. Designed for use by administrators and organizers to keep
 * a consistent audit trail of important actions across the Aurora app.
 *
 * <p>Each log entry contains metadata such as:</p>
 * <ul>
 *     <li><b>type</b> – logical category of the action
 *         (e.g., <code>event_created</code>, <code>profile_removed</code>,
 *         <code>notification_sent</code>).</li>
 *     <li><b>title</b> – a short label used for display in the Admin UI.</li>
 *     <li><b>message</b> – a readable description of the action performed.</li>
 *     <li><b>timestamp</b> – automatically assigned server timestamp for chronological ordering.</li>
 * </ul>
 *
 * <p>Optional contextual fields include event and user identifiers, email addresses,
 * communication channels, and short message previews. The logger supports common actions such as:</p>
 * <ul>
 *     <li>Registering or removing users</li>
 *     <li>Creating or deleting events</li>
 *     <li>Recording notifications sent to entrants</li>
 * </ul>
 *
 * <p>All logging methods are static so that any activity, service, or fragment can
 * record an action with one simple method call, ensuring traceability throughout
 * the lifecycle of the app.</p>
 *
 * @author Aurora Borealis Team
 * @version 1.0
 */
public class ActivityLogger {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static void addLog(Map<String, Object> data) {
        if (!data.containsKey("timestamp")) {
            data.put("timestamp", FieldValue.serverTimestamp());
        }
        db.collection("logs").add(data);
    }

    // -------- USERS --------

    public static void logUserRegistered(String userId, String email, String role) {
        Map<String, Object> log = new HashMap<>();
        log.put("type", "user_registered");
        log.put("title", "User registered");
        log.put("message", "User registered: " + email + " (" + role + ")");
        log.put("userId", userId);
        log.put("userEmail", email);
        log.put("userRole", role);
        addLog(log);
    }

    public static void logProfileRemoved(String email) {
        Map<String, Object> log = new HashMap<>();
        log.put("type", "profile_removed");
        log.put("title", "Profile removed");
        log.put("message", "Profile removed: " + email);
        log.put("userEmail", email);
        addLog(log);
    }

    // -------- EVENTS --------

    public static void logEventCreated(String eventId, String title) {
        Map<String, Object> log = new HashMap<>();
        log.put("type", "event_created");
        log.put("title", "Event created");
        log.put("message", "Event created: " + title);
        log.put("eventId", eventId);
        log.put("eventTitle", title);
        addLog(log);
    }

    // Backwards-compatible for existing AdminActivity code using only title
    public static void logEventRemoved(String title) {
        logEventRemoved(null, title);
    }

    public static void logEventRemoved(String eventId, String title) {
        Map<String, Object> log = new HashMap<>();
        log.put("type", "event_removed");
        log.put("title", "Event removed");
        log.put("message", "Event removed: " + title);
        log.put("eventId", eventId);
        log.put("eventTitle", title);
        addLog(log);
    }

    // -------- NOTIFICATIONS --------
    // Call this anywhere an organizer sends a notification to entrants.

    public static void logNotificationSent(
            String eventId,
            String eventTitle,
            String recipientUserId,
            String recipientEmail,
            String channel,        // e.g. "in_app", "email", "push"
            String bodyPreview     // short body or subject
    ) {
        Map<String, Object> log = new HashMap<>();
        log.put("type", "notification_sent");
        log.put("title", "Notification sent");
        log.put("message", "Notification to " + recipientEmail + " about \"" + eventTitle + "\"");

        log.put("eventId", eventId);
        log.put("eventTitle", eventTitle);
        log.put("recipientUserId", recipientUserId);
        log.put("recipientEmail", recipientEmail);
        log.put("channel", channel);
        log.put("bodyPreview", bodyPreview);

        // Grab organizer info if using FirebaseAuth
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current != null) {
            log.put("organizerUserId", current.getUid());
            log.put("organizerEmail", current.getEmail());
        }

        addLog(log);
    }
}
