package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.List;
import java.util.Random;

public class EnemyArcher extends Archer {

    private float actionCooldown = 0.8f;
    private float aimTimer = 0f;
    private float moveCooldown = 1.6f;
    private final Random random = new Random();
    private int damage = 25;
    private float difficulty = 1.0f; // Scales with level
    private int level = 1;

    private boolean readyToShoot = false;
    private float calculatedSpeed = 1050f;
    private int arrowElementColor = Color.parseColor("#FF2A55");
    private float burstTimer = 0f;
    private float patrolTimer = 0f;
    private float patrolDir = 1f;

    // Status debuffs from player weapons
    private float frozenTimer = 0f;
    private float burnTimer = 0f;
    private float burnTickTimer = 0f;
    private int burnDamage = 10;
    private float poisonTimer = 0f;
    private float poisonTickTimer = 0f;
    private int poisonDamage = 8;

    public void freeze(float duration) {
        this.frozenTimer = Math.max(this.frozenTimer, duration);
        this.isAiming = false;
        this.readyToShoot = false;
    }

    public void ignite(float duration, int dps) {
        this.burnTimer = duration;
        this.burnDamage = dps;
        this.burnTickTimer = 0f;
    }

    public void poison(float duration, int dps) {
        this.poisonTimer = duration;
        this.poisonDamage = dps;
        this.poisonTickTimer = 0f;
    }

    public boolean isFrozen() { return frozenTimer > 0f; }

    public EnemyArcher(float x, float y, String name, int capeColor, int bowColor, int maxHp, float difficulty) {
        this(x, y, name, capeColor, bowColor, maxHp, difficulty, 1);
    }

    public EnemyArcher(float x, float y, String name, int capeColor, int bowColor, int maxHp, float difficulty, int level) {
        super(x, y, false, name, capeColor, bowColor); // Facing left towards player on the left
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.difficulty = Math.max(1.0f, difficulty);
        this.level = level;
        setupStats();
    }

    public void setLevel(int level) {
        this.level = level;
        setupStats();
    }

    private void setupStats() {
        // Base damage scales noticeably: Level 1 = 16, Level 2 = 22, Level 3 = 28, Level 4 = 34, Level 5+ = 40+
        if (level <= 1) {
            this.damage = 16;
        } else if (level == 2) {
            this.damage = 22;
        } else if (level == 3) {
            this.damage = 28;
        } else if (level == 4) {
            this.damage = 34;
        } else {
            this.damage = Math.min(52, 34 + (level - 4) * 4);
        }

        // Visual and element variation based on level
        if (level >= 5) {
            arrowElementColor = Color.parseColor("#BF00FF"); // Void Shadow Arrow
            setBowColor(Color.parseColor("#BF00FF"));
        } else if (level >= 3) {
            arrowElementColor = Color.parseColor("#FF6600"); // Flaming Arrow
            setBowColor(Color.parseColor("#FF6600"));
        } else {
            arrowElementColor = Color.parseColor("#FF2A55"); // Crimson Arrow
            setBowColor(Color.parseColor("#FF2A55"));
        }

        // Initial attack cooldown: Level 1 has a gentle 2.2s warmup
        if (level <= 1) {
            actionCooldown = 2.2f + random.nextFloat() * 0.6f;
        } else if (level == 2) {
            actionCooldown = 1.7f + random.nextFloat() * 0.5f;
        } else {
            actionCooldown = Math.max(0.7f, 1.4f - (level * 0.1f)) + random.nextFloat() * 0.3f;
        }
    }

