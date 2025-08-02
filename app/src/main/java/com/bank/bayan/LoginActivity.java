package com.bank.bayan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
    }

    public void toVoice(View view) {
        Intent intent = new Intent(LoginActivity.this,VoiceLoginActivity.class);
        startActivity(intent);
    }

    public void future(View view) {
        Toast.makeText(this,"ستُتاح ميزة تسجيل الدخول عبر هيئة رعاية الأشخاص ذوي الاعاقة في الإصدار القادم",Toast.LENGTH_LONG).show();
    }
}