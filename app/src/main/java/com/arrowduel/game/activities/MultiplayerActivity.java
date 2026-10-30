package com.arrowduel.game.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.NetworkDuelManager;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.GameMode;

public class MultiplayerActivity extends BaseActivity implements NetworkDuelManager.NetworkEventListener {

    private TextView tvDeviceIp;
    private TextView tvStatusHeader;
    private TextView tvStatusSub;
    private ImageView ivRadar;
    private LinearLayout layoutJoinInput;
    private EditText etHostIp;
    private Button btnConnectNow;
    private Button btnCancelAction;
    private Button btnLangToggle;

    // Tournament UI Elements
    private Button btnModeQuick;
    private Button btnModeTournament;
    private LinearLayout layoutRoundSelector;
    private Button btnRound3, btnRound5, btnRound7, btnRound10;
    private TextView tvTournamentDesc;
    private TextView tvTournamentTitle;
    private TextView tvMultiplayerTitle;
    private TextView tvHotspotBanner;
    private Button btnCreateRoom, btnJoinRoom, btnQuickMatch, btnShowGuide;

    private boolean isTournament = true;
    private int selectedRounds = 3;
    private int selectedTargetWins = 2;

    private ObjectAnimator radarAnim;
    private SoundManager soundManager;
    private PreferencesManager prefs;
    private NetworkDuelManager netManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        soundManager = SoundManager.getInstance(this);
        prefs = new PreferencesManager(this);
        netManager = NetworkDuelManager.getInstance();
        netManager.setListener(this);

        tvDeviceIp = findViewById(R.id.tvDeviceIp);
        tvStatusHeader = findViewById(R.id.tvStatusHeader);
        tvStatusSub = findViewById(R.id.tvStatusSub);
        ivRadar = findViewById(R.id.ivRadar);
        layoutJoinInput = findViewById(R.id.layoutJoinInput);
        etHostIp = findViewById(R.id.etHostIp);
        btnConnectNow = findViewById(R.id.btnConnectNow);
        btnCancelAction = findViewById(R.id.btnCancelAction);
        btnLangToggle = findViewById(R.id.btnLangToggle);

        tvMultiplayerTitle = findViewById(R.id.tvMultiplayerTitle);
        tvHotspotBanner = findViewById(R.id.tvHotspotBanner);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnQuickMatch = findViewById(R.id.btnQuickMatch);
        btnShowGuide = findViewById(R.id.btnShowGuide);

        // Tournament Views
        tvTournamentTitle = findViewById(R.id.tvTournamentTitle);
        btnModeQuick = findViewById(R.id.btnModeQuick);
        btnModeTournament = findViewById(R.id.btnModeTournament);
        layoutRoundSelector = findViewById(R.id.layoutRoundSelector);
        btnRound3 = findViewById(R.id.btnRound3);
        btnRound5 = findViewById(R.id.btnRound5);
        btnRound7 = findViewById(R.id.btnRound7);
        btnRound10 = findViewById(R.id.btnRound10);
        tvTournamentDesc = findViewById(R.id.tvTournamentDesc);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Display current IP
        String localIp = NetworkDuelManager.getLocalIpAddress();
        tvDeviceIp.setText("Your IP: " + localIp);
        etHostIp.setText(localIp);

        // Radar animation
        radarAnim = ObjectAnimator.ofFloat(ivRadar, "rotation", 0f, 360f);
        radarAnim.setDuration(2500);
        radarAnim.setRepeatCount(ValueAnimator.INFINITE);
        radarAnim.setInterpolator(new LinearInterpolator());
        radarAnim.start();

        initTournamentControls();
        updateLanguageLabels();

        // Language Toggle button in Header
        btnLangToggle.setOnClickListener(v -> {
            soundManager.playButtonClick();
            boolean currentlyBangla = prefs.isBangla();
            prefs.setLanguage(currentlyBangla ? "en" : "bn");
            updateLanguageLabels();
            Toast.makeText(this, prefs.isBangla() ? "ভাষা: বাংলা" : "Language: English", Toast.LENGTH_SHORT).show();
        });

