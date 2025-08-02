package com.bank.bayan.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorderPCM {

    private static final String TAG = "AudioRecorderPCM";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private String outputFilePath;
    private FileOutputStream fileOutputStream;
    private Context context;
    private VoiceActivityDetector voiceDetector;

    private int bufferSize;

    public interface RecordingListener {
        void onRecordingStarted();
        void onSpeechDetected();
        void onRecordingFinished(long duration);
        void onRecordingError(String error);
    }

    private RecordingListener recordingListener;


    public AudioRecorderPCM(Context context) {
        this.context = context;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            bufferSize = SAMPLE_RATE * 2; // 2 bytes per sample for 16-bit
            Log.w(TAG, "Using fallback buffer size: " + bufferSize);
        }

        bufferSize *= BUFFER_SIZE_FACTOR;

        Log.d(TAG, "Buffer size: " + bufferSize);
    }

    public void setRecordingListener(RecordingListener listener) {
        this.recordingListener = listener;
    }

    public boolean startRecording(String filePath) {
        if (isRecording) {
            Log.w(TAG, "Recording is already in progress");
            return false;
        }

        if (!hasRecordAudioPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted");
            return false;
        }

        this.outputFilePath = filePath;

        try {
            File outputFile = new File(filePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.e(TAG, "Failed to create parent directories");
                    return false;
                }
            }

            try {
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                );
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception creating AudioRecord: " + e.getMessage());
                return false;
            }

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return false;
            }

            fileOutputStream = new FileOutputStream(outputFile);

            writeWavHeader(fileOutputStream);

            audioRecord.startRecording();
            isRecording = true;

            voiceDetector.reset();

            recordingThread = new Thread(this::recordingLoop);
            recordingThread.start();

            if (recordingListener != null) {
                recordingListener.onRecordingStarted();
            }

            Log.d(TAG, "Recording started successfully: " + filePath);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage(), e);
            cleanup();

            if (recordingListener != null) {
                recordingListener.onRecordingError("فشل في بدء التسجيل: " + e.getMessage());
            }

            return false;
        }
    }

    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Recording is not in progress");
            return;
        }

        isRecording = false;
        long startTime = System.currentTimeMillis();

        try {
            if (recordingThread != null) {
                recordingThread.join(1000);
            }

            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }

            if (fileOutputStream != null) {
                updateWavHeader();
                fileOutputStream.close();
                fileOutputStream = null;
            }

            long duration = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Recording stopped successfully");

            if (recordingListener != null) {
                recordingListener.onRecordingFinished(duration);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage(), e);
            if (recordingListener != null) {
                recordingListener.onRecordingError("خطأ في إيقاف التسجيل: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void recordingLoop() {
        byte[] buffer = new byte[bufferSize];
        long startTime = System.currentTimeMillis();

        while (isRecording && audioRecord != null) {
            try {
                int bytesRead = audioRecord.read(buffer, 0, bufferSize);

                if (bytesRead > 0) {
                    if (fileOutputStream != null) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }

                    short[] audioSamples = new short[bytesRead / 2];
                    for (int i = 0, j = 0; i < bytesRead - 1; i += 2, j++) {
                        audioSamples[j] = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                    }

                    voiceDetector.processAudioData(audioSamples, audioSamples.length);

                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading audio data: " + bytesRead);
                    break;
                }

            } catch (IOException e) {
                Log.e(TAG, "Error writing audio data: " + e.getMessage(), e);
                break;
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in recording loop: " + e.getMessage(), e);
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Recording loop ended - Duration: " + duration + "ms");

        if (recordingListener != null) {
            recordingListener.onRecordingFinished(duration);
        }
    }

    private void writeWavHeader(FileOutputStream out) throws IOException {
        byte[] header = new byte[44];

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';

        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        // fmt chunk
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';

        writeInt(header, 16, 16);
        writeShort(header, 20, (short) 1);
        writeShort(header, 22, (short) 1);
        writeInt(header, 24, SAMPLE_RATE);
        writeInt(header, 28, SAMPLE_RATE * 2);
        // Block align (NumChannels * BitsPerSample/8)
        writeShort(header, 32, (short) 2);
        writeShort(header, 34, (short) 16);

        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';

        out.write(header);
    }

    private void updateWavHeader() {
        try {
            File file = new File(outputFilePath);
            long fileSize = file.length();
            long dataSize = fileSize - 44;

            java.io.RandomAccessFile wavFile = new java.io.RandomAccessFile(file, "rw");

            wavFile.seek(4);
            wavFile.write(intToByteArray((int) (fileSize - 8)));

            wavFile.seek(40);
            wavFile.write(intToByteArray((int) dataSize));

            wavFile.close();

        } catch (IOException e) {
            Log.e(TAG, "Error updating WAV header: " + e.getMessage(), e);
        }
    }

    private void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
        data[offset + 2] = (byte) ((value >> 16) & 0xff);
        data[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private void writeShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    private byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private void cleanup() {
        try {
            if (audioRecord != null) {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release();
                }
                audioRecord = null;
            }

            if (fileOutputStream != null) {
                fileOutputStream.close();
                fileOutputStream = null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }

        isRecording = false;
        recordingThread = null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public int getSampleRate() {
        return SAMPLE_RATE;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    private boolean hasRecordAudioPermission() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot check permissions");
            return false;
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean checkPermission() {
        return hasRecordAudioPermission();
    }
}