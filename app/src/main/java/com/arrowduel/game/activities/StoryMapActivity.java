package com.arrowduel.game.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.GameMode;

public class StoryMapActivity extends BaseActivity {

    private GridLayout levelGrid;
    private TextView tvTotalStars;
    private PreferencesManager prefs;
    private SoundManager soundManager;
    private int currentTabStart = 1;
    private Typeface gameFont;
    private Button[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_map);

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);

        try {
            gameFont = ResourcesCompat.getFont(this, R.font.game_font);
        } catch (Exception ignored) {}

        levelGrid = findViewById(R.id.levelGrid);
        tvTotalStars = findViewById(R.id.tvTotalStars);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tabs = new Button[]{
                findViewById(R.id.tab1),
                findViewById(R.id.tab2),
                findViewById(R.id.tab3),
                findViewById(R.id.tab4),
                findViewById(R.id.tab5)
        };

        findViewById(R.id.tab1).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.tab2).setOnClickListener(v -> switchTab(11));
        findViewById(R.id.tab3).setOnClickListener(v -> switchTab(21));
        findViewById(R.id.tab4).setOnClickListener(v -> switchTab(31));
        findViewById(R.id.tab5).setOnClickListener(v -> switchTab(41));

        loadLevels();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLevels();
    }

    private void switchTab(int start) {
        soundManager.playButtonClick();
        currentTabStart = start;
        updateTabStyles();
        loadLevels();
    }

    private void updateTabStyles() {
        int activeIdx = (currentTabStart - 1) / 10;
        for (int i = 0; i < tabs.length; i++) {
            if (i == activeIdx) {
                tabs[i].setBackgroundResource(R.drawable.bg_blue_btn);
                tabs[i].setTextColor(Color.parseColor("#0A0E17"));
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_dark_btn);
                tabs[i].setTextColor(Color.parseColor("#E0E6ED"));
            }
        }
    }

    private void loadLevels() {
        levelGrid.removeAllViews();
        int maxUnlocked = prefs.getMaxStoryLevel();
        int totalStars = 0;

        for (int i = 1; i <= 100; i++) {
            totalStars += prefs.getLevelStars(i);
        }
        tvTotalStars.setText(totalStars + "/30");

        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < 10; i++) {
            final int levelNum = currentTabStart + i;
            boolean isUnlocked = levelNum <= maxUnlocked;
            int stars = prefs.getLevelStars(levelNum);

            LinearLayout node = new LinearLayout(this);
            node.setOrientation(LinearLayout.VERTICAL);
            node.setGravity(Gravity.CENTER);
            node.setBackgroundResource(R.drawable.bg_neon_card);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (112 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int margin = (int) (6 * density);
            params.setMargins(margin, margin, margin, margin);
            node.setLayoutParams(params);
            int pad = (int) (8 * density);
            node.setPadding(pad, pad, pad, pad);

            // Level Number or Lock Icon
            TextView tvNum = new TextView(this);
            tvNum.setText(isUnlocked ? String.valueOf(levelNum) : "🔒");
            tvNum.setTextSize(22);
            tvNum.setTextColor(isUnlocked ? Color.parseColor("#00E5FF") : Color.parseColor("#667788"));
            if (gameFont != null) tvNum.setTypeface(gameFont, Typeface.BOLD);
            tvNum.setGravity(Gravity.CENTER);
            node.addView(tvNum);

            // Map Area Name Subtitle (Medium, clear, gaming font!)
            TextView tvMapName = new TextView(this);
            String[] mapNames = {
                "Boral River", "Sajek Valley", "Sundarbans", "Lalbagh Fort", "Cox's Bazar", "Jaflong",
                "Sreemangal", "Saint Martin's", "Nilgiri", "Mahasthangarh", "Kaptai Lake", "Kuakata",
                "Bichanakandi", "Tanguar Haor", "Somapura", "Inani Beach"
            };
            String mName = mapNames[(levelNum - 1) % mapNames.length];
            tvMapName.setText(isUnlocked ? mName : "Locked");
            tvMapName.setTextSize(14); // MEDIUM SIZE - HIGH READABILITY!
            tvMapName.setTextColor(isUnlocked ? Color.WHITE : Color.parseColor("#556677"));
            if (gameFont != null) tvMapName.setTypeface(gameFont, Typeface.BOLD);
            tvMapName.setGravity(Gravity.CENTER);
            tvMapName.setPadding(0, (int) (3 * density), 0, (int) (4 * density));
            node.addView(tvMapName);

            // Stars Row
            LinearLayout starsLayout = new LinearLayout(this);
            starsLayout.setOrientation(LinearLayout.HORIZONTAL);
            starsLayout.setGravity(Gravity.CENTER);

            int starSize = (int) (18 * density);
            for (int s = 1; s <= 3; s++) {
                ImageView star = new ImageView(this);
                LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(starSize, starSize);
                starParams.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
                star.setLayoutParams(starParams);
                star.setImageResource(s <= stars ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
                starsLayout.addView(star);
            }
            node.addView(starsLayout);

            node.setOnClickListener(v -> {
                if (isUnlocked) {
                    soundManager.playButtonClick();
                    Intent intent = new Intent(this, GameActivity.class);
                    intent.putExtra("MODE", GameMode.STORY.name());
                    intent.putExtra("LEVEL", levelNum);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Complete Level " + (levelNum - 1) + " first!", Toast.LENGTH_SHORT).show();
                }
            });

            levelGrid.addView(node);
        }
    }
}
