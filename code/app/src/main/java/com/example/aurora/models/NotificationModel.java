package com.example.aurora.models;

public class NotificationModel {

    private String type;
    private String title;
    private String message;
    private String eventId;
    private String userId;
    private long createdAt;
    private String status;

    // REQUIRED EMPTY CONSTRUCTOR for Firestore
    public NotificationModel() {}

    // This matches your constructor call in OrganizerActivity
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
