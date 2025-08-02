package com.bank.bayan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private ImageView logo, aboveImage, bottomImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        startAdvancedAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startExitAnimation();
        }, 2500);
    }

    private void initViews() {
        logo = findViewById(R.id.logo);
        aboveImage = findViewById(R.id.above);
        bottomImage = findViewById(R.id.back);
    }

    private void startAdvancedAnimations() {
        Animation backgroundFade = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        findViewById(R.id.main).startAnimation(backgroundFade);

        new Handler().postDelayed(() -> {
            Animation topImageAnim = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade);
            aboveImage.startAnimation(topImageAnim);
            aboveImage.setAlpha(1f);
        }, 300);

        new Handler().postDelayed(() -> {
            Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_entrance);
            logo.startAnimation(logoAnimation);
            logo.setAlpha(1f);

            new Handler().postDelayed(() -> {
                Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
                logo.startAnimation(pulseAnim);
            }, 1500);
        }, 800);

        new Handler().postDelayed(() -> {
            Animation bottomImageAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
            bottomImage.startAnimation(bottomImageAnim);
            bottomImage.setAlpha(1f);
        }, 1200);
    }

    private void startExitAnimation() {
        logo.clearAnimation();

        Animation zoomOut = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        zoomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = new Intent(MainActivity.this, StartActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        findViewById(R.id.main).startAnimation(zoomOut);
    }
}
