package com.example.aurora;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private String eventId; // This will hold the Firestore document ID
    private String title;
    private String description;
    private String date;
    private String location;
    private String posterUrl;
    private List<String> waitingList;
    private String startDate;
    private String endDate;
    private String deepLink;

    public Event() {
        // Default constructor required for Firestore
    }

    // Constructor for testing (optional)
    public Event(String eventId, String title, String date, String location, String posterUrl) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.location = location;
        this.posterUrl = posterUrl;
        this.waitingList = new ArrayList<>();
    }

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public List<String> getWaitingList() { return waitingList; }
    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList; }


    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getDeepLink() {
        return deepLink;
    }
    //for qr code functionality
    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }
}
