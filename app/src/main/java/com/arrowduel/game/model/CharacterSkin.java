package com.arrowduel.game.model;

import android.graphics.Color;

public class CharacterSkin {
    private String id;
    private String name;
    private int primaryColor;
    private int capeColor;
    private int price;

    public CharacterSkin(String id, String name, int primaryColor, int capeColor, int price) {
        this.id = id;
        this.name = name;
        this.primaryColor = primaryColor;
        this.capeColor = capeColor;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrimaryColor() { return primaryColor; }
    public int getCapeColor() { return capeColor; }
    public int getPrice() { return price; }

    public static CharacterSkin[] getAllSkins() {
        return new CharacterSkin[] {
            new CharacterSkin("blue_archer", "Blue Archer", Color.BLACK, Color.parseColor("#00E5FF"), 0),
            new CharacterSkin("red_archer", "Red Archer", Color.BLACK, Color.parseColor("#FF2A55"), 500),
            new CharacterSkin("golden_ranger", "Golden Ranger", Color.BLACK, Color.parseColor("#FFB800"), 800),
            new CharacterSkin("shadow_ninja", "Shadow Ninja", Color.parseColor("#1A1A24"), Color.parseColor("#B829FF"), 1200),
            new CharacterSkin("emerald_hunter", "Emerald Hunter", Color.BLACK, Color.parseColor("#00FF88"), 1500)
        };
    }
}
