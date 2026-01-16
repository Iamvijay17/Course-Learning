package com.course_learning.backend.dto;

import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Size(max = 200, message = "Website URL cannot exceed 200 characters")
    private String website;

    // Constructors
    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String firstName, String lastName, String bio, String website) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.website = website;
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
