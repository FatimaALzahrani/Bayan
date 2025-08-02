package com.bank.bayan.audio;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioFeatureExtractor {

    private static final String TAG = "AudioFeatureExtractor";
    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_SIZE = 512;
    private static final int HOP_SIZE = 256;

    public static VoiceFeatures extractFeatures(File audioFile) {
        try {
            short[] audioData = readWavFile(audioFile);
            if (audioData == null || audioData.length < SAMPLE_RATE) {
                Log.e(TAG, "ملف الصوت قصير جداً أو تالف");
                return null;
            }

            VoiceFeatures features = new VoiceFeatures();

            features.duration = (double) audioData.length / SAMPLE_RATE;

            features.averageAmplitude = calculateAverageAmplitude(audioData);

            features.averagePitch = calculateAveragePitch(audioData);

            features.speechRate = calculateSpeechRate(audioData);

            features.spectralCentroid = calculateSpectralCentroid(audioData);
            features.spectralRolloff = calculateSpectralRolloff(audioData);

            features.mfccFeatures = calculateMFCC(audioData);

            features.voiceFingerprint = generateVoiceFingerprint(features);

            Log.d(TAG, "تم استخراج الخصائص بنجاح: " + features.toString());
            return features;

        } catch (Exception e) {
            Log.e(TAG, "خطأ في استخراج الخصائص: " + e.getMessage(), e);
            return null;
        }
    }

    public static double compareFeatures(VoiceFeatures features1, VoiceFeatures features2) {
        if (features1 == null || features2 == null) {
            return 0.0;
        }

        try {
            double totalSimilarity = 0.0;
            int featureCount = 0;

            double amplitudeSimilarity = 1.0 - Math.abs(features1.averageAmplitude - features2.averageAmplitude)
                    / Math.max(features1.averageAmplitude, features2.averageAmplitude);
            totalSimilarity += amplitudeSimilarity * 0.15;
            featureCount++;

            if (features1.averagePitch > 0 && features2.averagePitch > 0) {
                double pitchSimilarity = 1.0 - Math.abs(features1.averagePitch - features2.averagePitch)
                        / Math.max(features1.averagePitch, features2.averagePitch);
                totalSimilarity += pitchSimilarity * 0.25;
                featureCount++;
            }

            double speechRateSimilarity = 1.0 - Math.abs(features1.speechRate - features2.speechRate)
                    / Math.max(features1.speechRate, features2.speechRate);
            totalSimilarity += speechRateSimilarity * 0.10;
            featureCount++;

            if (features1.spectralCentroid > 0 && features2.spectralCentroid > 0) {
                double centroidSimilarity = 1.0 - Math.abs(features1.spectralCentroid - features2.spectralCentroid)
                        / Math.max(features1.spectralCentroid, features2.spectralCentroid);
                totalSimilarity += centroidSimilarity * 0.15;
                featureCount++;
            }

            if (features1.mfccFeatures != null && features2.mfccFeatures != null
                    && features1.mfccFeatures.length == features2.mfccFeatures.length) {
                double mfccSimilarity = calculateMFCCSimilarity(features1.mfccFeatures, features2.mfccFeatures);
                totalSimilarity += mfccSimilarity * 0.25;
                featureCount++;
            }

            if (features1.voiceFingerprint != null && features2.voiceFingerprint != null) {
                double fingerprintSimilarity = calculateFingerprintSimilarity(
                        features1.voiceFingerprint, features2.voiceFingerprint);
                totalSimilarity += fingerprintSimilarity * 0.10;
                featureCount++;
            }

            double finalSimilarity = totalSimilarity / featureCount;
            Log.d(TAG, "درجة التشابه النهائية: " + finalSimilarity);

            return Math.max(0.0, Math.min(1.0, finalSimilarity));

        } catch (Exception e) {
            Log.e(TAG, "خطأ في مقارنة الخصائص: " + e.getMessage(), e);
            return 0.0;
        }
    }


    private static short[] readWavFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        byte[] header = new byte[44];
        fis.read(header);

        byte[] audioBytes = new byte[(int) (file.length() - 44)];
        fis.read(audioBytes);
        fis.close();

        short[] audioData = new short[audioBytes.length / 2];
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer().get(audioData);

        return audioData;
    }

    private static double calculateAverageAmplitude(short[] audioData) {
        double sum = 0.0;
        for (short sample : audioData) {
            sum += Math.abs(sample);
        }
        return sum / audioData.length;
    }

    private static double calculateAveragePitch(short[] audioData) {
        try {
            List<Double> pitchValues = new ArrayList<>();

            for (int i = 0; i < audioData.length - FRAME_SIZE; i += HOP_SIZE) {
                short[] frame = Arrays.copyOfRange(audioData, i, i + FRAME_SIZE);
                double pitch = estimatePitch(frame);
                if (pitch > 50 && pitch < 800) {
                    pitchValues.add(pitch);
                }
            }

            if (pitchValues.isEmpty()) {
                return 0.0;
            }

            return pitchValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        } catch (Exception e) {
            Log.e(TAG, "خطأ في حساب التردد: " + e.getMessage());
            return 0.0;
        }
    }

    private static double estimatePitch(short[] frame) {
        double[] autocorr = new double[frame.length];

        for (int lag = 0; lag < frame.length; lag++) {
            double sum = 0.0;
            for (int i = 0; i < frame.length - lag; i++) {
                sum += frame[i] * frame[i + lag];
            }
            autocorr[lag] = sum;
        }

        double maxCorr = 0.0;
        int bestLag = 0;

        for (int lag = 20; lag < autocorr.length / 2; lag++) {
            if (autocorr[lag] > maxCorr) {
                maxCorr = autocorr[lag];
                bestLag = lag;
            }
        }

        return bestLag > 0 ? (double) SAMPLE_RATE / bestLag : 0.0;
    }

    private static double calculateSpeechRate(short[] audioData) {
        int threshold = (int) (calculateAverageAmplitude(audioData) * 0.1);
        int transitions = 0;
        boolean wasSilent = true;

        for (int i = 0; i < audioData.length; i += 160) {
            boolean isSilent = Math.abs(audioData[i]) < threshold;
            if (wasSilent && !isSilent) {
                transitions++;
            }
            wasSilent = isSilent;
        }

        double duration = (double) audioData.length / SAMPLE_RATE;
        return transitions / duration;
    }

    private static double calculateSpectralCentroid(short[] audioData) {
        double[] magnitude = calculateMagnitudeSpectrum(audioData);

        double weightedSum = 0.0;
        double magnitudeSum = 0.0;

        for (int i = 0; i < magnitude.length; i++) {
            double frequency = (double) i * SAMPLE_RATE / (2 * magnitude.length);
            weightedSum += frequency * magnitude[i];
            magnitudeSum += magnitude[i];
        }

        return magnitudeSum > 0 ? weightedSum / magnitudeSum : 0.0;
    }

    private static double calculateSpectralRolloff(short[] audioData) {
        double[] magnitude = calculateMagnitudeSpectrum(audioData);

        double totalEnergy = 0.0;
        for (double mag : magnitude) {
            totalEnergy += mag * mag;
        }

        double threshold = 0.85 * totalEnergy;
        double cumulativeEnergy = 0.0;

        for (int i = 0; i < magnitude.length; i++) {
            cumulativeEnergy += magnitude[i] * magnitude[i];
            if (cumulativeEnergy >= threshold) {
                return (double) i * SAMPLE_RATE / (2 * magnitude.length);
            }
        }

        return 0.0;
    }

    private static double[] calculateMagnitudeSpectrum(short[] audioData) {
        double[] windowed = new double[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            double w = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (audioData.length - 1));
            windowed[i] = audioData[i] * w;
        }

        int n = windowed.length / 2;
        double[] magnitude = new double[n];

        for (int k = 0; k < n; k++) {
            double real = 0.0, imag = 0.0;
            for (int t = 0; t < windowed.length; t++) {
                double angle = -2 * Math.PI * k * t / windowed.length;
                real += windowed[t] * Math.cos(angle);
                imag += windowed[t] * Math.sin(angle);
            }
            magnitude[k] = Math.sqrt(real * real + imag * imag);
        }

        return magnitude;
    }

    private static double[] calculateMFCC(short[] audioData) {
        double[] mfcc = new double[12];
        double[] magnitude = calculateMagnitudeSpectrum(audioData);

        for (int i = 0; i < 12; i++) {
            double sum = 0.0;
            for (int j = 0; j < magnitude.length; j++) {
                double melFreq = 2595 * Math.log10(1 + j * SAMPLE_RATE / (2.0 * magnitude.length * 700));
                double weight = Math.exp(-0.5 * Math.pow((melFreq - i * 300) / 100, 2));
                sum += magnitude[j] * weight;
            }
            mfcc[i] = Math.log(sum + 1e-10);
        }

        return mfcc;
    }

    private static String generateVoiceFingerprint(VoiceFeatures features) {
        StringBuilder fingerprint = new StringBuilder();

        fingerprint.append(String.format("%.0f", features.averagePitch / 10));
        fingerprint.append(String.format("%.0f", features.averageAmplitude / 100));
        fingerprint.append(String.format("%.1f", features.speechRate));
        fingerprint.append(String.format("%.0f", features.spectralCentroid / 100));

        if (features.mfccFeatures != null) {
            for (int i = 0; i < Math.min(4, features.mfccFeatures.length); i++) {
                fingerprint.append(String.format("%.1f", features.mfccFeatures[i]));
            }
        }

        return fingerprint.toString();
    }

    private static double calculateMFCCSimilarity(double[] mfcc1, double[] mfcc2) {
        double distance = 0.0;
        for (int i = 0; i < mfcc1.length; i++) {
            distance += Math.pow(mfcc1[i] - mfcc2[i], 2);
        }
        distance = Math.sqrt(distance);

        return Math.exp(-distance / 10.0);
    }

    private static double calculateFingerprintSimilarity(String fp1, String fp2) {
        if (fp1.length() != fp2.length()) {
            return 0.0;
        }

        int matches = 0;
        for (int i = 0; i < fp1.length(); i++) {
            if (fp1.charAt(i) == fp2.charAt(i)) {
                matches++;
            }
        }

        return (double) matches / fp1.length();
    }

    public static class VoiceFeatures {
        public double duration;
        public double averageAmplitude;
        public double averagePitch;
        public double speechRate;
        public double spectralCentroid;
        public double spectralRolloff;
        public double[] mfccFeatures;
        public String voiceFingerprint;

        public String toFeatureString() {
            StringBuilder sb = new StringBuilder();
            sb.append(duration).append(",");
            sb.append(averageAmplitude).append(",");
            sb.append(averagePitch).append(",");
            sb.append(speechRate).append(",");
            sb.append(spectralCentroid).append(",");
            sb.append(spectralRolloff).append(",");

            if (mfccFeatures != null) {
                for (double mfcc : mfccFeatures) {
                    sb.append(mfcc).append(",");
                }
            }

            sb.append(voiceFingerprint != null ? voiceFingerprint : "");

            return sb.toString();
        }

        public static VoiceFeatures fromFeatureString(String featureString) {
            try {
                String[] parts = featureString.split(",");
                VoiceFeatures features = new VoiceFeatures();

                features.duration = Double.parseDouble(parts[0]);
                features.averageAmplitude = Double.parseDouble(parts[1]);
                features.averagePitch = Double.parseDouble(parts[2]);
                features.speechRate = Double.parseDouble(parts[3]);
                features.spectralCentroid = Double.parseDouble(parts[4]);
                features.spectralRolloff = Double.parseDouble(parts[5]);

                List<Double> mfccList = new ArrayList<>();
                for (int i = 6; i < parts.length - 1; i++) {
                    if (!parts[i].isEmpty()) {
                        mfccList.add(Double.parseDouble(parts[i]));
                    }
                }

                if (!mfccList.isEmpty()) {
                    features.mfccFeatures = mfccList.stream().mapToDouble(Double::doubleValue).toArray();
                }

                if (parts.length > 0) {
                    features.voiceFingerprint = parts[parts.length - 1];
                }

                return features;

            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحليل البيانات: " + e.getMessage());
                return null;
            }
        }

        @Override
        public String toString() {
            return String.format("VoiceFeatures{duration=%.2f, amplitude=%.1f, pitch=%.1f, rate=%.1f}",
                    duration, averageAmplitude, averagePitch, speechRate);
        }
    }
}