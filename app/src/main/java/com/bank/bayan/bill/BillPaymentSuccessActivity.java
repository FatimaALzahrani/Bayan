package com.bank.bayan.bill;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bank.bayan.HomeActivity;
import com.bank.bayan.R;
import com.bank.bayan.MainActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class BillPaymentSuccessActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private double currentBalance;

    private enum ConversationState {
        READING_SUCCESS,
        ASKING_NEXT_ACTION
    }

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private TextView billTypeTextView;
    private TextView companyNameTextView;
    private TextView accountNumberTextView;
    private TextView billAmountTextView;
    private TextView dueDateTextView;
    private TextView paymentDateTextView;
    private TextView remainingBalanceTextView;
    private ImageView billIconImageView;
    private Button shareButton;
    private Button doneButton;

    private String billType;
    private String companyName;
    private String accountNumber;
    private double billAmount = 0.0;
    private String dueDate;
    private String paymentDate;
    private double remainingBalance = 0.0;
    private int iconResource;

    private boolean isListening = false;
    private ConversationState currentState = ConversationState.READING_SUCCESS;

    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_payment_success);

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        getIntentData();

        initializeViews();
        setupSpeechRecognition();
        setupClickListeners();
        updateUI();

        findViewById(R.id.mainLayout).postDelayed(() -> {
            readSuccessMessage();
        }, 1500);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        billType = intent.getStringExtra("bill_type");
        companyName = intent.getStringExtra("company_name");
        accountNumber = intent.getStringExtra("account_number");
        billAmount = intent.getDoubleExtra("bill_amount", 0.0);
        dueDate = intent.getStringExtra("due_date");
        paymentDate = intent.getStringExtra("payment_date");
        remainingBalance = intent.getDoubleExtra("remaining_balance", 0.0);
        currentBalance = intent.getDoubleExtra("current_balance", 0.0);
        iconResource = intent.getIntExtra("icon_resource", R.drawable.ic_transfer);

        // Default values if not passed
        if (billType == null) billType = "غير محدد";
        if (companyName == null) companyName = "غير محدد";
        if (accountNumber == null) accountNumber = "غير محدد";
        if (dueDate == null) dueDate = "غير محدد";
        if (paymentDate == null) paymentDate = "غير محدد";
    }

    private void initializeViews() {
//        billTypeTextView = findViewById(R.id.billTypeTextView);
        companyNameTextView = findViewById(R.id.companyNameTextView);
        accountNumberTextView = findViewById(R.id.accountNumberTextView);
        billAmountTextView = findViewById(R.id.billAmountTextView);
        dueDateTextView = findViewById(R.id.dueDateTextView);
//        paymentDateTextView = findViewById(R.id.paymentDateTextView);
        remainingBalanceTextView = findViewById(R.id.remainingBalanceTextView);
//        billIconImageView = findViewById(R.id.billIconImageView);
        shareButton = findViewById(R.id.shareButton);
        doneButton = findViewById(R.id.doneButton);
    }

    private void updateUI() {
//        billTypeTextView.setText(billType);
        companyNameTextView.setText(companyName);
        accountNumberTextView.setText(accountNumber);
        billAmountTextView.setText(decimalFormat.format(billAmount));
        dueDateTextView.setText(dueDate);
//        paymentDateTextView.setText(paymentDate);
        remainingBalanceTextView.setText(decimalFormat.format(currentBalance) + " ريال");

//        if (iconResource != 0) {
//            billIconImageView.setImageResource(iconResource);
//        }

        // Set content descriptions for accessibility
        billAmountTextView.setContentDescription("المبلغ المدفوع: " + decimalFormat.format(billAmount) + " ريال");
        companyNameTextView.setContentDescription("إلى: " + companyName);
        accountNumberTextView.setContentDescription("رقم الحساب: " + accountNumber);
        remainingBalanceTextView.setContentDescription("رصيدك الحالي: " + decimalFormat.format(currentBalance) + " ريال");
    }

    private void setupClickListeners() {
        shareButton.setOnClickListener(v -> {
            sharePaymentReceipt();
        });

        doneButton.setOnClickListener(v -> {
            goToMainScreen();
        });

        findViewById(R.id.mainLayout).setOnClickListener(v -> {
            if (currentState == ConversationState.ASKING_NEXT_ACTION) {
                speakAndListen("هل تود سداد فواتير أخرى أم الرجوع للشاشة الرئيسية؟");
            }
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
        spokenText = spokenText.trim().toLowerCase();

        switch (currentState) {
            case READING_SUCCESS:
                break;

            case ASKING_NEXT_ACTION:
                handleNextActionInput(spokenText);
                break;
        }
    }

    private void handleNextActionInput(String spokenText) {
        if (spokenText.contains("سداد") || spokenText.contains("فاتورة") ||
                spokenText.contains("فواتير") || spokenText.contains("أخرى")) {
            sharePaymentReceipt();

        } else if (spokenText.contains("تم") || spokenText.contains("انتهيت") ||
                spokenText.contains("رئيسية") || spokenText.contains("الشاشة الرئيسية") ||
                spokenText.contains("رجوع") || spokenText.contains("خلاص")) {

            goToMainScreen();

        } else if (spokenText.contains("إعادة") || spokenText.contains("كرر") ||
                spokenText.contains("اسمع") || spokenText.contains("مرة ثانية")) {

            readSuccessMessage();

        } else {
            speakAndListen("لم أفهم طلبك. قل مشاركة الإيصال، أو تم للرجوع للشاشة الرئيسية");
        }
    }

    private void readSuccessMessage() {
        currentState = ConversationState.READING_SUCCESS;

        String successMessage = "مبروك! تم دفع فاتورة " + billType + " من " + companyName +
                " بمبلغ " + decimalFormat.format(billAmount) + " ريال بنجاح. " +
                "رصيدك الحالي أصبح " + decimalFormat.format(currentBalance) + " ريال. " +
                "هل تود سداد فواتير أخرى أم الرجوع للشاشة الرئيسية؟";

        currentState = ConversationState.ASKING_NEXT_ACTION;
        speakAndListen(successMessage);
    }

    private void sharePaymentReceipt() {
        speak("جاري العودة لسداد فواتير أخرى");

        Intent intent = new Intent(this, BillPaymentActivity.class);
        startActivity(intent);
    }

    private void goToMainScreen() {
        speak("رجوع إلى الشاشة الرئيسية");

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
                public void onError(String utteranceId) {
                    runOnUiThread(() -> startListening());
                }
            });

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts1");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts1");
        }
    }

    private void startListening() {
        if (!isListening && currentState == ConversationState.ASKING_NEXT_ACTION) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.startListening(intent);
        }
    }

    private void handleSpeechError(int error) {
        String message;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "مشكلة في الصوت. حاول مرة أخرى";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "خطأ في التطبيق. حاول مرة أخرى";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "يرجى السماح بالوصول للميكروفون";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "مشكلة في الاتصال بالإنترنت";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "انتهت مدة الاتصال. حاول مرة أخرى";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "لم أسمع صوتك بوضوح. حاول مرة أخرى";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "الميكروفون مشغول. انتظر قليلاً";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "مشكلة في الخادم. حاول مرة أخرى";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "لم أسمع أي صوت. حاول التحدث مرة أخرى";
                break;
            default:
                message = "حدث خطأ. حاول مرة أخرى";
                break;
        }

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
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        speakAndListen("للرجوع للشاشة الرئيسية قل تم");
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