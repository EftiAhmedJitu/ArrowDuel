package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.List;

/**
 * Detailed Human Warrior Archer with Realistic Recurve Composite Bow:
 * - Anatomical human proportions (Assasin/Ranger Hood, leather armor, pauldrons, quiver, combat boots).
 * - Full horizontal walking steps & vertical multi-tier leaping/dropping.
 * - Crafted multi-curve recurve bow with leather grip, nocks, and dynamic string tension.
 */
public class Archer {

    public enum State {
        IDLE,
        WALKING,
        AIMING,
        SHOOTING,
        LEAPING,
        HURT,
        DEAD
    }

    protected float x;
    protected float y;
    protected float targetY;
    protected float scale = 130f; // Human warrior height
    protected boolean facingRight;
    protected String name;
    protected int maxHp = 100;
    protected int currentHp = 100;
    protected boolean hasShield = false;

    // Movement & Platforms (Horizontal & Vertical)
    protected int currentTier = 0; // 0 = Ground, 1 = Mid, 2 = High
    protected int maxTiers = 3;
    protected boolean isMovingVertical = false;
    protected float walkCycle = 0f;
    protected boolean isWalking = false;

    // Bow & Aiming
    protected int bowColor = Color.parseColor("#00E5FF");
    protected int capeColor = Color.parseColor("#00E5FF");
    protected float aimAngle = 35f; // degrees
    protected float drawPower = 0f; // 0.0 to 1.0
    protected boolean isAiming = false;
    protected State state = State.IDLE;

    // Visuals & Animation
    protected float animPhase = 0f;
    protected float hurtTimer = 0f;

    // Detailed Rendering Paints
    protected Paint armorPaint;
    protected Paint leatherPaint;
    protected Paint skinPaint;
    protected Paint hoodPaint;
    protected Paint eyePaint;
    protected Paint capePaint;
    protected Paint bowRiserPaint;
    protected Paint bowLimbPaint;
    protected Paint bowGlowPaint;
    protected Paint stringPaint;
    protected Paint arrowShaftPaint;
    protected Paint hudPaint;
    protected Paint shieldPaint;

    // Pre-allocated reusable graphics objects to completely prevent Skia/GC native heap leaks
    protected final Path capePath = new Path();
    protected final Path torsoPath = new Path();
    protected final Path hoodPath = new Path();
    protected final Path topLimbPath = new Path();
    protected final Path botLimbPath = new Path();
    protected final Path arrowHeadPath = new Path();

    protected final RectF qRect = new RectF();
    protected final RectF footRect = new RectF();
    protected final RectF headBoundsRect = new RectF();
    protected final RectF bodyBoundsRect = new RectF();
    protected final RectF hudBarRect = new RectF();
    protected final RectF hudHpRect = new RectF();

    protected Paint featherPaint;
    protected Paint goldPaint;
    protected Paint shadowPaint;
    protected Paint legPaint;
    protected Paint kneeGuard;
    protected Paint bootPaint;
    protected Paint gripPaint;
    protected Paint headMetal;
    protected Paint fletchPaint;

    public Archer(float x, float y, boolean facingRight, String name, int capeColor, int bowColor) {
        this.x = x;
        this.y = y;
        this.targetY = y;
        this.facingRight = facingRight;
        this.name = name;
        this.capeColor = capeColor;
        this.bowColor = bowColor;

        initPaints();
    }

