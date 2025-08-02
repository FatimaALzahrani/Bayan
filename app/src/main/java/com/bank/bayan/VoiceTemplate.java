package com.bank.bayan;

public class VoiceTemplate {
    private float[] meanFeatures;
    private float[] stdFeatures;
    private float[] minFeatures;
    private float[] maxFeatures;
    private float fundamentalFrequency;
    private float spectralCentroid;
    private float spectralRolloff;
    private float zeroCrossingRate;
    private float mfccVariance;
    private int frameCount;
    private float qualityScore;

    public VoiceTemplate() {
    }

    public float[] getMeanFeatures() {
        return meanFeatures;
    }

    public void setMeanFeatures(float[] meanFeatures) {
        this.meanFeatures = meanFeatures;
    }

    public float[] getStdFeatures() {
        return stdFeatures;
    }

    public void setStdFeatures(float[] stdFeatures) {
        this.stdFeatures = stdFeatures;
    }

    public float[] getMinFeatures() {
        return minFeatures;
    }

    public void setMinFeatures(float[] minFeatures) {
        this.minFeatures = minFeatures;
    }

    public float[] getMaxFeatures() {
        return maxFeatures;
    }

    public void setMaxFeatures(float[] maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public float getFundamentalFrequency() {
        return fundamentalFrequency;
    }

    public void setFundamentalFrequency(float fundamentalFrequency) {
        this.fundamentalFrequency = fundamentalFrequency;
    }

    public float getSpectralCentroid() {
        return spectralCentroid;
    }

    public void setSpectralCentroid(float spectralCentroid) {
        this.spectralCentroid = spectralCentroid;
    }

    public float getSpectralRolloff() {
        return spectralRolloff;
    }

    public void setSpectralRolloff(float spectralRolloff) {
        this.spectralRolloff = spectralRolloff;
    }

    public float getZeroCrossingRate() {
        return zeroCrossingRate;
    }

    public void setZeroCrossingRate(float zeroCrossingRate) {
        this.zeroCrossingRate = zeroCrossingRate;
    }

    public float getMfccVariance() {
        return mfccVariance;
    }

    public void setMfccVariance(float mfccVariance) {
        this.mfccVariance = mfccVariance;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public float getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(float qualityScore) {
        this.qualityScore = qualityScore;
    }

    public boolean isValid() {
        return meanFeatures != null && meanFeatures.length > 0 && qualityScore > 0.3f;
    }
}

