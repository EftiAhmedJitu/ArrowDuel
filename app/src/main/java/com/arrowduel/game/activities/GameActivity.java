package com.arrowduel.game.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.GameView;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.GameMode;
import com.arrowduel.game.views.VirtualJoystickView;

public class GameActivity extends BaseActivity implements GameView.GameEventListener {

    private GameView gameView;
    private TextView tvGameTitle;
    private TextView tvGameCoins;
    private TextView tvPotionCount;
    private TextView tvShieldCount;
    private TextView tvLightningCount;
    private ImageButton btnShoot;
    private TextView tvAutoFireStatus;

    // Overlays
    private LinearLayout overlayPause;
    private LinearLayout overlayVictory;
    private LinearLayout overlayDefeat;
    private TextView tvRewardCoins;
    private TextView tvRewardGems;
    private ImageView vStar1, vStar2, vStar3;

    private PreferencesManager prefs;
    private SoundManager soundManager;
    private GameMode gameMode = GameMode.STORY;
    private int currentLevel = 12;

    // Hotspot & Wi-Fi Multiplayer Continuous Duel State
    private boolean isNetwork = false;
    private boolean isHost = false;
    private String opponentName = "Opponent";
    private boolean isLocalReadyForNext = false;
    private boolean isOpponentReadyForNext = false;
    private Button btnNextLevel;
    private Button btnRetry;
    private TextView tvVictoryTitle;
    private TextView tvDefeatTitle;

