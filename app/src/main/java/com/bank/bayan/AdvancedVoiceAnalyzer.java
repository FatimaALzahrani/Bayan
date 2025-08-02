package com.bank.bayan;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedVoiceAnalyzer {

    private static final String TAG = "VoiceAnalyzer";
    private static final int NUM_MFCC = 13;
    private static final int NUM_FILTERS = 26;
    private static final double PRE_EMPHASIS = 0.97;
    private static final int FRAME_SIZE = 1024;
    private static final int FRAME_SHIFT = 512;

    public float[] preprocessAudio(float[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return new float[0];
        }

        float[] preEmphasized = applyPreEmphasis(audioData);

        float[] windowed = applyHammingWindow(preEmphasized);

        float[] normalized = normalizeAmplitude(windowed);

        return removeSilence(normalized);
    }

    /**
     * Extract MFCC features from audio frame
     */
    public float[] extractMFCC(float[] audioFrame, int sampleRate) {
        if (audioFrame == null || audioFrame.length < 64) {
            return new float[0];
        }

        try {
            // 1. Apply FFT
            Complex[] fftResult = performFFT(audioFrame);

            // 2. Calculate power spectrum
            double[] powerSpectrum = calculatePowerSpectrum(fftResult);

            // 3. Apply mel filter bank
            double[] melFiltered = applyMelFilterBank(powerSpectrum, sampleRate);

            // 4. Apply logarithm
            double[] logMel = applyLogarithm(melFiltered);

            // 5. Apply DCT to get MFCC
            float[] mfcc = applyDCT(logMel);

            // 6. Add delta and delta-delta features
            return addDynamicFeatures(mfcc);

        } catch (Exception e) {
            Log.e(TAG, "خطأ في استخراج MFCC", e);
            return new float[0];
        }
    }

    /**
     * Create voice template from multiple recordings
     */
    public VoiceTemplate createVoiceTemplate(List<float[]> recordedFrames) {
        if (recordedFrames == null || recordedFrames.isEmpty()) {
            return null;
        }

        Log.d(TAG, "إنشاء قالب صوتي من " + recordedFrames.size() + " إطار");

        try {
            // Calculate statistical features
            float[] meanFeatures = calculateMeanFeatures(recordedFrames);
            float[] stdFeatures = calculateStdFeatures(recordedFrames, meanFeatures);
            float[] minFeatures = calculateMinFeatures(recordedFrames);
            float[] maxFeatures = calculateMaxFeatures(recordedFrames);

            // Calculate additional discriminative features
            float fundamentalFreq = calculateFundamentalFrequency(recordedFrames);
            float spectralCentroid = calculateSpectralCentroid(recordedFrames);
            float spectralRolloff = calculateSpectralRolloff(recordedFrames);
            float zeroCrossingRate = calculateZeroCrossingRate(recordedFrames);
            float mfccVariance = calculateMfccVariance(recordedFrames);

            // Create voice template
            VoiceTemplate template = new VoiceTemplate();
            template.setMeanFeatures(meanFeatures);
            template.setStdFeatures(stdFeatures);
            template.setMinFeatures(minFeatures);
            template.setMaxFeatures(maxFeatures);
            template.setFundamentalFrequency(fundamentalFreq);
            template.setSpectralCentroid(spectralCentroid);
            template.setSpectralRolloff(spectralRolloff);
            template.setZeroCrossingRate(zeroCrossingRate);
            template.setMfccVariance(mfccVariance);
            template.setFrameCount(recordedFrames.size());
            template.setQualityScore(calculateQualityScore(recordedFrames));

            Log.d(TAG, "تم إنشاء قالب صوتي بجودة: " + template.getQualityScore());
            return template;

        } catch (Exception e) {
            Log.e(TAG, "خطأ في إنشاء القالب الصوتي", e);
            return null;
        }
    }

    /**
     * Compare two voice templates using advanced similarity metrics
     */
    public double compareVoiceTemplates(VoiceTemplate template1, VoiceTemplate template2) {
        if (template1 == null || template2 == null || !template1.isValid() || !template2.isValid()) {
            return 0.0;
        }

        try {
            // 1. MFCC Cosine Similarity (40% weight)
            double mfccSimilarity = calculateCosineSimilarity(
                    template1.getMeanFeatures(), template2.getMeanFeatures()) * 0.4;

            // 2. Statistical Features Similarity (25% weight)
            double statSimilarity = calculateStatisticalSimilarity(template1, template2) * 0.25;

            // 3. Fundamental Frequency Similarity (15% weight)
            double freqSimilarity = calculateFrequencySimilarity(
                    template1.getFundamentalFrequency(), template2.getFundamentalFrequency()) * 0.15;

            // 4. Spectral Features Similarity (10% weight)
            double spectralSimilarity = calculateSpectralSimilarity(template1, template2) * 0.10;

            // 5. Quality-weighted similarity (10% weight)
            double qualityWeight = Math.min(template1.getQualityScore(), template2.getQualityScore()) * 0.10;

            double totalSimilarity = mfccSimilarity + statSimilarity + freqSimilarity +
                    spectralSimilarity + qualityWeight;

            Log.d(TAG, String.format("تفاصيل المقارنة - MFCC: %.3f, Stat: %.3f, Freq: %.3f, Spectral: %.3f, Quality: %.3f, Total: %.3f",
                    mfccSimilarity, statSimilarity, freqSimilarity, spectralSimilarity, qualityWeight, totalSimilarity));

            return Math.max(0.0, Math.min(1.0, totalSimilarity));

        } catch (Exception e) {
            Log.e(TAG, "خطأ في مقارنة القوالب الصوتية", e);
            return 0.0;
        }
    }

    // === PRIVATE HELPER METHODS ===

    private float[] applyPreEmphasis(float[] signal) {
        float[] result = new float[signal.length];
        result[0] = signal[0];

        for (int i = 1; i < signal.length; i++) {
            result[i] = signal[i] - (float) (PRE_EMPHASIS * signal[i - 1]);
        }

        return result;
    }

    private float[] applyHammingWindow(float[] signal) {
        float[] result = new float[signal.length];
        int N = signal.length;

        for (int i = 0; i < N; i++) {
            double window = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (N - 1));
            result[i] = (float) (signal[i] * window);
        }

        return result;
    }

    private float[] normalizeAmplitude(float[] signal) {
        float max = 0;
        for (float value : signal) {
            max = Math.max(max, Math.abs(value));
        }

        if (max == 0) return signal;

        float[] result = new float[signal.length];
        for (int i = 0; i < signal.length; i++) {
            result[i] = signal[i] / max;
        }

        return result;
    }

    private float[] removeSilence(float[] signal) {
        // Simple voice activity detection
        double threshold = 0.02; // Adjust based on testing
        List<Float> activeFrames = new ArrayList<>();

        for (float value : signal) {
            if (Math.abs(value) > threshold) {
                activeFrames.add(value);
            }
        }

        if (activeFrames.isEmpty()) {
            return signal; // Return original if no activity detected
        }

        float[] result = new float[activeFrames.size()];
        for (int i = 0; i < activeFrames.size(); i++) {
            result[i] = activeFrames.get(i);
        }

        return result;
    }

    private Complex[] performFFT(float[] signal) {
        int n = signal.length;
        Complex[] x = new Complex[n];

        // Convert to complex numbers
        for (int i = 0; i < n; i++) {
            x[i] = new Complex(signal[i], 0);
        }

        return fft(x);
    }

    private Complex[] fft(Complex[] x) {
        int n = x.length;

        // Base case
        if (n == 1) return new Complex[]{x[0]};

        // Divide
        Complex[] even = new Complex[n/2];
        Complex[] odd = new Complex[n/2];

        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
            odd[k] = x[2*k + 1];
        }

        // Conquer
        Complex[] evenResult = fft(even);
        Complex[] oddResult = fft(odd);

        // Combine
        Complex[] result = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            result[k] = evenResult[k].plus(wk.times(oddResult[k]));
            result[k + n/2] = evenResult[k].minus(wk.times(oddResult[k]));
        }

        return result;
    }

    private double[] calculatePowerSpectrum(Complex[] fftResult) {
        double[] power = new double[fftResult.length / 2];

        for (int i = 0; i < power.length; i++) {
            power[i] = fftResult[i].abs() * fftResult[i].abs();
        }

        return power;
    }

    private double[] applyMelFilterBank(double[] powerSpectrum, int sampleRate) {
        double[] melFiltered = new double[NUM_FILTERS];

        // Mel scale conversion
        double melLow = 0;
        double melHigh = 2595 * Math.log10(1 + (sampleRate / 2.0) / 700);

        double[] melPoints = new double[NUM_FILTERS + 2];
        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = melLow + (melHigh - melLow) * i / (NUM_FILTERS + 1);
        }

        // Convert back to Hz
        double[] hzPoints = new double[melPoints.length];
        for (int i = 0; i < melPoints.length; i++) {
            hzPoints[i] = 700 * (Math.pow(10, melPoints[i] / 2595) - 1);
        }

        // Convert to FFT bin numbers
        int[] binPoints = new int[hzPoints.length];
        for (int i = 0; i < hzPoints.length; i++) {
            binPoints[i] = (int) Math.floor((powerSpectrum.length + 1) * hzPoints[i] / (sampleRate / 2.0));
        }

        // Apply triangular filters
        for (int m = 1; m <= NUM_FILTERS; m++) {
            double sum = 0;
            for (int k = binPoints[m - 1]; k < binPoints[m]; k++) {
                if (k < powerSpectrum.length) {
                    sum += powerSpectrum[k] * (k - binPoints[m - 1]) / (binPoints[m] - binPoints[m - 1]);
                }
            }
            for (int k = binPoints[m]; k < binPoints[m + 1]; k++) {
                if (k < powerSpectrum.length) {
                    sum += powerSpectrum[k] * (binPoints[m + 1] - k) / (binPoints[m + 1] - binPoints[m]);
                }
            }
            melFiltered[m - 1] = sum;
        }

        return melFiltered;
    }

    private double[] applyLogarithm(double[] melFiltered) {
        double[] logMel = new double[melFiltered.length];

        for (int i = 0; i < melFiltered.length; i++) {
            logMel[i] = Math.log(Math.max(melFiltered[i], 1e-10)); // Avoid log(0)
        }

        return logMel;
    }

    private float[] applyDCT(double[] logMel) {
        float[] mfcc = new float[NUM_MFCC];

        for (int i = 0; i < NUM_MFCC; i++) {
            double sum = 0;
            for (int j = 0; j < logMel.length; j++) {
                sum += logMel[j] * Math.cos(Math.PI * i * (j + 0.5) / logMel.length);
            }
            mfcc[i] = (float) sum;
        }

        return mfcc;
    }

    private float[] addDynamicFeatures(float[] mfcc) {
        // For simplicity, just return MFCC
        // In full implementation, add delta and delta-delta features
        return mfcc;
    }

    private float[] calculateMeanFeatures(List<float[]> frames) {
        if (frames.isEmpty()) return new float[0];

        int featureSize = frames.get(0).length;
        float[] mean = new float[featureSize];

        for (float[] frame : frames) {
            for (int i = 0; i < Math.min(featureSize, frame.length); i++) {
                mean[i] += frame[i];
            }
        }

        for (int i = 0; i < featureSize; i++) {
            mean[i] /= frames.size();
        }

        return mean;
    }

    private float[] calculateStdFeatures(List<float[]> frames, float[] mean) {
        if (frames.isEmpty() || mean.length == 0) return new float[0];

        int featureSize = mean.length;
        float[] variance = new float[featureSize];

        for (float[] frame : frames) {
            for (int i = 0; i < Math.min(featureSize, frame.length); i++) {
                float diff = frame[i] - mean[i];
                variance[i] += diff * diff;
            }
        }

        float[] std = new float[featureSize];
        for (int i = 0; i < featureSize; i++) {
            std[i] = (float) Math.sqrt(variance[i] / frames.size());
        }

        return std;
    }

    private float[] calculateMinFeatures(List<float[]> frames) {
        if (frames.isEmpty()) return new float[0];

        int featureSize = frames.get(0).length;
        float[] min = new float[featureSize];
        Arrays.fill(min, Float.MAX_VALUE);

        for (float[] frame : frames) {
            for (int i = 0; i < Math.min(featureSize, frame.length); i++) {
                min[i] = Math.min(min[i], frame[i]);
            }
        }

        return min;
    }

    private float[] calculateMaxFeatures(List<float[]> frames) {
        if (frames.isEmpty()) return new float[0];

        int featureSize = frames.get(0).length;
        float[] max = new float[featureSize];
        Arrays.fill(max, Float.MIN_VALUE);

        for (float[] frame : frames) {
            for (int i = 0; i < Math.min(featureSize, frame.length); i++) {
                max[i] = Math.max(max[i], frame[i]);
            }
        }

        return max;
    }

    private float calculateFundamentalFrequency(List<float[]> frames) {
        // Simplified F0 estimation
        // In full implementation, use autocorrelation or harmonic analysis
        return 150.0f; // Average human fundamental frequency
    }

    private float calculateSpectralCentroid(List<float[]> frames) {
        // Simplified spectral centroid calculation
        float sum = 0;
        for (float[] frame : frames) {
            for (int i = 0; i < frame.length; i++) {
                sum += frame[i] * i;
            }
        }
        return sum / (frames.size() * frames.get(0).length);
    }

    private float calculateSpectralRolloff(List<float[]> frames) {
        // Simplified spectral rolloff calculation
        return 0.85f; // 85% energy rolloff point
    }

    private float calculateZeroCrossingRate(List<float[]> frames) {
        int totalCrossings = 0;
        int totalSamples = 0;

        for (float[] frame : frames) {
            for (int i = 1; i < frame.length; i++) {
                if ((frame[i] > 0 && frame[i-1] <= 0) || (frame[i] <= 0 && frame[i-1] > 0)) {
                    totalCrossings++;
                }
            }
            totalSamples += frame.length;
        }

        return totalSamples > 0 ? (float) totalCrossings / totalSamples : 0;
    }

    private float calculateMfccVariance(List<float[]> frames) {
        if (frames.isEmpty()) return 0;

        float[] mean = calculateMeanFeatures(frames);
        float totalVariance = 0;

        for (float[] frame : frames) {
            for (int i = 0; i < Math.min(mean.length, frame.length); i++) {
                float diff = frame[i] - mean[i];
                totalVariance += diff * diff;
            }
        }

        return totalVariance / (frames.size() * mean.length);
    }

    private float calculateQualityScore(List<float[]> frames) {
        if (frames.isEmpty()) return 0;

        // Quality based on consistency and signal strength
        float consistency = 1.0f - calculateMfccVariance(frames);
        float signalStrength = calculateSignalStrength(frames);

        return Math.max(0, Math.min(1, (consistency + signalStrength) / 2));
    }

    private float calculateSignalStrength(List<float[]> frames) {
        float totalEnergy = 0;
        int totalSamples = 0;

        for (float[] frame : frames) {
            for (float value : frame) {
                totalEnergy += value * value;
            }
            totalSamples += frame.length;
        }

        return totalSamples > 0 ? Math.min(1.0f, totalEnergy / totalSamples * 1000) : 0;
    }

    private double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) return 0;

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) return 0;

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private double calculateStatisticalSimilarity(VoiceTemplate t1, VoiceTemplate t2) {
        double stdSim = 1.0 - calculateEuclideanDistance(t1.getStdFeatures(), t2.getStdFeatures());
        return Math.max(0, stdSim);
    }

    private double calculateFrequencySimilarity(float freq1, float freq2) {
        double diff = Math.abs(freq1 - freq2);
        double maxDiff = 100; // Max expected difference in Hz
        return Math.max(0, 1.0 - diff / maxDiff);
    }

    private double calculateSpectralSimilarity(VoiceTemplate t1, VoiceTemplate t2) {
        double centroidDiff = Math.abs(t1.getSpectralCentroid() - t2.getSpectralCentroid());
        double rolloffDiff = Math.abs(t1.getSpectralRolloff() - t2.getSpectralRolloff());

        return Math.max(0, 1.0 - (centroidDiff + rolloffDiff) / 2);
    }

    private double calculateEuclideanDistance(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) return 1.0;

        double sum = 0;
        for (int i = 0; i < vec1.length; i++) {
            double diff = vec1[i] - vec2[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum) / vec1.length;
    }

    // Complex number class for FFT
    private static class Complex {
        private final double re;   // the real part
        private final double im;   // the imaginary part

        public Complex(double real, double imag) {
            re = real;
            im = imag;
        }

        public Complex plus(Complex b) {
            Complex c = this;
            double real = c.re + b.re;
            double imag = c.im + b.im;
            return new Complex(real, imag);
        }

        public Complex minus(Complex b) {
            Complex c = this;
            double real = c.re - b.re;
            double imag = c.im - b.im;
            return new Complex(real, imag);
        }

        public Complex times(Complex b) {
            Complex c = this;
            double real = c.re * b.re - c.im * b.im;
            double imag = c.re * b.im + c.im * b.re;
            return new Complex(real, imag);
        }

        public double abs() {
            return Math.hypot(re, im);
        }
    }
}