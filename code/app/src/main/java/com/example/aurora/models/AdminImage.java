package com.example.aurora.models;

/**
 * AdminImage
 *
 * Simple model class used by the Admin panel to represent an event poster record.
 * Admins can view event posters, see which event they belong to, and remove them if needed.
 *
 * Fields Included:
 * - eventId: ID of the event this poster belongs to.
 * - eventTitle: Human-readable name of the event.
 * - organizerEmail: Email of the organizer who created the event.
 * - posterUrl: Download URL of the uploaded poster stored in Firebase Storage.
 */
public class AdminImage {
    public String eventId;
    public String eventTitle;
    public String organizerEmail;
    public String posterUrl;

    public AdminImage() {}

    public AdminImage(String eventId, String eventTitle, String organizerEmail, String posterUrl) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.organizerEmail = organizerEmail;
        this.posterUrl = posterUrl;
    }
}
