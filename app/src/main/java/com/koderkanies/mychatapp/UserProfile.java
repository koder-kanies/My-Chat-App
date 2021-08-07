package com.koderkanies.mychatapp;

public class UserProfile {
    String photoUrl;
    String username;
    String email;
    String contact;
    String bio;
    String dob;
    String gender;

    public UserProfile() {
    }

    public UserProfile( String photoUrl, String username, String email, String contact, String bio, String dob, String gender) {
        this.photoUrl = photoUrl;
        this.username = username;
        this.email = email;
        this.contact = contact;
        this.bio = bio;
        this.dob = dob;
        this.gender = gender;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
