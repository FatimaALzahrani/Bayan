package com.bank.bayan.transaction;

import android.app.AutomaticZenRule;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bank.bayan.R;

import java.text.DecimalFormat;
import java.util.Locale;

public class TransferConfirmationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private DatabaseHelper databaseHelper;

    private String contactName;
    private String accountNumber;
    private double transferAmount;
    private double currentBalance;

    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_confirmation);

        textToSpeech = new TextToSpeech(this, this);
        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        contactName = intent.getStringExtra("contact_name");
        accountNumber = intent.getStringExtra("account_number");
        transferAmount = intent.getDoubleExtra("transfer_amount", 0.0);
        currentBalance = intent.getDoubleExtra("current_balance", 0.0);

        initializeViews();
        setupClickListeners();

        String confirmationMessage = "صفحة تأكيد التحويل. ستقوم بتحويل " +
                decimalFormat.format(transferAmount) + " ريال إلى " +
                contactName + ". اضغط تأكيد لإتمام العملية أو رجوع للإلغاء";
        speak(confirmationMessage);
    }

    private void initializeViews() {
        TextView amountTextView = findViewById(R.id.confirmAmountTextView);
        TextView recipientNameTextView = findViewById(R.id.confirmRecipientNameTextView);
        TextView accountNumberTextView = findViewById(R.id.confirmAccountNumberTextView);
        TextView newBalanceTextView = findViewById(R.id.newBalanceTextView);

        amountTextView.setText(decimalFormat.format(transferAmount));
        recipientNameTextView.setText(contactName);
        accountNumberTextView.setText(accountNumber);

        double newBalance = currentBalance - transferAmount;
        newBalanceTextView.setText(decimalFormat.format(newBalance));

        amountTextView.setContentDescription("مبلغ التحويل: " + decimalFormat.format(transferAmount) + " ريال");
        recipientNameTextView.setContentDescription("المستلم: " + contactName);
        accountNumberTextView.setContentDescription("رقم الحساب: " + accountNumber);
        newBalanceTextView.setContentDescription("رصيدك بعد التحويل: " + decimalFormat.format(newBalance) + " ريال");
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        Button confirmButton = findViewById(R.id.confirmButton);

        backButton.setOnClickListener(v -> {
            speak("تم إلغاء التحويل");
            finish();
        });

        confirmButton.setOnClickListener(v -> {
            processTransfer();
        });
    }

    private void processTransfer() {
        speak("جاري معالجة التحويل...");

        Button confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setEnabled(false);
        confirmButton.setText("جاري المعالجة...");

        findViewById(R.id.mainLayout).postDelayed(() -> {
            double newBalance = currentBalance - transferAmount;

            speak("تم التحويل بنجاح. تم تحويل " + decimalFormat.format(transferAmount) +
                    " ريال إلى " + contactName);

            Intent intent = new Intent(this, TransactionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();

        }, 2000);
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
        super.onDestroy();
    }
}