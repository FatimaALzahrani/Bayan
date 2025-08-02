package com.bank.bayan.bill;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bank.bayan.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class BillPaymentActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private RecyclerView billTypesRecyclerView;
    private RecyclerView recentBillsRecyclerView;
    private BillTypesAdapter billTypesAdapter;
    private RecentBillsAdapter recentBillsAdapter;
    private List<BillType> billTypesList;
    private List<Bill> recentBillsList;

    private boolean isListening = false;
    private boolean isReadingNames = false;
    private boolean isSpeaking = false;
    private int currentReadingIndex = 0;
    private BillType selectedBillType = null;
    private Bill selectedBill = null;
    private ImageView backButton;
    private TextView addButton;

    // متغير لتتبع المهام المعلقة
    private Runnable pendingListeningTask = null;

    private final HashMap<String, Runnable> utteranceCallbacks = new HashMap<>();
    private String lastReadBillKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_payment);

        initializeViews();
        loadDataFromDatabase();

        // Initialize components
        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        setupRecyclerViews();
        setupAccessibility();
        setupSpeechRecognition();
        setupTextToSpeechListener();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_transfers);
    }

    private void initializeViews() {
        billTypesRecyclerView = findViewById(R.id.billTypesRecyclerView);
        recentBillsRecyclerView = findViewById(R.id.recentBillsRecyclerView);

        backButton = findViewById(R.id.backButton);
        addButton = findViewById(R.id.addButton);

        backButton.setOnClickListener(v -> {
            stopAllSpeechAndListening();
            speak("رجوع");
            finish();
        });

        addButton.setOnClickListener(v -> {
            stopAllSpeechAndListening();
            speak("إضافة فاتورة جديدة");
        });

        // Screen tap listener for interrupting name reading
        findViewById(R.id.mainLayout).setOnClickListener(v -> {
            if (isReadingNames) {
                stopReadingNames();
                speakAndListen("تم إيقاف القراءة. قل نوع الفاتورة أو اسم الجهة التي تريد سداد فاتورتها");
            }
        });
    }

    private void setupTextToSpeechListener() {
        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    isSpeaking = true;
                    if (isListening) {
                        speechRecognizer.stopListening();
                        isListening = false;
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    isSpeaking = false;

                    Runnable callback = utteranceCallbacks.remove(utteranceId);
                    if (callback != null) {
                        runOnUiThread(callback);
                    } else if (pendingListeningTask != null) {
                        runOnUiThread(() -> {
                            pendingListeningTask.run();
                            pendingListeningTask = null;
                        });
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    isSpeaking = false;
                    Runnable callback = utteranceCallbacks.remove(utteranceId);
                    if (callback != null) {
                        runOnUiThread(callback);
                    }
                }
            });
        }
    }

    private void loadDataFromDatabase() {
        if (billTypesList == null) billTypesList = new ArrayList<>();
        if (recentBillsList == null) recentBillsList = new ArrayList<>();

        billTypesList.clear();
        recentBillsList.clear();

        loadBillTypes();

        loadRecentBills();
    }

    private void loadBillTypes() {
        billTypesList.add(new BillType("الكهرباء", "الشركة السعودية للكهرباء", R.drawable.ic_electricity));
        billTypesList.add(new BillType("المياه", "شركة المياه الوطنية", R.drawable.ic_water));
        billTypesList.add(new BillType("الهاتف", "شركة الاتصالات السعودية", R.drawable.ic_phone));
        billTypesList.add(new BillType("الإنترنت", "شركة الاتصالات", R.drawable.ic_internet));
        billTypesList.add(new BillType("الغاز", "شركة الغاز والتصنيع", R.drawable.ic_gas));
        billTypesList.add(new BillType("التأمين", "شركات التأمين", R.drawable.baseline_shield_24));

        runOnUiThread(() -> {
            if (billTypesAdapter != null) {
                billTypesAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadRecentBills() {
        String userId = "-OWWLat4qAv0iBj5I0GQ";
        DatabaseReference billsRef = FirebaseDatabase.getInstance().getReference("bills").child(userId);

        billsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot billSnapshot : snapshot.getChildren()) {
                    Bill bill = billSnapshot.getValue(Bill.class);
                    if (bill != null) {
                        recentBillsList.add(bill);
                    }
                }

                runOnUiThread(() -> {
                    if (recentBillsAdapter != null) {
                        recentBillsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BillPaymentActivity.this, "فشل تحميل الفواتير", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerViews() {
        if (billTypesList == null) {
            billTypesList = new ArrayList<>();
        }

        if (recentBillsList == null) {
            recentBillsList = new ArrayList<>();
        }

        billTypesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        billTypesAdapter = new BillTypesAdapter(billTypesList, this::onBillTypeClick);
        billTypesRecyclerView.setAdapter(billTypesAdapter);

        recentBillsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentBillsAdapter = new RecentBillsAdapter(recentBillsList, this::onRecentBillClick);
        recentBillsRecyclerView.setAdapter(recentBillsAdapter);
    }


    private void setupAccessibility() {
        findViewById(R.id.mainLayout).setContentDescription("صفحة سداد الفواتير الرئيسية");
        findViewById(R.id.billTypesTitle).setContentDescription("أنواع الفواتير");
        findViewById(R.id.recentBillsTitle).setContentDescription("الفواتير الأخيرة");
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

        if (spokenText.contains("نعم") || spokenText.contains("أكد") || spokenText.contains("موافق")) {
            if (selectedBillType != null) {
                proceedToBillDetails(selectedBillType);
                return;
            } else if (selectedBill != null) {
                proceedToPayBill(selectedBill);
                return;
            } else {
                startReadingBillTypes();
                return;
            }
        }

        if (spokenText.contains("لا") || spokenText.contains("غير صحيح") || spokenText.contains("لأ")) {
            selectedBillType = null;
            selectedBill = null;
            speak("حسناً، قل نوع الفاتورة أو اسم الجهة التي تريد سداد فاتورتها");
            startListening();
            return;
        }

        if (spokenText.contains("اقرأ") || spokenText.contains("قائمة") || spokenText.contains("أسماء") || spokenText.contains("قراءة")) {
            startReadingBillTypes();
            return;
        }

        findBestMatch(spokenText);
    }

    private void findBestMatch(String spokenText) {
        spokenText = spokenText.trim().toLowerCase();

        BillType matchedBillType = null;
        int bestTypeScore = -1;

        for (BillType billType : billTypesList) {
            String typeName = billType.getName().toLowerCase();
            String companyName = billType.getCompany().toLowerCase();

            int typeScore = similarityScore(spokenText, typeName);
            int companyScore = similarityScore(spokenText, companyName);
            int maxScore = Math.max(typeScore, companyScore);

            if (maxScore > bestTypeScore) {
                bestTypeScore = maxScore;
                matchedBillType = billType;
            }

            // فحص تطابق جزئي
            if (typeName.contains(spokenText) || companyName.contains(spokenText) ||
                    spokenText.contains(typeName) || spokenText.contains(companyName)) {
                matchedBillType = billType;
                bestTypeScore = Math.max(bestTypeScore, spokenText.length());
            }
        }

        Bill matchedBill = null;
        int bestBillScore = -1;

        for (Bill bill : recentBillsList) {
            String billName = bill.getServiceProvider().toLowerCase();
            int billScore = similarityScore(spokenText, billName);

            if (billScore > bestBillScore) {
                bestBillScore = billScore;
                matchedBill = bill;
            }

            if (billName.contains(spokenText) || spokenText.contains(billName)) {
                matchedBill = bill;
                bestBillScore = Math.max(bestBillScore, spokenText.length());
            }
        }

        if (bestTypeScore > bestBillScore && matchedBillType != null && bestTypeScore > 0) {
            selectedBillType = matchedBillType;
            speakAndListen("هل تريد سداد فاتورة " + matchedBillType.getName() + " من " + matchedBillType.getCompany() + "؟");
        } else if (matchedBill != null && bestBillScore > 0) {
            selectedBill = matchedBill;
            speakAndListen("هل تريد سداد فاتورة " + matchedBill.getServiceProvider() + " بمبلغ " + matchedBill.getAmount() + " ريال؟");
        } else {
            speakAndListen("لم أجد فاتورة مطابقة. قل نوع الفاتورة مثل الكهرباء أو المياه، أو قل نعم لقراءة قائمة الأنواع المتاحة");
        }
    }

    private int similarityScore(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        int score = 0;

        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) == b.charAt(i)) {
                score++;
            } else {
                break;
            }
        }

        return score;
    }

    private void startReadingBillTypes() {
        isReadingNames = true;
        currentReadingIndex = 0;
        speakWithCallback("سأقرأ لك أنواع الفواتير المتاحة. اضغط على الشاشة لإيقاف القراءة في أي وقت",
                () -> readNextBillType());
    }

    private void readNextBillType() {
        if (!isReadingNames || currentReadingIndex >= billTypesList.size()) {
            isReadingNames = false;
            speakAndListen("انتهت قائمة أنواع الفواتير. قل نوع الفاتورة التي تريد سدادها");
            return;
        }

        BillType billType = billTypesList.get(currentReadingIndex);
        currentReadingIndex++;

        speakWithCallback(billType.getName() + " من " + billType.getCompany(), () -> {
            if (isReadingNames) {
                readNextBillType();
            }
        });
    }

    private void stopReadingNames() {
        isReadingNames = false;
        stopAllSpeechAndListening();
    }

    private void stopAllSpeechAndListening() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
        isSpeaking = false;
        pendingListeningTask = null;
    }

    private void proceedToBillDetails(BillType billType) {
        Intent intent = new Intent(this, BillDetailsActivity.class);
        intent.putExtra("bill_type", billType.getName());
        intent.putExtra("company_name", billType.getCompany());
        intent.putExtra("icon_resource", billType.getIconResource());
        startActivity(intent);
    }

    private void proceedToPayBill(Bill bill) {
        Intent intent = new Intent(this, BillPaymentDetailsActivity.class);
        intent.putExtra("bill_id", bill.getId());
        intent.putExtra("service_provider", bill.getServiceProvider());
        intent.putExtra("account_number", bill.getAccountNumber());
        intent.putExtra("amount", bill.getAmount());
        intent.putExtra("due_date", bill.getDueDate());
        startActivity(intent);
    }

    private void onBillTypeClick(BillType billType) {
        stopAllSpeechAndListening();
        selectedBillType = billType;
        String message = "تم اختيار " + billType.getName() + " من " + billType.getCompany() + ". هل تريد المتابعة؟";
        speakAndListen(message);
    }

    private void onRecentBillClick(Bill bill) {
        stopAllSpeechAndListening();
        selectedBill = bill;
        String message = "تم اختيار فاتورة " + bill.getServiceProvider() + " بمبلغ " + bill.getAmount() + " ريال. هل تريد سدادها؟";
        speakAndListen(message);
    }

    private void speakAndListen(String text) {
        pendingListeningTask = this::startListening;
        speak(text);
    }

    private void speakWithCallback(String text, Runnable callback) {
        if (textToSpeech != null) {
            String utteranceId = "utt_" + System.currentTimeMillis();
            utteranceCallbacks.put(utteranceId, callback);

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId);
        }
    }

    private void startListening() {
        if (!isListening && !isSpeaking) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.startListening(intent);
        }
    }

    private void handleSpeechError(int error) {
        String message = "حدث خطأ في التعرف على الصوت. حاول مرة أخرى";
        speakAndListen(message);
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            String utteranceId = "utterance_" + System.currentTimeMillis();
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
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
                speakAndListen("مرحباً بك في صفحة سداد الفواتير. قل نوع الفاتورة التي تريد سدادها، مثل الكهرباء أو المياه، أو قل نعم لقراءة قائمة الأنواع المتاحة");
            }
            setupTextToSpeechListener();
        }
    }

    @Override
    protected void onDestroy() {
        stopAllSpeechAndListening();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    public static class BillType {
        private String name;
        private String company;
        private int iconResource;

        public BillType() {}

        public BillType(String name, String company, int iconResource) {
            this.name = name;
            this.company = company;
            this.iconResource = iconResource;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }

        public int getIconResource() { return iconResource; }
        public void setIconResource(int iconResource) { this.iconResource = iconResource; }
    }

}