package com.example.aurora.models;

public class NotificationModel {

    private String type;
    private String title;
    private String message;
    private String eventId;
    private String userId;
    private long createdAt;
    private String status;


    public NotificationModel() {}

    /**
     * NotificationModel
     *
     * Data model representing a single notification in the Aurora app.
     *
     * This model is stored inside the "notifications" collection in Firestore.
     * Each notification contains:
     * a type (winner_selected, not_selected, reminder, etc.)
     * a title and message for display
     * the event ID it belongs to
     * the user this notification is intended for
     * the creation timestamp
     * a status field (default: "pending") that organizers or users may update later
     */
    public NotificationModel(String type, String title, String message,
                             String eventId, String userId, long createdAt) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.eventId = eventId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.status = "pending"; // default
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public long getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
