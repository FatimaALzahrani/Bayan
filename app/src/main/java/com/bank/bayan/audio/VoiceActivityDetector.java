package com.bank.bayan.audio;

import android.util.Log;


public class VoiceActivityDetector {

    private static final String TAG = "VoiceActivityDetector";

    private static final double SILENCE_THRESHOLD = 500.0;
    private static final int SILENCE_DURATION_MS = 1500;
    private static final int MIN_SPEECH_DURATION_MS = 2000;
    private static final int SAMPLE_RATE = 16000;
    private static final int WINDOW_SIZE_MS = 100;

    private boolean isSpeaking = false;
    private long lastSpeechTime = 0;
    private long speechStartTime = 0;
    private int samplesPerWindow;

    public interface VoiceActivityListener {
        void onSpeechStart();
        void onSpeechEnd();
        void onSilenceDetected(long speechDurationMs);
    }

    private VoiceActivityListener listener;

    public VoiceActivityDetector(VoiceActivityListener listener) {
        this.listener = listener;
        this.samplesPerWindow = (SAMPLE_RATE * WINDOW_SIZE_MS) / 1000;
    }

    public void processAudioData(short[] audioData, int length) {
        if (audioData == null || length == 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        double energy = calculateEnergy(audioData, length);

        boolean currentlySpeaking = energy > SILENCE_THRESHOLD;

        if (currentlySpeaking) {
            lastSpeechTime = currentTime;

            if (!isSpeaking) {
                isSpeaking = true;
                speechStartTime = currentTime;
                if (listener != null) {
                    listener.onSpeechStart();
                }
                Log.d(TAG, "Speech started - Energy: " + energy);
            }
        } else {
            if (isSpeaking) {
                long silenceDuration = currentTime - lastSpeechTime;
                long speechDuration = lastSpeechTime - speechStartTime;

                if (silenceDuration >= SILENCE_DURATION_MS && speechDuration >= MIN_SPEECH_DURATION_MS) {
                    isSpeaking = false;
                    if (listener != null) {
                        listener.onSpeechEnd();
                        listener.onSilenceDetected(speechDuration);
                    }
                    Log.d(TAG, "Speech ended - Duration: " + speechDuration + "ms, Silence: " + silenceDuration + "ms");
                }
            }
        }
    }

    private double calculateEnergy(short[] audioData, int length) {
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            sum += Math.abs(audioData[i]);
        }
        return sum / length;
    }

    public void reset() {
        isSpeaking = false;
        lastSpeechTime = 0;
        speechStartTime = 0;
    }


    public void setSilenceThreshold(double threshold) {
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }
}