package com.bank.bayan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StartActivity extends AppCompatActivity {

    private ImageView logo, aboveImage, bottomImage;
    private TextView titleText, subtitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        startMasterAnimationSequence();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startExitSequence();
        }, 4500);
    }

    private void initViews() {
        logo = findViewById(R.id.logo);
        aboveImage = findViewById(R.id.above);
        bottomImage = findViewById(R.id.back);
        subtitleText = findViewById(R.id.subtitle_text);
    }

    private void startMasterAnimationSequence() {
        // Phase 1: Background and Environment Setup (0-500ms)
        Animation backgroundAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        findViewById(R.id.main).startAnimation(backgroundAnim);

        // Phase 2: Top Image with Dramatic Entry (300ms)
        new Handler().postDelayed(() -> {
            Animation topAnim = AnimationUtils.loadAnimation(this, R.anim.slide_down_bounce);
            aboveImage.startAnimation(topAnim);
            aboveImage.setAlpha(1f);
        }, 300);

        // Phase 3: Bottom Image with Synchronized Entry (500ms)
        new Handler().postDelayed(() -> {
            Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_bounce);
            bottomImage.startAnimation(bottomAnim);
            bottomImage.setAlpha(1f);
        }, 500);

        // Phase 4: Logo with 3D Rotation Effect (800ms)
        new Handler().postDelayed(() -> {
            start3DLogoAnimation();
        }, 800);

//        new Handler().postDelayed(() -> {
//            startTitleAnimation();
//        }, 1500);

        // Phase 6: Subtitle with Typewriter Effect (2200ms)
        new Handler().postDelayed(() -> {
            startAdvancedTypingAnimation();
        }, 2200);

        // Phase 7: Logo Breathing Effect (3000ms)
        new Handler().postDelayed(() -> {
            startLogoBreathingEffect();
        }, 4000);
    }

    private void start3DLogoAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.0f, 1.2f, 1.0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(logo, "rotation", -180f, 360f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);

        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(scaleX, scaleY, rotation, alpha);
        logoSet.setDuration(1200);
        logoSet.start();

        logo.setAlpha(1f);
    }

//    private void startTitleAnimation() {
//        // Advanced slide animation with overshoot
//        titleText.setAlpha(0f);
//        titleText.setTranslationX(300f);
//
//        titleText.animate()
//                .alpha(1f)
//                .translationX(0f)
//                .setDuration(800)
//                .setInterpolator(new android.view.animation.OvershootInterpolator())
//                .start();
//    }

    private void startAdvancedTypingAnimation() {
        String fullText = "نسمعك، نراك ،ونيسر لك خدماتك المالية";
        subtitleText.setText("");
        subtitleText.setAlpha(1f);

        subtitleText.setAlpha(0f);
        subtitleText.animate().alpha(1f).setDuration(500).start();

        Handler handler = new Handler();
        for (int i = 0; i <= fullText.length(); i++) {
            final int index = i;
            int delay = i * 80;

            handler.postDelayed(() -> {
                if (index < fullText.length()) {
                    subtitleText.setText(fullText.substring(0, index + 1));

                    subtitleText.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(50)
                            .withEndAction(() -> {
                                subtitleText.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(50)
                                        .start();
                            })
                            .start();
                }
            }, delay);
        }
    }


    private void startLogoBreathingEffect() {
        ObjectAnimator breatheX = ObjectAnimator.ofFloat(logo, "scaleX", 1.0f, 1.05f);
        breatheX.setDuration(2000);
        breatheX.setRepeatCount(ValueAnimator.INFINITE);
        breatheX.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator breatheY = ObjectAnimator.ofFloat(logo, "scaleY", 1.0f, 1.05f);
        breatheY.setDuration(2000);
        breatheY.setRepeatCount(ValueAnimator.INFINITE);
        breatheY.setRepeatMode(ValueAnimator.REVERSE);

        breatheX.start();
        breatheY.start();
    }


    private void startExitSequence() {
        ObjectAnimator logoFade = ObjectAnimator.ofFloat(logo, "alpha", 1f, 0f);
        ObjectAnimator titleFade = ObjectAnimator.ofFloat(titleText, "alpha", 1f, 0f);
        ObjectAnimator subtitleSlide = ObjectAnimator.ofFloat(subtitleText, "translationY", 0f, 100f);
        ObjectAnimator subtitleFade = ObjectAnimator.ofFloat(subtitleText, "alpha", 1f, 0f);

        AnimatorSet exitSet = new AnimatorSet();
        exitSet.playTogether(logoFade, titleFade, subtitleSlide, subtitleFade);
        exitSet.setDuration(600);

        exitSet.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {}

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
                finish();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });

        exitSet.start();
    }
}