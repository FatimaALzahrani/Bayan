package com.bank.bayan;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bank.bayan.bill.BillPaymentActivity;
import com.bank.bayan.models.User;
import com.bank.bayan.models.Account;
import com.bank.bayan.models.Transaction;
import com.bank.bayan.services.ServicesActivity;
import com.bank.bayan.utils.FirebaseHelper;
import com.bank.bayan.transaction.TransactionActivity;
import com.bank.bayan.transaction.TransferDetailsActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private RecyclerView rvTransactions;
    private DatabaseReference transactionsRef;
    private String userName, userId;
    private VoiceHelper voiceHelper;

    private enum ConversationState {
        LOADING_DATA,
        MAIN_MENU,
        QUICK_ACTIONS,
        ACCOUNT_DETAILS,
        LISTENING_FOR_COMMAND
    }

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private TextView userNameTextView;
    private TextView balanceTextView;
    private TextView incomeTextView;
    private TextView expensesTextView;
    private LinearLayout quickTransferLayout;
    private LinearLayout quickBillLayout;
    private LinearLayout addUserLayout;
    private LinearLayout viewSummaryLayout;
    private RecyclerView transactionsRecyclerView;

    private TransactionAdapter transactionAdapter;
    private List<com.bank.bayan.Transaction> transactionList;
    private DatabaseReference databaseRef;

    private String currentUserId = "-OWWLat4qAv0iBj5I0GQ";
    private User currentUser;
    private List<Transaction> transactions = new ArrayList<>();
    private double currentBalance = 0.0;
    private double monthlyIncome = 0.0;
    private double monthlyExpenses = 0.0;

    private boolean isListening = false;
    private ConversationState currentState = ConversationState.LOADING_DATA;
    private FirebaseHelper firebaseHelper;
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));

    private String[] mainMenuOptions = {
            "تحويل سريع",
            "تسديد فاتورة",
            "إضافة مفوض جديد",
            "عرض الرصيد",
            "كشف الحساب",
            "تحليل المصروفات"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firebaseHelper = new FirebaseHelper();

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        initializeViews();
        setupSpeechRecognition();
        setupClickListeners();

        loadUserData();

        ImageView notificationIcon = findViewById(R.id.notificationIcon);
        notificationIcon.setOnClickListener(v -> {
            View sheetView = getLayoutInflater().inflate(R.layout.notification_dialog, null);
            AlertDialog dialog = new AlertDialog.Builder(HomeActivity.this)
                    .setView(sheetView)
                    .create();

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();

            ImageView closeButton = sheetView.findViewById(R.id.closeButton);
            closeButton.setOnClickListener(view -> dialog.dismiss());
        });

        View purchasesView = findViewById(R.id.category_purchases);
        setCategoryItem(purchasesView, "مشتريات", 57, R.color.primary_blue);

        View transfersView = findViewById(R.id.category_transfers);
        setCategoryItem(transfersView, "تحويلات", 30, R.color.pink_red);

        View billsView = findViewById(R.id.category_bills);
        setCategoryItem(billsView, "سداد فواتير", 10, R.color.orange_yellow);
    }


    private void setCategoryItem(View view, String name, int percentage, @ColorRes int colorRes) {
        TextView tvName = view.findViewById(R.id.tv_category_name);
        TextView tvPercent = view.findViewById(R.id.tv_category_percentage);
        ProgressBar progressBar = view.findViewById(R.id.progress_category);
        View colorIndicator = view.findViewById(R.id.category_color_indicator);

        tvName.setText(name);
        tvPercent.setText(percentage + "%");
        progressBar.setProgress(percentage);

        int color = ContextCompat.getColor(this, colorRes);
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
        colorIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void initializeViews() {
        userNameTextView = findViewById(R.id.user_name);
        balanceTextView = findViewById(R.id.tv_balance);
        incomeTextView = findViewById(R.id.tv_income);
        expensesTextView = findViewById(R.id.tv_expenses);
        quickTransferLayout = findViewById(R.id.quick_transfer_layout);
        quickBillLayout = findViewById(R.id.quick_bill_layout);
        addUserLayout = findViewById(R.id.add_user);
        viewSummaryLayout = findViewById(R.id.viewSummary);
        transactionsRecyclerView = findViewById(R.id.rv_transactions);
        rvTransactions = findViewById(R.id.rv_transactions);

        Intent intent = getIntent();
        userName = intent.getStringExtra("user_name");
        userId = intent.getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            userId = "-OWWLat4qAv0iBj5I0GQ";
        }
        voiceHelper = new VoiceHelper(this, textToSpeech);

        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList, voiceHelper);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);

        databaseRef = FirebaseDatabase.getInstance().getReference();
        transactionsRef = databaseRef.child("transactions").child(userId);

    }

    private void loadTransactions() {
        transactionsRef.orderByChild("date").limitToLast(10).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();
                transactions.clear();
                List<com.bank.bayan.Transaction> tempList = new ArrayList<>();

                for (DataSnapshot txnSnapshot : snapshot.getChildren()) {
                    com.bank.bayan.Transaction transaction = txnSnapshot.getValue(com.bank.bayan.Transaction.class);
                    if (transaction != null) {
                        transaction.setId(txnSnapshot.getKey());
                        tempList.add(transaction);

                        Transaction modelTransaction = new Transaction();
                        modelTransaction.setAmount(transaction.getAmount());
                        modelTransaction.setType(transaction.getType());
                        modelTransaction.setDescription(transaction.getDescription());
                        modelTransaction.setDate(transaction.getDate());
                        transactions.add(modelTransaction);
                    }
                }

                Collections.reverse(tempList);
                transactionList.addAll(tempList);

                runOnUiThread(() -> {
                    calculateMonthlyStats();
                    updateFinancialUI();
                    transactionAdapter.notifyDataSetChanged();

                    if (currentState == ConversationState.LOADING_DATA) {
                        startWelcomeMessage();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                speak("خطأ في تحميل المعاملات");
                Toast.makeText(HomeActivity.this, "خطأ في تحميل المعاملات: " + error.getMessage(), Toast.LENGTH_SHORT).show();

                runOnUiThread(() -> {
                    calculateMonthlyStats();
                    updateFinancialUI();
                    if (currentState == ConversationState.LOADING_DATA) {
                        startWelcomeMessage();
                    }
                });
            }
        });
    }
    private void setupClickListeners() {
        findViewById(R.id.main_layout).setOnLongClickListener(v -> {
            if (currentState != ConversationState.LOADING_DATA) {
                currentState = ConversationState.MAIN_MENU;
                startMainMenu();
            }
            return true;
        });

        quickTransferLayout.setOnClickListener(v -> {
            speak("انتقال إلى صفحة التحويل السريع");
            Intent intent = new Intent(this, TransactionActivity.class);
            startActivity(intent);
        });

        quickBillLayout.setOnClickListener(v -> {
            speak("انتقال الى صفحة المدفوعات وسداد الفواتير");
            Intent intent = new Intent(this, BillPaymentActivity.class);
            startActivity(intent);
        });

        addUserLayout.setOnClickListener(v -> {
            speak("انتقال إلى صفحة إضافة مفوض جديد");
            Intent intent = new Intent(this, AuthorizedPerson.class);
            startActivity(intent);
        });

        viewSummaryLayout.setOnClickListener(v -> {
            announceAccountSummary();
        });

        balanceTextView.setOnClickListener(v -> {
            announceBalance();
        });

        incomeTextView.setOnClickListener(v -> {
            announceIncome();
        });

        expensesTextView.setOnClickListener(v -> {
            announceExpenses();
        });
    }

    private void loadUserData() {
        currentState = ConversationState.LOADING_DATA;
        speak("جاري تحميل بيانات حسابك...");

        firebaseHelper.getUserData(currentUserId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                if (user.getAccount() != null) {
                    currentBalance = user.getAccount().getBalance();
                    runOnUiThread(() -> {
                        updateUserUI();
                        loadTransactions();
                    });
                } else {
                    createDefaultAccount();
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
                    speakAndListen("حدث خطأ في تحميل البيانات: " + error + ". هل تريد المحاولة مرة أخرى؟");
                    currentState = ConversationState.LOADING_DATA;
                });
            }
        });
    }

    private void createDefaultAccount() {
        Account defaultAccount = new Account();
        defaultAccount.setBalance(0.0);
        defaultAccount.setAccountNumber("0000 0000 0000 0000");
        defaultAccount.setAccountType("حساب جاري");
        defaultAccount.setActive(true);
        defaultAccount.setLastUpdated(System.currentTimeMillis());

        if (currentUser == null) {
            currentUser = new User();
            currentUser.setName("مستخدم جديد");
            currentUser.setUserId(currentUserId);
            currentUser.setCreatedAt(System.currentTimeMillis());
        }

        currentUser.setAccount(defaultAccount);
        currentBalance = 0.0;

        runOnUiThread(() -> {
            updateUserUI();
            loadTransactions();
        });
    }

    private void calculateMonthlyStats() {
        monthlyIncome = 0.0;
        monthlyExpenses = 0.0;

        Calendar currentMonth = Calendar.getInstance();
        int month = currentMonth.get(Calendar.MONTH);
        int year = currentMonth.get(Calendar.YEAR);

        if (transactions.isEmpty()) {
            return;
        }

        for (Transaction transaction : transactions) {
            if (transaction.getType().equals("income")) {
                monthlyIncome += transaction.getAmount();
            } else if (transaction.getType().equals("expense")) {
                monthlyExpenses += transaction.getAmount();
            }
        }
    }

    private void updateUserUI() {
        if (currentUser != null) {
            userNameTextView.setText("مرحباً " + currentUser.getName());
            balanceTextView.setText(decimalFormat.format(currentBalance));
        }
    }

    private void updateFinancialUI() {
        incomeTextView.setText(decimalFormat.format(monthlyIncome));
        expensesTextView.setText(decimalFormat.format(monthlyExpenses));
    }

    private void startWelcomeMessage() {
        currentState = ConversationState.MAIN_MENU;

        String welcomeMessage = "أهلاً وسهلاً " + (currentUser != null ? currentUser.getName() : "بك") +
                " في بنك بيان. " +
                "رصيدك الحالي " + decimalFormat.format(currentBalance) + " ريال سعودي. ";

        if (monthlyIncome > 0) {
            welcomeMessage += "إيداعاتك هذا الشهر " + decimalFormat.format(monthlyIncome) + " ريال. ";
        }

        if (monthlyExpenses > 0) {
            welcomeMessage += "مصروفاتك هذا الشهر " + decimalFormat.format(monthlyExpenses) + " ريال. ";
        }

        welcomeMessage += "اضغط مطولاً على الشاشة لسماع الخيارات المتاحة، أو انقر على أي خيار للوصول إليه مباشرة.";

        speak(welcomeMessage);
    }

    private void startMainMenu() {
        String menuMessage = "الخيارات المتاحة: " +
                "واحد: تحويل سريع، " +
                "اثنين: تسديد فاتورة، " +
                "ثلاثة: إضافة مفوض جديد، " +
                "أربعة: عرض الرصيد، " +
                "خمسة: كشف الحساب، " +
                "ستة: تحليل المصروفات، " +
                "سبعة: المدفوعات، "+
                "ثمانية: الخدمات. "+
                "قل رقم الخيار أو اسم الخدمة";

        currentState = ConversationState.LISTENING_FOR_COMMAND;
        speakAndListen(menuMessage);
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

            case LISTENING_FOR_COMMAND:
                handleMainMenuCommand(spokenText);
                break;

            case ACCOUNT_DETAILS:
                handleAccountDetailsCommand(spokenText);
                break;
        }
    }

    private void handleMainMenuCommand(String spokenText) {
        if (spokenText.contains("واحد") || spokenText.contains("١") ||
                spokenText.contains("تحويل") || spokenText.contains("حوالة")) {
            navigateToTransfer();
        } else if (spokenText.contains("اثنين") || spokenText.contains("٢") ||
                spokenText.contains("فاتورة") || spokenText.contains("فواتير")) {
            navigateToBillPayment();
        } else if (spokenText.contains("ثلاثة") || spokenText.contains("٣") ||
                spokenText.contains("مفوض") || spokenText.contains("إضافة")) {
            navigateToAddUser();
        } else if (spokenText.contains("أربعة") || spokenText.contains("٤") ||
                spokenText.contains("رصيد") || spokenText.contains("عرض")) {
            announceAccountDetails();
        } else if (spokenText.contains("خمسة") || spokenText.contains("٥") ||
                spokenText.contains("كشف") || spokenText.contains("حساب")) {
            announceAccountStatement();
        } else if (spokenText.contains("ستة") || spokenText.contains("٦") ||
                spokenText.contains("تحليل") || spokenText.contains("مصروفات")) {
            announceExpensesAnalysis();
        } else if (spokenText.contains("سبعة") || spokenText.contains("٧") ||
                spokenText.contains("مدفوعات") || spokenText.contains("سداد")){
            navigateToBillPayment();
        }
        else if(spokenText.contains("ثمانية") || spokenText.contains("٨") ||
                    spokenText.contains("خدمات") || spokenText.contains("مساعدة")){
            announceServices();
        }
        else if (spokenText.contains("رجوع") || spokenText.contains("إلغاء")) {
            currentState = ConversationState.MAIN_MENU;
            speak("تم الرجوع للقائمة الرئيسية");
        } else {
            speakAndListen("لم أفهم طلبك. قل رقم من واحد إلى ثمانية، أو اسم الخدمة التي تريدها");
        }
    }

    private void announceServices() {
        speak("انتقال الى صفحة الخدمات");
        Intent intent = new Intent(this, ServicesActivity.class);
        startActivity(intent);
    }

    private void handleAccountDetailsCommand(String spokenText) {
        if (spokenText.contains("رجوع") || spokenText.contains("قائمة") ||
                spokenText.contains("خيارات")) {
            currentState = ConversationState.MAIN_MENU;
            startMainMenu();
        } else if (spokenText.contains("إعادة") || spokenText.contains("مرة")) {
            announceAccountDetails();
        } else {
            speakAndListen("قل رجوع للعودة للقائمة الرئيسية، أو إعادة لسماع البيانات مرة أخرى");
        }
    }

    private void navigateToTransfer() {
        speak("انتقال إلى صفحة التحويل السريع");
        Intent intent = new Intent(this, TransactionActivity.class);
        startActivity(intent);
    }

    private void navigateToBillPayment() {
        speak("انتقال الى صفحة المدفوعات وسداد الفواتير");
        Intent intent = new Intent(this, BillPaymentActivity.class);
        startActivity(intent);
    }

    private void navigateToAddUser() {
        speak("صفحة إضافة مفوض جديد ستكون متاحة قريباً");
        currentState = ConversationState.MAIN_MENU;
    }

    private void announceBalance() {
        String message = "رصيدك الحالي " + decimalFormat.format(currentBalance) + " ريال سعودي";
        speak(message);
    }

    private void announceIncome() {
        String message = "إيداعاتك هذا الشهر " + decimalFormat.format(monthlyIncome) + " ريال سعودي";
        speak(message);
    }

    private void announceExpenses() {
        String message = "مصروفاتك هذا الشهر " + decimalFormat.format(monthlyExpenses) + " ريال سعودي";
        speak(message);
    }

    private void announceAccountDetails() {
        currentState = ConversationState.ACCOUNT_DETAILS;

        String accountDetails = "تفاصيل حسابك: " +
                "الرصيد الحالي " + decimalFormat.format(currentBalance) + " ريال سعودي. ";

        if (currentUser != null && currentUser.getAccount() != null) {
            accountDetails += "رقم الحساب " + currentUser.getAccount().getAccountNumber() + ". " +
                    "نوع الحساب " + currentUser.getAccount().getAccountType() + ". ";
        }

        accountDetails += "إيداعاتك هذا الشهر " + decimalFormat.format(monthlyIncome) + " ريال. " +
                "مصروفاتك هذا الشهر " + decimalFormat.format(monthlyExpenses) + " ريال. " +
                "قل رجوع للعودة للقائمة الرئيسية";

        speakAndListen(accountDetails);
    }

    private void announceAccountSummary() {
        announceAccountDetails();
    }

    private void announceAccountStatement() {
        String statement = "كشف الحساب: ";

        if (transactions.isEmpty()) {
            statement += "لا توجد معاملات مسجلة حالياً. ";
        } else {
            statement += "لديك " + transactions.size() + " معاملة. ";

            int count = Math.min(3, transactions.size());
            for (int i = 0; i < count; i++) {
                Transaction tx = transactions.get(i);
                statement += "معاملة " + (i + 1) + ": ";
                if (tx.getType().equals("income")) {
                    statement += "إيداع ";
                } else {
                    statement += "سحب ";
                }
                statement += decimalFormat.format(tx.getAmount()) + " ريال " +
                        tx.getDescription() + ". ";
            }
        }

        statement += "قل رجوع للعودة للقائمة الرئيسية";
        speakAndListen(statement);
    }

    private void announceExpensesAnalysis() {
        String analysis = "تحليل المصروفات: ";

        if (monthlyExpenses == 0) {
            analysis += "لا توجد مصروفات مسجلة هذا الشهر. ";
        } else {
            analysis += "إجمالي مصروفاتك هذا الشهر " + decimalFormat.format(monthlyExpenses) + " ريال. ";

            if (currentBalance > 0) {
                double percentage = (monthlyExpenses / currentBalance) * 100;
                analysis += "هذا يمثل " + String.format("%.1f", percentage) + " بالمائة من رصيدك الحالي. ";
            }
        }

        analysis += "قل رجوع للعودة للقائمة الرئيسية";
        speakAndListen(analysis);
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
        String message = "لم أسمع صوتك بوضوح. حاول مرة ثانية";
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
    protected void onResume() {
        super.onResume();
        if (currentState != ConversationState.LOADING_DATA) {
            loadUserData();
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

    @Override
    public void onBackPressed() {
        if (currentState == ConversationState.LISTENING_FOR_COMMAND ||
                currentState == ConversationState.ACCOUNT_DETAILS) {
            currentState = ConversationState.MAIN_MENU;
            speak("تم الرجوع للقائمة الرئيسية");
        } else {
            super.onBackPressed();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopListeningAndSpeaking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListeningAndSpeaking();
    }


    private void stopListeningAndSpeaking() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }
}