package com.bank.bayan;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VoiceLoginActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "VoiceLoginActivity";

    private TextView voiceFeedbackText;
    private ImageView voiceIcon;

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private DatabaseReference databaseReference;
    private DatabaseReference usersReference;

    private enum AppState {
        INITIAL_QUESTION,
        LOGIN_IDENTIFY,
        REGISTER_IDENTIFY,
        PROCESSING
    }

    private AppState currentState = AppState.INITIAL_QUESTION;
    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private String userIdentifier = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_login);

        initializeViews();
        initializeFirebase();
        initializeSpeechComponents();
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        usersReference = databaseReference.child("users");

        Log.d(TAG, "Firebase initialized successfully");
    }

    private void initializeViews() {
        voiceFeedbackText = findViewById(R.id.voice_feedback_text);
        voiceIcon = findViewById(R.id.voice_icon);

        voiceFeedbackText.setText("مرحباً بك في بيان");
    }

    private void initializeSpeechComponents() {
        textToSpeech = new TextToSpeech(this, this);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
                voiceFeedbackText.setText("أستمع إليك الآن...");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech started");
                voiceFeedbackText.setText("يتم التسجيل...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // يمكن استخدام هذا لإظهار مستوى الصوت
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
                voiceFeedbackText.setText("يتم معالجة ما قلته...");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                Log.e(TAG, "Speech recognition error: " + errorMessage);
                voiceFeedbackText.setText("خطأ في التعرف على الصوت: " + errorMessage);

                voiceIcon.postDelayed(() -> {
                    if (currentState != AppState.PROCESSING) {
                        startListening();
                    }
                }, 2000);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    Log.d(TAG, "Recognized speech: " + spokenText);
                    processSpeechResult(spokenText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("ar"));

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Arabic language not supported");
                textToSpeech.setLanguage(Locale.US);
            }

            startInitialConversation();
        } else {
            Log.e(TAG, "TTS initialization failed");
            Toast.makeText(this, "فشل في تهيئة التحويل النصي للصوت",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startInitialConversation() {
        currentState = AppState.INITIAL_QUESTION;
        speakText("مرحباً بك في بنك بيان. هل لديك حساب؟ قل نعم أو لا");

        textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> startListening());
            }

            @Override
            public void onError(String utteranceId) {}
        });
    }

    private void speakText(String text) {
        voiceFeedbackText.setText(text);
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_id");
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private void processSpeechResult(String spokenText) {
        voiceFeedbackText.setText("سمعت: " + spokenText);

        switch (currentState) {
            case INITIAL_QUESTION:
                handleInitialResponse(spokenText);
                break;

            case LOGIN_IDENTIFY:
                handleLoginIdentification(spokenText);
                break;

            case REGISTER_IDENTIFY:
                handleRegistrationIdentification(spokenText);
                break;

            default:
                break;
        }
    }

    private void handleInitialResponse(String response) {
        String normalizedResponse = response.trim().toLowerCase();

        if (normalizedResponse.contains("نعم") || normalizedResponse.contains("yes")) {
            currentState = AppState.LOGIN_IDENTIFY;
            loginAttempts = 0;
            speakText("من فضلك عرف عن نفسك بالاسم أو الرقم");

        } else if (normalizedResponse.contains("لا") || normalizedResponse.contains("no")) {
            currentState = AppState.REGISTER_IDENTIFY;
            speakText("لإنشاء حساب جديد، من فضلك قل اسمك أو رقمك الذي ترغب في استخدامه كمعرف صوتي");

        } else {
            speakText("لم أفهم إجابتك. من فضلك قل نعم إذا كان لديك حساب، أو لا إذا لم يكن لديك حساب");
        }
    }

    private void handleLoginIdentification(String identifier) {
        userIdentifier = identifier.trim();
        currentState = AppState.PROCESSING;


        authenticateWithFirebase(userIdentifier);
    }

    private void handleRegistrationIdentification(String identifier) {
        userIdentifier = identifier.trim();
        currentState = AppState.PROCESSING;

        createAccountInFirebase(userIdentifier);
    }

    private void authenticateWithFirebase(String identifier) {
        voiceFeedbackText.setText("يتم التحقق من هويتك...");

        usersReference.orderByChild("name").equalTo(identifier)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                String storedVoiceprint = userSnapshot.child("voiceprint_data").getValue(String.class);
                                String userId = userSnapshot.child("userId").getValue(String.class);

                                if (verifyVoiceprint(identifier, storedVoiceprint)) {
                                    runOnUiThread(() -> {
                                        speakText("مرحباً بك " + identifier + ". تم تسجيل الدخول بنجاح");

                                        voiceIcon.postDelayed(() -> {
                                            Intent intent = new Intent(VoiceLoginActivity.this, HomeActivity.class);
                                            intent.putExtra("user_name", identifier);
                                            intent.putExtra("userId", userId);
                                            startActivity(intent);
                                            finish();
                                        }, 3000);
                                    });
                                    return;
                                }
                            }

                            handleAuthenticationFailure();

                        } else if(identifier.contains("حساب") || identifier.contains("جديد") || identifier.equals("لا")) {
                            createAccountInFirebase(identifier);
                        }
                        else{
                            runOnUiThread(() -> {
                                loginAttempts++;
                                int remainingAttempts = MAX_LOGIN_ATTEMPTS - loginAttempts;

                                if (remainingAttempts > 0) {
                                    speakText("عذراً، لم يتم العثور على حساب بهذا الاسم. يرجى المحاولة مرة أخرى. تبقى لديك " +
                                            remainingAttempts + " محاولة");
                                    currentState = AppState.LOGIN_IDENTIFY;
                                } else {
                                    speakText("لقد تجاوزت الحد الأقصى للمحاولات. يرجى إعادة تشغيل التطبيق");
                                    voiceIcon.postDelayed(() -> finish(), 3000);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Firebase error: " + databaseError.getMessage());
                        runOnUiThread(() -> {
                            voiceFeedbackText.setText("خطأ في الاتصال بقاعدة البيانات");
                            Toast.makeText(VoiceLoginActivity.this,
                                    "خطأ في الاتصال بقاعدة البيانات",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void createAccountInFirebase(String identifier) {
        voiceFeedbackText.setText("يتم إنشاء حسابك...");

        usersReference.orderByChild("name").equalTo(identifier)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            runOnUiThread(() -> {
                                speakText("يوجد حساب بهذا الاسم مسبقاً. يرجى اختيار اسم آخر");
                                currentState = AppState.REGISTER_IDENTIFY;
                            });
                        } else {
                            String userId = usersReference.push().getKey();

                            if (userId != null) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", identifier);
                                userData.put("userId", userId);
                                userData.put("voiceprint_data", generateVoiceprint(identifier));
                                userData.put("created_at", System.currentTimeMillis());

                                usersReference.child(userId).setValue(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User created successfully");
                                            runOnUiThread(() -> {
                                                speakText("تم إنشاء حسابك بنجاح باسم " + identifier +
                                                        ". يمكنك الآن تسجيل الدخول باستخدام صوتك");

                                                voiceIcon.postDelayed(() -> {
                                                    Intent intent = new Intent(VoiceLoginActivity.this, HomeActivity.class);
                                                    intent.putExtra("user_name", identifier);
                                                    intent.putExtra("userId",userId);
                                                    intent.putExtra("is_new_user", true);
                                                    startActivity(intent);
                                                    finish();
                                                }, 3000);
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to create user", e);
                                            runOnUiThread(() -> {
                                                voiceFeedbackText.setText("فشل في إنشاء الحساب");
                                                Toast.makeText(VoiceLoginActivity.this,
                                                        "فشل في إنشاء الحساب",
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Firebase error: " + databaseError.getMessage());
                        runOnUiThread(() -> {
                            voiceFeedbackText.setText("خطأ في الاتصال بقاعدة البيانات");
                            Toast.makeText(VoiceLoginActivity.this,
                                    "خطأ في الاتصال بقاعدة البيانات",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private String generateVoiceprint(String identifier) {
        return identifier.toLowerCase().trim();
    }

    private boolean verifyVoiceprint(String spokenText, String storedVoiceprint) {
        String normalizedSpoken = spokenText.toLowerCase().trim();
        String normalizedStored = storedVoiceprint.toLowerCase().trim();

        return normalizedSpoken.equals(normalizedStored);
    }

    private void handleAuthenticationFailure() {
        runOnUiThread(() -> {
            loginAttempts++;
            int remainingAttempts = MAX_LOGIN_ATTEMPTS - loginAttempts;

            if (remainingAttempts > 0) {
                speakText("عذراً، لم يتم التعرف على بصمتك الصوتية. يرجى المحاولة مرة أخرى. تبقى لديك " +
                        remainingAttempts + " محاولة");
                currentState = AppState.LOGIN_IDENTIFY;
            } else {
                speakText("لقد تجاوزت الحد الأقصى للمحاولات. يرجى إعادة تشغيل التطبيق");
                voiceIcon.postDelayed(() -> finish(), 3000);
            }
        });
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "خطأ في الصوت";
            case SpeechRecognizer.ERROR_CLIENT:
                return "خطأ في العميل";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "صلاحيات غير كافية";
            case SpeechRecognizer.ERROR_NETWORK:
                return "خطأ في الشبكة";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "انتهت مهلة الشبكة";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "لم يتم العثور على تطابق";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "المتعرف مشغول";
            case SpeechRecognizer.ERROR_SERVER:
                return "خطأ في الخادم";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "انتهت مهلة الكلام";
            default:
                return "خطأ غير معروف";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentState != AppState.PROCESSING && currentState != AppState.INITIAL_QUESTION) {
            voiceIcon.postDelayed(() -> startListening(), 1000);
        }
    }
}

