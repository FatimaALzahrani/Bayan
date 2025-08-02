package com.bank.bayan.transaction;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bank.bayan.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TransferSummaryActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private enum ConversationState {
        READING_SUMMARY,
        ASKING_CONFIRMATION
    }

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private TextView transferAmountTextView;
    private TextView recipientNameTextView;
    private TextView accountNumberTextView;
    private TextView transferPurposeTextView;
    private TextView currentBalanceTextView;
    private TextView remainingBalanceTextView;
    private TextView dateTimeTextView;
    private Button confirmButton;

    private double transferAmount = 0.0;
    private String recipientName = "";
    private String recipientAccount = "";
    private String transferPurpose = "";
    private double currentBalance = 0.0;
    private double remainingBalance = 0.0;

    private boolean isListening = false;
    private ConversationState currentState = ConversationState.READING_SUMMARY;

    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - hh:mm a", new Locale("ar"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_summary);

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        getIntentData();

        initializeViews();
        setupSpeechRecognition();
        setupClickListeners();
        updateUI();

        findViewById(R.id.mainLayout).postDelayed(() -> {
            readTransferSummary();
        }, 1000);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        transferAmount = intent.getDoubleExtra("amount", 0.0);
        recipientName = intent.getStringExtra("receiver");
        recipientAccount = intent.getStringExtra("account_number");
        transferPurpose = intent.getStringExtra("purpose");
        currentBalance = intent.getDoubleExtra("current_balance", 0.0);
        remainingBalance = currentBalance - transferAmount;

        if (recipientName == null) recipientName = "غير محدد";
        if (recipientAccount == null) recipientAccount = "غير محدد";
        if (transferPurpose == null) transferPurpose = "غير محدد";
    }

    private void initializeViews() {
        transferAmountTextView = findViewById(R.id.transferAmountTextView);
        recipientNameTextView = findViewById(R.id.recipientNameTextView);
        accountNumberTextView = findViewById(R.id.accountNumberTextView);
        transferPurposeTextView = findViewById(R.id.transferPurposeTextView);
//        currentBalanceTextView = findViewById(R.id.currentBalanceTextView);
//        remainingBalanceTextView = findViewById(R.id.remainingBalanceTextView);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        confirmButton = findViewById(R.id.confirmButton);
    }

    private void updateUI() {
        transferAmountTextView.setText(decimalFormat.format(transferAmount));
        recipientNameTextView.setText(recipientName);
        accountNumberTextView.setText(recipientAccount);
        transferPurposeTextView.setText(transferPurpose);
//        currentBalanceTextView.setText(decimalFormat.format(currentBalance) + " ريال");
//        remainingBalanceTextView.setText(decimalFormat.format(remainingBalance) + " ريال");

        String currentDateTime = dateFormat.format(new Date());
//        dateTimeTextView.setText(currentDateTime);

        transferAmountTextView.setContentDescription("مبلغ التحويل: " + decimalFormat.format(transferAmount) + " ريال");
        recipientNameTextView.setContentDescription("اسم المستلم: " + recipientName);
        accountNumberTextView.setContentDescription("رقم الحساب: " + recipientAccount);
        transferPurposeTextView.setContentDescription("الغرض من التحويل: " + transferPurpose);
    }

    private void setupClickListeners() {
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            speak("رجوع إلى الصفحة السابقة");
            finish();
        });

        confirmButton.setOnClickListener(v -> {
            confirmTransfer();
        });

        findViewById(R.id.mainLayout).setOnClickListener(v -> {
            if (currentState == ConversationState.ASKING_CONFIRMATION) {
                speakAndListen("هل تود تأكيد هذا التحويل؟ قل نعم للتأكيد أو لا للرجوع");
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
            case READING_SUMMARY:
                break;

            case ASKING_CONFIRMATION:
                handleConfirmationInput(spokenText);
                break;
        }
    }

    private void handleConfirmationInput(String spokenText) {
        if (spokenText.contains("نعم") || spokenText.contains("إيوه") ||
                spokenText.contains("موافق") || spokenText.contains("أكد") ||
                spokenText.contains("متابعة") || spokenText.contains("تأكيد")) {

            confirmTransfer();

        } else if (spokenText.contains("لا") || spokenText.contains("لأ") ||
                spokenText.contains("إلغاء") || spokenText.contains("رجوع")) {

            speak("تم إلغاء العملية. رجوع إلى الصفحة السابقة");
            finish();

        } else if (spokenText.contains("إعادة") || spokenText.contains("كرر") ||
                spokenText.contains("اسمع") || spokenText.contains("مرة ثانية")) {

            readTransferSummary();

        } else {
            speakAndListen("لم أفهم إجابتك. قل نعم لتأكيد التحويل، أو لا للإلغاء، أو قل اسمع الملخص مرة ثانية");
        }
    }

    private void readTransferSummary() {
        currentState = ConversationState.READING_SUMMARY;

        String summaryMessage = "ملخص حوالتك: " +
                "المبلغ " + decimalFormat.format(transferAmount) + " ريال، " +
                "إلى " + recipientName + "، " +
                "رقم الحساب " + formatAccountNumberForSpeech(recipientAccount) + "، " +
                "الغرض من الحوالة " + transferPurpose + "، " +
                "رصيدك الحالي " + decimalFormat.format(currentBalance) + " ريال، " +
                "رصيدك بعد التحويل " + decimalFormat.format(remainingBalance) + " ريال. " +
                "هل تود تأكيد هذا التحويل؟";

        currentState = ConversationState.ASKING_CONFIRMATION;
        speakAndListen(summaryMessage);
    }

    private String formatAccountNumberForSpeech(String accountNumber) {
        if (accountNumber != null && accountNumber.length() > 4) {
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < accountNumber.length(); i++) {
                if (Character.isDigit(accountNumber.charAt(i))) {
                    formatted.append(accountNumber.charAt(i)).append(" ");
                }
            }
            return formatted.toString().trim();
        }
        return accountNumber;
    }

    private void confirmTransfer() {
        speak("جاري تنفيذ التحويل...");

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
//        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
//                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
//                : "-OWWLat4qAv0iBj5I0GQ";

        DatabaseReference userAccountRef = database.child("users").child("-OWWLat4qAv0iBj5I0GQ").child("account");

        userAccountRef.child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double currentBalance = snapshot.getValue(Double.class);

                    if (transferAmount > currentBalance) {
                        Toast.makeText(getApplicationContext(), "الرصيد غير كافٍ لإجراء التحويل", Toast.LENGTH_LONG).show();
                        return;
                    }

                    double newBalance = currentBalance - transferAmount;
                    userAccountRef.child("balance").setValue(newBalance);

                    Intent intent = new Intent(getApplicationContext(), TransferSuccessActivity.class);
                    intent.putExtra("amount", transferAmount);
                    intent.putExtra("receiver", recipientName);
                    intent.putExtra("account_number", recipientAccount);
                    intent.putExtra("purpose", transferPurpose);
                    intent.putExtra("remaining_balance", newBalance);
                    intent.putExtra("transfer_date", dateFormat.format(new Date()));

                    findViewById(R.id.mainLayout).postDelayed(() -> {
                        startActivity(intent);
                        finish();
                    }, 2000);
                } else {
                    Toast.makeText(getApplicationContext(), "لم يتم العثور على الرصيد", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "فشل الاتصال بقاعدة البيانات", Toast.LENGTH_SHORT).show();
            }
        });
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
        if (!isListening && currentState == ConversationState.ASKING_CONFIRMATION) {
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
        speak("رجوع إلى الصفحة السابقة");
        super.onBackPressed();
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