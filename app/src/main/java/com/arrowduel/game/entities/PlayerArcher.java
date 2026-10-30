package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

public class PlayerArcher extends Archer {

    private Paint trajectoryPaint;
    private Paint shadowPaint;
    private Path trajectoryPath;
    private int equippedDamage = 35;
    private float arrowSpeedMultiplier = 1.0f;

    public PlayerArcher(float x, float y, String name, int capeColor, int bowColor) {
        this(x, y, true, name, capeColor, bowColor); // Default facing right towards enemy on the right
    }

    public PlayerArcher(float x, float y, boolean facingRight, String name, int capeColor, int bowColor) {
        super(x, y, facingRight, name, capeColor, bowColor);

        // Dark shadow outline for contrast against bright sky/water backgrounds
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(6.5f);
        shadowPaint.setColor(Color.parseColor("#B3000000"));
        shadowPaint.setPathEffect(new DashPathEffect(new float[]{12f, 10f}, 0));

        // Crisp, pure solid white core line
        trajectoryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trajectoryPaint.setStyle(Paint.Style.STROKE);
        trajectoryPaint.setStrokeWidth(3.8f);
        trajectoryPaint.setColor(Color.WHITE);
        trajectoryPaint.setPathEffect(new DashPathEffect(new float[]{12f, 10f}, 0));

        trajectoryPath = new Path();
    }

    private String equippedBowId = "lightning_bow";

    public void setBowStats(int damage, float speedMult, int color, String bowId) {
        this.equippedDamage = damage;
        this.arrowSpeedMultiplier = speedMult;
        this.equippedBowId = (bowId != null) ? bowId : "lightning_bow";
        setBowColor(color);
    }

    public void setBowStats(int damage, float speedMult, int color) {
        setBowStats(damage, speedMult, color, "lightning_bow");
    }

    public String getEquippedBowId() { return equippedBowId; }

    public Arrow shoot() {
        float power = Math.max(0.35f, drawPower);

        float rad = (float) Math.toRadians(aimAngle);
        float speed = (450f + power * 650f) * arrowSpeedMultiplier;
        float dir = facingRight ? 1f : -1f;
        float vx = (float) Math.cos(rad) * speed * dir;
        float vy = -(float) Math.sin(rad) * speed;

        float spawnX = x + (float) Math.cos(rad) * scale * 0.44f * dir;
        float spawnY = (y - scale * 0.76f) + 12f - (float) Math.sin(rad) * scale * 0.44f;

        drawPower = 0f;
        isAiming = false;

        Arrow a = new Arrow(spawnX, spawnY, vx, vy, Arrow.Owner.PLAYER, bowColor, equippedDamage);
        a.setBowId(equippedBowId);
        return a;
    }

    public void drawTrajectory(Canvas canvas, float wind, float gravity) {
        if (!isAiming) return;

        float effectivePower = Math.max(0.2f, drawPower);

        float rad = (float) Math.toRadians(aimAngle);
        float speed = (450f + effectivePower * 650f) * arrowSpeedMultiplier;
        float dir = facingRight ? 1f : -1f;
        float vx = (float) Math.cos(rad) * speed * dir;
        float vy = -(float) Math.sin(rad) * speed;

        float startPx = x + (float) Math.cos(rad) * scale * 0.45f * dir;
        float startPy = (y - scale * 0.88f) + scale * 0.25f - (float) Math.sin(rad) * scale * 0.45f;

        trajectoryPath.reset();
        trajectoryPath.moveTo(startPx, startPy);

        float px = startPx;
        float py = startPy;
        float simVx = vx;
        float simVy = vy;
        float dt = 0.04f;

        // Preview trajectory arc up to 25 steps
        for (int i = 0; i < 24; i++) {
            simVy += gravity * dt;
            simVx += wind * dt;
            px += simVx * dt;
            py += simVy * dt;
            trajectoryPath.lineTo(px, py);
        }

        // 1. Draw dark contrast shadow outline behind line
        shadowPaint.setAlpha(220);
        canvas.drawPath(trajectoryPath, shadowPaint);

        // 2. Draw crisp pure white core line (100% visible across any background)
        trajectoryPaint.setAlpha(255);
        canvas.drawPath(trajectoryPath, trajectoryPaint);

        // 3. Draw high-visibility white target reticle at predicted landing point
        Paint dotBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotBorder.setStyle(Paint.Style.STROKE);
        dotBorder.setColor(Color.BLACK);
        dotBorder.setStrokeWidth(2.5f);

        Paint dotFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotFill.setStyle(Paint.Style.FILL);
        dotFill.setColor(Color.WHITE);

        canvas.drawCircle(px, py, 6f, dotBorder);
        canvas.drawCircle(px, py, 5f, dotFill);
    }
}