        // 1. CREATE ROOM (HOST)
        btnCreateRoom.setOnClickListener(v -> {
            soundManager.playButtonClick();
            layoutJoinInput.setVisibility(View.GONE);
            btnCancelAction.setVisibility(View.VISIBLE);

            String hostMsg = prefs.isBangla()
                    ? "রুম হোস্ট করা হয়েছে (পোর্ট " + NetworkDuelManager.PORT + ")"
                    : "ROOM HOSTED (PORT " + NetworkDuelManager.PORT + ")";
            String hostSub = prefs.isBangla()
                    ? (isTournament ? "টুর্নামেন্ট: " + selectedRounds + " রাউন্ড | প্রতিপক্ষকে 192.168.43.1 দিয়ে যুক্ত হতে বলুন" : "কুইক ডুয়েল | প্রতিপক্ষকে 192.168.43.1 দিয়ে যুক্ত হতে বলুন")
                    : (isTournament ? "Tournament: Best of " + selectedRounds + " | Tell Opponent to join via 192.168.43.1" : "Quick Duel | Tell Opponent to join via 192.168.43.1");

            tvStatusHeader.setText(hostMsg);
            tvStatusSub.setText(hostSub);

            netManager.setTournamentConfig(isTournament, selectedRounds, selectedTargetWins);
            netManager.startHosting(prefs.getPlayerName());
        });

        // 2. JOIN ROOM (CLIENT)
        btnJoinRoom.setOnClickListener(v -> {
            soundManager.playButtonClick();
            layoutJoinInput.setVisibility(View.VISIBLE);
            btnCancelAction.setVisibility(View.VISIBLE);
            tvStatusHeader.setText(prefs.isBangla() ? "হোস্টের হটস্পট আইপি দিন" : "ENTER HOST HOTSPOT IP");
            tvStatusSub.setText(prefs.isBangla() ? "হটস্পটে যুক্ত থাকলে 192.168.43.1 রেখে CONNECT চাপুন" : "Connected to Hotspot? Keep 192.168.43.1 and tap CONNECT");
            if (etHostIp.getText().toString().trim().isEmpty()) {
                etHostIp.setText("192.168.43.1");
            }
        });

        btnConnectNow.setOnClickListener(v -> {
            soundManager.playButtonClick();
            String hostIp = etHostIp.getText().toString().trim();
            if (hostIp.isEmpty()) {
                Toast.makeText(this, prefs.isBangla() ? "হোস্ট আইপি লিখুন!" : "Please enter Host IP!", Toast.LENGTH_SHORT).show();
                return;
            }
            tvStatusHeader.setText((prefs.isBangla() ? "যুক্ত হওয়া হচ্ছে: " : "CONNECTING TO ") + hostIp + "...");
            tvStatusSub.setText(prefs.isBangla() ? "দয়া করে অপেক্ষা করুন..." : "Please wait while connecting...");
            netManager.joinRoom(hostIp, prefs.getPlayerName());
        });

