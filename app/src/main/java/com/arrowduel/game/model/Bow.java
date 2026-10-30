package com.arrowduel.game.model;

import android.graphics.Color;

public class Bow {
    private String id;
    private String name;
    private String rarity;
    private int rarityColor;
    private int price; // In coins, 0 if starter
    private int gemPrice;
    private int color;
    private float power; // 0.0 to 1.0 (maps to damage)
    private float range; // 0.0 to 1.0 (maps to arrow velocity)
    private float reload; // 0.0 to 1.0 (maps to recharge time)
    private String specialTag;
    private String perkTitle;
    private String effectDescription;

    public Bow(String id, String name, String rarity, int rarityColor, int price, int gemPrice, int color,
               float power, float range, float reload, String specialTag, String perkTitle, String effectDescription) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.rarityColor = rarityColor;
        this.price = price;
        this.gemPrice = gemPrice;
        this.color = color;
        this.power = power;
        this.range = range;
        this.reload = reload;
        this.specialTag = specialTag;
        this.perkTitle = perkTitle;
        this.effectDescription = effectDescription;
    }

    public Bow(String id, String name, int price, int gemPrice, int color, float power, float range, float reload, String effectDescription) {
        this(id, name, "RARE", Color.parseColor("#00E5FF"), price, gemPrice, color, power, range, reload, "SPECIAL", "WEAPON PERK", effectDescription);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRarity() { return rarity; }
    public int getRarityColor() { return rarityColor; }
    public int getPrice() { return price; }
    public int getGemPrice() { return gemPrice; }
    public int getColor() { return color; }
    public float getPower() { return power; }
    public float getRange() { return range; }
    public float getReload() { return reload; }
    public String getSpecialTag() { return specialTag; }
    public String getPerkTitle() { return perkTitle; }
    public String getEffectDescription() { return effectDescription; }

    public static Bow[] getAllBows() {
        return new Bow[] {
            new Bow("lightning_bow", "Lightning Bow", "STARTER", Color.parseColor("#00E5FF"),
                    0, 0, Color.parseColor("#00E5FF"),
                    0.70f, 0.88f, 0.80f,
                    "STUN ⚡", "⚡ LIGHTNING SHOCK",
                    "Electric sparks with 35% chance to stun & paralyze the target for 1.2s."),

            new Bow("freeze_bow", "Frost Freeze Bow", "RARE", Color.parseColor("#80D8FF"),
                    1200, 0, Color.parseColor("#00F0FF"),
                    0.78f, 0.90f, 0.85f,
                    "FREEZE ❄️", "❄️ ABSOLUTE ZERO FREEZE",
                    "Freezes the target solid for 2.5s! Enemy is trapped in ice and cannot move, dodge, or shoot."),

            new Bow("fire_bow", "Inferno Fire Bow", "EPIC", Color.parseColor("#FF6600"),
                    1600, 0, Color.parseColor("#FF3300"),
                    0.86f, 0.80f, 0.75f,
                    "BURNING 🔥", "🔥 INFERNO BLAZE",
                    "Fiery blast causing +30 burning splash damage over 3 seconds."),

            new Bow("titan_bow", "Titan ExtraDmg Bow", "LEGENDARY", Color.parseColor("#FF0055"),
                    2200, 20, Color.parseColor("#FF0055"),
                    1.00f, 0.95f, 0.70f,
                    "EXTRA DMG 💥", "💥 TITAN CRIT (+100% DAMAGE)",
                    "Nuclear kinetic kinetic power delivering massive 75-95 damage with heavy screen shake!"),

            new Bow("golden_bow", "Golden Midas Bow", "EPIC", Color.parseColor("#FFD700"),
                    2800, 25, Color.parseColor("#FFD700"),
                    0.82f, 0.88f, 0.82f,
                    "GOLD+ 🪙", "🪙 MIDAS TREASURE",
                    "Grants +25 bonus coins on direct hits and +50% extra coins at match victory!"),

            new Bow("venom_bow", "Toxic Venom Bow", "LEGENDARY", Color.parseColor("#00FF66"),
                    3200, 35, Color.parseColor("#00FF66"),
                    0.80f, 0.75f, 0.95f,
                    "POISON 🧪", "🧪 NEUROTOXIC ACID",
                    "Injects corrosive venom dealing 3 consecutive poison ticks over 3 seconds."),

            new Bow("void_bow", "Void Singularity Bow", "MYTHIC", Color.parseColor("#B829FF"),
                    4000, 50, Color.parseColor("#B829FF"),
                    0.96f, 0.92f, 0.82f,
                    "VOID 🌀", "🌀 BLACK HOLE GRAVITY",
                    "Spawns a cosmic singularity that pulls the enemy and deals crushing dark matter damage.")
        };
    }
}
