package com.example.aurora;

/**
 * Simple model for Firestore "users" collection used by the admin panel.
 */
public class AppUser {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;

    public AppUser() {} // Firestore needs empty constructor

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