        // 3. OFFLINE BOT / QUICK DUEL
        btnQuickMatch.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("MODE", GameMode.MULTIPLAYER.name());
            intent.putExtra("LEVEL", 1);
            intent.putExtra("IS_HOST", true);
            intent.putExtra("OPPONENT_NAME", "Archer Bot");
            intent.putExtra("IS_TOURNAMENT", isTournament);
            intent.putExtra("TOURNAMENT_ROUNDS", selectedRounds);
            intent.putExtra("TOURNAMENT_TARGET_WINS", selectedTargetWins);
            startActivity(intent);
            finish();
        });

        // 4. CONNECTION GUIDE DIALOG (WITH BANGLA & ENGLISH TOGGLE)
        btnShowGuide.setOnClickListener(v -> {
            soundManager.playButtonClick();
            showBilingualGuideDialog();
        });

        btnCancelAction.setOnClickListener(v -> {
            soundManager.playButtonClick();
            netManager.disconnect();
            layoutJoinInput.setVisibility(View.GONE);
            btnCancelAction.setVisibility(View.GONE);
            tvStatusHeader.setText(prefs.isBangla() ? "হোস্ট বা জয়েন সিলেক্ট করুন" : "SELECT CREATE OR JOIN");
            tvStatusSub.setText(prefs.isBangla() ? "লোকাল ডুয়েল যুদ্ধের জন্য প্রস্তুত" : "Ready for local duel battle");
        });
    }

    private void initTournamentControls() {
        btnModeQuick.setOnClickListener(v -> {
            soundManager.playButtonClick();
            isTournament = false;
            updateTournamentUI();
        });

        btnModeTournament.setOnClickListener(v -> {
            soundManager.playButtonClick();
            isTournament = true;
            updateTournamentUI();
        });

        btnRound3.setOnClickListener(v -> selectRounds(3, 2));
        btnRound5.setOnClickListener(v -> selectRounds(5, 3));
        btnRound7.setOnClickListener(v -> selectRounds(7, 4));
        btnRound10.setOnClickListener(v -> selectRounds(10, 6));

        updateTournamentUI();
    }

    private void selectRounds(int rounds, int targetWins) {
        soundManager.playButtonClick();
        this.selectedRounds = rounds;
        this.selectedTargetWins = targetWins;
        this.isTournament = true;
        updateTournamentUI();
    }

    private void updateTournamentUI() {
        boolean bn = prefs.isBangla();
        if (isTournament) {
            btnModeTournament.setBackgroundResource(R.drawable.bg_tab_active_gold);
            btnModeTournament.setTextColor(android.graphics.Color.parseColor("#0A0E1A"));
            btnModeQuick.setBackgroundResource(R.drawable.bg_tab_inactive);
            btnModeQuick.setTextColor(android.graphics.Color.parseColor("#8C9EC0"));
            layoutRoundSelector.setVisibility(View.VISIBLE);

            btnRound3.setBackgroundResource(selectedRounds == 3 ? R.drawable.bg_tab_active_gold : R.drawable.bg_tab_inactive);
            btnRound3.setTextColor(selectedRounds == 3 ? android.graphics.Color.parseColor("#0A0E1A") : android.graphics.Color.WHITE);

            btnRound5.setBackgroundResource(selectedRounds == 5 ? R.drawable.bg_tab_active_gold : R.drawable.bg_tab_inactive);
            btnRound5.setTextColor(selectedRounds == 5 ? android.graphics.Color.parseColor("#0A0E1A") : android.graphics.Color.WHITE);

            btnRound7.setBackgroundResource(selectedRounds == 7 ? R.drawable.bg_tab_active_gold : R.drawable.bg_tab_inactive);
            btnRound7.setTextColor(selectedRounds == 7 ? android.graphics.Color.parseColor("#0A0E1A") : android.graphics.Color.WHITE);

            btnRound10.setBackgroundResource(selectedRounds == 10 ? R.drawable.bg_tab_active_gold : R.drawable.bg_tab_inactive);
            btnRound10.setTextColor(selectedRounds == 10 ? android.graphics.Color.parseColor("#0A0E1A") : android.graphics.Color.WHITE);

            tvTournamentDesc.setText(bn
                    ? ("🏆 টুর্নামেন্ট: " + selectedRounds + " রাউন্ডের সিরিজ (যে আগে " + selectedTargetWins + " রাউন্ড জিতবে সে চ্যাম্পিয়ন!)")
                    : ("🏆 Tournament: Best of " + selectedRounds + " Rounds (First to " + selectedTargetWins + " wins is Champion!)"));
        } else {
            btnModeQuick.setBackgroundResource(R.drawable.bg_tab_active_cyan);
            btnModeQuick.setTextColor(android.graphics.Color.parseColor("#0A0E1A"));
            btnModeTournament.setBackgroundResource(R.drawable.bg_tab_inactive);
            btnModeTournament.setTextColor(android.graphics.Color.parseColor("#8C9EC0"));
            layoutRoundSelector.setVisibility(View.GONE);
            tvTournamentDesc.setText(bn ? "⚡ একক ডুয়েল ম্যাচ (যিনি জিতবেন তিনিই জয়ী!)" : "⚡ 1v1 Single Match (Winner takes all!)");
        }
    }

    private void updateLanguageLabels() {
        boolean bn = prefs.isBangla();
        btnLangToggle.setText(bn ? "🌐 বাংলা" : "🌐 English");
        tvMultiplayerTitle.setText(bn ? "হটস্পট মাল্টিপ্লেয়ার" : "HOTSPOT MULTIPLAYER");
        btnCreateRoom.setText(bn ? "রুম তৈরি করুন (HOST)" : "CREATE ROOM (HOST)");
        btnJoinRoom.setText(bn ? "রুমে যুক্ত হোন (CLIENT)" : "JOIN ROOM (CLIENT)");
        btnQuickMatch.setText(bn ? "বট ডুয়েল (OFFLINE)" : "VS AI DUEL (OFFLINE)");
        btnShowGuide.setText(bn ? "📖 কীভাবে খেলবেন? (GUIDE)" : "📖 HOW TO PLAY? (GUIDE)");
        tvHotspotBanner.setText(bn
                ? "🔥 হটস্পট মোড: ফোন ১-এ Hotspot চালু করুন, ফোন ২ দিয়ে Wi-Fi-এ যুক্ত হোন!"
                : "🔥 Hotspot Mode: Turn on Hotspot on Phone 1, connect Phone 2 to it, then Host & Join!");
        btnConnectNow.setText(bn ? "কানেক্ট" : "CONNECT");
        btnCancelAction.setText(bn ? "বাতিল" : "CANCEL");
        tvTournamentTitle.setText(bn ? "🏆 টুর্নামেন্ট মোড" : "🏆 TOURNAMENT MODE");
        btnModeQuick.setText(bn ? "একক ডুয়েল (১v১)" : "QUICK DUEL (1v1)");
        btnModeTournament.setText(bn ? "🏆 টুর্নামেন্ট" : "🏆 TOURNAMENT");
        btnRound3.setText(bn ? "৩ রাউন্ড" : "3 ROUNDS");
        btnRound5.setText(bn ? "৫ রাউন্ড" : "5 ROUNDS");
        btnRound7.setText(bn ? "৭ রাউন্ড" : "7 ROUNDS");
        btnRound10.setText(bn ? "১০ রাউন্ড" : "10 ROUNDS");
        updateTournamentUI();
    }

    private void showBilingualGuideDialog() {
        final boolean[] showBn = new boolean[]{prefs.isBangla()};
        final float density = getResources().getDisplayMetrics().density;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int)(16 * density), (int)(16 * density), (int)(16 * density), (int)(14 * density));
        layout.setBackgroundResource(R.drawable.bg_dialog_premium);

        // Header Title
        TextView tvDialogTitle = new TextView(this);
        tvDialogTitle.setText(showBn[0] ? "📱 হটস্পট কানেকশন ও টুর্নামেন্ট গাইড" : "📱 Hotspot Connection & Tournament Guide");
        tvDialogTitle.setTextColor(android.graphics.Color.parseColor("#FFD700"));
        tvDialogTitle.setTextSize(16f);
        tvDialogTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDialogTitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvDialogTitle);

        // Top toggle row
        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(android.view.Gravity.CENTER);
        toggleRow.setPadding(0, (int)(10 * density), 0, (int)(8 * density));

        TextView btnSwitchBn = new TextView(this);
        btnSwitchBn.setText("🇧🇩 বাংলা");
        btnSwitchBn.setTextSize(13f);
        btnSwitchBn.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSwitchBn.setGravity(android.view.Gravity.CENTER);
        btnSwitchBn.setPadding(0, (int)(8 * density), 0, (int)(8 * density));
        LinearLayout.LayoutParams pBn = new LinearLayout.LayoutParams(0, (int)(40 * density), 1f);
        btnSwitchBn.setLayoutParams(pBn);

        TextView btnSwitchEn = new TextView(this);
        btnSwitchEn.setText("🇬🇧 English");
        btnSwitchEn.setTextSize(13f);
        btnSwitchEn.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSwitchEn.setGravity(android.view.Gravity.CENTER);
        btnSwitchEn.setPadding(0, (int)(8 * density), 0, (int)(8 * density));
        LinearLayout.LayoutParams pEn = new LinearLayout.LayoutParams(0, (int)(40 * density), 1f);
        pEn.setMarginStart((int)(8 * density));
        btnSwitchEn.setLayoutParams(pEn);

        toggleRow.addView(btnSwitchBn);
        toggleRow.addView(btnSwitchEn);
        layout.addView(toggleRow);

        // Guide text view inside scroll
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(220 * density)));
        sv.setPadding((int)(4 * density), (int)(6 * density), (int)(4 * density), (int)(6 * density));

        TextView tvGuideText = new TextView(this);
        tvGuideText.setTextColor(android.graphics.Color.WHITE);
        tvGuideText.setTextSize(12.5f);
        tvGuideText.setLineSpacing(4f, 1.25f);
        sv.addView(tvGuideText);
        layout.addView(sv);

        // Got it button
        TextView btnGotIt = new TextView(this);
        btnGotIt.setText(showBn[0] ? "বুঝেছি (OK)" : "GOT IT (OK)");
        btnGotIt.setTextSize(14f);
        btnGotIt.setTypeface(null, android.graphics.Typeface.BOLD);
        btnGotIt.setTextColor(android.graphics.Color.parseColor("#0A0E1A"));
        btnGotIt.setBackgroundResource(R.drawable.bg_play_btn);
        btnGotIt.setGravity(android.view.Gravity.CENTER);
        btnGotIt.setPadding(0, (int)(10 * density), 0, (int)(10 * density));
        LinearLayout.LayoutParams pOk = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(42 * density));
        pOk.topMargin = (int)(10 * density);
        btnGotIt.setLayoutParams(pOk);
        btnGotIt.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnGotIt);

        Runnable updateDialogText = () -> {
            if (showBn[0]) {
                btnSwitchBn.setBackgroundResource(R.drawable.bg_tab_active_gold);
                btnSwitchBn.setTextColor(android.graphics.Color.parseColor("#0A0E1A"));
                btnSwitchEn.setBackgroundResource(R.drawable.bg_tab_inactive);
                btnSwitchEn.setTextColor(android.graphics.Color.WHITE);

                tvDialogTitle.setText("📱 হটস্পট কানেকশন ও টুর্নামেন্ট গাইড");
                btnGotIt.setText("বুঝেছি (OK)");
                tvGuideText.setText(
                        "১. ফোন ১ (Host / হোস্ট):\n" +
                        "• ফোনের Personal Hotspot চালু করুন (মোবাইল ডেটা প্রয়োজন নেই)।\n" +
                        "• টুর্নামেন্ট বা কুইক ডুয়েল সিলেক্ট করুন (৩, ৫, ৭, বা ১০ রাউন্ড)।\n" +
                        "• এরপর 'CREATE ROOM (HOST)' চাপুন।\n\n" +
                        "২. ফোন ২ (Client / ক্লায়েন্ট):\n" +
                        "• ফোন ১-এর হটস্পটের সাথে Wi-Fi দিয়ে যুক্ত হোন।\n" +
                        "• গেমে এসে 'JOIN ROOM (CLIENT)' চাপুন।\n" +
                        "• আইপি বক্সে 192.168.43.1 রেখে 'CONNECT' চাপুন।\n\n" +
                        "৩. টুর্নামেন্ট ও লড়াইয়ের নিয়মাবলী:\n" +
                        "• প্রতি রাউন্ডে নিজের স্ক্রিনের বাম দিক থেকে ডানের প্রতিপক্ষকে তীর মারবেন!\n" +
                        "• টুর্নামেন্টে যে নির্দিষ্ট সংখ্যক রাউন্ড (যেমন ৫ রাউন্ডের মধ্যে ৩টি) আগে জিতবে, সে গ্র্যান্ড চ্যাম্পিয়ন হবে!\n" +
                        "• জয়স্টিক দিয়ে প্ল্যাটফর্মে হাঁটা এবং উপরে-নিচে লাফানো যাবে।\n" +
                        "• তীর টেনে নিশানা করে ছেড়ে দিলেই তীর ছুটবে!"
                );
            } else {
                btnSwitchEn.setBackgroundResource(R.drawable.bg_tab_active_gold);
                btnSwitchEn.setTextColor(android.graphics.Color.parseColor("#0A0E1A"));
                btnSwitchBn.setBackgroundResource(R.drawable.bg_tab_inactive);
                btnSwitchBn.setTextColor(android.graphics.Color.WHITE);

                tvDialogTitle.setText("📱 Hotspot Connection & Tournament Guide");
                btnGotIt.setText("GOT IT (OK)");
                tvGuideText.setText(
                        "1. Phone 1 (Host):\n" +
                        "• Turn on Personal Hotspot (No mobile data required).\n" +
                        "• Choose Quick Duel or Tournament (3, 5, 7, or 10 Rounds).\n" +
                        "• Tap 'CREATE ROOM (HOST)'.\n\n" +
                        "2. Phone 2 (Client):\n" +
                        "• Connect to Phone 1's Hotspot via Wi-Fi.\n" +
                        "• Open Arrow Duel and tap 'JOIN ROOM (CLIENT)'.\n" +
                        "• Keep Host IP 192.168.43.1 and tap 'CONNECT'.\n\n" +
                        "3. Tournament & Battle Rules:\n" +
                        "• Shoot arrows from your left platform towards opponent on the right!\n" +
                        "• In Tournament mode, the first archer to reach target round wins is crowned Grand Champion!\n" +
                        "• Use virtual joystick to move and jump between multi-tier platforms.\n" +
                        "• Drag bow to aim and release to fire!"
                );
            }
        };

        btnSwitchBn.setOnClickListener(v -> {
            soundManager.playButtonClick();
            showBn[0] = true;
            prefs.setLanguage("bn");
            updateLanguageLabels();
            updateDialogText.run();
        });

        btnSwitchEn.setOnClickListener(v -> {
            soundManager.playButtonClick();
            showBn[0] = false;
            prefs.setLanguage("en");
            updateLanguageLabels();
            updateDialogText.run();
        });

        updateDialogText.run();
        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(460 * density), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    @Override
    public void onConnected(boolean isHost, String opponentName) {
        soundManager.playHeadshot();
        Toast.makeText(this, "Connected with " + opponentName + "!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("MODE", GameMode.MULTIPLAYER.name());
        intent.putExtra("IS_NETWORK", true);
        intent.putExtra("IS_HOST", isHost);
        intent.putExtra("OPPONENT_NAME", opponentName);
        intent.putExtra("LEVEL", 1); // Play on Boral River Map
        intent.putExtra("IS_TOURNAMENT", isTournament);
        intent.putExtra("TOURNAMENT_ROUNDS", selectedRounds);
        intent.putExtra("TOURNAMENT_TARGET_WINS", selectedTargetWins);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTournamentConfig(boolean isTourn, int totalRounds, int targetWins) {
        this.isTournament = isTourn;
        this.selectedRounds = totalRounds;
        this.selectedTargetWins = targetWins;
        runOnUiThread(this::updateTournamentUI);
    }

    @Override
    public void onConnectionFailed(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        tvStatusHeader.setText("CONNECTION FAILED");
        tvStatusSub.setText(error);
    }

    @Override
    public void onOpponentMove(float normX, float normY, int tier, boolean isWalking) {}
    @Override
    public void onOpponentAim(float angle, float power) {}
    @Override
    public void onOpponentAimCancel() {}
    @Override
    public void onOpponentShoot(float normSx, float normSy, float vx, float vy, int color, int damage) {}
    @Override
    public void onOpponentHit(int damage, boolean isHeadshot, int remainingHp) {}
    @Override
    public void onOpponentPowerup(String type) {}
    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Opponent Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (radarAnim != null) radarAnim.cancel();
    }
}
