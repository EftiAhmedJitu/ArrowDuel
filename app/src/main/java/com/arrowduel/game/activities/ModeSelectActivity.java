package com.arrowduel.game.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.GameMode;

public class ModeSelectActivity extends BaseActivity {

    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);

        soundManager = SoundManager.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 1. Story Mode -> StoryMapActivity
        findViewById(R.id.cardStoryMode).setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, StoryMapActivity.class));
        });

        // 2. Survival Mode -> GameActivity with SURVIVAL
        findViewById(R.id.cardSurvivalMode).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("MODE", GameMode.SURVIVAL.name());
            intent.putExtra("LEVEL", 1);
            startActivity(intent);
        });

        // 3. Multiplayer -> MultiplayerActivity
        findViewById(R.id.cardMultiplayer).setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, MultiplayerActivity.class));
        });

        // 4. Boss Fight -> GameActivity with BOSS_FIGHT
        findViewById(R.id.cardBossFight).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("MODE", GameMode.BOSS_FIGHT.name());
            intent.putExtra("LEVEL", 1);
            startActivity(intent);
        });

        // 5. Practice Mode -> GameActivity with PRACTICE
        findViewById(R.id.cardPractice).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("MODE", GameMode.PRACTICE.name());
            intent.putExtra("LEVEL", 1);
            startActivity(intent);
        });

        // 6. Tournament
        findViewById(R.id.cardTournament).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Toast.makeText(this, "Tournament unlocked at Level 20!", Toast.LENGTH_SHORT).show();
        });
    }
}
