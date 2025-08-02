package com.bank.bayan;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class VoiceHelper {

    private static final String TAG = "VoiceHelper";
    private Context context;
    private TextToSpeech textToSpeech;
    private Queue<String> speechQueue;
    private boolean isSpeaking = false;
    private boolean isInitialized = false;
    private Handler mainHandler;

    private float speechRate = 0.9f;
    private float pitch = 1.0f;

    public interface SpeechCallback {
        void onSpeechStarted();
        void onSpeechCompleted();
        void onSpeechError();
    }

    private SpeechCallback speechCallback;

//    public VoiceHelper(Context context, TextToSpeech textToSpeech) {
//        this.context = context;
//        this.textToSpeech = textToSpeech;
//        this.speechQueue = new LinkedList<>();
//        this.mainHandler = new Handler(Looper.getMainLooper());
//
//        setupTextToSpeech();
//    }

    public VoiceHelper(Context context, TextToSpeech textToSpeech) {
        this.context = context;
        this.textToSpeech = textToSpeech;
        setupUtteranceListener();
    }

    private void setupTextToSpeech() {
        if (textToSpeech != null) {
            int result = textToSpeech.setLanguage(new Locale("ar"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Arabic language not supported");
            } else {
                isInitialized = true;
                textToSpeech.setSpeechRate(speechRate);
                textToSpeech.setPitch(pitch);

                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        isSpeaking = true;
                        if (speechCallback != null) {
                            mainHandler.post(() -> speechCallback.onSpeechStarted());
                        }
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        isSpeaking = false;
                        if (speechCallback != null) {
                            mainHandler.post(() -> speechCallback.onSpeechCompleted());
                        }

                        mainHandler.post(() -> processQueue());
                    }

                    @Override
                    public void onError(String utteranceId) {
                        isSpeaking = false;
                        if (speechCallback != null) {
                            mainHandler.post(() -> speechCallback.onSpeechError());
                        }

                        mainHandler.post(() -> processQueue());
                    }
                });
            }
        }
    }

    public void queue(String text) {
        if (!isInitialized || text == null || text.trim().isEmpty()) {
            return;
        }

        speechQueue.offer(text);

        if (!isSpeaking) {
            processQueue();
        }
    }

    private void speakImmediately(String text) {
        if (textToSpeech != null && isInitialized) {
            textToSpeech.stop();

            text = prepareTextForSpeech(text);

            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "immediate_" + System.currentTimeMillis());

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    private void processQueue() {
        if (!speechQueue.isEmpty() && !isSpeaking && isInitialized) {
            String text = speechQueue.poll();
            if (text != null) {
                speakQueued(text);
            }
        }
    }

    private void speakQueued(String text) {
        if (textToSpeech != null && isInitialized) {
            text = prepareTextForSpeech(text);

            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "queued_" + System.currentTimeMillis());

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    private String prepareTextForSpeech(String text) {
        if (text == null) return "";

        text = replaceNumbersWithWords(text);

        text = addNaturalPauses(text);

        text = fixPronunciation(text);

        return text;
    }

    private String replaceNumbersWithWords(String text) {
        text = text.replace("1", "واحد");
        text = text.replace("2", "اثنين");
        text = text.replace("3", "ثلاثة");
        text = text.replace("4", "أربعة");
        text = text.replace("5", "خمسة");
        text = text.replace("6", "ستة");
        text = text.replace("7", "سبعة");
        text = text.replace("8", "ثمانية");
        text = text.replace("9", "تسعة");
        text = text.replace("10", "عشرة");

        return text;
    }

    private String addNaturalPauses(String text) {
        text = text.replace(".", ". ");
        text = text.replace("،", "، ");
        text = text.replace(":", ": ");

        text = text.replace("رصيدك", "رصيدك");
        text = text.replace("المبلغ", "المبلغ");

        return text;
    }

    private String fixPronunciation(String text) {
        text = text.replace("ريال سعودي", "ريال سعودي");
        text = text.replace("SAR", "ريال سعودي");
        text = text.replace("SR", "ريال سعودي");

        text = text.replace("ATM", "الصراف الآلي");
        text = text.replace("SMS", "رسالة نصية");
        text = text.replace("PIN", "الرقم السري");

        return text;
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            speechQueue.clear();
            isSpeaking = false;
        }
    }

    public void setSpeechRate(float rate) {
        this.speechRate = rate;
        if (textToSpeech != null && isInitialized) {
            textToSpeech.setSpeechRate(rate);
        }
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (textToSpeech != null && isInitialized) {
            textToSpeech.setPitch(pitch);
        }
    }

    public void setSpeechCallback(SpeechCallback callback) {
        this.speechCallback = callback;
    }

    public void speakBalance(double balance) {
        String balanceText = formatCurrencyForSpeech(balance);
        speak("رصيدك الحالي هو " + balanceText);
    }

    public void speakAmount(double amount, String context) {
        String amountText = formatCurrencyForSpeech(amount);
        if (context != null && !context.isEmpty()) {
            speak(context + " " + amountText);
        } else {
            speak("المبلغ " + amountText);
        }
    }

    public void speakTransaction(String description, double amount, String type) {
        String amountText = formatCurrencyForSpeech(Math.abs(amount));
        String transactionType = "income".equals(type) ? "إيراد" : "مصروف";

        speak(transactionType + " " + description + " بمبلغ " + amountText);
    }

    public void speakError(String errorMessage) {
        speak("خطأ: " + errorMessage);
    }

    public void speakSuccess(String successMessage) {
        speak("تم بنجاح: " + successMessage);
    }

    public void speakWelcome(String userName, double balance) {
        String welcomeText = "مرحباً بك";
        if (userName != null && !userName.isEmpty()) {
            welcomeText += " " + userName;
        }
        welcomeText += " في تطبيق البنك الرقمي. رصيدك الحالي " + formatCurrencyForSpeech(balance);

        speak(welcomeText);
    }

    public void speakMenu() {
        String menuText = "اختر من الخيارات التالية: " +
                "واحد للتحويل، " +
                "اثنين للمدفوعات، " +
                "ثلاثة للخدمات، " +
                "أربعة لعرض الرصيد، " +
                "خمسة لإضافة مفوض جديد";

        speak(menuText);
    }

    public void speakConfirmation(String action) {
        speak("هل تريد تأكيد " + action + "؟ قل نعم للتأكيد أو لا للإلغاء");
    }

    public void speakInstructions(String instructions) {
        speak("التعليمات: " + instructions);
    }

    private String formatCurrencyForSpeech(double amount) {
        if (amount == 0) {
            return "صفر ريال";
        }

        boolean isNegative = amount < 0;
        amount = Math.abs(amount);

        String formattedAmount;
        if (amount == (long) amount) {
            formattedAmount = String.valueOf((long) amount);
        } else {
            formattedAmount = String.format("%.2f", amount);
            formattedAmount = formattedAmount.replace(".", " فاصلة ");
        }

        formattedAmount = convertNumberToArabicWords(formattedAmount);

        String result = formattedAmount + " ريال سعودي";

        if (isNegative) {
            result = "ناقص " + result;
        }

        return result;
    }

    private String convertNumberToArabicWords(String number) {

        if (number.equals("0")) return "صفر";
        if (number.equals("1")) return "واحد";
        if (number.equals("2")) return "اثنان";
        if (number.equals("3")) return "ثلاثة";
        if (number.equals("4")) return "أربعة";
        if (number.equals("5")) return "خمسة";
        if (number.equals("10")) return "عشرة";
        if (number.equals("100")) return "مائة";
        if (number.equals("1000")) return "ألف";

        return number;
    }

    public void announceListNavigation(int currentItem, int totalItems, String itemDescription) {
        speak("العنصر " + (currentItem + 1) + " من " + totalItems + ". " + itemDescription);
    }

    public void announcePageChange(String pageName) {
        speak("انتقلت إلى صفحة " + pageName);
    }

    public void announceLoading() {
        speak("جاري التحميل، يرجى الانتظار");
    }

    public void announceLoadingComplete() {
        speak("تم التحميل بنجاح");
    }

    public void announceNetworkError() {
        speak("خطأ في الاتصال بالشبكة. تحقق من اتصال الإنترنت وحاول مرة أخرى");
    }

    public void announcePermissionRequired(String permission) {
        speak("يتطلب إذن " + permission + " لاستخدام هذه الميزة");
    }

    public void cleanup() {
        stop();
        speechQueue.clear();
        speechCallback = null;

        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(null);
        }
    }

    public void speakWithDelay(String text, long delayMillis) {
        mainHandler.postDelayed(() -> speak(text), delayMillis);
    }

    public void queueWithDelay(String text, long delayMillis) {
        mainHandler.postDelayed(() -> queue(text), delayMillis);
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public int getQueueSize() {
        return speechQueue.size();
    }

    public void clearQueue() {
        speechQueue.clear();
    }

    private VoiceHelperCallback callback;

    public interface VoiceHelperCallback {
        void onSpeechFinished(boolean shouldListen);
        void onSpeechStarted();
        void onSpeechError();
    }



    public void setCallback(VoiceHelperCallback callback) {
        this.callback = callback;
    }

    private void setupUtteranceListener() {
        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.d(TAG, "Speech started: " + utteranceId);
                    if (callback != null) {
                        callback.onSpeechStarted();
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.d(TAG, "Speech finished: " + utteranceId);

                    boolean shouldListen = utteranceId != null && utteranceId.contains("LISTEN");

                    if (callback != null) {
                        callback.onSpeechFinished(shouldListen);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "Speech error: " + utteranceId);
                    if (callback != null) {
                        callback.onSpeechError();
                    }
                }
            });
        }
    }

    /**
     * نطق النص بدون الاستماع بعد الانتهاء
     */
    public void speak(String text) {
        speak(text, false);
    }

    /**
     * نطق النص مع إمكانية الاستماع بعد الانتهاء
     * @param text النص المراد نطقه
     * @param listenAfter هل يجب الاستماع بعد انتهاء النطق
     */
    public void speak(String text, boolean listenAfter) {
        if (textToSpeech != null) {
            Log.d(TAG, "Speaking: " + text + " (Listen after: " + listenAfter + ")");

            Bundle params = new Bundle();
            String utteranceId = "TTS_" + System.currentTimeMillis();

            if (listenAfter) {
                utteranceId += "_LISTEN";
            }

            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);

            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error in text-to-speech");
                if (callback != null) {
                    callback.onSpeechError();
                }
            }
        } else {
            Log.e(TAG, "TextToSpeech is null");
            if (callback != null) {
                callback.onSpeechError();
            }
        }
    }

    /**
     * إيقاف النطق الحالي
     */
    public void stopSpeaking() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    /**
     * التحقق من حالة النطق
     */
    public boolean isSpeaking() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }

    /**
     * تدمير المساعد
     */
    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
