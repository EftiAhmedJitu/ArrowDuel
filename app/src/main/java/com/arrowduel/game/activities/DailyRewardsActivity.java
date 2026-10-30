package com.arrowduel.game.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.DailyReward;

public class DailyRewardsActivity extends BaseActivity {

    private GridLayout rewardsGrid;
    private PreferencesManager prefs;
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_rewards);

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);

        rewardsGrid = findViewById(R.id.rewardsGrid);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        buildGrid();
    }

    private void buildGrid() {
        rewardsGrid.removeAllViews();
        final float density = getResources().getDisplayMetrics().density;
        boolean bn = prefs.isBangla();
        DailyReward[] rewards = DailyReward.getSevenDaysRewards();
        int currentClaimedDay = prefs.getDailyClaimDay();

        for (DailyReward r : rewards) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding((int)(8 * density), (int)(8 * density), (int)(8 * density), (int)(8 * density));
            card.setBackgroundResource(R.drawable.bg_neon_card);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = 0;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, (r.getDay() == 7) ? 2f : 1f);
            glp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.setMargins((int)(5 * density), (int)(5 * density), (int)(5 * density), (int)(5 * density));
            card.setLayoutParams(glp);

            // Day label
            TextView dayLabel = new TextView(this);
            dayLabel.setText(bn ? ("দিন " + r.getDay()) : ("Day " + r.getDay()));
            dayLabel.setTextColor(Color.parseColor("#8C9EC0"));
            dayLabel.setTextSize(11.5f);
            dayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(dayLabel);

            // Icon
            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams pIcon = new LinearLayout.LayoutParams((int)(34 * density), (int)(34 * density));
            pIcon.topMargin = (int)(2 * density);
            pIcon.bottomMargin = (int)(2 * density);
            icon.setLayoutParams(pIcon);
            if ("coin".equals(r.getIconType())) icon.setImageResource(R.drawable.ic_coin);
            else if ("gem".equals(r.getIconType())) icon.setImageResource(R.drawable.ic_gem);
            else if ("bow".equals(r.getIconType())) icon.setImageResource(R.drawable.ic_bow);
            else icon.setImageResource(R.drawable.ic_star_filled);
            card.addView(icon);

            // Reward Title
            TextView title = new TextView(this);
            title.setText(r.getTitle());
            title.setTextColor(Color.WHITE);
            title.setTextSize(12f);
            title.setGravity(Gravity.CENTER);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(title);

            // Custom Styled Claim Button (Never clipped, high contrast)
            TextView btn = new TextView(this);
            LinearLayout.LayoutParams pBtn = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int)(32 * density));
            pBtn.topMargin = (int)(6 * density);
            btn.setLayoutParams(pBtn);
            btn.setGravity(Gravity.CENTER);
            btn.setTextSize(11.5f);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);

            if (r.getDay() < currentClaimedDay) {
                btn.setText(bn ? "✓ সংগ্রহিত" : "✓ CLAIMED");
                btn.setBackgroundResource(R.drawable.bg_tab_inactive);
                btn.setTextColor(Color.parseColor("#8C9EC0"));
                btn.setEnabled(false);
            } else if (r.getDay() == currentClaimedDay) {
                btn.setText(bn ? "🎁 সংগ্রহ করুন" : "🎁 CLAIM NOW");
                btn.setBackgroundResource(R.drawable.bg_play_btn);
                btn.setTextColor(Color.parseColor("#0A0E1A"));
                btn.setEnabled(true);
                btn.setOnClickListener(v -> claimReward(r));
            } else {
                btn.setText(bn ? "🔒 লকড" : "🔒 LOCKED");
                btn.setBackgroundResource(R.drawable.bg_tab_inactive);
                btn.setTextColor(Color.parseColor("#556677"));
                btn.setEnabled(false);
            }
            card.addView(btn);

            rewardsGrid.addView(card);
        }
    }

    private void claimReward(DailyReward r) {
        soundManager.playPotion();
        if (r.getCoins() > 0) prefs.addCoins(r.getCoins());
        if (r.getGems() > 0) prefs.addGems(r.getGems());
        if ("bow".equals(r.getIconType())) prefs.unlockBow("lightning_bow");

        prefs.setDailyClaimDay(r.getDay() + 1);
        Toast.makeText(this, "Claimed " + r.getTitle() + "!", Toast.LENGTH_SHORT).show();
        buildGrid();
    }
}
