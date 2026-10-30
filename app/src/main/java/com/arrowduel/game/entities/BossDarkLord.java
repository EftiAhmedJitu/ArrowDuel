package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BossDarkLord {

    private float x;
    private float y;
    private float scale = 220f; // Giant titan size
    private int maxHp = 800;
    private int currentHp = 800;
    private int healthBarsRemaining = 5; // x5 bars
    private int hpPerBar = 160;

    private float attackCooldown = 2.0f;
    private float animPhase = 0f;
    private float hurtTimer = 0f;
    private final Random random = new Random();

    // Paints
    private Paint armorPaint;
    private Paint eyePaint;
    private Paint flamePaint;
    private Paint hudPaint;

    // Meteors
    public static class Meteor {
        public float x, y, vy = 600f, vx = -150f;
        public boolean active = true;
        public Meteor(float x, float y) { this.x = x; this.y = y; }
    }
    private final List<Meteor> activeMeteors = new ArrayList<>();

    public BossDarkLord(float x, float y) {
        this.x = x;
        this.y = y;

        armorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armorPaint.setColor(Color.parseColor("#150A1A"));
        armorPaint.setStyle(Paint.Style.FILL);

        eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(Color.parseColor("#FF0033"));
        eyePaint.setStyle(Paint.Style.FILL);

        flamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isDead() { return currentHp <= 0; }
    public List<Meteor> getActiveMeteors() { return activeMeteors; }

    public RectF getHeadBounds() {
        return new RectF(x - scale * 0.25f, y - scale * 0.95f, x + scale * 0.25f, y - scale * 0.65f);
    }

    public RectF getBodyBounds() {
        return new RectF(x - scale * 0.45f, y - scale * 0.70f, x + scale * 0.45f, y);
    }

    public boolean takeDamage(int amount, boolean headshot) {
        int finalDmg = headshot ? (int) (amount * 1.5f) : amount;
        currentHp = Math.max(0, currentHp - finalDmg);
        hurtTimer = 0.25f;
        healthBarsRemaining = (int) Math.ceil((double) currentHp / hpPerBar);
        return headshot;
    }

    private float frozenTimer = 0f;

    public void freeze(float duration) {
        this.frozenTimer = Math.max(this.frozenTimer, duration);
        this.attackCooldown += duration;
    }

    public boolean isFrozen() { return frozenTimer > 0f; }

    public void update(float dt, Archer player, List<Arrow> arrowsOut, ParticleSystem particleSystem) {
        animPhase += dt * 3f;

        if (hurtTimer > 0) hurtTimer -= dt;
        if (frozenTimer > 0) {
            frozenTimer -= dt;
            return;
        }

        // Update active meteors
        for (Meteor m : activeMeteors) {
            if (m.active) {
                m.x += m.vx * dt;
                m.y += m.vy * dt;
                particleSystem.emitArrowTrail(m.x, m.y, Color.parseColor("#FF4500"));
                if (m.y > y + 50f) {
                    m.active = false;
                    particleSystem.emitBurst(m.x, m.y, Color.parseColor("#FF2200"), 12, 180f);
                }
            }
        }

        attackCooldown -= dt;
        if (attackCooldown <= 0) {
            int attackType = random.nextInt(3);
            if (attackType == 0) {
                // Triple dark arrow volley
                fireDarkArrows(arrowsOut);
                attackCooldown = 2.4f;
            } else if (attackType == 1) {
                // Meteor rain
                spawnMeteorRain(player.getX());
                attackCooldown = 3.2f;
            } else {
                // Single high velocity doom shot
                arrowsOut.add(new Arrow(x - scale * 0.3f, y - scale * 0.5f,
                        -850f, -300f, Arrow.Owner.BOSS, Color.parseColor("#FF0055"), 40));
                attackCooldown = 2.0f;
            }
        }
    }

    private void fireDarkArrows(List<Arrow> arrowsOut) {
        float startX = x - scale * 0.35f;
        float startY = y - scale * 0.5f;
        arrowsOut.add(new Arrow(startX, startY, -700f, -400f, Arrow.Owner.BOSS, Color.parseColor("#FF0055"), 25));
        arrowsOut.add(new Arrow(startX, startY, -750f, -250f, Arrow.Owner.BOSS, Color.parseColor("#FF0055"), 25));
        arrowsOut.add(new Arrow(startX, startY, -680f, -100f, Arrow.Owner.BOSS, Color.parseColor("#FF0055"), 25));
    }

    private void spawnMeteorRain(float playerX) {
        for (int i = 0; i < 3; i++) {
            float meteorX = playerX + (i - 1) * 120f + (random.nextFloat() - 0.5f) * 60f;
            activeMeteors.add(new Meteor(meteorX - 100f, -50f - i * 80f));
        }
    }

    public void draw(Canvas canvas) {
        // Draw boss titan
        armorPaint.setColor((hurtTimer > 0) ? Color.parseColor("#5A0000") : Color.parseColor("#150A1A"));

        // 1. Spiked Shoulders / Torso
        Path body = new Path();
        body.moveTo(x - scale * 0.45f, y - scale * 0.65f);
        body.lineTo(x - scale * 0.55f, y - scale * 0.85f); // Left spike
        body.lineTo(x - scale * 0.35f, y - scale * 0.70f);
        body.lineTo(x, y - scale * 0.60f); // Neck
        body.lineTo(x + scale * 0.35f, y - scale * 0.70f);
        body.lineTo(x + scale * 0.55f, y - scale * 0.85f); // Right spike
        body.lineTo(x + scale * 0.45f, y - scale * 0.65f);
        body.lineTo(x + scale * 0.38f, y);
        body.lineTo(x - scale * 0.38f, y);
        body.close();
        canvas.drawPath(body, armorPaint);

        // 2. Horned Demonic Helm
        float headY = y - scale * 0.80f;
        float headR = scale * 0.18f;
        canvas.drawCircle(x, headY, headR, armorPaint);

        // Curved Horns
        flamePaint.setStyle(Paint.Style.STROKE);
        flamePaint.setStrokeWidth(12f);
        flamePaint.setColor(Color.parseColor("#FF0033"));
        Path leftHorn = new Path();
        leftHorn.moveTo(x - headR * 0.8f, headY);
        leftHorn.cubicTo(x - headR * 2.2f, headY - scale * 0.25f, x - headR * 1.5f, headY - scale * 0.45f, x - headR * 0.5f, headY - scale * 0.35f);
        canvas.drawPath(leftHorn, flamePaint);

        Path rightHorn = new Path();
        rightHorn.moveTo(x + headR * 0.8f, headY);
        rightHorn.cubicTo(x + headR * 2.2f, headY - scale * 0.25f, x + headR * 1.5f, headY - scale * 0.45f, x + headR * 0.5f, headY - scale * 0.35f);
        canvas.drawPath(rightHorn, flamePaint);

        // 3. Glowing Demonic Red Eyes
        eyePaint.setColor(Color.parseColor("#FF0022"));
        canvas.drawCircle(x - headR * 0.45f, headY, 8f, eyePaint);
        canvas.drawCircle(x + headR * 0.45f, headY, 8f, eyePaint);

        // Glowing Chest Core
        eyePaint.setColor(Color.parseColor("#FF4500"));
        canvas.drawCircle(x, y - scale * 0.45f, 18f + 4f * (float) Math.sin(animPhase * 4f), eyePaint);

        // 4. Draw Meteors
        flamePaint.setStyle(Paint.Style.FILL);
        flamePaint.setColor(Color.parseColor("#FF4500"));
        for (Meteor m : activeMeteors) {
            if (m.active) {
                canvas.drawCircle(m.x, m.y, 16f, flamePaint);
            }
        }

        // 5. Boss Health Bar HUD at top
        drawBossHud(canvas);
    }

    private void drawBossHud(Canvas canvas) {
        float w = canvas.getWidth();
        float barW = w * 0.55f;
        float barH = 22f;
        float barX = (w - barW) / 2f;
        float barY = 65f;

        // Title: "Dark Lord"
        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextSize(26f);
        hudPaint.setTextAlign(Paint.Align.CENTER);
        hudPaint.setFakeBoldText(true);
        canvas.drawText("Dark Lord", w / 2f, barY - 10f, hudPaint);

        // Bar background
        hudPaint.setStyle(Paint.Style.FILL);
        hudPaint.setColor(Color.parseColor("#44000000"));
        canvas.drawRoundRect(barX - 4, barY - 4, barX + barW + 4, barY + barH + 4, 8f, 8f, hudPaint);

        // Bar fill
        float currentBarHp = currentHp % hpPerBar;
        if (currentBarHp == 0 && currentHp > 0) currentBarHp = hpPerBar;
        float ratio = currentBarHp / (float) hpPerBar;

        hudPaint.setColor(Color.parseColor("#FF0044"));
        canvas.drawRoundRect(barX, barY, barX + barW * ratio, barY + barH, 6f, 6f, hudPaint);

        // Multiplier tag: "x5", "x4", etc.
        hudPaint.setColor(Color.parseColor("#FFCC00"));
        hudPaint.setTextSize(22f);
        hudPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("x" + Math.max(1, healthBarsRemaining), barX + barW + 14f, barY + barH - 3f, hudPaint);
    }
}
