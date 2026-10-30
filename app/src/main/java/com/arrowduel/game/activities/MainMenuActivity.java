package com.arrowduel.game.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;

public class MainMenuActivity extends BaseActivity {

    private TextView tvCoins;
    private TextView tvGems;
    private TextView tvPlayerName;
    private PreferencesManager prefs;
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);

        tvCoins = findViewById(R.id.tvCoins);
        tvGems = findViewById(R.id.tvGems);
        tvPlayerName = findViewById(R.id.tvPlayerName);

        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnMultiplayer = findViewById(R.id.btnMultiplayer);
        Button btnShop = findViewById(R.id.btnShop);
        Button btnCustomize = findViewById(R.id.btnCustomize);

        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        Button btnFriends = findViewById(R.id.btnFriends);
        Button btnMissions = findViewById(R.id.btnMissions);
        Button btnDailyRewards = findViewById(R.id.btnDailyRewards);
        Button btnMoreGames = findViewById(R.id.btnMoreGames);

        btnPlay.setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, ModeSelectActivity.class));
        });

        btnMultiplayer.setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, MultiplayerActivity.class));
        });

        btnShop.setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, ShopActivity.class));
        });

        btnCustomize.setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, CharacterActivity.class));
        });

        btnDailyRewards.setOnClickListener(v -> {
            soundManager.playButtonClick();
            startActivity(new Intent(this, DailyRewardsActivity.class));
        });

        btnSettings.setOnClickListener(v -> showSettingsDialog());

        btnLeaderboard.setOnClickListener(v -> {
            soundManager.playButtonClick();
            showLeaderboardDialog();
        });

        btnFriends.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Toast.makeText(this, "Friends List: 4 Online Archers", Toast.LENGTH_SHORT).show();
        });

        btnMissions.setOnClickListener(v -> {
            soundManager.playButtonClick();
            showMissionsDialog();
        });

        btnMoreGames.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Toast.makeText(this, "Check out more exciting Archer games soon!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvCoins.setText(String.valueOf(prefs.getCoins()));
        tvGems.setText(String.valueOf(prefs.getGems()));
        tvPlayerName.setText(prefs.getPlayerName());
    }

    private AlertDialog settingsDialog;
    private AlertDialog subDialog;

    private void showSettingsDialog() {
        soundManager.playButtonClick();
        if (settingsDialog != null && settingsDialog.isShowing()) {
            try { settingsDialog.dismiss(); } catch (Exception ignored) {}
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        int currentFps = prefs.getTargetFps();
        String currentSide = prefs.getJoystickSide();
        String currentScale = prefs.getControlScale();

        String[] options = {
            "Sound SFX: " + (soundManager.isSoundEnabled() ? "ON" : "OFF"),
            "Vibration: " + (soundManager.isVibrationEnabled() ? "ON" : "OFF"),
            "Frame Rate: " + currentFps + " FPS",
            "Customize Control: " + ("RIGHT".equals(currentSide) ? "Joystick Right / Fire Left" : "Joystick Left / Fire Right"),
            "Button Size: " + currentScale,
            "Change Player Name"
        };
        builder.setItems(options, (dialog, which) -> {
            soundManager.playButtonClick();
            dialog.dismiss();
            if (which == 0) {
                soundManager.setSoundEnabled(!soundManager.isSoundEnabled());
                Toast.makeText(this, "Sound: " + (soundManager.isSoundEnabled() ? "Enabled" : "Muted"), Toast.LENGTH_SHORT).show();
                showSettingsDialog();
            } else if (which == 1) {
                soundManager.setVibrationEnabled(!soundManager.isVibrationEnabled());
                Toast.makeText(this, "Vibration: " + (soundManager.isVibrationEnabled() ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
                showSettingsDialog();
            } else if (which == 2) {
                // Toggle between 60 FPS and 120 FPS directly
                int newFps = (currentFps == 120) ? 60 : 120;
                prefs.setTargetFps(newFps);
                Toast.makeText(this, "Frame Rate: " + newFps + " FPS", Toast.LENGTH_SHORT).show();
                showSettingsDialog();
            } else if (which == 3) {
                showControlPositionDialog();
            } else if (which == 4) {
                showControlSizeDialog();
            } else if (which == 5) {
                showNameChangeDialog();
            }
        });
        builder.setNegativeButton("Close", null);
        settingsDialog = builder.create();
        settingsDialog.show();
    }

    private void showControlPositionDialog() {
        soundManager.playButtonClick();
        if (subDialog != null && subDialog.isShowing()) {
            try { subDialog.dismiss(); } catch (Exception ignored) {}
        }
        String[] options = {
            "Joystick Right, Fire Left (Default)",
            "Joystick Left, Fire Right"
        };
        int currentIdx = "LEFT".equals(prefs.getJoystickSide()) ? 1 : 0;
        subDialog = new AlertDialog.Builder(this)
                .setTitle("Control Position")
                .setSingleChoiceItems(options, currentIdx, (dialog, which) -> {
                    soundManager.playButtonClick();
                    String side = (which == 1) ? "LEFT" : "RIGHT";
                    prefs.setJoystickSide(side);
                    Toast.makeText(this, "Control set to: " + (which == 1 ? "Joystick Left" : "Joystick Right"), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    showSettingsDialog();
                })
                .setNegativeButton("Cancel", (d, w) -> showSettingsDialog())
                .create();
        subDialog.show();
    }

    private void showControlSizeDialog() {
        soundManager.playButtonClick();
        if (subDialog != null && subDialog.isShowing()) {
            try { subDialog.dismiss(); } catch (Exception ignored) {}
        }
        String[] options = {
            "Small (Compact - Enemy clearly visible)",
            "Medium (Standard)",
            "Large (Spacious)"
        };
        String currentScale = prefs.getControlScale();
        int currentIdx = "SMALL".equals(currentScale) ? 0 : ("LARGE".equals(currentScale) ? 2 : 1);
        subDialog = new AlertDialog.Builder(this)
                .setTitle("Button Size")
                .setSingleChoiceItems(options, currentIdx, (dialog, which) -> {
                    soundManager.playButtonClick();
                    String scale = (which == 0) ? "SMALL" : ((which == 2) ? "LARGE" : "MEDIUM");
                    prefs.setControlScale(scale);
                    Toast.makeText(this, "Button Size set to: " + scale, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    showSettingsDialog();
                })
                .setNegativeButton("Cancel", (d, w) -> showSettingsDialog())
                .create();
        subDialog.show();
    }

    private void showNameChangeDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(prefs.getPlayerName());
        new AlertDialog.Builder(this)
                .setTitle("Enter Player Name")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        prefs.setPlayerName(newName);
                        tvPlayerName.setText(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLeaderboardDialog() {
        final float density = getResources().getDisplayMetrics().density;
        boolean bn = prefs.isBangla();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int)(20 * density), (int)(18 * density), (int)(20 * density), (int)(16 * density));
        layout.setBackgroundResource(R.drawable.bg_dialog_premium);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Header Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(bn ? "🏆 গ্লোবাল লিডারবোর্ড 🏆" : "🏆 ARROW DUEL LEADERBOARD 🏆");
        tvTitle.setTextColor(Color.parseColor("#FFD700"));
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(bn ? "শীর্ষস্থানীয় মাস্টার আর্চারবৃন্দ" : "Top Ranked Archer Masters");
        tvSub.setTextColor(Color.parseColor("#80D8FF"));
        tvSub.setTextSize(11.5f);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, 0, 0, (int)(12 * density));
        layout.addView(tvSub);

        // Player entries
        String[][] players = new String[][]{
                {"🥇", "ShadowMaster", "9,420 Pts", "#2A2000", "#FFD700", "#FFE082"},
                {"🥈", prefs.getPlayerName() + " (You)", "4,850 Pts", "#0D253A", "#00E5FF", "#80D8FF"},
                {"🥉", "ArcherKing", "4,120 Pts", "#281710", "#CD7F32", "#FFCCBC"},
                {"🏹", "DarkSniper", "3,900 Pts", "#121A2D", "#2E4166", "#E0E6ED"},
                {"🎯", "DragonEye", "3,250 Pts", "#121A2D", "#2E4166", "#E0E6ED"}
        };

        for (int i = 0; i < players.length; i++) {
            String badge = players[i][0];
            String name = players[i][1];
            String score = players[i][2];
            int bgColor = Color.parseColor(players[i][3]);
            int strokeColor = Color.parseColor(players[i][4]);
            int textColor = Color.parseColor(players[i][5]);

            LinearLayout rowCard = new LinearLayout(this);
            rowCard.setOrientation(LinearLayout.HORIZONTAL);
            rowCard.setGravity(Gravity.CENTER);
            rowCard.setPadding((int)(12 * density), (int)(8 * density), (int)(12 * density), (int)(8 * density));

            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(10 * density);
            gd.setColor(bgColor);
            gd.setStroke((int)(1.8f * density), strokeColor);
            rowCard.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int)(4 * density);
            lp.bottomMargin = (int)(4 * density);
            rowCard.setLayoutParams(lp);

            TextView tvBadge = new TextView(this);
            tvBadge.setText(badge + " #" + (i + 1));
            tvBadge.setTextSize(13f);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setTextColor(strokeColor);
            rowCard.addView(tvBadge);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(13.5f);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(textColor);
            tvName.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams pName = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(pName);
            rowCard.addView(tvName);

            TextView tvScore = new TextView(this);
            tvScore.setText(score);
            tvScore.setTextSize(13f);
            tvScore.setTypeface(null, Typeface.BOLD);
            tvScore.setTextColor(strokeColor);
            rowCard.addView(tvScore);

            layout.addView(rowCard);
        }

        // Close button
        TextView btnClose = new TextView(this);
        btnClose.setText(bn ? "বুঝেছি (OK)" : "CLOSE (OK)");
        btnClose.setTextSize(13.5f);
        btnClose.setTypeface(null, Typeface.BOLD);
        btnClose.setTextColor(Color.parseColor("#0A0E1A"));
        btnClose.setBackgroundResource(R.drawable.bg_play_btn);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setPadding(0, (int)(10 * density), 0, (int)(10 * density));
        LinearLayout.LayoutParams pClose = new LinearLayout.LayoutParams(
                (int)(180 * density), (int)(40 * density));
        pClose.topMargin = (int)(12 * density);
        btnClose.setLayoutParams(pClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnClose);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(460 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showMissionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Daily Missions")
                .setMessage("1. Score 3 Headshots [2/3] - Reward: 150 Coins\n2. Win 1 Duel with Lightning Bow [1/1] - Completed!\n3. Defeat Boss Dark Lord [0/1] - Reward: 20 Gems")
                .setPositiveButton("Claim All", (d, w) -> {
                    prefs.addCoins(100);
                    tvCoins.setText(String.valueOf(prefs.getCoins()));
                    Toast.makeText(this, "Claimed 100 Coins!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
