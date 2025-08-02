package com.bank.bayan.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class TransactionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private DatabaseHelper databaseHelper;
    private RecyclerView favoritesRecyclerView;
    private RecyclerView transferListRecyclerView;
    private FavoritesAdapter favoritesAdapter;
    private TransferAdapter transferAdapter;
    private List<Contact> favoritesList;
    private List<Contact> transferList;

    private boolean isListening = false;
    private boolean isReadingNames = false;
    private boolean isSpeaking = false;
    private int currentReadingIndex = 0;
    private Contact selectedContact = null;
    private ImageView backButton;
    private TextView addButton;

    private Runnable pendingListeningTask = null;

    private List<Contact> allContacts = new ArrayList<>();
    private final HashMap<String, Runnable> utteranceCallbacks = new HashMap<>();
    private String lastReadContactKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        initializeViews();
        loadDataFromDatabase();

        textToSpeech = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        databaseHelper = new DatabaseHelper(this);


        setupRecyclerViews();
        setupAccessibility();
        setupSpeechRecognition();
        setupTextToSpeechListener();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_transactions);

    }

    private void initializeViews() {
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        transferListRecyclerView = findViewById(R.id.transferListRecyclerView);

        backButton = findViewById(R.id.backButton);
        addButton = findViewById(R.id.addButton);

        backButton.setOnClickListener(v -> {
            stopAllSpeechAndListening();
            speak("رجوع");
            finish();
        });

        addButton.setOnClickListener(v -> {
            stopAllSpeechAndListening();
            speak("إضافة جهة اتصال جديدة");
        });

        findViewById(R.id.mainLayout).setOnClickListener(v -> {
            if (isReadingNames) {
                stopReadingNames();
                speakAndListen("تم إيقاف القراءة. قل اسم المستفيد الذي تريد التحويل له");
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
        if (favoritesList == null) favoritesList = new ArrayList<>();
        if (transferList == null) transferList = new ArrayList<>();

        favoritesList.clear();
        transferList.clear();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("contacts").child("-OWWLat4qAv0iBj5I0GQ");


        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    Contact contact = contactSnapshot.getValue(Contact.class);
                    if (contact != null) {
                        if (contact.isFavorite()) {
                            favoritesList.add(contact);
                        } else {
                            transferList.add(contact);
                        }
                    }
                }

                runOnUiThread(() -> {
                    favoritesAdapter.notifyDataSetChanged();
                    transferAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransactionActivity.this, "فشل تحميل البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSampleData() {
        databaseHelper.addContact(new Contact("نورة علي الغامدي", "1234567890123456", 5000.0, true));
        databaseHelper.addContact(new Contact("فاطمة محمد الزهراني", "2345678901234567", 3340.55, true));
        databaseHelper.addContact(new Contact("إيلاف آل عبدالله", "1092873619731234", 7500.0, false));
        databaseHelper.addContact(new Contact("حلا المبارك", "3456789012345678", 2100.25, false));
        databaseHelper.addContact(new Contact("فيّ الحربي", "4567890123456789", 9800.75, false));
    }

    private void setupRecyclerViews() {
        if (favoritesList == null) {
            favoritesList = new ArrayList<>();
        }

        if (transferList == null) {
            transferList = new ArrayList<>();
        }

        favoritesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoritesAdapter = new FavoritesAdapter(favoritesList, this::onFavoriteClick);
        favoritesRecyclerView.setAdapter(favoritesAdapter);

        transferListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transferAdapter = new TransferAdapter(transferList, this::onTransferClick);
        transferListRecyclerView.setAdapter(transferAdapter);
    }


    private void setupAccessibility() {
        findViewById(R.id.mainLayout).setContentDescription("صفحة التحويل الرئيسية");
        findViewById(R.id.favoritesTitle).setContentDescription("المفضلون");
        findViewById(R.id.transferTitle).setContentDescription("تحويل إلى");
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
            if (selectedContact != null) {
                confirmTransfer(selectedContact);
                return;
            } else {
                // إذا لم يكن هناك جهة اتصال محددة، ابدأ قراءة الأسماء
                startReadingNames();
                return;
            }
        }

        if (spokenText.contains("لا") || spokenText.contains("غير صحيح") || spokenText.contains("لأ")) {
            if (selectedContact != null) {
                selectedContact = null; // إلغاء الاختيار
                speak("حسناً، قل اسم المستفيد مرة أخرى");
            } else {
                speak("حسناً، قل اسم المستفيد الذي تريد التحويل له");
            }
            startListening();
            return;
        }

        if (spokenText.contains("اقرأ") || spokenText.contains("قائمة") || spokenText.contains("أسماء") || spokenText.contains("قراءة")) {
            startReadingNames();
            return;
        }

//        // Search for matching contact
//        Contact matchedContact = findBestMatch(spokenText);
//        if (matchedContact != null) {
//            selectedContact = matchedContact;
//            // إزالة قراءة رقم الحساب
//            String confirmMessage = "هل تريد التحويل ل " + matchedContact.getName() + "؟";
//            speakAndListen(confirmMessage);
//        } else {
//            speakAndListen("لم أجد اسماً مطابقاً. قل الاسم مرة أخرى، أو قل نعم لقراءة قائمة الأسماء");
//        }
        findBestMatch(spokenText);
        return;
    }

    private void findBestMatch(String spokenName) {
        String userId = "-OWWLat4qAv0iBj5I0GQ";
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("contacts").child(userId);

        spokenName = spokenName.trim().toLowerCase();

        String finalSpokenName = spokenName;
        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Contact matchedContact = null;
                int bestScore = -1;

                for (DataSnapshot contactSnap : snapshot.getChildren()) {
                    Contact contact = contactSnap.getValue(Contact.class);
                    if (contact != null) {
                        String contactName = contact.getName().toLowerCase();

                        int score = similarityScore(finalSpokenName, contactName);

                        if (score > bestScore) {
                            bestScore = score;
                            matchedContact = contact;
                        } else if (score == bestScore && matchedContact != null) {

                        }
                    }
                }

                if (matchedContact != null && bestScore > 0) {
                    selectedContact = matchedContact;
                    speakAndListen("هل تريد التحويل إلى " + matchedContact.getName() + "؟");
                } else {
                    Contact fallbackContact = null;

                    for (DataSnapshot contactSnap : snapshot.getChildren()) {
                        Contact contact = contactSnap.getValue(Contact.class);
                        if (contact != null) {
                            String contactName = contact.getName().toLowerCase();

                            if (contactName.contains(finalSpokenName)) {
                                fallbackContact = contact;
                                break;
                            }

                            String[] contactParts = contactName.split(" ");
                            String[] spokenParts = finalSpokenName.split(" ");

                            outerLoop:
                            for (String partSpoken : spokenParts) {
                                for (String partContact : contactParts) {
                                    if (partContact.startsWith(partSpoken) && partSpoken.length() > 2) {
                                        fallbackContact = contact;
                                        break outerLoop;
                                    }
                                }
                            }
                        }
                    }

                    if (fallbackContact != null) {
                        selectedContact = fallbackContact;
                        speakAndListen("هل تريد التحويل إلى " + fallbackContact.getName() + "؟");
                    } else {
                        speakAndListen("لم أجد اسماً مطابقاً. قل الاسم مرة أخرى، أو قل نعم لقراءة قائمة الأسماء");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                speakAndListen("حدث خطأ أثناء البحث عن الاسم. حاول مرة أخرى.");
            }
        });
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

    private void startReadingNames() {
        isReadingNames = true;
        currentReadingIndex = 0;
        speakWithCallback("سأقرأ لك قائمة الأسماء المحفوظة. اضغط على الشاشة لإيقاف القراءة في أي وقت",
                () -> readNextName());
    }


    private void startReadingNamesFromFirebase() {
        isReadingNames = true;
        lastReadContactKey = null;
        readNextName();
    }

    private void readNextName() {
        if (!isReadingNames) return;

        String userId = "-OWWLat4qAv0iBj5I0GQ";
        DatabaseReference contactsRef = FirebaseDatabase.getInstance()
                .getReference("contacts")
                .child(userId);

        Query query;
        if (lastReadContactKey == null) {
            query = contactsRef.orderByKey().limitToFirst(1);
        } else {
            query = contactsRef.orderByKey().startAfter(lastReadContactKey).limitToFirst(1);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isReadingNames) return;

                if (snapshot.exists()) {
                    for (DataSnapshot contactSnap : snapshot.getChildren()) {
                        lastReadContactKey = contactSnap.getKey();
                        Contact contact = contactSnap.getValue(Contact.class);
                        if (contact != null) {
                            speakWithCallback(contact.getName(), () -> {
                                if (isReadingNames) {
                                    readNextName();
                                }
                            });
                            return;
                        }
                    }
                } else {
                    // انتهت القائمة
                    isReadingNames = false;
                    speakAndListen("انتهت قائمة الأسماء. قل اسم المستفيد الذي تريد التحويل له");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isReadingNames = false;
                speakAndListen("حدث خطأ أثناء تحميل الأسماء. حاول مرة أخرى");
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

    private void confirmTransfer(Contact contact) {
        findViewById(R.id.mainLayout).postDelayed(() -> {
            Intent intent = new Intent(this, TransferDetailsActivity.class);
            intent.putExtra("contact_name", contact.getName());
            intent.putExtra("account_number", contact.getAccountNumber());
            intent.putExtra("current_balance", contact.getBalance());
            startActivity(intent);
        }, 2000);
    }

    private void onFavoriteClick(Contact contact) {
        stopAllSpeechAndListening();
        selectedContact = contact;
        String message = "تم اختيار " + contact.getName() + " من المفضلين. هل تريد المتابعة؟";
        speakAndListen(message);
    }

    private void onTransferClick(Contact contact) {
        stopAllSpeechAndListening();
        selectedContact = contact;
        String message = "تم اختيار " + contact.getName() + "هل تريد المتابعة للتحويل؟";
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
            } else  {
                speakAndListen("مرحباً بك في صفحة التحويلات. قل اسم المستفيد الذي تريد التحويل له، أو قل نعم لقراءة قائمة الأسماء المحفوظة");
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

    public static class Contact {
        private String name;
        private String accountNumber;
        private double balance;
        private double amount;
        private String iban;
        private boolean favorite;

        public void setName(String name) {
            this.name = name;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getIban() {
            return iban;
        }

        public void setIban(String iban) {
            this.iban = iban;
        }

        public void setFavorite(boolean favorite) {
            this.favorite = favorite;
        }

        public Contact() {
        }

        public Contact(String name, String accountNumber, double balance, boolean favorite) {
            this.name = name;
            this.accountNumber = accountNumber;
            this.balance = balance;
            this.favorite = favorite;
        }

        public String getName() { return name; }
        public String getAccountNumber() { return accountNumber; }
        public double getBalance() { return balance; }
        public boolean isFavorite() { return favorite; }
    }
}
