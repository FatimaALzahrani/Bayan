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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bank.bayan.R;
import com.bank.bayan.models.User;
import com.bank.bayan.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class BillPaymentDetailsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    // حالات المحادثة
    private enum ConversationState {
        LOADING_BILL_DATA,
        SHOWING_BILL_DETAILS,
        ASKING_CONFIRMATION
    }

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private TextView billTypeTextView;
    private TextView companyNameTextView;
    private TextView accountNumberTextView;
    private TextView billAmountTextView;
    private TextView dueDateTextView;
    private TextView currentBalanceTextView;
    private TextView remainingBalanceTextView;
    private ImageView billIconImageView;
    private Button payButton;

    private String billType;
    private String companyName;
    private String accountNumber;
    private int iconResource;
    private double billAmount = 0.0;
    private double currentBalance = 0.0;
    private String dueDate = "";
    private boolean isListening = false;
    private ConversationState currentState = ConversationState.LOADING_BILL_DATA;

    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private String currentUserId = "-OWWLat4qAv0iBj5I0GQ";

    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_payment_details);

        firebaseHelper = new FirebaseHelper();

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        String billId = getIntent().getStringExtra("bill_id");

        Toast.makeText(this,billId,Toast.LENGTH_LONG).show();

        getIntentData();

        initializeViews();
        setupSpeechRecognition();
        setupClickListeners();

        loadUserDataAndBill();
    }

    private void getIntentData() {
        Intent intent = getIntent();

        billType = intent.getStringExtra("bill_type");
        companyName = intent.getStringExtra("company_name");
        accountNumber = intent.getStringExtra("account_number");
        iconResource = intent.getIntExtra("icon_resource", R.drawable.ic_transfer);

        if (billType == null) {
            billType = intent.getStringExtra("service_provider");
            companyName = billType; // Use service provider as company name
            accountNumber = intent.getStringExtra("account_number");
            billAmount = intent.getDoubleExtra("amount", 0.0);
            dueDate = intent.getStringExtra("due_date");
        }
    }

    private void initializeViews() {
        billTypeTextView = findViewById(R.id.billTypeTextView);
        companyNameTextView = findViewById(R.id.companyNameTextView);
        accountNumberTextView = findViewById(R.id.accountNumberTextView);
        billAmountTextView = findViewById(R.id.billAmountTextView);
        dueDateTextView = findViewById(R.id.dueDateTextView);
        currentBalanceTextView = findViewById(R.id.currentBalanceTextView);
        remainingBalanceTextView = findViewById(R.id.remainingBalanceTextView);
        billIconImageView = findViewById(R.id.billIconImageView);
        payButton = findViewById(R.id.payButton);

        payButton.setEnabled(false);
        payButton.setAlpha(0.5f);
    }

    private void loadUserDataAndBill() {
        speak("جاري تحميل بيانات الفاتورة...");

        firebaseHelper.getUserData(currentUserId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                if (user.getAccount() != null) {
                    currentBalance = user.getAccount().getBalance();

                    simulateBillDataLoading();
                } else {
                    onError("لم يتم العثور على بيانات الحساب");
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(BillPaymentDetailsActivity.this, error, Toast.LENGTH_LONG).show();
                    speakAndListen("حدث خطأ في تحميل البيانات: " + error + ". هل تريد المحاولة مرة أخرى؟");
                });
            }
        });
    }

    private void simulateBillDataLoading() {
        findViewById(R.id.mainLayout).postDelayed(() -> {
            if (billAmount == 0.0) {
                Random random = new Random();
                billAmount = 50 + (random.nextDouble() * 450);
            }

            if (dueDate == null || dueDate.isEmpty()) {
                dueDate = "15/09/2025";
            }

            runOnUiThread(() -> {
                updateUI();
                currentState = ConversationState.SHOWING_BILL_DETAILS;
                showBillDetails();
            });
        }, 2000);
    }

    private void updateUI() {
        billTypeTextView.setText(billType);
        companyNameTextView.setText(companyName);
        accountNumberTextView.setText(accountNumber);
        billAmountTextView.setText(decimalFormat.format(billAmount));
        dueDateTextView.setText(dueDate);
        currentBalanceTextView.setText(decimalFormat.format(currentBalance));

        double remainingBalance = currentBalance - billAmount;
        remainingBalanceTextView.setText(decimalFormat.format(remainingBalance));

        if (iconResource != 0) {
            billIconImageView.setImageResource(iconResource);
        }

        if (currentBalance >= billAmount) {
            payButton.setEnabled(true);
            payButton.setAlpha(1.0f);
        } else {
            payButton.setEnabled(false);
            payButton.setAlpha(0.5f);
        }

        billAmountTextView.setContentDescription("مبلغ الفاتورة: " + decimalFormat.format(billAmount) + " ريال");
        currentBalanceTextView.setContentDescription("رصيدك الحالي: " + decimalFormat.format(currentBalance) + " ريال");
        remainingBalanceTextView.setContentDescription("رصيدك بعد الدفع: " + decimalFormat.format(remainingBalance) + " ريال");
    }

    private void showBillDetails() {
        String message;

        if (currentBalance >= billAmount) {
            double remainingBalance = currentBalance - billAmount;
            message = "تم العثور على فاتورة " + billType + " بمبلغ " + decimalFormat.format(billAmount) + " ريال. " +
                    "تاريخ الاستحقاق " + dueDate + ". " +
                    "رصيدك الحالي " + decimalFormat.format(currentBalance) + " ريال. " +
                    "رصيدك بعد الدفع " + decimalFormat.format(remainingBalance) + " ريال. " +
                    "هل تريد دفع هذه الفاتورة؟";
        } else {
            message = "تم العثور على فاتورة " + billType + " بمبلغ " + decimalFormat.format(billAmount) + " ريال. " +
                    "لكن رصيدك الحالي " + decimalFormat.format(currentBalance) + " ريال غير كافي للدفع. " +
                    "يرجى شحن رصيدك أولاً أو اختيار طريقة دفع أخرى.";
        }

        currentState = ConversationState.ASKING_CONFIRMATION;
        speakAndListen(message);
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            speak("رجوع إلى الصفحة السابقة");
            finish();
        });

        payButton.setOnClickListener(v -> {
            if (currentBalance >= billAmount) {
                proceedToPayment();
            } else {
                speakAndListen("رصيدك غير كافي لدفع هذه الفاتورة");
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
            case LOADING_BILL_DATA:
                if (spokenText.contains("نعم") || spokenText.contains("موافق")) {
                    loadUserDataAndBill();
                } else {
                    speakAndListen("سيتم المحاولة مرة أخرى");
                    loadUserDataAndBill();
                }
                break;

            case SHOWING_BILL_DETAILS:
                break;

            case ASKING_CONFIRMATION:
                handleConfirmationInput(spokenText);
                break;
        }
    }

    private void handleConfirmationInput(String spokenText) {
        if (spokenText.contains("نعم") || spokenText.contains("إيوه") ||
                spokenText.contains("موافق") || spokenText.contains("ادفع") ||
                spokenText.contains("متابعة") || spokenText.contains("تأكيد")) {

            if (currentBalance >= billAmount) {
                proceedToPayment();
            } else {
                speakAndListen("عذراً، رصيدك غير كافي لدفع هذه الفاتورة. يرجى شحن رصيدك أولاً");
            }

        } else if (spokenText.contains("لا") || spokenText.contains("لأ") ||
                spokenText.contains("إلغاء") || spokenText.contains("رجوع")) {

            speak("تم إلغاء العملية. رجوع إلى الصفحة السابقة");
            finish();

        } else if (spokenText.contains("إعادة") || spokenText.contains("كرر") ||
                spokenText.contains("اسمع") || spokenText.contains("مرة ثانية")) {

            showBillDetails();

        } else {
            speakAndListen("لم أفهم إجابتك. قل نعم لدفع الفاتورة، أو لا للإلغاء، أو قل اسمع التفاصيل مرة ثانية");
        }
    }

    private void proceedToPayment() {
        speak("جاري الانتقال إلى صفحة تأكيد الدفع");

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

//        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
//                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
//                : "-OWWLat4qAv0iBj5I0GQ";

        DatabaseReference userAccountRef = database.child("users").child(currentUserId).child("account");

        userAccountRef.child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double currentBalance = snapshot.getValue(Double.class);

                    double newBalance = currentBalance - billAmount;
                    if (newBalance < 0) {
                        Toast.makeText(getApplicationContext(), "الرصيد غير كافٍ لدفع الفاتورة", Toast.LENGTH_LONG).show();
                        return;
                    }

                    userAccountRef.child("balance").setValue(newBalance);

                    // حذف الفاتورة
                    Intent intentget = getIntent();
                    String billId = intentget.getStringExtra("bill_id");
                    if (billId != null && !billId.isEmpty()) {
                        DatabaseReference billRef = database.child("bills").child(currentUserId).child(billId);
                        billRef.removeValue();
                    }

                    // الانتقال
                    Intent intent = new Intent(getApplicationContext(), BillPaymentSuccessActivity.class);
                    intent.putExtra("bill_type", billType);
                    intent.putExtra("company_name", companyName);
                    intent.putExtra("account_number", accountNumber);
                    intent.putExtra("bill_amount", billAmount);
                    intent.putExtra("due_date", dueDate);
                    intent.putExtra("current_balance", newBalance);
                    intent.putExtra("icon_resource", iconResource);
                    startActivity(intent);
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
                    if (currentState == ConversationState.ASKING_CONFIRMATION) {
                        runOnUiThread(() -> startListening());
                    }
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
        if (!isListening && currentState == ConversationState.ASKING_CONFIRMATION) {
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