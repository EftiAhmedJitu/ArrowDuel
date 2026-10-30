package com.arrowduel.game.entities;

public class PowerUp {
    public enum Type {
        HEALTH_POTION,
        SHIELD,
        LIGHTNING_STRIKE
    }

    private Type type;
    private int count;

    public PowerUp(Type type, int count) {
        this.type = type;
        this.count = count;
    }

    public Type getType() { return type; }
    public int getCount() { return count; }

    public boolean use() {
        if (count > 0) {
            count--;
            return true;
        }
        return false;
    }

    public void add(int amount) {
        this.count += amount;
    }
}