    public void updateAI(float dt, Archer player, List<Platform> enemyPlatforms, List<Arrow> incomingArrows, float wind) {
        super.update(dt);
        if (isDead()) return;

        // 0. Handle Freeze (Immobilized in ice solid!)
        if (frozenTimer > 0) {
            frozenTimer -= dt;
            isAiming = false;
            readyToShoot = false;
            return;
        }

        // Burn damage over time
        if (burnTimer > 0) {
            burnTimer -= dt;
            burnTickTimer += dt;
            if (burnTickTimer >= 0.9f) {
                burnTickTimer = 0f;
                takeDamage(burnDamage, false);
            }
        }

        // Poison damage over time
        if (poisonTimer > 0) {
            poisonTimer -= dt;
            poisonTickTimer += dt;
            if (poisonTickTimer >= 0.9f) {
                poisonTickTimer = 0f;
                takeDamage(poisonDamage, false);
            }
        }

        // 1. Up / Down Platform Mobility & Dodging
        moveCooldown -= dt;

        // Threat detection: incoming player arrow
        boolean incomingDanger = false;
        for (Arrow a : incomingArrows) {
            if (a.getOwner() == Arrow.Owner.PLAYER && a.isActive() && !a.isStuck()) {
                if (a.getX() < x && (x - a.getX()) < 450f && Math.abs(a.getY() - (y - scale * 0.5f)) < 140f) {
                    incomingDanger = true;
                    break;
                }
            }
        }

        int maxT = (enemyPlatforms != null && !enemyPlatforms.isEmpty()) ? enemyPlatforms.size() : maxTiers;

        // Tactical dodge reaction (Level 1: 15% dodge chance, Level 2: 35%, Level 3: 60%, Level 4+: 85%)
        float dodgeChance = (level <= 1) ? 0.15f : (level == 2 ? 0.35f : (level == 3 ? 0.60f : 0.85f));
        if (incomingDanger && moveCooldown <= 0.6f && random.nextFloat() < dodgeChance) {
            if (currentTier == 0) {
                moveUp(enemyPlatforms);
            } else if (currentTier >= maxT - 1) {
                moveDown(enemyPlatforms);
            } else {
                if (random.nextBoolean()) moveUp(enemyPlatforms);
                else moveDown(enemyPlatforms);
            }
            moveCooldown = Math.max(1.0f, 3.2f - level * 0.4f);
        } else if (moveCooldown <= 0) {
            // Strategic repositioning up/down (rare on Level 1, active on higher levels)
            float repositionChance = (level <= 1) ? 0.25f : Math.min(0.85f, 0.40f + level * 0.10f);
            moveCooldown = (level <= 1) ? (3.5f + random.nextFloat() * 1.5f) : (2.0f + random.nextFloat() * 1.5f);
            if (random.nextFloat() < repositionChance) {
                if (currentTier < player.getCurrentTier() && currentTier < maxT - 1) {
                    moveUp(enemyPlatforms);
                } else if (currentTier > player.getCurrentTier() && currentTier > 0) {
                    moveDown(enemyPlatforms);
                } else {
                    if (random.nextBoolean()) moveUp(enemyPlatforms);
                    else moveDown(enemyPlatforms);
                }
            }
        }

        // Horizontal tactical patrolling on current platform
        if (enemyPlatforms != null && currentTier < enemyPlatforms.size()) {
            Platform curP = enemyPlatforms.get(currentTier);
            float patrolSpeed = 60f + level * 15f;
            patrolTimer -= dt;
            if (patrolTimer <= 0) {
                patrolTimer = 1.0f + random.nextFloat() * 1.8f;
                patrolDir = (random.nextFloat() < 0.5f) ? 1f : -1f;
            }
            float minX = curP.getLeft() + 25f;
            float maxX = curP.getRight() - 25f;
            if (maxX > minX) {
                walk(patrolDir * patrolSpeed * dt, minX, maxX);
            }
        }

        // Burst shot timer (at higher levels / difficulty)
        if (burstTimer > 0) {
            burstTimer -= dt;
            if (burstTimer <= 0) {
                readyToShoot = true;
            }
        }

        // 2. Aiming and Firing Logic (Fast and active!)
        actionCooldown -= dt;
        if (actionCooldown <= 0 && !isAiming && !readyToShoot) {
            startAiming(player, wind);
        }

        if (isAiming) {
            aimTimer += dt;
            // Aim duration: Level 1 gives plenty of reaction time (1.35s), Level 2 = 0.95s, Level 3 = 0.70s, Level 4+ = 0.45s
            float aimDuration;
            if (level <= 1) aimDuration = 1.35f;
            else if (level == 2) aimDuration = 0.95f;
            else if (level == 3) aimDuration = 0.70f;
            else aimDuration = Math.max(0.38f, 0.58f - (level * 0.04f));

            drawPower = Math.min(1.0f, aimTimer / aimDuration);
            if (aimTimer >= aimDuration) {
                isAiming = false;
                readyToShoot = true;

                // Follow-up burst shot chance on Level 4+
                if (level >= 4 && random.nextFloat() < 0.28f) {
                    burstTimer = 0.22f;
                }

                // Attack turnaround cooldown
                float cdBase;
                if (level <= 1) cdBase = 2.1f;
                else if (level == 2) cdBase = 1.6f;
                else if (level == 3) cdBase = 1.25f;
                else cdBase = Math.max(0.65f, 1.1f - (level * 0.08f));

                actionCooldown = cdBase + random.nextFloat() * 0.4f;
            }
        }
    }

