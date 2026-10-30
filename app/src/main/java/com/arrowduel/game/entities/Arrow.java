package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class Arrow {
    public enum Owner {
        PLAYER,
        ENEMY,
        BOSS
    }

    private float x;
    private float y;
    private float vx;
    private float vy;
    private float gravity = 480f; // Gravity acceleration
    private float angle;
    private Owner owner;
    private int elementColor;
    private int damage;
    private boolean active = true;
    private boolean stuck = false;
    private float stuckTimer = 0f;
    private float quiverAngle = 0f;

    private final Paint arrowPaint;
    private final Paint glowPaint;
    private final Paint tipPaint;
    private final Path arrowHeadPath;

    public Arrow(float x, float y, float vx, float vy, Owner owner, int elementColor, int damage) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.owner = owner;
        this.elementColor = elementColor;
        this.damage = damage;
        this.angle = (float) Math.toDegrees(Math.atan2(vy, vx));

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStrokeWidth(3.5f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(elementColor);
        glowPaint.setStrokeWidth(8f);
        glowPaint.setAlpha(160);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);

        tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setColor(elementColor);
        tipPaint.setStyle(Paint.Style.FILL);

        arrowHeadPath = new Path();
        arrowHeadPath.moveTo(12f, 0f);
        arrowHeadPath.lineTo(0f, -5f);
        arrowHeadPath.lineTo(2f, 0f);
        arrowHeadPath.lineTo(0f, 5f);
        arrowHeadPath.close();
    }

    private String bowId = "lightning_bow";

    public float getX() { return x; }
    public float getY() { return y; }
    public float getVx() { return vx; }
    public float getVy() { return vy; }
    public Owner getOwner() { return owner; }
    public int getDamage() { return damage; }
    public int getElementColor() { return elementColor; }
    public boolean isActive() { return active; }
    public boolean isStuck() { return stuck; }
    public String getBowId() { return bowId; }
    public void setBowId(String bowId) { this.bowId = bowId; }

    public void update(float dt, float wind, ParticleSystem particleSystem) {
        if (!active) return;

        if (stuck) {
            stuckTimer += dt;
            // Quiver vibration
            quiverAngle = (float) Math.sin(stuckTimer * 30f) * Math.max(0, 8f - stuckTimer * 8f);
            if (stuckTimer > 2.5f) {
                active = false;
            }
            return;
        }

        // Apply physics
        vy += gravity * dt;
        vx += wind * dt;
        x += vx * dt;
        y += vy * dt;

        angle = (float) Math.toDegrees(Math.atan2(vy, vx));

        // Emit particle trail
        if (particleSystem != null) {
            particleSystem.emitArrowTrail(x, y, elementColor);
        }

        // Screen bounds cleanup
        if (y > 2500 || x < -200 || x > 3500) {
            active = false;
        }
    }

    public void stick() {
        stuck = true;
        vx = 0;
        vy = 0;
    }

    public void deactivate() {
        active = false;
    }

    public void draw(Canvas canvas) {
        if (!active) return;

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angle + quiverAngle);

        float arrowLen = 42f;

        // Glowing outer trail
        canvas.drawLine(-arrowLen, 0, 0, 0, glowPaint);

        // Arrow Shaft
        canvas.drawLine(-arrowLen, 0, 0, 0, arrowPaint);

        // Arrow Tip
        canvas.drawPath(arrowHeadPath, tipPaint);

        // Fletching / Feathers at back
        arrowPaint.setStrokeWidth(2f);
        canvas.drawLine(-arrowLen, 0, -arrowLen - 6f, -6f, arrowPaint);
        canvas.drawLine(-arrowLen, 0, -arrowLen - 6f, 6f, arrowPaint);
        canvas.drawLine(-arrowLen + 6f, 0, -arrowLen, -6f, arrowPaint);
        canvas.drawLine(-arrowLen + 6f, 0, -arrowLen, 6f, arrowPaint);

        canvas.restore();
    }
}
