package com.bank.bayan;

public class UserPreferences {
    private boolean voiceEnabled;
    private String language;
    private boolean notifications;
    private float speechRate;
    private float speechPitch;
    private boolean hapticFeedback;
    private boolean continuousListening;

    public UserPreferences() {
        this.voiceEnabled = true;
        this.language = "ar";
        this.notifications = true;
        this.speechRate = 0.9f;
        this.speechPitch = 1.0f;
        this.hapticFeedback = true;
        this.continuousListening = false;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public float getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(float speechRate) {
        this.speechRate = speechRate;
    }

    public float getSpeechPitch() {
        return speechPitch;
    }

    public void setSpeechPitch(float speechPitch) {
        this.speechPitch = speechPitch;
    }

    public boolean isHapticFeedback() {
        return hapticFeedback;
    }

    public void setHapticFeedback(boolean hapticFeedback) {
        this.hapticFeedback = hapticFeedback;
    }

    public boolean isContinuousListening() {
        return continuousListening;
    }

    public void setContinuousListening(boolean continuousListening) {
        this.continuousListening = continuousListening;
    }
}