    private void startAiming(Archer player, float wind) {
        isAiming = true;
        aimTimer = 0f;

        // Player target position (center of player body)
        float targetX = player.getX();
        float targetY = player.getY() - player.getScale() * 0.55f;

        // Launch position estimation
        float dir = facingRight ? 1f : -1f;
        float spawnX = x + dir * scale * 0.35f;
        float spawnY = (y - scale * 0.70f);

        // Distance: Enemy shooting towards player
        float D = Math.max(100f, Math.abs(targetX - spawnX));
        float H = targetY - spawnY; // Positive if player is below release point

        float g = 480f; // Gravity acceleration

        // Scale velocity so arrow reliably spans the gap with swift velocity
        float v = Math.max(980f, 750f + (D * 0.36f) + (difficulty * 60f));

        // Wind compensation estimation: flight time T ~ D / (v * cos(35 deg))
        float approxT = D / (v * 0.82f);
        float windDrift = 0.5f * (-wind * dir) * approxT * approxT;
        float Deff = Math.max(50f, D + windDrift);

        // Ballistic trajectory equation:
        // tan^2(theta) - 2p*tan(theta) + (1 - 2pH/Deff) = 0
        // where p = v^2 / (g * Deff)
        float p = (v * v) / (g * Deff);
        float disc = p * p - 1f + (2f * p * H / Deff);

        float thetaDeg;
        if (disc >= 0) {
            float tanTheta = p - (float) Math.sqrt(disc);
            thetaDeg = (float) Math.toDegrees(Math.atan(tanTheta));
        } else {
            v = (float) Math.sqrt(g * (Deff + Math.sqrt(Deff * Deff + H * H))) * 1.15f;
            thetaDeg = 36f;
        }

        thetaDeg = Math.max(-25f, Math.min(65f, thetaDeg));

        // Accuracy spread per level:
        // Level 1: spread +-6.5 deg (fair and easy, beginner friendly)
        // Level 2: spread +-3.8 deg
        // Level 3: spread +-2.2 deg
        // Level 4+: pinpoint laser accuracy
        float spread;
        if (level <= 1) spread = 6.5f;
        else if (level == 2) spread = 3.8f;
        else if (level == 3) spread = 2.2f;
        else spread = Math.max(0.6f, 1.6f - (level * 0.18f));

        float error = (random.nextFloat() - 0.5f) * spread;

        // Lead player if walking (level 3+)
        if (level >= 3 && player.isWalking()) {
            float leadOffset = (random.nextFloat() - 0.5f) * 1.5f;
            thetaDeg += leadOffset;
        }

        aimAngle = thetaDeg + error;
        aimAngle = Math.max(-25f, Math.min(75f, aimAngle));
        calculatedSpeed = v;
    }

    public Arrow tryShoot() {
        if (readyToShoot) {
            readyToShoot = false;
            aimTimer = 0f;

            float rad = (float) Math.toRadians(aimAngle);
            float dir = facingRight ? 1f : -1f;
            float vx = (float) Math.cos(rad) * calculatedSpeed * dir; // Shoot towards player
            float vy = -(float) Math.sin(rad) * calculatedSpeed;

            float spawnX = x + (float) Math.cos(rad) * scale * 0.44f * dir;
            float spawnY = (y - scale * 0.76f) + 12f - (float) Math.sin(rad) * scale * 0.44f;

            return new Arrow(spawnX, spawnY, vx, vy, Arrow.Owner.ENEMY, arrowElementColor, damage);
        }
        return null;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (frozenTimer > 0 && !isDead()) {
            Paint icePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            icePaint.setColor(Color.parseColor("#4D00F0FF"));
            icePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y - scale * 0.45f, scale * 0.52f, icePaint);

            Paint iceRing = new Paint(Paint.ANTI_ALIAS_FLAG);
            iceRing.setColor(Color.parseColor("#80D8FF"));
            iceRing.setStyle(Paint.Style.STROKE);
            iceRing.setStrokeWidth(3.5f);
            canvas.drawCircle(x, y - scale * 0.45f, scale * 0.52f, iceRing);

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.parseColor("#80D8FF"));
            textPaint.setTextSize(20f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);
            canvas.drawText("FROZEN \u2744\uFE0F", x, y - scale * 0.95f, textPaint);
        } else if (burnTimer > 0 && !isDead()) {
            Paint burnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            burnPaint.setColor(Color.parseColor("#FF5500"));
            burnPaint.setTextSize(18f);
            burnPaint.setTextAlign(Paint.Align.CENTER);
            burnPaint.setFakeBoldText(true);
            canvas.drawText("BURNING \uD83D\uDD25", x, y - scale * 0.95f, burnPaint);
        } else if (poisonTimer > 0 && !isDead()) {
            Paint poisonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            poisonPaint.setColor(Color.parseColor("#00FF66"));
            poisonPaint.setTextSize(18f);
            poisonPaint.setTextAlign(Paint.Align.CENTER);
            poisonPaint.setFakeBoldText(true);
            canvas.drawText("POISONED \uD83E\uDDEA", x, y - scale * 0.95f, poisonPaint);
        }
    }
}
