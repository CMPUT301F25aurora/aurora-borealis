package com.example.aurora;
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
