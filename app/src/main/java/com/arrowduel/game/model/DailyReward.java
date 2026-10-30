package com.arrowduel.game.model;

public class DailyReward {
    private int day;
    private String title;
    private int coins;
    private int gems;
    private String iconType; // "coin", "gem", "chest", "bow"

    public DailyReward(int day, String title, int coins, int gems, String iconType) {
        this.day = day;
        this.title = title;
        this.coins = coins;
        this.gems = gems;
        this.iconType = iconType;
    }

    public int getDay() { return day; }
    public String getTitle() { return title; }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public String getIconType() { return iconType; }

    public static DailyReward[] getSevenDaysRewards() {
        return new DailyReward[] {
            new DailyReward(1, "100 Coins", 100, 0, "coin"),
            new DailyReward(2, "50 Gems", 0, 50, "gem"),
            new DailyReward(3, "Silver Chest", 250, 15, "chest"),
            new DailyReward(4, "200 Coins", 200, 0, "coin"),
            new DailyReward(5, "100 Gems", 0, 100, "gem"),
            new DailyReward(6, "Golden Chest", 500, 30, "chest"),
            new DailyReward(7, "Epic Lightning Bow", 1000, 50, "bow")
        };
    }
}