    // Tournament State
    private boolean isTournament = false;
    private int tournamentTotalRounds = 3;
    private int tournamentTargetWins = 2;
    private int tournamentRoundNumber = 1;
    private int playerTournamentScore = 0;
    private int opponentTournamentScore = 0;
    private boolean isTournamentFinished = false;
    private TextView tvTournamentPlayerWins;
    private TextView tvTournamentOpponentWins;
    private TextView tvTournamentScoreMiddle;
    private View hudStars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);

        String modeStr = getIntent().getStringExtra("MODE");
        if (modeStr != null) {
            try {
                gameMode = GameMode.valueOf(modeStr);
            } catch (Exception ignored) {}
        }
        currentLevel = getIntent().getIntExtra("LEVEL", 12);

        // Tournament Intent Extras
        isTournament = getIntent().getBooleanExtra("IS_TOURNAMENT", false);
        tournamentTotalRounds = getIntent().getIntExtra("TOURNAMENT_ROUNDS", 3);
        tournamentTargetWins = getIntent().getIntExtra("TOURNAMENT_TARGET_WINS", 2);
        tournamentRoundNumber = 1;
        playerTournamentScore = 0;
        opponentTournamentScore = 0;
        isTournamentFinished = false;

        gameView = findViewById(R.id.gameView);

        isNetwork = getIntent().getBooleanExtra("IS_NETWORK", false);
        isHost = getIntent().getBooleanExtra("IS_HOST", false);
        String oppName = getIntent().getStringExtra("OPPONENT_NAME");
        if (oppName != null && !oppName.trim().isEmpty()) {
            opponentName = oppName;
        }
        if (isNetwork) {
            gameView.setNetworkMatch(true, isHost, opponentName);
        }

        gameView.setGameMode(gameMode, currentLevel);
        gameView.setEventListener(this);

        tvGameTitle = findViewById(R.id.tvGameTitle);
        tvGameCoins = findViewById(R.id.tvGameCoins);
        tvPotionCount = findViewById(R.id.tvPotionCount);
        tvShieldCount = findViewById(R.id.tvShieldCount);
        tvLightningCount = findViewById(R.id.tvLightningCount);

        overlayPause = findViewById(R.id.overlayPause);
        overlayVictory = findViewById(R.id.overlayVictory);
        overlayDefeat = findViewById(R.id.overlayDefeat);
        tvVictoryTitle = findViewById(R.id.tvVictoryTitle);
        tvDefeatTitle = findViewById(R.id.tvDefeatTitle);
        tvRewardCoins = findViewById(R.id.tvRewardCoins);
        tvRewardGems = findViewById(R.id.tvRewardGems);
        vStar1 = findViewById(R.id.vStar1);
        vStar2 = findViewById(R.id.vStar2);
        vStar3 = findViewById(R.id.vStar3);
        tvTournamentPlayerWins = findViewById(R.id.tvTournamentPlayerWins);
        tvTournamentOpponentWins = findViewById(R.id.tvTournamentOpponentWins);
        tvTournamentScoreMiddle = findViewById(R.id.tvTournamentScoreMiddle);
        hudStars = findViewById(R.id.hudStars);

        setupHud();
        setupControls();
        applyControlCustomization();
        applyDisplayRefreshRate();
        updateTournamentHud();
    }

    private void updateTournamentHud() {
        if (tvGameTitle == null) return;
        boolean bn = prefs.isBangla();
        if (isTournament) {
            String title = bn
                    ? ("🏆 টুর্নামেন্ট • রাউন্ড " + tournamentRoundNumber + "/" + tournamentTotalRounds)
                    : ("🏆 TOURNAMENT • ROUND " + tournamentRoundNumber + "/" + tournamentTotalRounds);
            tvGameTitle.setText(title);

            if (tvTournamentScoreMiddle != null) {
                tvTournamentScoreMiddle.setVisibility(View.VISIBLE);
                tvTournamentScoreMiddle.setText(bn
                        ? ("স্কোর: " + playerTournamentScore + " - " + opponentTournamentScore)
                        : ("SCORE: " + playerTournamentScore + " - " + opponentTournamentScore));
            }

            if (tvTournamentPlayerWins != null) {
                tvTournamentPlayerWins.setVisibility(View.VISIBLE);
                tvTournamentPlayerWins.setText(bn
                        ? ("আপনি: " + playerTournamentScore + " জয়")
                        : ("YOU: " + playerTournamentScore + " WINS"));
            }

            if (tvTournamentOpponentWins != null) {
                tvTournamentOpponentWins.setVisibility(View.VISIBLE);
                String opp = (opponentName != null && !opponentName.isEmpty()) ? opponentName : (bn ? "প্রতিপক্ষ" : "OPPONENT");
                tvTournamentOpponentWins.setText(bn
                        ? (opp + ": " + opponentTournamentScore + " জয়")
                        : (opp + ": " + opponentTournamentScore + " WINS"));
            }

            if (hudStars != null) {
                hudStars.setVisibility(View.GONE);
            }
        } else {
            tvGameTitle.setText(gameMode == GameMode.MULTIPLAYER ? (bn ? "ডুয়েল ম্যাচ" : "DUEL MATCH") : ((bn ? "লেভেল " : "LEVEL ") + currentLevel));
            if (tvTournamentScoreMiddle != null) tvTournamentScoreMiddle.setVisibility(View.GONE);
            if (tvTournamentPlayerWins != null) tvTournamentPlayerWins.setVisibility(View.GONE);
            if (tvTournamentOpponentWins != null) tvTournamentOpponentWins.setVisibility(View.GONE);
            if (hudStars != null) hudStars.setVisibility(gameMode == GameMode.STORY ? View.VISIBLE : View.GONE);
        }
    }

    private void applyDisplayRefreshRate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                android.view.Display display = getDisplay();
                if (display != null) {
                    android.view.Display.Mode[] modes = display.getSupportedModes();
                    int targetFps = prefs.getTargetFps();
                    android.view.Display.Mode selectedMode = null;
                    if (targetFps == 120) {
                        for (android.view.Display.Mode m : modes) {
                            if (selectedMode == null || m.getRefreshRate() > selectedMode.getRefreshRate()) {
                                selectedMode = m;
                            }
                        }
                    } else {
                        // Closest to 60Hz standard mode
                        for (android.view.Display.Mode m : modes) {
                            if (Math.abs(m.getRefreshRate() - 60f) < 2f) {
                                selectedMode = m;
                                break;
                            }
                        }
                    }
                    if (selectedMode != null) {
                        android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.preferredDisplayModeId = selectedMode.getModeId();
                        getWindow().setAttributes(lp);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupHud() {
        if (gameMode == GameMode.BOSS_FIGHT) {
            tvGameTitle.setText("BOSS FIGHT");
        } else if (gameMode == GameMode.SURVIVAL) {
            tvGameTitle.setText("SURVIVAL WAVE 1");
        } else if (gameMode == GameMode.MULTIPLAYER || isNetwork) {
            tvGameTitle.setText("DUEL - LEVEL " + currentLevel);
        } else if (gameMode == GameMode.PRACTICE) {
            tvGameTitle.setText("PRACTICE RANGE");
        } else {
            tvGameTitle.setText("LEVEL " + currentLevel);
        }

        tvGameCoins.setText(String.valueOf(prefs.getCoins()));
        updatePowerupCounts();
    }

    private void updatePowerupCounts() {
        tvPotionCount.setText("x" + gameView.getPotion().getCount());
        tvShieldCount.setText("x" + gameView.getShield().getCount());
        tvLightningCount.setText("x" + gameView.getLightning().getCount());
    }

    private void setupControls() {
        // Virtual Joystick Listener (4-Way Movement)
        VirtualJoystickView joystick = findViewById(R.id.virtualJoystick);
        if (joystick != null) {
            joystick.setJoystickListener(new VirtualJoystickView.JoystickListener() {
                @Override
                public void onJoystickMoved(float xPercent, float yPercent) {
                    gameView.onJoystickMoved(xPercent, yPercent);
                }

                @Override
                public void onJoystickReleased() {
                    gameView.onJoystickReleased();
                }
            });
        }

        // Fire Button: instant shot along current aim line whenever clicked!
        btnShoot = findViewById(R.id.btnShoot);
        tvAutoFireStatus = findViewById(R.id.tvAutoFireStatus);
        if (btnShoot != null) {
            btnShoot.setOnClickListener(v -> {
                soundManager.playButtonClick();
                gameView.shootLastAim();
                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(60).start();
                }).start();
            });
            btnShoot.setOnLongClickListener(v -> {
                boolean active = gameView.toggleAutoFire();
                updateAutoFireButton(active);
                return true;
            });
        }

        // Power-ups
        findViewById(R.id.btnPowerupPotion).setOnClickListener(v -> {
            gameView.usePotion();
            updatePowerupCounts();
        });

        findViewById(R.id.btnPowerupShield).setOnClickListener(v -> {
            gameView.useShield();
            updatePowerupCounts();
        });

        findViewById(R.id.btnPowerupLightning).setOnClickListener(v -> {
            gameView.useLightningStrike();
            updatePowerupCounts();
        });

        // Pause
        Button btnPauseFps = findViewById(R.id.btnPauseFps);
        if (btnPauseFps != null) {
            btnPauseFps.setText(prefs.getTargetFps() + " FPS");
            btnPauseFps.setOnClickListener(v -> {
                soundManager.playButtonClick();
                int currentFps = prefs.getTargetFps();
                int newFps = (currentFps == 120) ? 60 : 120;
                prefs.setTargetFps(newFps);
                gameView.setTargetFps(newFps);
                btnPauseFps.setText(newFps + " FPS");
                android.widget.Toast.makeText(this, newFps + " FPS", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        findViewById(R.id.btnPause).setOnClickListener(v -> {
            soundManager.playButtonClick();
            gameView.pauseGame();
            if (btnPauseFps != null) {
                btnPauseFps.setText(prefs.getTargetFps() + " FPS");
            }
            overlayPause.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btnResume).setOnClickListener(v -> {
            soundManager.playButtonClick();
            overlayPause.setVisibility(View.GONE);
            gameView.resumeGame();
        });

        findViewById(R.id.btnPauseRestart).setOnClickListener(v -> {
            if (isNetwork) {
                android.widget.Toast.makeText(this, "Match in progress. Use Leave Match to exit.", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                restartCurrentGame();
            }
        });
        findViewById(R.id.btnPauseLeave).setOnClickListener(v -> leaveGameToMenu());

        // Victory Dialog buttons
        btnNextLevel = findViewById(R.id.btnNextLevel);
        if (btnNextLevel != null) {
            btnNextLevel.setOnClickListener(v -> {
                soundManager.playButtonClick();
                if (isTournamentFinished) {
                    leaveGameToMenu();
                    return;
                }
                if (isNetwork) {
                    requestNextRound();
                } else {
                    if (isTournament) {
                        currentLevel++;
                        handleNextRoundStart(currentLevel);
                    } else {
                        Intent intent = new Intent(this, GameActivity.class);
                        intent.putExtra("MODE", gameMode.name());
                        intent.putExtra("LEVEL", currentLevel + 1);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }

        findViewById(R.id.btnVictoryMainMenu).setOnClickListener(v -> leaveGameToMenu());

        // Defeat Dialog buttons
        btnRetry = findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                soundManager.playButtonClick();
                if (isTournamentFinished) {
                    leaveGameToMenu();
                    return;
                }
                if (isNetwork) {
                    requestNextRound();
                } else {
                    if (isTournament) {
                        currentLevel++;
                        handleNextRoundStart(currentLevel);
                    } else {
                        restartCurrentGame();
                    }
                }
            });
        }
        findViewById(R.id.btnDefeatMainMenu).setOnClickListener(v -> leaveGameToMenu());
    }

    private void updateAutoFireButton(boolean active) {
        if (btnShoot != null) {
            btnShoot.setBackgroundResource(active ? R.drawable.bg_auto_fire_active : R.drawable.bg_red_btn);
        }
        if (tvAutoFireStatus != null) {
            if (active) {
                tvAutoFireStatus.setText("AUTO: ON (2s)");
                tvAutoFireStatus.setTextColor(Color.parseColor("#00E5FF"));
            } else {
                tvAutoFireStatus.setText("AUTO FIRE");
                tvAutoFireStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }
    }

    private void restartCurrentGame() {
        soundManager.playButtonClick();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("MODE", gameMode.name());
        intent.putExtra("LEVEL", currentLevel);
        startActivity(intent);
        finish();
    }

    private void requestNextRound() {
        if (isLocalReadyForNext) return;
        isLocalReadyForNext = true;
        int nextLvl = currentLevel + 1;

        if (btnNextLevel != null) {
            btnNextLevel.setText("⏳ WAITING FOR OPPONENT...");
            btnNextLevel.setEnabled(false);
            btnNextLevel.setTextColor(Color.WHITE);
            btnNextLevel.setBackgroundResource(R.drawable.bg_waiting_btn);
        }
        if (btnRetry != null) {
            btnRetry.setText("⏳ WAITING FOR OPPONENT...");
            btnRetry.setEnabled(false);
            btnRetry.setTextColor(Color.WHITE);
            btnRetry.setBackgroundResource(R.drawable.bg_waiting_btn);
        }

        com.arrowduel.game.engine.NetworkDuelManager.getInstance().sendNextReady(nextLvl);

        // If local is Host and Opponent was ALREADY ready, Host starts the round!
        if (isHost && isOpponentReadyForNext) {
            com.arrowduel.game.engine.NetworkDuelManager.getInstance().sendNextRoundStart(nextLvl);
            handleNextRoundStart(nextLvl);
        }
    }

    @Override
    public void onOpponentNextReady(int level) {
        isOpponentReadyForNext = true;
        runOnUiThread(() -> {
            if (!isLocalReadyForNext) {
                if (btnNextLevel != null) {
                    btnNextLevel.setText("OPPONENT READY! NEXT ROUND");
                    btnNextLevel.setTextColor(Color.parseColor("#0A0E1A"));
                    btnNextLevel.setBackgroundResource(R.drawable.bg_play_btn);
                }
                if (btnRetry != null) {
                    btnRetry.setText("OPPONENT READY! NEXT ROUND");
                    btnRetry.setTextColor(Color.parseColor("#0A0E1A"));
                    btnRetry.setBackgroundResource(R.drawable.bg_play_btn);
                }
            }
        });
        // If local player already clicked Next Round, and this device is Host, start the round!
        if (isHost && isLocalReadyForNext) {
            int nextLvl = (level > currentLevel) ? level : currentLevel + 1;
            com.arrowduel.game.engine.NetworkDuelManager.getInstance().sendNextRoundStart(nextLvl);
            handleNextRoundStart(nextLvl);
        }
    }

    @Override
    public void onNextRoundStart(int level) {
        handleNextRoundStart(level);
    }

    private void handleNextRoundStart(int nextLevel) {
        currentLevel = nextLevel;
        isLocalReadyForNext = false;
        isOpponentReadyForNext = false;
        if (isTournament) {
            tournamentRoundNumber++;
        }

        runOnUiThread(() -> {
            overlayVictory.setVisibility(View.GONE);
            overlayDefeat.setVisibility(View.GONE);

            updateTournamentHud();

            if (btnNextLevel != null) {
                btnNextLevel.setText(isNetwork ? "NEXT ROUND" : "NEXT LEVEL");
                btnNextLevel.setEnabled(true);
                btnNextLevel.setTextColor(Color.parseColor("#0A0E1A"));
                btnNextLevel.setBackgroundResource(R.drawable.bg_play_btn);
            }
            if (btnRetry != null) {
                btnRetry.setText(isNetwork ? "NEXT ROUND" : "RETRY");
                btnRetry.setEnabled(true);
                btnRetry.setTextColor(Color.parseColor("#0A0E1A"));
                btnRetry.setBackgroundResource(R.drawable.bg_play_btn);
            }

            gameView.startNextRound(currentLevel);
        });
    }

    private void leaveGameToMenu() {
        soundManager.playButtonClick();
        if (isNetwork) {
            com.arrowduel.game.engine.NetworkDuelManager.getInstance().sendLeaveMatch();
            com.arrowduel.game.engine.NetworkDuelManager.getInstance().disconnect();
        }
        finish();
    }

    @Override
    public void onOpponentLeave() {
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "Opponent left the match", android.widget.Toast.LENGTH_LONG).show();
            if (btnNextLevel != null && isLocalReadyForNext) {
                btnNextLevel.setText("OPPONENT LEFT");
                btnNextLevel.setEnabled(false);
                btnNextLevel.setTextColor(Color.parseColor("#FF5555"));
                btnNextLevel.setBackgroundResource(R.drawable.bg_waiting_btn);
            }
            if (btnRetry != null && isLocalReadyForNext) {
                btnRetry.setText("OPPONENT LEFT");
                btnRetry.setEnabled(false);
                btnRetry.setTextColor(Color.parseColor("#FF5555"));
                btnRetry.setBackgroundResource(R.drawable.bg_waiting_btn);
            }
        });
    }

    @Override
    public void onBackPressed() {
        leaveGameToMenu();
    }

    @Override
    public void onGameVictory(int stars, int coinsEarned, int gemsEarned) {
        updateAutoFireButton(false);
        tvRewardCoins.setText("+" + coinsEarned);
        tvRewardGems.setText("+" + gemsEarned);

        vStar1.setImageResource(stars >= 1 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        vStar2.setImageResource(stars >= 2 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        vStar3.setImageResource(stars >= 3 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);

        tvGameCoins.setText(String.valueOf(prefs.getCoins()));

        if (isTournament) {
            playerTournamentScore++;
            updateTournamentHud();
            if (playerTournamentScore >= tournamentTargetWins) {
                isTournamentFinished = true;
                if (tvVictoryTitle != null) {
                    tvVictoryTitle.setText(prefs.isBangla() ? "🏆 টুর্নামেন্ট চ্যাম্পিয়ন! 🏆" : "🏆 TOURNAMENT CHAMPION! 🏆");
                }
                tvRewardCoins.setText("+500");
                tvRewardGems.setText("+50");
                prefs.addCoins(500);
                prefs.addGems(50);
                if (btnNextLevel != null) {
                    btnNextLevel.setText(prefs.isBangla() ? "🏆 টুর্নামেন্ট শেষ (মেনু)" : "🏆 FINISH TOURNAMENT");
                    btnNextLevel.setEnabled(true);
                    btnNextLevel.setTextColor(Color.parseColor("#0A0E1A"));
                    btnNextLevel.setBackgroundResource(R.drawable.bg_play_btn);
                }
            } else {
                if (tvVictoryTitle != null) {
                    tvVictoryTitle.setText(prefs.isBangla()
                            ? ("রাউন্ড " + tournamentRoundNumber + " জয়! (" + playerTournamentScore + "-" + opponentTournamentScore + ")")
                            : ("ROUND " + tournamentRoundNumber + " WIN! (" + playerTournamentScore + "-" + opponentTournamentScore + ")"));
                }
                if (btnNextLevel != null) {
                    btnNextLevel.setText(prefs.isBangla()
                            ? ("পরের রাউন্ড " + (tournamentRoundNumber + 1) + " শুরু")
                            : ("START ROUND " + (tournamentRoundNumber + 1)));
                    btnNextLevel.setEnabled(true);
                    btnNextLevel.setTextColor(Color.parseColor("#0A0E1A"));
                    btnNextLevel.setBackgroundResource(R.drawable.bg_play_btn);
                }
            }
        } else {
            if (btnNextLevel != null) {
                btnNextLevel.setText(isNetwork ? "NEXT ROUND" : "NEXT LEVEL");
                btnNextLevel.setEnabled(true);
                btnNextLevel.setTextColor(Color.parseColor("#0A0E1A"));
                btnNextLevel.setBackgroundResource(R.drawable.bg_play_btn);
            }
        }
        overlayVictory.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGameDefeat() {
        updateAutoFireButton(false);
        if (isTournament) {
            opponentTournamentScore++;
            updateTournamentHud();
            if (opponentTournamentScore >= tournamentTargetWins) {
                isTournamentFinished = true;
                if (tvDefeatTitle != null) {
                    tvDefeatTitle.setText(prefs.isBangla() ? "টুর্নামেন্ট সমাপ্ত (রানার-আপ)" : "TOURNAMENT RUNNER-UP");
                }
                if (btnRetry != null) {
                    btnRetry.setText(prefs.isBangla() ? "মেনুতে ফিরুন" : "MAIN MENU");
                    btnRetry.setEnabled(true);
                    btnRetry.setTextColor(Color.parseColor("#0A0E1A"));
                    btnRetry.setBackgroundResource(R.drawable.bg_play_btn);
                }
            } else {
                if (tvDefeatTitle != null) {
                    tvDefeatTitle.setText(prefs.isBangla()
                            ? ("রাউন্ড " + tournamentRoundNumber + " পরাজয় (" + playerTournamentScore + "-" + opponentTournamentScore + ")")
                            : ("ROUND " + tournamentRoundNumber + " LOSS (" + playerTournamentScore + "-" + opponentTournamentScore + ")"));
                }
                if (btnRetry != null) {
                    btnRetry.setText(prefs.isBangla()
                            ? ("পরের রাউন্ড " + (tournamentRoundNumber + 1) + " খেলুন")
                            : ("PLAY ROUND " + (tournamentRoundNumber + 1)));
                    btnRetry.setEnabled(true);
                    btnRetry.setTextColor(Color.parseColor("#0A0E1A"));
                    btnRetry.setBackgroundResource(R.drawable.bg_play_btn);
                }
            }
        } else {
            if (btnRetry != null) {
                btnRetry.setText(isNetwork ? "NEXT ROUND" : "RETRY");
                btnRetry.setEnabled(true);
                btnRetry.setTextColor(Color.parseColor("#0A0E1A"));
                btnRetry.setBackgroundResource(R.drawable.bg_play_btn);
            }
        }
        overlayDefeat.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPlayerHpChanged(int currentHp, int maxHp) {}

    @Override
    public void onEnemyHpChanged(int currentHp, int maxHp) {}

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyControlCustomization();
        if (gameView != null && overlayPause.getVisibility() != View.VISIBLE) {
            gameView.resumeGame();
        }
    }

    private void applyControlCustomization() {
        String side = prefs.getJoystickSide();
        float scale = prefs.getControlScaleFactor();

        if (gameView != null) {
            gameView.setJoystickSide(side);
        }

        VirtualJoystickView joystick = findViewById(R.id.virtualJoystick);
        View layoutShootBtn = findViewById(R.id.layoutShootBtn);

        float density = getResources().getDisplayMetrics().density;

        if (joystick != null) {
            joystick.setScaleMultiplier(scale);
            android.view.ViewGroup.LayoutParams rawLp = joystick.getLayoutParams();
            if (rawLp instanceof android.widget.RelativeLayout.LayoutParams) {
                android.widget.RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) rawLp;
                lp.width = (int) (180 * density * scale);
                lp.height = (int) (150 * density * scale);

                if ("LEFT".equalsIgnoreCase(side)) {
                    lp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
                    lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                    lp.setMarginStart((int) (12 * density));
                    lp.setMarginEnd(0);
                } else {
                    lp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                    lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
                    lp.setMarginEnd((int) (12 * density));
                    lp.setMarginStart(0);
                }
                joystick.setLayoutParams(lp);
            }
        }

        if (layoutShootBtn != null) {
            android.view.ViewGroup.LayoutParams rawLp = layoutShootBtn.getLayoutParams();
            if (rawLp instanceof android.widget.RelativeLayout.LayoutParams) {
                android.widget.RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) rawLp;
                if ("LEFT".equalsIgnoreCase(side)) {
                    lp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                    lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
                    lp.setMarginEnd((int) (28 * density));
                    lp.setMarginStart(0);
                } else {
                    lp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
                    lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                    lp.setMarginStart((int) (28 * density));
                    lp.setMarginEnd(0);
                }
                layoutShootBtn.setLayoutParams(lp);
            }
        }

        if (btnShoot != null) {
            android.view.ViewGroup.LayoutParams lp = btnShoot.getLayoutParams();
            if (lp != null) {
                lp.width = (int) (66 * density * scale);
                lp.height = (int) (66 * density * scale);
                btnShoot.setLayoutParams(lp);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isNetwork) {
            com.arrowduel.game.engine.NetworkDuelManager.getInstance().disconnect();
        }
    }
}
