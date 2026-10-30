package com.arrowduel.game.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.Bow;
import com.arrowduel.game.views.BowPreviewView;

import java.util.Set;

public class ShopActivity extends BaseActivity {

    private TextView tvShopCoins;
    private TextView tvShopGems;
    private BowPreviewView bowPreviewView;
    private TextView tvBowSpecialTag;
    private TextView tvBowRarity;
    private TextView tvBowName;
    private TextView tvPerkTitle;
    private TextView tvBowDescription;
    private ProgressBar pbPower, pbRange, pbReload;
    private TextView tvPowerVal, tvRangeVal, tvReloadVal;
    private Button btnEquipBuy;
    private LinearLayout bowsRow;

    private Button tabBows, tabArrows, tabEffects;

    private PreferencesManager prefs;
    private SoundManager soundManager;
    private Bow[] allBows;
    private int selectedBowIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);
        allBows = Bow.getAllBows();

        tvShopCoins = findViewById(R.id.tvShopCoins);
        tvShopGems = findViewById(R.id.tvShopGems);
        bowPreviewView = findViewById(R.id.bowPreviewView);
        tvBowSpecialTag = findViewById(R.id.tvBowSpecialTag);
        tvBowRarity = findViewById(R.id.tvBowRarity);
        tvBowName = findViewById(R.id.tvBowName);
        tvPerkTitle = findViewById(R.id.tvPerkTitle);
        tvBowDescription = findViewById(R.id.tvBowDescription);
        pbPower = findViewById(R.id.pbPower);
        pbRange = findViewById(R.id.pbRange);
        pbReload = findViewById(R.id.pbReload);
        tvPowerVal = findViewById(R.id.tvPowerVal);
        tvRangeVal = findViewById(R.id.tvRangeVal);
        tvReloadVal = findViewById(R.id.tvReloadVal);
        btnEquipBuy = findViewById(R.id.btnEquipBuy);
        bowsRow = findViewById(R.id.bowsRow);

        tabBows = findViewById(R.id.tabBows);
        tabArrows = findViewById(R.id.tabArrows);
        tabEffects = findViewById(R.id.tabEffects);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initial tab handlers
        tabBows.setOnClickListener(v -> {
            soundManager.playButtonClick();
            tabBows.setBackgroundResource(R.drawable.bg_blue_btn);
            tabArrows.setBackgroundResource(R.drawable.bg_dark_btn);
            tabEffects.setBackgroundResource(R.drawable.bg_dark_btn);
        });

        tabArrows.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Toast.makeText(this, "Arrow custom fletchings unlock at Level 15!", Toast.LENGTH_SHORT).show();
        });

        tabEffects.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Toast.makeText(this, "Equip bows below to activate unique weapon perks!", Toast.LENGTH_SHORT).show();
        });

        // Find initial equipped bow
        String equippedId = prefs.getEquippedBow();
        for (int i = 0; i < allBows.length; i++) {
            if (allBows[i].getId().equals(equippedId)) {
                selectedBowIndex = i;
                break;
            }
        }

        buildBowsRow();
        selectBow(selectedBowIndex);

        btnEquipBuy.setOnClickListener(v -> handleEquipOrBuy());
    }

    private void updateCurrencies() {
        tvShopCoins.setText(String.valueOf(prefs.getCoins()));
        tvShopGems.setText(String.valueOf(prefs.getGems()));
    }

    private void buildBowsRow() {
        bowsRow.removeAllViews();
        Set<String> unlocked = prefs.getUnlockedBows();
        String equippedId = prefs.getEquippedBow();

        float density = getResources().getDisplayMetrics().density;
        int cardWidth = (int) (112 * density);

        for (int i = 0; i < allBows.length; i++) {
            final int index = i;
            Bow b = allBows[i];
            boolean isUnlocked = unlocked.contains(b.getId());
            boolean isEquipped = equippedId.equals(b.getId());
            boolean isSelected = (index == selectedBowIndex);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
            card.setBackgroundResource(isSelected ? R.drawable.bg_store_item_selected : R.drawable.bg_store_item_card);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins((int) (4 * density), 0, (int) (4 * density), 0);
            card.setLayoutParams(lp);

            // 1. Top Rarity Tag
            TextView rarityTag = new TextView(this);
            rarityTag.setText(b.getRarity());
            rarityTag.setTextSize(7.5f);
            rarityTag.setTextColor(b.getRarityColor());
            rarityTag.setGravity(Gravity.CENTER);
            card.addView(rarityTag);

            // 2. Bow Hologram Icon
            ImageView icon = new ImageView(this);
            int iconSize = (int) (19 * density);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.setMargins(0, (int) (1 * density), 0, (int) (1 * density));
            icon.setLayoutParams(iconLp);
            icon.setImageResource(R.drawable.ic_bow);
            icon.setColorFilter(b.getColor());
            card.addView(icon);

            // 3. Bow Name
            TextView name = new TextView(this);
            name.setText(b.getName());
            name.setTextSize(9.5f);
            name.setTextColor(Color.WHITE);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            card.addView(name);

            // 4. Status or Price Pill
            TextView status = new TextView(this);
            status.setTextSize(8.5f);
            status.setGravity(Gravity.CENTER);
            if (isEquipped) {
                status.setText("\u2713 EQUIPPED");
                status.setTextColor(Color.parseColor("#00E5FF"));
            } else if (isUnlocked) {
                status.setText("\u2713 OWNED");
                status.setTextColor(Color.parseColor("#00FF88"));
            } else {
                if (b.getGemPrice() > 0) {
                    status.setText(b.getPrice() + " \uD83E\uDE99 + " + b.getGemPrice() + " \uD83D\uDC8E");
                } else {
                    status.setText(b.getPrice() + " \uD83E\uDE99");
                }
                status.setTextColor(Color.parseColor("#FFD700"));
            }
            card.addView(status);

            card.setOnClickListener(v -> selectBow(index));
            bowsRow.addView(card);
        }
    }

    private void selectBow(int index) {
        soundManager.playButtonClick();
        selectedBowIndex = index;
        Bow b = allBows[index];

        bowPreviewView.setBowColor(b.getColor());
        tvBowName.setText(b.getName());
        tvBowRarity.setText(b.getRarity());
        tvBowRarity.setTextColor(b.getRarityColor());
        tvBowSpecialTag.setText(b.getSpecialTag());
        tvBowSpecialTag.setTextColor(b.getColor());
        tvPerkTitle.setText(b.getPerkTitle());
        tvPerkTitle.setTextColor(b.getColor());
        tvBowDescription.setText(b.getEffectDescription());

        int pVal = (int) (b.getPower() * 100);
        int rVal = (int) (b.getRange() * 100);
        int lVal = (int) (b.getReload() * 100);

        pbPower.setProgress(pVal);
        pbRange.setProgress(rVal);
        pbReload.setProgress(lVal);

        tvPowerVal.setText(pVal + "%");
        tvRangeVal.setText(rVal + "%");
        tvReloadVal.setText(lVal + "%");

        Set<String> unlocked = prefs.getUnlockedBows();
        boolean isUnlocked = unlocked.contains(b.getId());
        String equippedId = prefs.getEquippedBow();

        if (equippedId.equals(b.getId())) {
            btnEquipBuy.setText("\u2713 CURRENTLY EQUIPPED IN LOADOUT");
            btnEquipBuy.setBackgroundResource(R.drawable.bg_store_equipped_btn);
            btnEquipBuy.setTextColor(Color.parseColor("#00E5FF"));
            btnEquipBuy.setEnabled(false);
        } else if (isUnlocked) {
            btnEquipBuy.setText("\u26A1 EQUIP TO ARSENAL");
            btnEquipBuy.setBackgroundResource(R.drawable.bg_store_equip_btn);
            btnEquipBuy.setTextColor(Color.WHITE);
            btnEquipBuy.setEnabled(true);
        } else {
            if (b.getGemPrice() > 0) {
                btnEquipBuy.setText("\uD83D\uDED2 UNLOCK WEAPON \u2022 " + b.getPrice() + " COINS + " + b.getGemPrice() + " GEMS");
            } else {
                btnEquipBuy.setText("\uD83D\uDED2 UNLOCK WEAPON \u2022 " + b.getPrice() + " COINS");
            }
            btnEquipBuy.setBackgroundResource(R.drawable.bg_store_buy_btn);
            btnEquipBuy.setTextColor(Color.parseColor("#0A0E1A"));
            btnEquipBuy.setEnabled(true);
        }

        updateCurrencies();
        buildBowsRow();
    }

    private void handleEquipOrBuy() {
        Bow b = allBows[selectedBowIndex];
        Set<String> unlocked = prefs.getUnlockedBows();
        boolean isUnlocked = unlocked.contains(b.getId());

        if (isUnlocked) {
            // Equip bow
            prefs.setEquippedBow(b.getId());
            soundManager.playBowPull();
            Toast.makeText(this, b.getName() + " equipped for battle!", Toast.LENGTH_SHORT).show();
            selectBow(selectedBowIndex);
        } else {
            // Attempt to buy with coins and gems
            int userCoins = prefs.getCoins();
            int userGems = prefs.getGems();

            if (userCoins < b.getPrice()) {
                Toast.makeText(this, "Need " + (b.getPrice() - userCoins) + " more coins to unlock!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (b.getGemPrice() > 0 && userGems < b.getGemPrice()) {
                Toast.makeText(this, "Need " + (b.getGemPrice() - userGems) + " more gems to unlock!", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.addCoins(-b.getPrice());
            if (b.getGemPrice() > 0) {
                prefs.addGems(-b.getGemPrice());
            }
            prefs.unlockBow(b.getId());
            prefs.setEquippedBow(b.getId()); // auto-equip upon purchase!
            soundManager.playVictoryFanfare();
            Toast.makeText(this, "\uD83C\uDF89 Unlocked & Equipped " + b.getName() + "!", Toast.LENGTH_LONG).show();
            selectBow(selectedBowIndex);
        }
    }
}
