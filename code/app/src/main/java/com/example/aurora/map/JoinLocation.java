package com.example.aurora.map;
/**
 * JoinLocation
 *
 * Simple Firestore-serializable model representing the geographic
 * location where an entrant joined an event’s waiting list.
 *
 * Stored under:
 *      events/{eventId}/waitingLocations/{doc}
 *
 * Fields:
 *  userKey : The email or identifier of the joining user.
 *  lat     : Latitude of the join location.
 *  lng     : Longitude of the join location.
 *
 * Firestore requires:
 *  Public fields OR getters/setters.
 *  A public no-argument constructor.
 */
public class JoinLocation {
    public String userKey;
    public double lat;
    public double lng;

    public JoinLocation() {}

/**
 * Creates a populated join location entry.
 *
 * @param userKey email or unique identifier of the user
 * @param lat     latitude where user joined
 * @param lng     longitude where user joined
 */
    public JoinLocation(String userKey, double lat, double lng) {
        this.userKey = userKey;
        this.lat = lat;
        this.lng = lng;
    }
}
