/*
 * References:
 *
 * 1) FirebaseUI Android — "Model class requirements"
 *    https://firebaseopensource.com/projects/firebase/firebaseui-android/firestore/readme/
 *    Used as a reference for Firestore model classes needing an empty constructor and standard getters/setters.
 *
 * 2) author: Stack Overflow user — "Android Firestore limitations to custom object models"
 *    https://stackoverflow.com/questions/54193629/android-firestore-limitations-to-custom-object-models
 *    Used as a reference for how Firestore maps document fields into POJO fields and list properties.
 */

package com.example.aurora.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified Event model that:
 * - Works with old docs (name, date, capacity, etc.) without crashing.
 * - Works with new docs (title, startDate/endDate, maxSpots, geoRequired, etc.).
 *
 * NOTE: We intentionally DO NOT have any field or getter/setter called "capacity"
 * so Firestore won't try to map the legacy "capacity" field and crash.
 */




public class Event {

    // Not stored; you set it from doc.getId()
    private String eventId;

    // New-ish fields
    private String title;
    private String description;
    private String location;
    private String category;

    // Old & new date fields
    private String date;         // legacy combined date field
    private String startDate;    // new
    private String endDate;      // new

    private String registrationStart;
    private String registrationEnd;

    private String posterUrl;
    private String deepLink;

    private Boolean geoRequired; // optional

    // Numeric capacity for new events
    private Long maxSpots;       // we’ll save this in Firestore going forward

    // Legacy title field
    private String name;         // old code used this

    // Lists for waiting/selected/final entrants
    private List<String> waitingList;
    private List<String> selectedEntrants;
    private List<String> acceptedEntrants;
    private List<String> finalEntrants;
    private List<String> cancelledEntrants;

    // Required empty constructor for Firestore
    public Event() {}

    // ID (local-only)

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // Title / Name 

    public String getTitle() {
        if (title != null && !title.isEmpty()) return title;
        if (name != null && !name.isEmpty()) return name;
        return "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Description

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Location / Category 

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Dates

    /**
     * Unified getter used by cards: prefer legacy "date" if set,
     * otherwise fall back to "startDate".
     */
    public String getDate() {
        if (date != null && !date.isEmpty()) return date;
        if (startDate != null) return startDate;
        return "";
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(String registrationStart) {
        this.registrationStart = registrationStart;
    }

    public String getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(String registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    // Poster / Deep link

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }

    // Capacity (new) 

    public Long getMaxSpots() {
        return maxSpots;
    }

    public void setMaxSpots(Long maxSpots) {
        this.maxSpots = maxSpots;
    }

    // Geo requirement
    public boolean getGeoRequired() {
        if (geoRequired == null) return false;
        return geoRequired;
    }

    public void setGeoRequired(Boolean geoRequired) {
        this.geoRequired = geoRequired;
    }

    // Lists 

    public List<String> getWaitingList() {
        if (waitingList == null) waitingList = new ArrayList<>();
        return waitingList;
    }

    public void setWaitingList(List<String> waitingList) {
        this.waitingList = waitingList;
    }

    public List<String> getSelectedEntrants() {
        if (selectedEntrants == null) selectedEntrants = new ArrayList<>();
        return selectedEntrants;
    }

    public void setSelectedEntrants(List<String> selectedEntrants) {
        this.selectedEntrants = selectedEntrants;
    }

    public List<String> getAcceptedEntrants() {
        if (acceptedEntrants == null) acceptedEntrants = new ArrayList<>();
        return acceptedEntrants;
    }

    public void setAcceptedEntrants(List<String> acceptedEntrants) {
        this.acceptedEntrants = acceptedEntrants;
    }

    public List<String> getFinalEntrants() {
        if (finalEntrants == null) finalEntrants = new ArrayList<>();
        return finalEntrants;
    }

    public void setFinalEntrants(List<String> finalEntrants) {
        this.finalEntrants = finalEntrants;
    }

    public List<String> getCancelledEntrants() {
        if (cancelledEntrants == null) cancelledEntrants = new ArrayList<>();
        return cancelledEntrants;
    }

    public void setCancelledEntrants(List<String> cancelledEntrants) {
        this.cancelledEntrants = cancelledEntrants;
    }
}
