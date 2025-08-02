package com.bank.bayan.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bank.bayan.R;
import com.bank.bayan.models.User;
import com.bank.bayan.models.Account;
import com.bank.bayan.utils.FirebaseHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class TransferDetailsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private enum ConversationState {
        LOADING_DATA,
        ASKING_AMOUNT,
        ASKING_TRANSFER_TYPE,
        ASKING_CONFIRMATION,
        ASKING_MODIFY_OR_BACK
    }

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private TextView transferAmountTextView;
    private TextView currentBalanceTextView;
    private TextView recipientNameTextView;
    private TextView accountNumberTextView;
    private TextView transferPurposeTextView;
    private Button nextButton;

    private String currentUserId = "-OWWLat4qAv0iBj5I0GQ";
    private String contactName;
    private String accountNumber;
    private double currentBalance = 0.0;
    private double transferAmount = 0.0;
    private String transferPurpose = "";
    private boolean isListening = false;
    private ConversationState currentState = ConversationState.LOADING_DATA;

    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    // خيارات نوع الحوالة
    private String[] transferTypes = {
            "حوالات شخصية",
            "دفع فواتير",
            "أجور ومرتبات",
            "استثمارات",
            "مدفوعات تجارية"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_details);

        firebaseHelper = new FirebaseHelper();

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        Intent intent = getIntent();
        contactName = intent.getStringExtra("contact_name");
        accountNumber = intent.getStringExtra("account_number");

        initializeViews();
        setupSpeechRecognition();
        setupClickListeners();

        loadUserData();
    }

    private void loadUserData() {
        speak("جاري تحميل بيانات حسابك...");

        firebaseHelper.getUserData(currentUserId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                if (user.getAccount() != null) {
                    currentBalance = user.getAccount().getBalance();
                    runOnUiThread(() -> {
                        updateUI();
                        startConversation();
                    });
                } else {
                    onError("لم يتم العثور على بيانات الحساب");
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(TransferDetailsActivity.this, error, Toast.LENGTH_LONG).show();
                    speakAndListen("حدث خطأ في تحميل البيانات: " + error + ". هل تريد المحاولة مرة أخرى؟");
                });
            }
        });
    }

    private void updateUI() {
        currentBalanceTextView.setText(decimalFormat.format(currentBalance));
        recipientNameTextView.setText(contactName);
        accountNumberTextView.setText(accountNumber);
    }

    private void startConversation() {
        String welcomeMessage = "أهلاً بك " + currentUser.getName() +
                " في صفحة التحويل إلى " + contactName +
                ". رصيدك الحالي " + decimalFormat.format(currentBalance) + " ريال. " +
                "كم المبلغ الذي تريد تحويله؟";
        currentState = ConversationState.ASKING_AMOUNT;
        speakAndListen(welcomeMessage);
    }

    private void initializeViews() {
        transferAmountTextView = findViewById(R.id.transferAmountTextView);
        currentBalanceTextView = findViewById(R.id.currentBalanceTextView);
        recipientNameTextView = findViewById(R.id.recipientNameTextView);
        accountNumberTextView = findViewById(R.id.accountNumberTextView);
        transferPurposeTextView = findViewById(R.id.transferPurposeTextView);
        nextButton = findViewById(R.id.nextButton);

        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            speak("رجوع إلى الصفحة السابقة");
            finish();
        });

        nextButton.setOnClickListener(v -> {
            if (transferAmount > 0 && !transferPurpose.isEmpty()) {
                proceedToConfirmation();
            } else {
                speakAndListen("يرجى إكمال جميع البيانات أولاً");
            }
        });

        findViewById(R.id.amountContainer).setOnClickListener(v -> {
            if (currentState != ConversationState.LOADING_DATA) {
                currentState = ConversationState.ASKING_AMOUNT;
                speakAndListen("كم المبلغ الذي تريد تحويله؟");
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
            case LOADING_DATA:
                if (spokenText.contains("نعم") || spokenText.contains("موافق")) {
                    loadUserData();
                } else {
                    speakAndListen("سيتم المحاولة مرة أخرى");
                    loadUserData();
                }
                break;
            case ASKING_AMOUNT:
                handleAmountInput(spokenText);
                break;
            case ASKING_TRANSFER_TYPE:
                handleTransferTypeInput(spokenText);
                break;
            case ASKING_CONFIRMATION:
                handleConfirmationInput(spokenText);
                break;
            case ASKING_MODIFY_OR_BACK:
                handleModifyOrBackInput(spokenText);
                break;
        }
    }

    private void handleAmountInput(String spokenText) {
        if (spokenText.contains("رجوع") || spokenText.contains("إلغاء") || spokenText.contains("خروج")) {
            speak("تم الإلغاء. رجوع للصفحة السابقة");
            finish();
            return;
        }

        double amount = extractAmountFromSpeech(spokenText);
        if (amount > 0) {
            if (amount > currentBalance) {
                speakAndListen("المبلغ " + decimalFormat.format(amount) +
                        " ريال أكبر من رصيدك الحالي " + decimalFormat.format(currentBalance) +
                        " ريال. غير كافي. قل مبلغاً أقل أو قل رجوع إذا كنت تريد الرجوع");
            } else {
                setTransferAmount(amount);
                double remainingBalance = currentBalance - amount;
                String confirmMessage = "المبلغ " + decimalFormat.format(amount) + " ريال. " +
                        "رصيدك بعد التحويل سيصبح " + decimalFormat.format(remainingBalance) + " ريال. " +
                        "ماهو نوع الحوالة؟ " +
                        "واحد: حوالات شخصية، " +
                        "اثنين: دفع فواتير، " +
                        "ثلاثة: أجور ومرتبات، " +
                        "أربعة: استثمارات، " +
                        "خمسة: مدفوعات تجارية";
                currentState = ConversationState.ASKING_TRANSFER_TYPE;
                speakAndListen(confirmMessage);
            }
        } else {
            speakAndListen("لم أفهم . قل المبلغ بوضوح، مثل: مائة ريال أو خمسمائة ريال");
        }
    }

    private void handleTransferTypeInput(String spokenText) {
        int selectedType = -1;
        String selectedPurpose = "";

        // التعرف على نوع الحوالة
        if (spokenText.contains("واحد") || spokenText.contains("١") ||
                spokenText.contains("شخصية") || spokenText.contains("شخصي")) {
            selectedType = 0;
        } else if (spokenText.contains("اثنين") || spokenText.contains("٢") ||
                spokenText.contains("فواتير") || spokenText.contains("فاتورة")) {
            selectedType = 1;
        } else if (spokenText.contains("ثلاثة") || spokenText.contains("٣") ||
                spokenText.contains("أجور") || spokenText.contains("مرتبات") || spokenText.contains("راتب")) {
            selectedType = 2;
        } else if (spokenText.contains("أربعة") || spokenText.contains("٤") ||
                spokenText.contains("استثمار")) {
            selectedType = 3;
        } else if (spokenText.contains("خمسة") || spokenText.contains("٥") ||
                spokenText.contains("تجارية") || spokenText.contains("تجاري")) {
            selectedType = 4;
        }

        if (selectedType >= 0 && selectedType < transferTypes.length) {
            selectedPurpose = transferTypes[selectedType];
            setTransferPurpose(selectedPurpose);

            String message = "تم اختيار " + selectedPurpose + ". " +
                    "هل تود الذهاب لسماع ملخص الحوالة؟";
            currentState = ConversationState.ASKING_CONFIRMATION;
            speakAndListen(message);
        } else {
            speakAndListen("لم أفهم اختيارك. قل رقم من واحد إلى خمسة، أو قل اسم نوع الحوالة");
        }
    }

    private void handleConfirmationInput(String spokenText) {
        if (spokenText.contains("نعم") || spokenText.contains("موافق") ||
                spokenText.contains("أكد") || spokenText.contains("متابعة") || spokenText.contains("إيوه")) {
            Intent intent = new Intent(this, TransferSummaryActivity.class);
            intent.putExtra("amount", transferAmount);
            intent.putExtra("purpose", transferPurpose);
            intent.putExtra("receiver", contactName);
            intent.putExtra("account_number", accountNumber);
            intent.putExtra("current_balance", currentBalance);
            startActivity(intent);
        } else if (spokenText.contains("لا") || spokenText.contains("لأ")) {
            currentState = ConversationState.ASKING_MODIFY_OR_BACK;
            speakAndListen("هل تريد تعديل المبلغ؟");
        } else {
            speakAndListen("قل نعم للذهاب للملخص، أو لا إذا إذا كنت تريد تعديل شيء");
        }
    }

    private void handleModifyOrBackInput(String spokenText) {
        if (spokenText.contains("نعم") || spokenText.contains("إيوه") ||
                spokenText.contains("أريد") || spokenText.contains("أبغى")) {
            currentState = ConversationState.ASKING_AMOUNT;
            speakAndListen("كم المبلغ الجديد الذي تريد تحويله؟");
        } else if (spokenText.contains("لا") || spokenText.contains("لأ")) {
            speakAndListen("هل تود الرجوع للصفحة السابقة؟");
            findViewById(R.id.mainLayout).postDelayed(() -> {
                speakAndListen("قل نعم للرجوع أو لا للبقاء في الصفحة");
            }, 2000);
        } else if (spokenText.contains("رجوع") || spokenText.contains("خروج")) {
            speak("رجوع للصفحة السابقة");
            finish();
        } else {
            speakAndListen("قل نعم إذا كنت تريد تعديل المبلغ، أو لا إذا كنت لا تريد التعديل");
        }
    }

    private double extractAmountFromSpeech(String speech) {
        speech = speech.replaceAll("ريال|ريالاً|تحويل|أريد|أبغى|تبغى", "").trim();

        try {
            if (speech.matches(".*\\d+.*")) {
                String numberStr = speech.replaceAll("[^\\d.]", "");
                if (!numberStr.isEmpty()) {
                    return Double.parseDouble(numberStr);
                }
            }

            return parseArabicNumbers(speech);

        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseArabicNumbers(String speech) {
        double amount = 0;

        if (speech.contains("مائة") || speech.contains("مئة")) amount += 100;
        if (speech.contains("مائتان") || speech.contains("مئتان")) amount += 200;
        if (speech.contains("ثلاثمائة") || speech.contains("ثلاثمئة")) amount += 300;
        if (speech.contains("أربعمائة") || speech.contains("أربعمئة")) amount += 400;
        if (speech.contains("خمسمائة") || speech.contains("خمسمئة")) amount += 500;
        if (speech.contains("ستمائة") || speech.contains("ستمئة")) amount += 600;
        if (speech.contains("سبعمائة") || speech.contains("سبعمئة")) amount += 700;
        if (speech.contains("ثمانمائة") || speech.contains("ثمانمئة")) amount += 800;
        if (speech.contains("تسعمائة") || speech.contains("تسعمئة")) amount += 900;

        if (speech.contains("ألف")) {
            if (speech.contains("ألفان") || speech.contains("ألفين")) amount += 2000;
            else if (speech.contains("ثلاثة آلاف")) amount += 3000;
            else if (speech.contains("أربعة آلاف")) amount += 4000;
            else if (speech.contains("خمسة آلاف")) amount += 5000;
            else amount += 1000;
        }

        if (speech.contains("عشرة") || speech.contains("عشر")) amount += 10;
        if (speech.contains("عشرون") || speech.contains("عشرين")) amount += 20;
        if (speech.contains("ثلاثون") || speech.contains("ثلاثين")) amount += 30;
        if (speech.contains("أربعون") || speech.contains("أربعين")) amount += 40;
        if (speech.contains("خمسون") || speech.contains("خمسين")) amount += 50;
        if (speech.contains("ستون") || speech.contains("ستين")) amount += 60;
        if (speech.contains("سبعون") || speech.contains("سبعين")) amount += 70;
        if (speech.contains("ثمانون") || speech.contains("ثمانين")) amount += 80;
        if (speech.contains("تسعون") || speech.contains("تسعين")) amount += 90;

        if (speech.contains("واحد") || speech.contains("أحد")) amount += 1;
        if (speech.contains("اثنان") || speech.contains("اثنين") || speech.contains("إثنان")) amount += 2;
        if (speech.contains("ثلاثة")) amount += 3;
        if (speech.contains("أربعة")) amount += 4;
        if (speech.contains("خمسة")) amount += 5;
        if (speech.contains("ستة")) amount += 6;
        if (speech.contains("سبعة")) amount += 7;
        if (speech.contains("ثمانية")) amount += 8;
        if (speech.contains("تسعة")) amount += 9;

        return amount;
    }

    private void setTransferAmount(double amount) {
        transferAmount = amount;
        transferAmountTextView.setText(decimalFormat.format(amount));
        transferAmountTextView.setContentDescription("مبلغ التحويل: " + decimalFormat.format(amount) + " ريال");

        checkIfCanProceed();
    }

    private void setTransferPurpose(String purpose) {
        transferPurpose = purpose;
        if (transferPurposeTextView != null) {
            transferPurposeTextView.setText(purpose);
        }

        checkIfCanProceed();
    }

    private void checkIfCanProceed() {
        if (transferAmount > 0 && !transferPurpose.isEmpty()) {
            nextButton.setEnabled(true);
            nextButton.setAlpha(1.0f);
        }
    }

    private void proceedToConfirmation() {
        if (transferAmount <= 0) {
            currentState = ConversationState.ASKING_AMOUNT;
            speakAndListen("يرجى إدخال مبلغ التحويل أولاً");
            return;
        }

        if (transferPurpose.isEmpty()) {
            currentState = ConversationState.ASKING_TRANSFER_TYPE;
            speakAndListen("يرجى اختيار نوع الحوالة أولاً");
            return;
        }

        speak("جاري الانتقال إلى صفحة ملخص الحوالة");

        // Navigate to summary screen
        Intent intent = new Intent(this, TransferSummaryActivity.class);
        intent.putExtra("amount", transferAmount);
        intent.putExtra("receiver", contactName);
        intent.putExtra("account_number", accountNumber);
        intent.putExtra("purpose", transferPurpose);
        intent.putExtra("current_balance", currentBalance);
        startActivity(intent);
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
        if (!isListening && currentState != ConversationState.LOADING_DATA) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.startListening(intent);
        }
    }

    private void handleSpeechError(int error) {
        String message = "لم أسمع صوتك بوضوح ، حاول مره أخرى";
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