package com.bank.bayan.bill;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bank.bayan.R;

import java.util.ArrayList;
import java.util.Locale;

public class BillDetailsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private enum ConversationState {
        ASKING_ACCOUNT_NUMBER,
        CONFIRMING_ACCOUNT_NUMBER,
        ASKING_PROCEED
    }

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private TextView billTypeTextView;
    private TextView companyNameTextView;
    private TextView accountNumberTextView;
    private ImageView billIconImageView;
    private Button nextButton;

    private String billType;
    private String companyName;
    private int iconResource;
    private String enteredAccountNumber = "";
    private boolean isListening = false;
    private ConversationState currentState = ConversationState.ASKING_ACCOUNT_NUMBER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_details);

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        Intent intent = getIntent();
        billType = intent.getStringExtra("bill_type");
        companyName = intent.getStringExtra("company_name");
        iconResource = intent.getIntExtra("icon_resource", R.drawable.ic_transfer);

        initializeViews();
        setupSpeechRecognition();
        setupClickListeners();
        updateUI();
    }

    private void initializeViews() {
        billTypeTextView = findViewById(R.id.billTypeTextView);
        companyNameTextView = findViewById(R.id.companyNameTextView);
        accountNumberTextView = findViewById(R.id.accountNumberTextView);
        billIconImageView = findViewById(R.id.billIconImageView);
        nextButton = findViewById(R.id.nextButton);

        // Initially disable next button
        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
    }

    private void updateUI() {
        billTypeTextView.setText(billType);
        companyNameTextView.setText(companyName);
        billIconImageView.setImageResource(iconResource);

        // Set accessibility descriptions
        billTypeTextView.setContentDescription("نوع الفاتورة: " + billType);
        companyNameTextView.setContentDescription("الشركة: " + companyName);
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            speak("رجوع إلى الصفحة السابقة");
            finish();
        });

        nextButton.setOnClickListener(v -> {
            if (!enteredAccountNumber.isEmpty()) {
                proceedToPayment();
            } else {
                speakAndListen("يرجى إدخال رقم الحساب أولاً");
            }
        });

        // Add click listener to account number area for voice input
        findViewById(R.id.accountNumberContainer).setOnClickListener(v -> {
            currentState = ConversationState.ASKING_ACCOUNT_NUMBER;
            speakAndListen("قل رقم حساب " + billType + " الخاص بك");
        });
    }

    private void setupSpeechRecognition() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
            }

            @Override
            public void onError(int error) {
                isListening = false;
                handleSpeechError(error);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processSpeechResult(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void processSpeechResult(String spokenText) {
        spokenText = spokenText.trim();

        switch (currentState) {
            case ASKING_ACCOUNT_NUMBER:
                handleAccountNumberInput(spokenText);
                break;
            case CONFIRMING_ACCOUNT_NUMBER:
                handleAccountNumberConfirmation(spokenText);
                break;
            case ASKING_PROCEED:
                handleProceedInput(spokenText);
                break;
        }
    }

    private void handleAccountNumberInput(String spokenText) {
        if (spokenText.toLowerCase().contains("رجوع") ||
                spokenText.toLowerCase().contains("إلغاء") ||
                spokenText.toLowerCase().contains("خروج")) {
            speak("تم الإلغاء. رجوع للصفحة السابقة");
            finish();
            return;
        }

        String accountNumber = extractAccountNumberFromSpeech(spokenText);

        if (!accountNumber.isEmpty() && accountNumber.length() >= 8) {
            enteredAccountNumber = accountNumber;

            String formattedNumber = formatAccountNumberForSpeech(accountNumber);
            currentState = ConversationState.CONFIRMING_ACCOUNT_NUMBER;
            speakAndListen("رقم الحساب هو " + formattedNumber + ". هل هذا صحيح؟");

        } else if (!accountNumber.isEmpty()) {
            speakAndListen("رقم الحساب قصير جداً. يجب أن يكون على الأقل 8 أرقام. قل رقم الحساب مرة أخرى");
        } else {
            speakAndListen("لم أتمكن من فهم رقم الحساب. قل الأرقام بوضوح وببطء");
        }
    }

    private void handleAccountNumberConfirmation(String spokenText) {
        String lowerText = spokenText.toLowerCase();

        if (lowerText.contains("نعم") || lowerText.contains("صحيح") ||
                lowerText.contains("إيوه") || lowerText.contains("موافق")) {

            accountNumberTextView.setText(enteredAccountNumber);
            nextButton.setEnabled(true);
            nextButton.setAlpha(1.0f);

            currentState = ConversationState.ASKING_PROCEED;
            speakAndListen("تم حفظ رقم الحساب. هل تريد المتابعة لجلب تفاصيل الفاتورة؟");

        } else if (lowerText.contains("لا") || lowerText.contains("لأ") ||
                lowerText.contains("غير صحيح") || lowerText.contains("خطأ")) {

            enteredAccountNumber = "";
            currentState = ConversationState.ASKING_ACCOUNT_NUMBER;
            speakAndListen("حسناً، قل رقم الحساب الصحيح");

        } else {
            speakAndListen("قل نعم إذا كان رقم الحساب صحيح، أو لا إذا كان خطأ");
        }
    }

    private void handleProceedInput(String spokenText) {
        String lowerText = spokenText.toLowerCase();

        if (lowerText.contains("نعم") || lowerText.contains("متابعة") ||
                lowerText.contains("إيوه") || lowerText.contains("موافق")) {

            proceedToPayment();

        } else if (lowerText.contains("لا") || lowerText.contains("تعديل")) {

            currentState = ConversationState.ASKING_ACCOUNT_NUMBER;
            speakAndListen("هل تريد تعديل رقم الحساب؟");

        } else {
            speakAndListen("قل نعم للمتابعة أو لا للتعديل");
        }
    }

    private String extractAccountNumberFromSpeech(String speech) {
        speech = speech.replaceAll("رقم الحساب|حساب|الرقم", "").trim();

        StringBuilder accountNumber = new StringBuilder();

        for (char c : speech.toCharArray()) {
            if (Character.isDigit(c)) {
                accountNumber.append(c);
            } else if (c == ' ' && accountNumber.length() > 0) {
                continue;
            }
        }

        String result = accountNumber.toString();
        result = convertArabicNumberWords(speech, result);

        return result;
    }

    private String convertArabicNumberWords(String speech, String currentResult) {
        if (!currentResult.isEmpty() && currentResult.length() >= 8) {
            return currentResult;
        }

        StringBuilder result = new StringBuilder();
        String[] words = speech.toLowerCase().split("\\s+");

        for (String word : words) {
            switch (word) {
                case "صفر": result.append("0"); break;
                case "واحد": result.append("1"); break;
                case "اثنان": case "اثنين": case "إثنان": result.append("2"); break;
                case "ثلاثة": result.append("3"); break;
                case "أربعة": result.append("4"); break;
                case "خمسة": result.append("5"); break;
                case "ستة": result.append("6"); break;
                case "سبعة": result.append("7"); break;
                case "ثمانية": result.append("8"); break;
                case "تسعة": result.append("9"); break;
            }
        }

        return result.length() > currentResult.length() ? result.toString() : currentResult;
    }

    private String formatAccountNumberForSpeech(String accountNumber) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < accountNumber.length(); i++) {
            formatted.append(accountNumber.charAt(i));
            if (i < accountNumber.length() - 1 && (i + 1) % 2 == 0) {
                formatted.append(" ");
            }
        }
        return formatted.toString();
    }

    private void proceedToPayment() {
        speak("جاري جلب تفاصيل الفاتورة...");

        Intent intent = new Intent(this, BillPaymentDetailsActivity.class);
        intent.putExtra("bill_type", billType);
        intent.putExtra("company_name", companyName);
        intent.putExtra("account_number", enteredAccountNumber);
        intent.putExtra("icon_resource", iconResource);

        findViewById(R.id.mainLayout).postDelayed(() -> {
            startActivity(intent);
        }, 2000);
    }

    private void speakAndListen(String text) {
        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> startListening());
                }

                @Override
                public void onError(String utteranceId) {}
            });

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts1");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts1");
        }
    }

    private void startListening() {
        if (!isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.startListening(intent);
        }
    }

    private void handleSpeechError(int error) {
        String message = "لم أسمع صوتك بوضوح. حاول مرة أخرى";
        speakAndListen(message);
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("ar"));
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Arabic language not supported", Toast.LENGTH_SHORT).show();
            } else {
                speakAndListen("أهلاً بك في صفحة سداد فاتورة " + billType + " من " + companyName +
                        ". قل رقم حساب " + billType + " الخاص بك");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}