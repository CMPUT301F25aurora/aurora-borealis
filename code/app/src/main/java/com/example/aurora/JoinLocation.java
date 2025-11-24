package com.example.aurora;

public class JoinLocation {
    public String userKey;
    public double lat;
    public double lng;

    public JoinLocation() {}

    public JoinLocation(String userKey, double lat, double lng) {
        this.userKey = userKey;
        this.lat = lat;
        this.lng = lng;
    }
}
