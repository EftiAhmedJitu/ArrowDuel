package com.arrowduel.game.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.PreferencesManager;
import com.arrowduel.game.engine.SoundManager;
import com.arrowduel.game.model.CharacterSkin;

import java.util.Set;

public class CharacterActivity extends BaseActivity {

    private TextView tvCharCoins;
    private TextView tvSkinName;
    private Button btnEquipSkin;
    private LinearLayout skinsList;
    private ImageView ivSkinPreview;

    private PreferencesManager prefs;
    private SoundManager soundManager;
    private CharacterSkin[] allSkins;
    private int selectedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        prefs = new PreferencesManager(this);
        soundManager = SoundManager.getInstance(this);
        allSkins = CharacterSkin.getAllSkins();

        tvCharCoins = findViewById(R.id.tvCharCoins);
        tvSkinName = findViewById(R.id.tvSkinName);
        btnEquipSkin = findViewById(R.id.btnEquipSkin);
        skinsList = findViewById(R.id.skinsList);
        ivSkinPreview = findViewById(R.id.ivSkinPreview);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String equipped = prefs.getEquippedSkin();
        for (int i = 0; i < allSkins.length; i++) {
            if (allSkins[i].getId().equals(equipped)) {
                selectedIndex = i;
                break;
            }
        }

        buildSkinsList();
        selectSkin(selectedIndex);

        btnEquipSkin.setOnClickListener(v -> handleEquipOrBuySkin());
    }

    private void buildSkinsList() {
        skinsList.removeAllViews();
        Set<String> unlocked = prefs.getUnlockedSkins();

        for (int i = 0; i < allSkins.length; i++) {
            final int index = i;
            CharacterSkin skin = allSkins[i];
            boolean isUnlocked = unlocked.contains(skin.getId());
            boolean isSelected = (index == selectedIndex);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(16, 12, 16, 12);
            card.setBackgroundResource(isSelected ? R.drawable.bg_blue_btn : R.drawable.bg_neon_card);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            lp.setMargins(0, 4, 0, 4);
            card.setLayoutParams(lp);

            // Color circle indicator
            View colorDot = new View(this);
            colorDot.setLayoutParams(new LinearLayout.LayoutParams(26, 26));
            colorDot.setBackgroundColor(skin.getCapeColor());
            card.addView(colorDot);

            TextView name = new TextView(this);
            name.setText(skin.getName());
            name.setTextColor(Color.WHITE);
            name.setTextSize(13);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setPadding(14, 0, 0, 0);
            card.addView(name);

            TextView status = new TextView(this);
            status.setTextSize(11);
            if (isUnlocked) {
                status.setText("OWNED");
                status.setTextColor(Color.parseColor("#00FF88"));
            } else {
                status.setText(skin.getPrice() + " 🪙");
                status.setTextColor(Color.parseColor("#FFCC00"));
            }
            card.addView(status);

            card.setOnClickListener(v -> selectSkin(index));
            skinsList.addView(card);
        }
    }

    private void selectSkin(int index) {
        soundManager.playButtonClick();
        selectedIndex = index;
        CharacterSkin skin = allSkins[index];

        tvSkinName.setText(skin.getName());
        tvSkinName.setTextColor(skin.getCapeColor());
        ivSkinPreview.setColorFilter(skin.getCapeColor());
        tvCharCoins.setText(String.valueOf(prefs.getCoins()));

        Set<String> unlocked = prefs.getUnlockedSkins();
        boolean isUnlocked = unlocked.contains(skin.getId());
        String equipped = prefs.getEquippedSkin();

        if (equipped.equals(skin.getId())) {
            btnEquipSkin.setText("USING");
            btnEquipSkin.setBackgroundResource(R.drawable.bg_dark_btn);
            btnEquipSkin.setEnabled(false);
        } else if (isUnlocked) {
            btnEquipSkin.setText("EQUIP");
            btnEquipSkin.setBackgroundResource(R.drawable.bg_green_btn);
            btnEquipSkin.setEnabled(true);
        } else {
            btnEquipSkin.setText("BUY: " + skin.getPrice());
            btnEquipSkin.setBackgroundResource(R.drawable.bg_play_btn);
            btnEquipSkin.setEnabled(true);
        }

        buildSkinsList();
    }

    private void handleEquipOrBuySkin() {
        CharacterSkin skin = allSkins[selectedIndex];
        Set<String> unlocked = prefs.getUnlockedSkins();

        if (unlocked.contains(skin.getId())) {
            prefs.setEquippedSkin(skin.getId());
            soundManager.playBowPull();
            Toast.makeText(this, skin.getName() + " Selected!", Toast.LENGTH_SHORT).show();
            selectSkin(selectedIndex);
        } else {
            if (prefs.getCoins() >= skin.getPrice()) {
                prefs.addCoins(-skin.getPrice());
                prefs.unlockSkin(skin.getId());
                soundManager.playPotion();
                Toast.makeText(this, "Unlocked " + skin.getName() + "!", Toast.LENGTH_SHORT).show();
                selectSkin(selectedIndex);
            } else {
                Toast.makeText(this, "Need " + skin.getPrice() + " coins!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
