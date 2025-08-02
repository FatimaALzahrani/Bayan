package com.bank.bayan.audio;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class VoicePassphraseVerifier {

    private static final String TAG = "VoicePassphraseVerifier";

    // كلمات المرور الصوتية المقبولة
    private static final String[] ACCEPTED_PASSPHRASES = {
            "صوتي هو كلمة مروري",
            "أنا صاحب هذا الحساب",
            "تحقق من هويتي الصوتية",
            "هذا صوتي الحقيقي",
            "أريد الدخول إلى حسابي"
    };

    private static final double TEXT_SIMILARITY_THRESHOLD = 0.7;

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private VerificationCallback callback;
    private String expectedPassphrase;

    public interface VerificationCallback {
        void onVerificationSuccess(String recognizedText, double confidence);
        void onVerificationFailed(String reason);
        void onError(String error);
    }

    public static class VerificationResult {
        public boolean isValid;
        public String recognizedText;
        public String expectedText;
        public double textSimilarity;
        public double overallConfidence;
        public String failureReason;

        public VerificationResult(boolean isValid, String recognizedText, String expectedText) {
            this.isValid = isValid;
            this.recognizedText = recognizedText;
            this.expectedText = expectedText;
        }
    }

    public VoicePassphraseVerifier(Context context) {
        this.context = context;
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "جاهز للاستماع لكلمة المرور");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "بدء نطق كلمة المرور");
            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "انتهاء نطق كلمة المرور");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                Log.e(TAG, "خطأ في التعرف على كلمة المرور: " + errorMessage);
                if (callback != null) {
                    callback.onError("خطأ في التعرف على الكلام: " + errorMessage);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                float[] confidenceScores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    float confidence = (confidenceScores != null && confidenceScores.length > 0)
                            ? confidenceScores[0] : 0.5f;

                    Log.d(TAG, "تم التعرف على النص: " + recognizedText + " بثقة: " + confidence);

                    VerificationResult result = verifyPassphrase(recognizedText, expectedPassphrase);

                    if (result.isValid && callback != null) {
                        callback.onVerificationSuccess(recognizedText, result.overallConfidence);
                    } else if (callback != null) {
                        callback.onVerificationFailed(result.failureReason);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    public void startVerification(String passphrase, VerificationCallback callback) {
        this.expectedPassphrase = passphrase;
        this.callback = callback;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.startListening(intent);
    }

    public void stopVerification() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public static VerificationResult verifyPassphrase(String recognizedText, String expectedText) {
        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            return new VerificationResult(false, recognizedText, expectedText) {{
                failureReason = "لم يتم التعرف على أي نص";
                textSimilarity = 0.0;
                overallConfidence = 0.0;
            }};
        }

        String cleanRecognized = cleanText(recognizedText);
        String cleanExpected = cleanText(expectedText);

        Log.d(TAG, "النص المتوقع: " + cleanExpected);
        Log.d(TAG, "النص المُعرَّف عليه: " + cleanRecognized);

        double similarity = calculateTextSimilarity(cleanRecognized, cleanExpected);

        Log.d(TAG, "درجة التشابه: " + similarity);

        boolean isValid = similarity >= TEXT_SIMILARITY_THRESHOLD;

        return new VerificationResult(isValid, recognizedText, expectedText) {{
            textSimilarity = similarity;
            overallConfidence = similarity;
            failureReason = isValid ? null :
                    "كلمة المرور غير صحيحة. درجة التشابه: " + String.format("%.2f", similarity * 100) + "%";
        }};
    }

    private static String cleanText(String text) {
        if (text == null) return "";

        return text.trim()
                .toLowerCase()
                .replaceAll("[\\p{Punct}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static double calculateTextSimilarity(String text1, String text2) {
        if (text1.equals(text2)) {
            return 1.0;
        }

        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");

        double wordSimilarity = calculateWordSimilarity(words1, words2);

        double charSimilarity = calculateLevenshteinSimilarity(text1, text2);

        return (wordSimilarity * 0.7) + (charSimilarity * 0.3);
    }

    private static double calculateWordSimilarity(String[] words1, String[] words2) {
        if (words1.length == 0 && words2.length == 0) {
            return 1.0;
        }

        if (words1.length == 0 || words2.length == 0) {
            return 0.0;
        }

        int matchingWords = 0;
        List<String> words2List = new ArrayList<>(Arrays.asList(words2));

        for (String word1 : words1) {
            for (int i = 0; i < words2List.size(); i++) {
                String word2 = words2List.get(i);
                if (word1.equals(word2) || calculateLevenshteinSimilarity(word1, word2) > 0.8) {
                    matchingWords++;
                    words2List.remove(i);
                    break;
                }
            }
        }

        double avgLength = (words1.length + words2.length) / 2.0;
        return matchingWords / avgLength;
    }

    private static double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = calculateLevenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / maxLength);
    }

    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    public static String getRandomPassphrase() {
        int randomIndex = (int) (Math.random() * ACCEPTED_PASSPHRASES.length);
        return ACCEPTED_PASSPHRASES[randomIndex];
    }

    public static boolean isAcceptedPassphrase(String passphrase) {
        String cleanPassphrase = cleanText(passphrase);

        for (String accepted : ACCEPTED_PASSPHRASES) {
            String cleanAccepted = cleanText(accepted);
            if (calculateTextSimilarity(cleanPassphrase, cleanAccepted) >= TEXT_SIMILARITY_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "مشكلة في الصوت";
            case SpeechRecognizer.ERROR_CLIENT:
                return "خطأ في العميل";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "أذونات غير كافية";
            case SpeechRecognizer.ERROR_NETWORK:
                return "مشكلة في الشبكة";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "انتهت مهلة الشبكة";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "لم يتم العثور على تطابق";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "المتعرف مشغول";
            case SpeechRecognizer.ERROR_SERVER:
                return "خطأ في الخادم";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "انتهت مهلة الكلام";
            default:
                return "خطأ غير معروف: " + errorCode;
        }
    }

    public void cleanup() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}

