package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Paint;

public class FloatingText {
    public float x;
    public float y;
    public String text;
    public int color;
    public float life;
    public float maxLife;
    public float textSize;
    public boolean active;

    public void init(float x, float y, String text, int color, float textSize, float duration) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.textSize = textSize;
        this.life = duration;
        this.maxLife = duration;
        this.active = true;
    }

    public void update(float dt) {
        if (!active) return;
        y -= 40f * dt;
        life -= dt;
        if (life <= 0) active = false;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setAlpha((int) ((life / maxLife) * 255));
        canvas.drawText(text, x, y, paint);
    }
}
