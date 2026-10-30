package com.arrowduel.game.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;

public class SplashActivity extends BaseActivity {

    private ProgressBar progressBar;
    private TextView tvLoadingStage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        tvLoadingStage = findViewById(R.id.tvLoadingStage);

        // 4-Second smooth loading animation
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(4000);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            progressBar.setProgress(progress);

            if (progress < 25) {
                tvLoadingStage.setText("INITIALIZING BATTLE ENGINE... " + progress + "%");
            } else if (progress < 50) {
                tvLoadingStage.setText("FORGING WEAPONS & ARROWS... " + progress + "%");
            } else if (progress < 80) {
                tvLoadingStage.setText("SYNCHRONIZING COMBAT ARENA... " + progress + "%");
            } else {
                tvLoadingStage.setText("READY! ENTER THE DUEL! " + progress + "%");
            }

            if (progress >= 100) {
                startActivity(new Intent(SplashActivity.this, MainMenuActivity.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        animator.start();
    }
}
