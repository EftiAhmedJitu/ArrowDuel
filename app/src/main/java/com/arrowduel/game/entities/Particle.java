package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Particle {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float size;
    public int color;
    public float alpha;
    public float life;
    public float maxLife;
    public boolean active;

    public void init(float x, float y, float vx, float vy, float size, int color, float maxLife) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.alpha = 1.0f;
        this.active = true;
    }

    public void update(float dt) {
        if (!active) return;
        x += vx * dt;
        y += vy * dt;
        life -= dt;
        if (life <= 0) {
            active = false;
        } else {
            alpha = life / maxLife;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setColor(color);
        paint.setAlpha((int) (alpha * 255));
        canvas.drawCircle(x, y, size * alpha, paint);
    }
}
