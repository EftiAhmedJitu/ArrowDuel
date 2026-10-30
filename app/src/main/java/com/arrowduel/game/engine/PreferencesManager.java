package com.arrowduel.game.engine;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferencesManager {

    private static final String PREF_NAME = "ArrowDuelPrefs";
    private static final String KEY_COINS = "coins";
    private static final String KEY_GEMS = "gems";
    private static final String KEY_EQUIPPED_BOW = "equipped_bow";
    private static final String KEY_UNLOCKED_BOWS = "unlocked_bows";
    private static final String KEY_EQUIPPED_SKIN = "equipped_skin";
    private static final String KEY_UNLOCKED_SKINS = "unlocked_skins";
    private static final String KEY_MAX_STORY_LEVEL = "max_story_level";
    private static final String KEY_DAILY_CLAIM_DAY = "daily_claim_day";
    private static final String KEY_DAILY_LAST_TIME = "daily_last_time";
    private static final String KEY_PLAYER_NAME = "player_name";
    private static final String KEY_HIGH_SCORE_SURVIVAL = "high_score_survival";
    private static final String KEY_TARGET_FPS = "target_fps";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initDefaults();
    }

    private void initDefaults() {
        if (!prefs.contains(KEY_COINS)) {
            prefs.edit()
                    .putInt(KEY_COINS, 640) // Match screenshot start
                    .putInt(KEY_GEMS, 120)  // Match screenshot start
                    .putString(KEY_EQUIPPED_BOW, "lightning_bow")
                    .putString(KEY_EQUIPPED_SKIN, "blue_archer")
                    .putInt(KEY_MAX_STORY_LEVEL, 12) // Unlocked up to Level 12 as in screenshot
                    .putString(KEY_PLAYER_NAME, "Player123")
                    .putInt(KEY_TARGET_FPS, 60)
                    .apply();

            Set<String> bows = new HashSet<>();
            bows.add("lightning_bow");
            saveUnlockedBows(bows);

            Set<String> skins = new HashSet<>();
            skins.add("blue_archer");
            saveUnlockedSkins(skins);
        }
    }

    public int getCoins() { return prefs.getInt(KEY_COINS, 640); }
    public void addCoins(int amount) {
        prefs.edit().putInt(KEY_COINS, Math.max(0, getCoins() + amount)).apply();
    }

    public int getGems() { return prefs.getInt(KEY_GEMS, 120); }
    public void addGems(int amount) {
        prefs.edit().putInt(KEY_GEMS, Math.max(0, getGems() + amount)).apply();
    }

    public String getEquippedBow() { return prefs.getString(KEY_EQUIPPED_BOW, "lightning_bow"); }
    public void setEquippedBow(String bowId) {
        prefs.edit().putString(KEY_EQUIPPED_BOW, bowId).apply();
    }

    public Set<String> getUnlockedBows() {
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_UNLOCKED_BOWS, new HashSet<>()));
        set.add("lightning_bow");
        return set;
    }
    public void unlockBow(String bowId) {
        Set<String> set = new HashSet<>(getUnlockedBows());
        set.add(bowId);
        saveUnlockedBows(set);
    }
    private void saveUnlockedBows(Set<String> set) {
        prefs.edit().putStringSet(KEY_UNLOCKED_BOWS, set).apply();
    }

    public String getEquippedSkin() { return prefs.getString(KEY_EQUIPPED_SKIN, "blue_archer"); }
    public void setEquippedSkin(String skinId) {
        prefs.edit().putString(KEY_EQUIPPED_SKIN, skinId).apply();
    }

    public Set<String> getUnlockedSkins() {
        return prefs.getStringSet(KEY_UNLOCKED_SKINS, new HashSet<>());
    }
    public void unlockSkin(String skinId) {
        Set<String> set = new HashSet<>(getUnlockedSkins());
        set.add(skinId);
        saveUnlockedSkins(set);
    }
    private void saveUnlockedSkins(Set<String> set) {
        prefs.edit().putStringSet(KEY_UNLOCKED_SKINS, set).apply();
    }

    public int getMaxStoryLevel() { return prefs.getInt(KEY_MAX_STORY_LEVEL, 12); }
    public void unlockNextLevel(int completedLevel) {
        if (completedLevel >= getMaxStoryLevel()) {
            prefs.edit().putInt(KEY_MAX_STORY_LEVEL, completedLevel + 1).apply();
        }
    }

    public int getLevelStars(int level) {
        return prefs.getInt("stars_level_" + level, (level < 12) ? 3 : 0);
    }
    public void setLevelStars(int level, int stars) {
        int oldStars = getLevelStars(level);
        if (stars > oldStars) {
            prefs.edit().putInt("stars_level_" + level, stars).apply();
        }
    }

    public int getDailyClaimDay() { return prefs.getInt(KEY_DAILY_CLAIM_DAY, 2); }
    public void setDailyClaimDay(int day) {
        prefs.edit().putInt(KEY_DAILY_CLAIM_DAY, day).apply();
    }

    public String getPlayerName() { return prefs.getString(KEY_PLAYER_NAME, "Player123"); }
    public void setPlayerName(String name) {
        prefs.edit().putString(KEY_PLAYER_NAME, name).apply();
    }

    public int getHighScoreSurvival() { return prefs.getInt(KEY_HIGH_SCORE_SURVIVAL, 0); }
    public void setHighScoreSurvival(int score) {
        if (score > getHighScoreSurvival()) {
            prefs.edit().putInt(KEY_HIGH_SCORE_SURVIVAL, score).apply();
        }
    }

    public int getTargetFps() {
        return prefs.getInt(KEY_TARGET_FPS, 60);
    }

    public void setTargetFps(int fps) {
        prefs.edit().putInt(KEY_TARGET_FPS, (fps == 120) ? 120 : 60).apply();
    }

    public static final String KEY_JOYSTICK_SIDE = "joystick_side";
    public static final String KEY_CONTROL_SCALE = "control_scale";
    public static final String KEY_LANGUAGE = "app_language";

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "bn");
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public boolean isBangla() {
        return !"en".equalsIgnoreCase(getLanguage());
    }

    public String getJoystickSide() {
        return prefs.getString(KEY_JOYSTICK_SIDE, "RIGHT");
    }

    public void setJoystickSide(String side) {
        prefs.edit().putString(KEY_JOYSTICK_SIDE, side).apply();
    }

    public String getControlScale() {
        return prefs.getString(KEY_CONTROL_SCALE, "MEDIUM");
    }

    public void setControlScale(String scale) {
        prefs.edit().putString(KEY_CONTROL_SCALE, scale).apply();
    }

    public float getControlScaleFactor() {
        String s = getControlScale();
        if ("SMALL".equals(s)) return 0.8f;
        if ("LARGE".equals(s)) return 1.25f;
        return 1.0f;
    }
}
