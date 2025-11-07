package com.example.aurora;

/**
 * Represents a lightweight data model used by the Admin dashboard
 * to display basic event information in a list.
 *
 * <p>This class only stores key event details like title, date, and
 * location, which are enough for the admin interface to show and
 * manage events efficiently without loading full event documents.</p>
 *
 * <p>Each {@code AdminEventItem} includes the event ID, title, date,
 * location, category, maximum spots, and the current waiting list
 * count.</p>
 *
 * <p><b>Usage:</b> Objects of this class are typically created when
 * mapping Firestore documents into list items for the Admin view.</p>
 */

public class AdminEventItem {
    private String id;
    private String title;
    private String date;
    private String location;
    private String category;
    private long maxSpots;
    private int waitingCount;

    public AdminEventItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getMaxSpots() { return maxSpots; }
    public void setMaxSpots(long maxSpots) { this.maxSpots = maxSpots; }

    public int getWaitingCount() { return waitingCount; }
    public void setWaitingCount(int waitingCount) { this.waitingCount = waitingCount; }
}