    private void initPaints() {
        armorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armorPaint.setStyle(Paint.Style.FILL);
        armorPaint.setColor(Color.parseColor("#1C2436"));

        leatherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leatherPaint.setStyle(Paint.Style.FILL);
        leatherPaint.setColor(Color.parseColor("#4A2E18"));

        skinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        skinPaint.setStyle(Paint.Style.FILL);
        skinPaint.setColor(Color.parseColor("#D49B74"));

        hoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hoodPaint.setStyle(Paint.Style.FILL);
        hoodPaint.setColor(Color.parseColor("#151D2F"));

        eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setStyle(Paint.Style.FILL);
        eyePaint.setColor(bowColor);

        capePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        capePaint.setStyle(Paint.Style.FILL);
        capePaint.setColor(capeColor);

        bowRiserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bowRiserPaint.setStyle(Paint.Style.FILL);
        bowRiserPaint.setColor(Color.parseColor("#5C3A21"));

        bowLimbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bowLimbPaint.setStyle(Paint.Style.STROKE);
        bowLimbPaint.setStrokeCap(Paint.Cap.ROUND);
        bowLimbPaint.setStrokeWidth(5f);
        bowLimbPaint.setColor(Color.parseColor("#2B1D14"));

        bowGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bowGlowPaint.setStyle(Paint.Style.STROKE);
        bowGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        bowGlowPaint.setStrokeWidth(12f);
        bowGlowPaint.setColor(bowColor);
        bowGlowPaint.setAlpha(130);

        stringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stringPaint.setStyle(Paint.Style.STROKE);
        stringPaint.setStrokeWidth(2.5f);
        stringPaint.setColor(Color.parseColor("#EEEEEE"));

        arrowShaftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowShaftPaint.setColor(Color.parseColor("#E0C097"));
        arrowShaftPaint.setStrokeWidth(3.5f);

        hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldPaint.setStyle(Paint.Style.STROKE);
        shieldPaint.setStrokeWidth(6f);
        shieldPaint.setColor(Color.parseColor("#00E5FF"));

        featherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        featherPaint.setColor(Color.parseColor("#EEEEEE"));
        featherPaint.setStrokeWidth(2f);

        goldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goldPaint.setColor(Color.parseColor("#FFCC00"));

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#090D18"));

        legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legPaint.setColor(Color.parseColor("#121829"));
        legPaint.setStrokeWidth(9f);
        legPaint.setStrokeCap(Paint.Cap.ROUND);

        kneeGuard = new Paint(Paint.ANTI_ALIAS_FLAG);
        kneeGuard.setColor(Color.parseColor("#2C3A55"));

        bootPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bootPaint.setColor(Color.parseColor("#331F14"));
        bootPaint.setStrokeWidth(9f);
        bootPaint.setStrokeCap(Paint.Cap.ROUND);

        gripPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gripPaint.setColor(Color.parseColor("#5A371F"));
        gripPaint.setStrokeWidth(10f);
        gripPaint.setStrokeCap(Paint.Cap.ROUND);

        headMetal = new Paint(Paint.ANTI_ALIAS_FLAG);
        headMetal.setColor(Color.parseColor("#E0E0E0"));
        headMetal.setStyle(Paint.Style.FILL);

        fletchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fletchPaint.setColor(Color.WHITE);
        fletchPaint.setStrokeWidth(2.5f);
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; this.targetY = y; }
    public float getScale() { return scale; }
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int hp) { this.maxHp = hp; this.currentHp = hp; }
    public boolean isDead() { return currentHp <= 0; }
    public void setHp(int hp) {
        int oldHp = this.currentHp;
        this.currentHp = Math.max(0, Math.min(maxHp, hp));
        if (this.currentHp < oldHp) {
            this.hurtTimer = 0.35f;
        }
        if (this.currentHp <= 0) {
            this.state = State.DEAD;
        } else if (this.state == State.DEAD) {
            this.state = State.IDLE;
        }
    }
    public boolean hasShield() { return hasShield; }
    public void setShield(boolean shield) { this.hasShield = shield; }
    public int getCurrentTier() { return currentTier; }
    public void setCurrentTier(int tier, java.util.List<Platform> platforms) {
        int max = (platforms != null && !platforms.isEmpty()) ? platforms.size() : maxTiers;
        this.currentTier = Math.max(0, Math.min(max - 1, tier));
        updatePlatformTarget(platforms);
        if (Math.abs(y - targetY) > 4f) {
            this.isMovingVertical = true;
            this.state = State.LEAPING;
        }
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
        if (Math.abs(this.y - targetY) > 4f) {
            this.isMovingVertical = true;
            this.state = State.LEAPING;
        }
    }

    public void setMovingVertical(boolean moving) {
        this.isMovingVertical = moving;
    }

    public float getAimAngle() { return aimAngle; }
    public void setAimAngle(float angle) { this.aimAngle = angle; }
    public float getDrawPower() { return drawPower; }
    public void setDrawPower(float power) { this.drawPower = Math.max(0f, Math.min(1.0f, power)); }
    public boolean isAiming() { return isAiming; }
    public void setAiming(boolean aiming) { this.isAiming = aiming; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }

    public void setBowColor(int color) {
        this.bowColor = color;
        eyePaint.setColor(color);
        bowGlowPaint.setColor(color);
    }
    public void setCapeColor(int color) {
        this.capeColor = color;
        capePaint.setColor(color);
    }

    public void startWalking(boolean movingForward) {
        this.isWalking = true;
        if (!isAiming && state != State.LEAPING) state = State.WALKING;
    }

    /**
     * Walk horizontally along platform (Forward / Backward)
     */
    public void walk(float dx, float minX, float maxX) {
        if (state == State.DEAD) return;
        x = Math.max(minX, Math.min(maxX, x + dx));
        walkCycle += Math.abs(dx) * 0.15f;
        isWalking = true;
        if (!isAiming && state != State.LEAPING) state = State.WALKING;
    }

    public void stopWalking() {
        isWalking = false;
        if (state == State.WALKING) state = State.IDLE;
    }

    public boolean isWalking() {
        return isWalking;
    }

    /**
     * Move UP to higher platform tier
     */
    public boolean moveUp(List<Platform> myPlatforms) {
        int max = (myPlatforms != null && !myPlatforms.isEmpty()) ? myPlatforms.size() : maxTiers;
        if (currentTier < max - 1 && !isMovingVertical) {
            currentTier++;
            updatePlatformTarget(myPlatforms);
            isMovingVertical = true;
            state = State.LEAPING;
            return true;
        }
        return false;
    }

    /**
     * Move DOWN to lower platform tier
     */
    public boolean moveDown(List<Platform> myPlatforms) {
        if (currentTier > 0 && !isMovingVertical) {
            currentTier--;
            updatePlatformTarget(myPlatforms);
            isMovingVertical = true;
            state = State.LEAPING;
            return true;
        }
        return false;
    }

    public void setMaxTiers(int tiers) {
        this.maxTiers = Math.max(1, tiers);
    }

    private void updatePlatformTarget(List<Platform> platforms) {
        if (platforms == null) return;
        for (Platform p : platforms) {
            if (p.getTierIndex() == currentTier) {
                targetY = p.getTop();
                // Safely clamp x within the platform surface
                float minX = p.getLeft() + 25f;
                float maxX = p.getRight() - 25f;
                if (maxX > minX) {
                    if (x < minX) x = minX;
                    else if (x > maxX) x = maxX;
                }
                break;
            }
        }
    }

    public void update(float dt) {
        animPhase += dt * 3.5f;

        if (isWalking) {
            walkCycle += dt * 14f;
        }

        if (hurtTimer > 0) {
            hurtTimer -= dt;
            if (hurtTimer <= 0 && state == State.HURT) state = State.IDLE;
        }

        if (isMovingVertical) {
            float dy = targetY - y;
            if (Math.abs(dy) > 4f) {
                y += dy * 16f * dt;
            } else {
                y = targetY;
                isMovingVertical = false;
                if (state == State.LEAPING) state = State.IDLE;
            }
        }
    }

    public boolean takeDamage(int amount, boolean headshot) {
        if (hasShield) {
            hasShield = false;
            return false;
        }
        int finalDmg = headshot ? (int) (amount * 1.5f) : amount;
        currentHp = Math.max(0, currentHp - finalDmg);
        hurtTimer = 0.35f;
        state = (currentHp <= 0) ? State.DEAD : State.HURT;
        return headshot;
    }

    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    public RectF getHeadBounds() {
        float headR = scale * 0.14f;
        float hy = y - scale * 0.88f;
        headBoundsRect.set(x - headR * 1.3f, hy - headR * 1.3f, x + headR * 1.3f, hy + headR * 1.3f);
        return headBoundsRect;
    }

    public RectF getBodyBounds() {
        bodyBoundsRect.set(x - scale * 0.28f, y - scale * 0.88f, x + scale * 0.28f, y);
        return bodyBoundsRect;
    }

    public void draw(Canvas canvas) {
        float dir = facingRight ? 1f : -1f;

        float headY = y - scale * 0.88f;
        float neckY = y - scale * 0.76f;
        float chestY = y - scale * 0.62f;
        float waistY = y - scale * 0.44f;
        float kneeY = y - scale * 0.22f;

        // 1. Flowing Dual-Layer Cape (Drawn behind body)
        drawWarriorCape(canvas, x - dir * 8f, neckY, dir);

        // 2. Leather Arrow Quiver strapped to Back
        drawArrowQuiver(canvas, x - dir * 14f, chestY, dir);

        // 3. Legs & Leather Boots with Stepping Walk Cycle
        drawWarriorLegs(canvas, x, waistY, kneeY, y, dir);

        // 4. Armored Torso with Leather Straps & Pauldrons
        drawArmoredTorso(canvas, x, neckY, chestY, waistY, dir);

        // 5. Human Warrior Head & Assassin Hood with Glowing Mask Eyes
        drawWarriorHead(canvas, x, headY, dir);

        // 6. Realistic Recurve Composite Bow & Muscular Arms
        drawBowAndArms(canvas, x, neckY, chestY, dir);

        // 7. Energy Shield Bubble
        if (hasShield) {
            shieldPaint.setAlpha((int) (140 + 70 * Math.sin(animPhase * 5f)));
            canvas.drawCircle(x, y - scale * 0.45f, scale * 0.60f, shieldPaint);
        }

        // 8. Health Bar HUD
        drawHealthHud(canvas, x, headY - scale * 0.18f);
    }

    private void drawWarriorCape(Canvas canvas, float cx, float cy, float dir) {
        capePaint.setColor(capeColor);
        capePaint.setAlpha(180);

        float wave1 = (float) Math.sin(animPhase * 3f) * 10f;
        float wave2 = (float) Math.cos(animPhase * 4f) * 8f;

        capePath.reset();
        capePath.moveTo(cx, cy);
        capePath.cubicTo(cx - dir * scale * 0.35f, cy + scale * 0.3f + wave1,
                cx - dir * scale * 0.55f, y - 10f + wave2,
                cx - dir * scale * 0.20f, y);
        capePath.lineTo(cx - dir * 6f, cy + scale * 0.3f);
        capePath.close();
        canvas.drawPath(capePath, capePaint);

        // Inner shadow layer
        capePaint.setColor(Color.parseColor("#33000000"));
        canvas.drawPath(capePath, capePaint);
    }

    private void drawArrowQuiver(Canvas canvas, float qx, float qy, float dir) {
        // Quiver tube
        leatherPaint.setColor(Color.parseColor("#3D2314"));
        canvas.save();
        canvas.translate(qx, qy);
        canvas.rotate(dir * 25f);

        qRect.set(-8f, -scale * 0.35f, 8f, scale * 0.15f);
        canvas.drawRoundRect(qRect, 4f, 4f, leatherPaint);

        // 4 Feathered Hunting Arrows sticking out of quiver
        for (int i = -1; i <= 2; i++) {
            float ax = i * 4f;
            float ay = -scale * 0.35f;
            canvas.drawLine(ax, ay, ax + i * 2f, ay - 24f, arrowShaftPaint);
            // Feather fletching
            canvas.drawLine(ax + i * 2f, ay - 14f, ax + i * 2f - 4f, ay - 20f, featherPaint);
            canvas.drawLine(ax + i * 2f, ay - 14f, ax + i * 2f + 4f, ay - 20f, featherPaint);
        }
        canvas.restore();
    }

    private void drawArmoredTorso(Canvas canvas, float tx, float neckY, float chestY, float waistY, float dir) {
        armorPaint.setColor((hurtTimer > 0) ? Color.RED : Color.parseColor("#151D2F"));

        // Muscular chest & torso shape
        torsoPath.reset();
        torsoPath.moveTo(tx - scale * 0.18f, neckY + 4f);
        torsoPath.lineTo(tx + scale * 0.18f, neckY + 4f);
        torsoPath.lineTo(tx + scale * 0.15f, waistY);
        torsoPath.lineTo(tx - scale * 0.15f, waistY);
        torsoPath.close();
        canvas.drawPath(torsoPath, armorPaint);

        // Leather harness strap diagonally across chest
        leatherPaint.setColor(Color.parseColor("#5A371F"));
        leatherPaint.setStrokeWidth(5f);
        leatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(tx - dir * scale * 0.12f, neckY + 8f, tx + dir * scale * 0.10f, waistY - 6f, leatherPaint);

        // Metallic belt & golden buckle at waist
        leatherPaint.setStyle(Paint.Style.FILL);
        leatherPaint.setColor(Color.parseColor("#382013"));
        canvas.drawRect(tx - scale * 0.16f, waistY - 8f, tx + scale * 0.16f, waistY + 4f, leatherPaint);

        canvas.drawRect(tx - 6f, waistY - 8f, tx + 6f, waistY + 4f, goldPaint);

        // Shoulder Pauldron (armor guard)
        armorPaint.setColor(Color.parseColor("#26354D"));
        canvas.drawCircle(tx + dir * scale * 0.14f, neckY + 10f, scale * 0.08f, armorPaint);
    }

    private void drawWarriorHead(Canvas canvas, float hx, float hy, float dir) {
        float headR = scale * 0.13f;

        // Assassin / Ranger Hood
        hoodPaint.setColor((hurtTimer > 0) ? Color.RED : Color.parseColor("#151D2F"));
        hoodPath.reset();
        hoodPath.moveTo(hx - dir * headR * 1.2f, hy + headR * 0.9f);
        hoodPath.cubicTo(hx - dir * headR * 1.4f, hy - headR * 1.2f,
                hx + dir * headR * 0.8f, hy - headR * 1.4f,
                hx + dir * headR * 1.2f, hy + headR * 0.6f);
        hoodPath.lineTo(hx, hy + headR);
        hoodPath.close();
        canvas.drawPath(hoodPath, hoodPaint);

        // Shadowed face cavity
        canvas.drawCircle(hx + dir * headR * 0.2f, hy, headR * 0.75f, shadowPaint);

        // Glowing warrior visor / eyes
        eyePaint.setColor(bowColor);
        canvas.drawCircle(hx + dir * headR * 0.45f, hy - 2f, 4f, eyePaint);
        // Eye flare glow
        eyePaint.setAlpha(120);
        canvas.drawCircle(hx + dir * headR * 0.45f, hy - 2f, 8f, eyePaint);
        eyePaint.setAlpha(255);
    }

    private void drawWarriorLegs(Canvas canvas, float wx, float waistY, float kneeY, float footY, float dir) {
        armorPaint.setColor(Color.parseColor("#121724"));
        leatherPaint.setColor(Color.parseColor("#382013"));
        leatherPaint.setStyle(Paint.Style.FILL);

        float legOffset = isWalking ? (float) Math.sin(walkCycle) * 16f : 0f;

        // Back Leg
        float l1x = wx - dir * 10f - legOffset;
        drawLegSegment(canvas, wx - dir * 6f, waistY, l1x, kneeY, l1x - 4f, footY);

        // Forward Leg
        float l2x = wx + dir * 10f + legOffset;
        drawLegSegment(canvas, wx + dir * 6f, waistY, l2x, kneeY, l2x + 4f, footY);
    }

    private void drawLegSegment(Canvas canvas, float hipX, float hipY, float kneeX, float kneeY, float footX, float footY) {
        // Thigh
        canvas.drawLine(hipX, hipY, kneeX, kneeY, legPaint);

        // Knee Plate
        canvas.drawCircle(kneeX, kneeY, 6f, kneeGuard);

        // Calf & High Leather Combat Boot
        canvas.drawLine(kneeX, kneeY, footX, footY, bootPaint);

        // Boot foot
        canvas.drawRect(footX - 6f, footY - 4f, footX + 10f, footY, bootPaint);
    }

    private void drawBowAndArms(Canvas canvas, float ax, float neckY, float chestY, float dir) {
        float rad = (float) Math.toRadians(aimAngle);

        float shoulderX = ax + dir * 8f;
        float shoulderY = neckY + 12f;

        // Bow holding hand
        float bowDist = scale * 0.44f;
        float bowHandX = shoulderX + (float) Math.cos(rad) * bowDist * dir;
        float bowHandY = shoulderY - (float) Math.sin(rad) * bowDist;

        // String pull hand
        float pullDist = drawPower * scale * 0.36f;
        float stringHandX = bowHandX - (float) Math.cos(rad) * pullDist * dir;
        float stringHandY = bowHandY + (float) Math.sin(rad) * pullDist;

        // 1. Front Arm holding Bow
        skinPaint.setColor(Color.parseColor("#D49B74"));
        skinPaint.setStrokeWidth(8f);
        skinPaint.setStrokeCap(Paint.Cap.ROUND);
        skinPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(shoulderX, shoulderY, bowHandX, bowHandY, skinPaint);

        // Leather bracer / vambrace on front arm
        leatherPaint.setColor(Color.parseColor("#4A2E18"));
        leatherPaint.setStrokeWidth(10f);
        float bracerMidX = (shoulderX + bowHandX) / 2f;
        float bracerMidY = (shoulderY + bowHandY) / 2f;
        canvas.drawLine(bracerMidX - dir * 6f, bracerMidY, bracerMidX + dir * 6f, bracerMidY, leatherPaint);

        // 2. Back Arm pulling String
        canvas.drawLine(shoulderX - dir * 10f, shoulderY, stringHandX, stringHandY, skinPaint);

        // 3. Draw REAL RECURVE COMPOSITE BOW
        drawRealRecurveBow(canvas, bowHandX, bowHandY, stringHandX, stringHandY, rad, dir);
    }

    private void drawRealRecurveBow(Canvas canvas, float bx, float by, float px, float py, float rad, float dir) {
        float bowSpan = scale * 0.82f;
        float normalAngle = rad + (float) (Math.PI / 2);

        // Limb tips (Recurve horns)
        float topTipX = bx + (float) Math.cos(normalAngle) * bowSpan * 0.52f;
        float topTipY = by - (float) Math.sin(normalAngle) * bowSpan * 0.52f;
        float botTipX = bx - (float) Math.cos(normalAngle) * bowSpan * 0.52f;
        float botTipY = by + (float) Math.sin(normalAngle) * bowSpan * 0.52f;

        // Recurve forward curve points
        float bend = (scale * 0.20f) - (drawPower * 10f);
        float midX = bx + (float) Math.cos(rad) * bend * dir;
        float midY = by - (float) Math.sin(rad) * bend;

        // Top Recurve Limb Path
        topLimbPath.reset();
        topLimbPath.moveTo(bx, by);
        topLimbPath.cubicTo(midX + (topTipX - bx) * 0.4f, midY + (topTipY - by) * 0.4f,
                topTipX - (float) Math.cos(rad) * 12f * dir, topTipY + (float) Math.sin(rad) * 12f,
                topTipX, topTipY);

        // Bottom Recurve Limb Path
        botLimbPath.reset();
        botLimbPath.moveTo(bx, by);
        botLimbPath.cubicTo(midX + (botTipX - bx) * 0.4f, midY + (botTipY - by) * 0.4f,
                botTipX - (float) Math.cos(rad) * 12f * dir, botTipY + (float) Math.sin(rad) * 12f,
                botTipX, botTipY);

        // 1. Glowing Elemental Magic aura along the limbs
        canvas.drawPath(topLimbPath, bowGlowPaint);
        canvas.drawPath(botLimbPath, bowGlowPaint);

        // 2. Sculpted Composite Wooden Limbs
        bowLimbPaint.setStrokeWidth(6.5f);
        bowLimbPaint.setColor(Color.parseColor("#382315"));
        canvas.drawPath(topLimbPath, bowLimbPaint);
        canvas.drawPath(botLimbPath, bowLimbPaint);

        // Inner glowing core inlay
        bowLimbPaint.setStrokeWidth(2.5f);
        bowLimbPaint.setColor(bowColor);
        canvas.drawPath(topLimbPath, bowLimbPaint);
        canvas.drawPath(botLimbPath, bowLimbPaint);

        // 3. Ergonomic Riser / Leather Wrapped Handle
        canvas.drawLine(bx - (float) Math.cos(normalAngle) * 10f, by + (float) Math.sin(normalAngle) * 10f,
                bx + (float) Math.cos(normalAngle) * 10f, by - (float) Math.sin(normalAngle) * 10f, gripPaint);

        // 4. Bowstring tied from top nock to arrow nock to bottom nock
        canvas.drawLine(topTipX, topTipY, px, py, stringPaint);
        canvas.drawLine(botTipX, botTipY, px, py, stringPaint);

        // 5. Nocked Real Hunting Arrow when aiming
        if (drawPower > 0.05f) {
            float arrowLen = scale * 0.70f;
            float tipX = px + (float) Math.cos(rad) * arrowLen * dir;
            float tipY = py - (float) Math.sin(rad) * arrowLen;

            // Wooden shaft
            canvas.drawLine(px, py, tipX, tipY, arrowShaftPaint);

            // Steel Broadhead Tip
            arrowHeadPath.reset();
            arrowHeadPath.moveTo(tipX, tipY);
            arrowHeadPath.lineTo(tipX - (float) Math.cos(rad) * 14f * dir - (float) Math.sin(rad) * 5f,
                    tipY + (float) Math.sin(rad) * 14f - (float) Math.cos(rad) * 5f);
            arrowHeadPath.lineTo(tipX - (float) Math.cos(rad) * 14f * dir + (float) Math.sin(rad) * 5f,
                    tipY + (float) Math.sin(rad) * 14f + (float) Math.cos(rad) * 5f);
            arrowHeadPath.close();
            canvas.drawPath(arrowHeadPath, headMetal);

            // Feathers (Fletchings) at back
            float fX = px + (float) Math.cos(rad) * 12f * dir;
            float fY = py - (float) Math.sin(rad) * 12f;
            canvas.drawLine(fX, fY, fX - (float) Math.cos(rad) * 8f * dir - 6f, fY + 6f, fletchPaint);
            canvas.drawLine(fX, fY, fX - (float) Math.cos(rad) * 8f * dir + 6f, fY - 6f, fletchPaint);
        }
    }

    private void drawHealthHud(Canvas canvas, float hx, float hy) {
        float barW = 110f;
        float barH = 12f;
        float left = hx - barW / 2f;

        // Nameplate
        hudPaint.setTextSize(18f);
        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextAlign(Paint.Align.CENTER);
        hudPaint.setFakeBoldText(true);
        canvas.drawText(name, hx, hy - 8f, hudPaint);

        // Bar background
        hudPaint.setStyle(Paint.Style.FILL);
        hudPaint.setColor(Color.parseColor("#44000000"));
        canvas.drawRect(left - 2, hy - 2, left + barW + 2, hy + barH + 2, hudPaint);

        // Bar border
        hudPaint.setStyle(Paint.Style.STROKE);
        hudPaint.setStrokeWidth(2f);
        hudPaint.setColor(Color.parseColor("#446699"));
        canvas.drawRect(left, hy, left + barW, hy + barH, hudPaint);

        // HP Fill
        hudPaint.setStyle(Paint.Style.FILL);
        float hpRatio = (float) currentHp / maxHp;
        hudPaint.setColor(facingRight ? Color.parseColor("#00FF66") : Color.parseColor("#FF2244"));
        canvas.drawRect(left, hy, left + (barW * hpRatio), hy + barH, hudPaint);

        // HP number
        hudPaint.setTextSize(11f);
        hudPaint.setColor(Color.WHITE);
        canvas.drawText(currentHp + "", hx, hy + barH - 2f, hudPaint);
    }
}
