package com.arrowduel.game.engine;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.arrowduel.game.entities.Archer;
import com.arrowduel.game.entities.Arrow;
import com.arrowduel.game.entities.BossDarkLord;
import com.arrowduel.game.entities.EnemyArcher;
import com.arrowduel.game.entities.FloatingText;
import com.arrowduel.game.entities.ParticleSystem;
import com.arrowduel.game.entities.Platform;
import com.arrowduel.game.entities.PlayerArcher;
import com.arrowduel.game.entities.PowerUp;
import com.arrowduel.game.model.Bow;
import com.arrowduel.game.model.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    public interface GameEventListener {
        void onGameVictory(int stars, int coinsEarned, int gemsEarned);
        void onGameDefeat();
        void onPlayerHpChanged(int currentHp, int maxHp);
        void onEnemyHpChanged(int currentHp, int maxHp);
        default void onOpponentNextReady(int level) {}
        default void onNextRoundStart(int level) {}
        default void onOpponentLeave() {}
    }

    private GameThread gameThread;
    private GameEventListener eventListener;
    private GameRenderer gameRenderer;
    private SoundManager soundManager;
    private PreferencesManager prefs;

    private GameMode gameMode = GameMode.STORY;
    private int currentLevel = 12;

    // Entities
    private final Object gameLock = new Object();
    private PlayerArcher player;
    private EnemyArcher enemy;
    private BossDarkLord boss;
    private final List<Platform> playerPlatforms = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Platform> enemyPlatforms = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Platform> centerPlatforms = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Arrow> arrows = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<FloatingText> floatingTexts = new java.util.concurrent.CopyOnWriteArrayList<>();
    private ParticleSystem particleSystem;
    private float netMoveSyncTimer = 0f;
    private float netAimSyncTimer = 0f;

    // Physics & Environment
    private float wind = 8f;
    private float windTimer = 0f;
    private float gravity = 480f;
    private float screenShake = 0f;
    private final Random random = new Random();

    // Touch controls
    private boolean isDraggingBow = false;
    private float touchStartX = 0f;
    private float touchStartY = 0f;

    // Power-ups
    private final PowerUp potion = new PowerUp(PowerUp.Type.HEALTH_POTION, 3);
    private final PowerUp shield = new PowerUp(PowerUp.Type.SHIELD, 2);
    private final PowerUp lightning = new PowerUp(PowerUp.Type.LIGHTNING_STRIKE, 1);

    private boolean isGameOver = false;
    private boolean isPaused = false;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Joystick & Movement state
    private float joystickX = 0f;
    private float joystickY = 0f;
    private float leapDebounce = 0f;

    // Aiming memory & Auto-fire (every 2.0s in last aimed direction)
    private float lastAimAngle = 38f;
    private float lastDrawPower = 0.85f;
    private boolean isAutoFireActive = false;
    private float autoFireTimer = 0f;
    private static final float AUTO_FIRE_INTERVAL = 2.0f;

    // Network Multiplayer state
    private boolean isNetworkGame = false;
    private boolean isNetworkHost = false;
    private String opponentName = "Opponent";
    private String joystickSide = "RIGHT";

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
        setFocusable(true);

        gameRenderer = new GameRenderer(context);
        soundManager = SoundManager.getInstance(context);
        prefs = new PreferencesManager(context);
        joystickSide = prefs.getJoystickSide();
        particleSystem = new ParticleSystem();
    }

    public void setJoystickSide(String side) {
        if (side != null) {
            this.joystickSide = side;
        }
    }

    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }

    public void setGameMode(GameMode mode, int level) {
        this.gameMode = mode;
        this.currentLevel = level;
    }

    public PowerUp getPotion() { return potion; }
    public PowerUp getShield() { return shield; }
    public PowerUp getLightning() { return lightning; }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setupWorld(getWidth(), getHeight());
        int targetFps = (prefs != null) ? prefs.getTargetFps() : 60;
        gameThread = new GameThread(holder, this, targetFps);
        gameThread.setRunning(true);
        gameThread.start();
    }

    public void setTargetFps(int fps) {
        if (gameThread != null) {
            gameThread.setTargetFps(fps);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        if (gameThread != null) {
            gameThread.setRunning(false);
            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void pauseGame() { isPaused = true; }
    public void resumeGame() { isPaused = false; }

    private void setupWorld(int w, int h) {
        synchronized (gameLock) {
            playerPlatforms.clear();
            enemyPlatforms.clear();
            centerPlatforms.clear();
            arrows.clear();
            floatingTexts.clear();
            isGameOver = false;

            // Setup dynamic multi-tier platform layouts & expanded arena
            setupPlatformsForLevel(w, h, currentLevel);
            if (gameRenderer != null) {
                gameRenderer.resetLevelBannerTimer();
            }
            float groundY = (!playerPlatforms.isEmpty()) ? playerPlatforms.get(0).getTop() : h * 0.78f;

            // Setup Player Archer (Human Warrior on LEFT, facing RIGHT towards enemy)
            String equippedBowId = prefs.getEquippedBow();
            Bow equippedBow = Bow.getAllBows()[0];
            for (Bow b : Bow.getAllBows()) {
                if (b.getId().equals(equippedBowId)) {
                    equippedBow = b;
                    break;
                }
            }

            player = new PlayerArcher(w * 0.18f, groundY, prefs.getPlayerName(),
                    Color.parseColor("#00E5FF"), equippedBow.getColor());
            player.setFacingRight(true);
            if (isNetworkGame) {
                player.setMaxHp(100);
                player.setHp(100);
            }
            int dmg = (int) (30 + equippedBow.getPower() * 30);
            if ("titan_bow".equals(equippedBow.getId())) {
                dmg = (int) (dmg * 1.55f); // 1.55x massive damage for Titan Extra-Dmg Bow!
            }
            float spdMult = 0.9f + equippedBow.getRange() * 0.35f;
            player.setBowStats(dmg, spdMult, equippedBow.getColor(), equippedBow.getId());

            // Setup Opponent on the RIGHT, facing LEFT towards player
            if (gameMode == GameMode.BOSS_FIGHT) {
                boss = new BossDarkLord(w * 0.82f, groundY);
                enemy = null;
            } else if (isNetworkGame) {
                boss = null;
                enemy = new EnemyArcher(w * 0.82f, groundY, opponentName,
                        Color.parseColor("#FF2A55"), Color.parseColor("#FF2A55"), 100, 1.0f, 1);
                enemy.setFacingRight(false);
                enemy.setMaxHp(100);
                enemy.setHp(100);
            } else {
                boss = null;
                // Progressive level-based HP scaling: Level 1 = 60 HP (easy), Level 2 = 80 HP, Level 3 = 100 HP, Level 4 = 125 HP
                int enemyHp;
                if (currentLevel <= 1) enemyHp = 60;
                else if (currentLevel == 2) enemyHp = 80;
                else if (currentLevel == 3) enemyHp = 100;
                else if (currentLevel == 4) enemyHp = 125;
                else enemyHp = 115 + (currentLevel * 15);

                float diff = 1.0f + (currentLevel * 0.15f);
                enemy = new EnemyArcher(w * 0.82f, groundY, "Enemy",
                        Color.parseColor("#FF2A55"), Color.parseColor("#FF2A55"), enemyHp, diff, currentLevel);
                enemy.setFacingRight(false);
            }

            if (player != null) player.setMaxTiers(playerPlatforms.size());
            if (enemy != null) enemy.setMaxTiers(enemyPlatforms.size());
        }
    }

    private void setupPlatformsForLevel(int w, int h, int level) {
        playerPlatforms.clear();
        enemyPlatforms.clear();
        centerPlatforms.clear();

        float groundY = h * 0.78f;

        // Left for player, Right for enemy
        float playerGroundRight = w * 0.44f;
        float enemyGroundLeft = w * 0.56f;

        playerPlatforms.add(new Platform(0, groundY, playerGroundRight, h, 0));
        enemyPlatforms.add(new Platform(enemyGroundLeft, groundY, w, h, 0));

        int mapId = (gameMode == GameMode.BOSS_FIGHT) ? 5 : Math.abs((level - 1) % 16);

        // Platform layout variety across all 16 maps
        int layoutVariant = (mapId == 0 || mapId == 10) ? 0 :
                            (mapId == 1 || mapId == 6 || mapId == 11) ? 1 :
                            (mapId == 2 || mapId == 7 || mapId == 12) ? 2 :
                            (mapId == 3 || mapId == 8 || mapId == 13) ? 3 : 4;

        if (layoutVariant == 0) {
            // 3 Tiers - Spacious & Natural (Boral River, Kaptai Lake)
            float midY = h * 0.60f;
            float highY = h * 0.42f;

            playerPlatforms.add(new Platform(w * 0.05f, midY, w * 0.40f, midY + 30f, 1));
            playerPlatforms.add(new Platform(w * 0.02f, highY, w * 0.28f, highY + 30f, 2));

            enemyPlatforms.add(new Platform(w * 0.60f, midY, w * 0.95f, midY + 30f, 1));
            enemyPlatforms.add(new Platform(w * 0.72f, highY, w * 0.98f, highY + 30f, 2));

            centerPlatforms.add(new Platform(w * 0.43f, h * 0.70f, w * 0.57f, h * 0.70f + 25f, 0));

        } else if (layoutVariant == 1) {
            // 4 Tiers - Staggered Mountain Steps (Sajek, Sreemangal, Kuakata)
            float t1 = h * 0.64f;
            float t2 = h * 0.49f;
            float t3 = h * 0.34f;

            playerPlatforms.add(new Platform(w * 0.07f, t1, w * 0.41f, t1 + 28f, 1));
            playerPlatforms.add(new Platform(w * 0.02f, t2, w * 0.32f, t2 + 28f, 2));
            playerPlatforms.add(new Platform(w * 0.12f, t3, w * 0.30f, t3 + 28f, 3));

            enemyPlatforms.add(new Platform(w * 0.59f, t1, w * 0.93f, t1 + 28f, 1));
            enemyPlatforms.add(new Platform(w * 0.68f, t2, w * 0.98f, t2 + 28f, 2));
            enemyPlatforms.add(new Platform(w * 0.70f, t3, w * 0.88f, t3 + 28f, 3));

            centerPlatforms.add(new Platform(w * 0.44f, h * 0.69f, w * 0.56f, h * 0.69f + 25f, 0));
            centerPlatforms.add(new Platform(w * 0.46f, h * 0.54f, w * 0.54f, h * 0.54f + 22f, 1));

        } else if (layoutVariant == 2) {
            // 4 Tiers - Twisted Canopies & Coral Steps (Sundarbans, St. Martin's, Bichanakandi)
            float t1 = h * 0.63f;
            float t2 = h * 0.48f;
            float t3 = h * 0.33f;

            playerPlatforms.add(new Platform(w * 0.04f, t1, w * 0.38f, t1 + 28f, 1));
            playerPlatforms.add(new Platform(w * 0.14f, t2, w * 0.42f, t2 + 28f, 2));
            playerPlatforms.add(new Platform(w * 0.03f, t3, w * 0.24f, t3 + 28f, 3));

            enemyPlatforms.add(new Platform(w * 0.62f, t1, w * 0.96f, t1 + 28f, 1));
            enemyPlatforms.add(new Platform(w * 0.58f, t2, w * 0.86f, t2 + 28f, 2));
            enemyPlatforms.add(new Platform(w * 0.76f, t3, w * 0.97f, t3 + 28f, 3));

            centerPlatforms.add(new Platform(w * 0.43f, h * 0.68f, w * 0.57f, h * 0.68f + 25f, 0));
            centerPlatforms.add(new Platform(w * 0.45f, h * 0.51f, w * 0.55f, h * 0.51f + 22f, 1));

        } else if (layoutVariant == 3) {
            // 4 Tiers - Fortified Battlements & Wetland Ledges (Lalbagh, Nilgiri, Tanguar Haor)
            float t1 = h * 0.63f;
            float t2 = h * 0.47f;
            float t3 = h * 0.32f;

            playerPlatforms.add(new Platform(w * 0.06f, t1, w * 0.40f, t1 + 28f, 1));
            playerPlatforms.add(new Platform(w * 0.02f, t2, w * 0.29f, t2 + 28f, 2));
            playerPlatforms.add(new Platform(w * 0.14f, t3, w * 0.33f, t3 + 28f, 3));

            enemyPlatforms.add(new Platform(w * 0.60f, t1, w * 0.94f, t1 + 28f, 1));
            enemyPlatforms.add(new Platform(w * 0.71f, t2, w * 0.98f, t2 + 28f, 2));
            enemyPlatforms.add(new Platform(w * 0.67f, t3, w * 0.86f, t3 + 28f, 3));

            centerPlatforms.add(new Platform(w * 0.42f, h * 0.66f, w * 0.58f, h * 0.66f + 25f, 0));

        } else {
            // 5 Tiers - Maximum Verticality (Cox's Bazar, Jaflong, Mahasthangarh, Somapura, Inani)
            float t1 = h * 0.66f;
            float t2 = h * 0.53f;
            float t3 = h * 0.39f;
            float t4 = h * 0.25f;

            playerPlatforms.add(new Platform(w * 0.08f, t1, w * 0.42f, t1 + 26f, 1));
            playerPlatforms.add(new Platform(w * 0.03f, t2, w * 0.33f, t2 + 26f, 2));
            playerPlatforms.add(new Platform(w * 0.14f, t3, w * 0.38f, t3 + 26f, 3));
            playerPlatforms.add(new Platform(w * 0.04f, t4, w * 0.22f, t4 + 26f, 4));

            enemyPlatforms.add(new Platform(w * 0.58f, t1, w * 0.92f, t1 + 26f, 1));
            enemyPlatforms.add(new Platform(w * 0.67f, t2, w * 0.97f, t2 + 26f, 2));
            enemyPlatforms.add(new Platform(w * 0.62f, t3, w * 0.86f, t3 + 26f, 3));
            enemyPlatforms.add(new Platform(w * 0.78f, t4, w * 0.96f, t4 + 26f, 4));

            centerPlatforms.add(new Platform(w * 0.44f, h * 0.72f, w * 0.56f, h * 0.72f + 24f, 0));
            centerPlatforms.add(new Platform(w * 0.46f, h * 0.46f, w * 0.54f, h * 0.46f + 22f, 1));
        }
    }

    public void onJoystickMoved(float xPercent, float yPercent) {
        this.joystickX = xPercent;
        this.joystickY = yPercent;
    }

    public void onJoystickReleased() {
        this.joystickX = 0f;
        this.joystickY = 0f;
        if (player != null) {
            player.stopWalking();
            if (isNetworkGame && getWidth() > 0 && getHeight() > 0) {
                NetworkDuelManager.getInstance().sendMove(player.getX() / (float) getWidth(),
                        player.getY() / (float) getHeight(), player.getCurrentTier(), false);
            }
        }
    }

    public void setNetworkMatch(boolean isNetwork, boolean isHost, String oppName) {
        this.isNetworkGame = isNetwork;
        this.isNetworkHost = isHost;
        this.opponentName = oppName;
        if (isNetwork) {
            setupNetworkListeners();
        }
    }

    private void setupNetworkListeners() {
        NetworkDuelManager.getInstance().setListener(new NetworkDuelManager.NetworkEventListener() {
            @Override
            public void onConnected(boolean isHost, String name) {}

            @Override
            public void onConnectionFailed(String error) {}

            @Override
            public void onOpponentMove(float normX, float normY, int tier, boolean isWalking) {
                synchronized (gameLock) {
                    if (enemy != null && getWidth() > 0) {
                        float oppX = getWidth() * (1.0f - normX);
                        enemy.setX(oppX);
                        if (enemyPlatforms != null && !enemyPlatforms.isEmpty()) {
                            enemy.setCurrentTier(tier, enemyPlatforms);
                        }
                        if (normY > 0 && getHeight() > 0) {
                            float oppY = getHeight() * normY;
                            enemy.setTargetY(oppY);
                        }
                        if (isWalking) {
                            enemy.startWalking(true);
                        } else {
                            enemy.stopWalking();
                        }
                    }
                }
            }

            @Override
            public void onOpponentAim(float angle, float power) {
                synchronized (gameLock) {
                    if (enemy != null) {
                        enemy.setAiming(true);
                        enemy.setAimAngle(angle);
                        enemy.setDrawPower(power);
                    }
                }
            }

            @Override
            public void onOpponentAimCancel() {
                synchronized (gameLock) {
                    if (enemy != null) {
                        enemy.setAiming(false);
                        enemy.setDrawPower(0f);
                    }
                }
            }

            @Override
            public void onOpponentShoot(float normSx, float normSy, float vx, float vy, int col, int dmg) {
                synchronized (gameLock) {
                    if (enemy != null) {
                        enemy.setAiming(false);
                        enemy.setDrawPower(0f);
                    }
                    float w = getWidth() > 0 ? getWidth() : 1920f;
                    float h = getHeight() > 0 ? getHeight() : 1080f;
                    float oppSx = w * (1.0f - normSx);
                    float oppSy = h * normSy;
                    // Mirrored horizontal velocity: flies towards player on the left
                    arrows.add(new Arrow(oppSx, oppSy, -vx, vy, Arrow.Owner.ENEMY, col, dmg));
                    soundManager.playArrowShoot();
                }
            }

            @Override
            public void onLocalPlayerHitByOpponent(int damage, boolean isHeadshot, int remainingHp) {
                synchronized (gameLock) {
                    if (player != null && !player.isDead()) {
                        player.takeDamage(damage, isHeadshot);
                        if (remainingHp >= 0 && remainingHp < player.getCurrentHp()) {
                            player.setHp(remainingHp);
                        }
                        if (isHeadshot) {
                            soundManager.playHeadshot();
                            screenShake = 6f;
                            particleSystem.emitBurst(player.getX(), player.getY() - 40f, Color.RED, 15, 150f);
                            addFloatingText(player.getX(), player.getY() - 80f, "HEADSHOT! -" + damage, Color.RED, 28f);
                        } else {
                            soundManager.playArrowHit();
                            screenShake = 3f;
                            particleSystem.emitBurst(player.getX(), player.getY() - 40f, Color.parseColor("#FF5555"), 10, 100f);
                            addFloatingText(player.getX(), player.getY() - 80f, "-" + damage, Color.parseColor("#FF5555"), 22f);
                        }
                        if (eventListener != null) eventListener.onPlayerHpChanged(player.getCurrentHp(), player.getMaxHp());
                        checkGameOver();
                    }
                }
            }

            @Override
            public void onOpponentConfirmedHit(int damage, boolean isHeadshot, int remainingHp) {
                synchronized (gameLock) {
                    if (enemy != null && !enemy.isDead()) {
                        if (remainingHp >= 0) {
                            enemy.setHp(Math.min(enemy.getCurrentHp(), remainingHp));
                        } else {
                            enemy.takeDamage(damage, isHeadshot);
                        }
                        if (eventListener != null) eventListener.onEnemyHpChanged(enemy.getCurrentHp(), enemy.getMaxHp());
                        checkGameOver();
                    }
                }
            }

            @Override
            public void onOpponentHit(int damage, boolean isHeadshot, int remainingHp) {
                // Fallback for legacy protocol
                onLocalPlayerHitByOpponent(damage, isHeadshot, remainingHp);
            }

            @Override
            public void onOpponentMatchResult(String result) {
                synchronized (gameLock) {
                    if (isGameOver) return;
                    if ("I_LOST".equals(result) || "WIN".equals(result)) {
                        // Remote player died -> Local user is victorious!
                        if (enemy != null) enemy.setHp(0);
                        triggerVictory();
                    } else if ("I_WON".equals(result) || "LOSE".equals(result)) {
                        // Remote player won -> Local user is defeated!
                        if (player != null) player.setHp(0);
                        triggerDefeat();
                    }
                }
            }

            @Override
            public void onOpponentPowerup(String type) {
                synchronized (gameLock) {
                    if (enemy != null) {
                        if ("SHIELD".equals(type)) {
                            enemy.setShield(true);
                            soundManager.playShieldBlock();
                            addFloatingText(enemy.getX(), enemy.getY() - 90f, "SHIELD ACTIVE!", Color.parseColor("#00E5FF"), 24f);
                        } else if ("POTION".equals(type)) {
                            enemy.heal(40);
                            soundManager.playPotion();
                            addFloatingText(enemy.getX(), enemy.getY() - 90f, "+40 HP", Color.parseColor("#00FF66"), 24f);
                            if (eventListener != null) eventListener.onEnemyHpChanged(enemy.getCurrentHp(), enemy.getMaxHp());
                        } else if ("LIGHTNING".equals(type)) {
                            if (player != null && !player.isDead()) {
                                soundManager.playExplosion();
                                screenShake = 6f;
                                player.takeDamage(60, true);
                                particleSystem.emitBurst(player.getX(), player.getY() - 40f, Color.parseColor("#FFD700"), 40, 260f);
                                addFloatingText(player.getX(), player.getY() - 120f, "STRUCK BY LIGHTNING! -60", Color.parseColor("#FFD700"), 30f);
                                if (eventListener != null) eventListener.onPlayerHpChanged(player.getCurrentHp(), player.getMaxHp());
                                checkGameOver();
                            }
                        }
                    }
                }
            }

            @Override
            public void onOpponentNextReady(int level) {
                if (eventListener != null) eventListener.onOpponentNextReady(level);
            }

            @Override
            public void onNextRoundStart(int level) {
                startNextRound(level);
                if (eventListener != null) eventListener.onNextRoundStart(level);
            }

            @Override
            public void onOpponentLeave() {
                if (eventListener != null) eventListener.onOpponentLeave();
            }

            @Override
            public void onDisconnected() {
                addFloatingText(getWidth() / 2f, getHeight() / 2f, "OPPONENT LEFT", Color.RED, 30f);
            }
        });
    }

    /**
     * Start Next Round in Continuous Multiplayer without tearing down socket or spawning bots
     */
    public void startNextRound(int newLevel) {
        this.currentLevel = newLevel;
        post(() -> {
            setupWorld(getWidth(), getHeight());
            resumeGame();
            if (isNetworkGame && player != null && getWidth() > 0 && getHeight() > 0) {
                NetworkDuelManager.getInstance().sendMove(
                        player.getX() / (float) getWidth(),
                        player.getY() / (float) getHeight(),
                        player.getCurrentTier(),
                        false
                );
            }
        });
    }

    /**
     * Move player UP to higher platform tier
     */
    public void movePlayerUp() {
        if (player != null && !isGameOver) {
            boolean moved = player.moveUp(playerPlatforms);
            if (moved) {
                soundManager.playBowPull();
                if (isNetworkGame && getWidth() > 0 && getHeight() > 0) {
                    NetworkDuelManager.getInstance().sendMove(player.getX() / (float) getWidth(),
                            player.getY() / (float) getHeight(), player.getCurrentTier(), false);
                }
            }
        }
    }

    /**
     * Move player DOWN to lower platform tier
     */
    public void movePlayerDown() {
        if (player != null && !isGameOver) {
            boolean moved = player.moveDown(playerPlatforms);
            if (moved) {
                soundManager.playBowPull();
                if (isNetworkGame && getWidth() > 0 && getHeight() > 0) {
                    NetworkDuelManager.getInstance().sendMove(player.getX() / (float) getWidth(),
                            player.getY() / (float) getHeight(), player.getCurrentTier(), false);
                }
            }
        }
    }

    /**
     * Use Health Potion
     */
    public void usePotion() {
        if (potion.use() && player != null && !isGameOver) {
            player.heal(40);
            soundManager.playPotion();
            addFloatingText(player.getX(), player.getY() - 100f, "+40 HP", Color.parseColor("#00FF66"), 26f);
            particleSystem.emitBurst(player.getX(), player.getY() - 50f, Color.parseColor("#00FF66"), 15, 120f);
            if (eventListener != null) eventListener.onPlayerHpChanged(player.getCurrentHp(), player.getMaxHp());
            if (isNetworkGame) {
                NetworkDuelManager.getInstance().sendPowerup("POTION");
            }
        }
    }

    /**
     * Use Energy Shield
     */
    public void useShield() {
        if (shield.use() && player != null && !isGameOver) {
            player.setShield(true);
            soundManager.playShieldBlock();
            addFloatingText(player.getX(), player.getY() - 100f, "SHIELD ACTIVE!", Color.parseColor("#00E5FF"), 24f);
            particleSystem.emitBurst(player.getX(), player.getY() - 50f, Color.parseColor("#00E5FF"), 20, 140f);
            if (isNetworkGame) {
                NetworkDuelManager.getInstance().sendPowerup("SHIELD");
            }
        }
    }

    /**
     * Use Lightning Strike on Opponent
     */
    public void useLightningStrike() {
        if (lightning.use() && !isGameOver) {
            soundManager.playExplosion();
            float targetX = (enemy != null) ? enemy.getX() : (boss != null ? boss.getX() : getWidth() * 0.8f);
            float targetY = (enemy != null) ? enemy.getY() : (boss != null ? boss.getY() : getHeight() * 0.7f);

            // Create massive lightning strike effect
            particleSystem.emitBurst(targetX, targetY - 40f, Color.parseColor("#FFD700"), 40, 260f);
            screenShake = 6f;

            if (isNetworkGame) {
                if (enemy != null && !enemy.isDead()) {
                    enemy.takeDamage(60, true);
                    addFloatingText(targetX, targetY - 120f, "LIGHTNING STRIKE! -60", Color.parseColor("#FFD700"), 30f);
                    if (eventListener != null) eventListener.onEnemyHpChanged(enemy.getCurrentHp(), enemy.getMaxHp());
                    checkGameOver();
                }
                NetworkDuelManager.getInstance().sendPowerup("LIGHTNING");
            } else if (enemy != null) {
                enemy.takeDamage(60, true);
                addFloatingText(targetX, targetY - 120f, "LIGHTNING STRIKE! -60", Color.parseColor("#FFD700"), 30f);
                if (eventListener != null) eventListener.onEnemyHpChanged(enemy.getCurrentHp(), enemy.getMaxHp());
                checkGameOver();
            } else if (boss != null) {
                boss.takeDamage(120, true);
                addFloatingText(targetX, targetY - 120f, "LIGHTNING STRIKE! -120", Color.parseColor("#FFD700"), 32f);
                checkGameOver();
            }
        }
    }

    public void quickAimShoot() {
        shootLastAim();
    }

    public void shootLastAim() {
        synchronized (gameLock) {
            if (player != null && !isGameOver && !player.isDead()) {
                player.setAimAngle(lastAimAngle);
                player.setDrawPower(lastDrawPower);
                Arrow a = player.shoot();
                if (a != null) {
                    arrows.add(a);
                    soundManager.playArrowShoot();
                    particleSystem.emitBurst(a.getX(), a.getY(), a.getElementColor(), 12, 100f);

                    if (isNetworkGame && getWidth() > 0 && getHeight() > 0) {
                        float normSx = a.getX() / (float) getWidth();
                        float normSy = a.getY() / (float) getHeight();
                        NetworkDuelManager.getInstance().sendShoot(normSx, normSy,
                                a.getVx(), a.getVy(), a.getElementColor(), a.getDamage());
                    }
                }
            }
        }
    }

    public boolean toggleAutoFire() {
        if (isGameOver || player == null || player.isDead()) {
            isAutoFireActive = false;
            return false;
        }
        isAutoFireActive = !isAutoFireActive;
        if (isAutoFireActive) {
            shootLastAim();
            autoFireTimer = 0f;
            addFloatingText(player.getX(), player.getY() - 100f, "AUTO FIRE: ON (2s)", Color.parseColor("#00E5FF"), 24f);
        } else {
            addFloatingText(player.getX(), player.getY() - 100f, "AUTO FIRE: OFF", Color.parseColor("#FF5555"), 24f);
        }
        return isAutoFireActive;
    }

    public boolean isAutoFireActive() {
        return isAutoFireActive;
    }

    public void setAutoFireActive(boolean active) {
        this.isAutoFireActive = active;
        if (active && player != null && !isGameOver && !player.isDead()) {
            shootLastAim();
            autoFireTimer = 0f;
        }
    }

    private void addFloatingText(float x, float y, String text, int color, float size) {
        FloatingText ft = new FloatingText();
        ft.init(x, y, text, color, size, 1.2f);
        floatingTexts.add(ft);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            synchronized (gameLock) {
                if (isGameOver || isPaused || player == null) return false;

                float touchX = event.getX();
                float touchY = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if ("LEFT".equalsIgnoreCase(joystickSide)) {
                            // Joystick on LEFT (bottom-left area)
                            if (touchX < getWidth() * 0.45f && touchY > getHeight() * 0.40f) {
                                return false;
                            }
                            // Fire button on RIGHT (bottom-right area)
                            if (touchX > getWidth() * 0.75f && touchY > getHeight() * 0.65f) {
                                return false;
                            }
                        } else {
                            // Joystick on RIGHT (bottom-right area)
                            if (touchX > getWidth() * 0.55f && touchY > getHeight() * 0.40f) {
                                return false;
                            }
                            // Fire button on LEFT (bottom-left area)
                            if (touchX < getWidth() * 0.25f && touchY > getHeight() * 0.65f) {
                                return false;
                            }
                        }
                        // Start pulling bow
                        isDraggingBow = true;
                        touchStartX = touchX;
                        touchStartY = touchY;
                        player.setAiming(true);
                        soundManager.playBowPull();
                        if (isNetworkGame) {
                            NetworkDuelManager.getInstance().sendAim(player.getAimAngle(), player.getDrawPower());
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (isDraggingBow) {
                            // For player on LEFT facing RIGHT:
                            // Slingshot pull: dragging left (touchStartX - touchX > 0) and down draws the bowstring
                            float dx = touchStartX - touchX;
                            float dy = touchY - touchStartY;

                            // Also support direct dragging forward towards target
                            if (dx < 0 && Math.abs(dx) > 10f) {
                                dx = touchX - touchStartX;
                                dy = touchStartY - touchY;
                            }

                            float dist = (float) Math.sqrt(dx * dx + dy * dy);
                            float power = Math.min(1.0f, dist / 220f);
                            player.setDrawPower(power);

                            // Angle calculation from drag vector
                            float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
                            if (angleDeg < -25f) angleDeg = -25f;
                            if (angleDeg > 85f) angleDeg = 85f;
                            player.setAimAngle(angleDeg);
                            lastAimAngle = angleDeg;
                            lastDrawPower = Math.max(0.35f, power);

                            if (isNetworkGame) {
                                NetworkDuelManager.getInstance().sendAim(player.getAimAngle(), player.getDrawPower());
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (isDraggingBow) {
                            isDraggingBow = false;
                            lastAimAngle = player.getAimAngle();
                            lastDrawPower = Math.max(0.35f, player.getDrawPower());
                            autoFireTimer = 0f;

                            Arrow shotArrow = player.shoot();
                            if (shotArrow != null) {
                                arrows.add(shotArrow);
                                soundManager.playArrowShoot();
                                particleSystem.emitBurst(shotArrow.getX(), shotArrow.getY(), shotArrow.getElementColor(), 12, 100f);

                                if (isNetworkGame && getWidth() > 0 && getHeight() > 0) {
                                    float normSx = shotArrow.getX() / (float) getWidth();
                                    float normSy = shotArrow.getY() / (float) getHeight();
                                    NetworkDuelManager.getInstance().sendShoot(normSx, normSy,
                                            shotArrow.getVx(), shotArrow.getVy(), shotArrow.getElementColor(), shotArrow.getDamage());
                                }
                            } else if (isNetworkGame) {
                                NetworkDuelManager.getInstance().sendAimCancel();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        if (isDraggingBow) {
                            isDraggingBow = false;
                            if (player != null) {
                                player.setAiming(false);
                            }
                            if (isNetworkGame) {
                                NetworkDuelManager.getInstance().sendAimCancel();
                            }
                        }
                        return true;
                }
            }
        } catch (Throwable t) {
            Log.e("GameView", "Unhandled exception in onTouchEvent", t);
        }
        return super.onTouchEvent(event);
    }

    public void update(float dt) {
        if (isPaused) return;

        synchronized (gameLock) {
            // Auto-repeat fire every 2.0s towards last aim angle
        if (isAutoFireActive && !isGameOver && !isPaused && player != null && !player.isDead()) {
            autoFireTimer += dt;
            if (autoFireTimer >= AUTO_FIRE_INTERVAL) {
                autoFireTimer = 0f;
                shootLastAim();
            }
        }

        // Wind drift cycle (Calm steady arena for fair network duel)
        if (!isNetworkGame) {
            windTimer += dt;
            if (windTimer > 4f) {
                windTimer = 0f;
                wind = -25f + random.nextFloat() * 50f;
            }
        } else {
            wind = 0f;
        }

        if (screenShake > 0) screenShake = Math.max(0f, screenShake - dt * 50f);

        gameRenderer.update(dt);
        particleSystem.update(dt);

        for (Platform p : playerPlatforms) p.update(dt);
        for (Platform p : enemyPlatforms) p.update(dt);
        for (Platform p : centerPlatforms) p.update(dt);

        // Update Floating Text
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update(dt);
            if (!ft.active) floatingTexts.remove(i);
        }

        // Joystick Walking & Vertical Leap
        if (leapDebounce > 0) leapDebounce -= dt;

        if (player != null && !isGameOver) {
            boolean wasWalking = player.isWalking();
            if (Math.abs(joystickX) > 0.12f) {
                float walkSpeed = 240f;
                int tier = player.getCurrentTier();
                Platform curPlatform = (tier < playerPlatforms.size()) ? playerPlatforms.get(tier) : playerPlatforms.get(0);
                player.walk(joystickX * walkSpeed * dt, curPlatform.getLeft() + 20f, curPlatform.getRight() - 20f);
            } else {
                player.stopWalking();
            }

            if (leapDebounce <= 0) {
                if (joystickY < -0.55f) {
                    movePlayerUp();
                    leapDebounce = 0.40f;
                } else if (joystickY > 0.55f) {
                    movePlayerDown();
                    leapDebounce = 0.40f;
                }
            }

            if (isNetworkGame && getWidth() > 0 && getHeight() > 0) {
                netMoveSyncTimer += dt;
                if (netMoveSyncTimer >= 0.033f || wasWalking != player.isWalking()) {
                    netMoveSyncTimer = 0f;
                    float normX = player.getX() / (float) getWidth();
                    float normY = player.getY() / (float) getHeight();
                    NetworkDuelManager.getInstance().sendMove(normX, normY, player.getCurrentTier(), player.isWalking());
                }
            }
        }

        if (player != null) player.update(dt);

        // Update Enemy (AI or Network)
        if (enemy != null && !isGameOver) {
            if (!isNetworkGame) {
                enemy.updateAI(dt, player, enemyPlatforms, arrows, wind);
                Arrow enemyArrow = enemy.tryShoot();
                if (enemyArrow != null) {
                    arrows.add(enemyArrow);
                    soundManager.playArrowShoot();
                }
            } else {
                enemy.update(dt);
            }
        }

        // Update Boss
        if (boss != null && !isGameOver) {
            boss.update(dt, player, arrows, particleSystem);
            // Check Boss Meteors hitting player
            for (BossDarkLord.Meteor m : boss.getActiveMeteors()) {
                if (m.active && player.getBodyBounds().contains(m.x, m.y)) {
                    m.active = false;
                    boolean hs = player.takeDamage(25, false);
                    soundManager.playExplosion();
                    addFloatingText(player.getX(), player.getY() - 80f, "METEOR HIT! -25", Color.RED, 26f);
                    screenShake = 5f;
                    if (eventListener != null) eventListener.onPlayerHpChanged(player.getCurrentHp(), player.getMaxHp());
                    checkGameOver();
                }
            }
        }

        // Update Arrows & Hit Detection
        for (int i = arrows.size() - 1; i >= 0; i--) {
            Arrow a = arrows.get(i);
            a.update(dt, wind, particleSystem);

            if (!a.isActive()) {
                arrows.remove(i);
                continue;
            }

            if (a.isStuck()) continue;

            // Check collision with platforms
            for (Platform p : playerPlatforms) checkPlatformCollision(a, p);
            for (Platform p : enemyPlatforms) checkPlatformCollision(a, p);
            for (Platform p : centerPlatforms) checkPlatformCollision(a, p);

            // Check collision with Player
            if (a.getOwner() != Arrow.Owner.PLAYER && player != null && !player.isDead()) {
                if (player.getHeadBounds().contains(a.getX(), a.getY())) {
                    // Headshot on player!
                    a.stick();
                    int finalDmg = (int) (a.getDamage() * 1.5f);
                    player.takeDamage(a.getDamage(), true);
                    soundManager.playHeadshot();
                    screenShake = 6f;
                    addFloatingText(player.getX(), player.getY() - 80f, "HEADSHOT! -" + finalDmg, Color.RED, 28f);
                    particleSystem.emitBurst(a.getX(), a.getY(), Color.RED, 15, 150f);
                    if (eventListener != null) eventListener.onPlayerHpChanged(player.getCurrentHp(), player.getMaxHp());
                    if (isNetworkGame) {
                        NetworkDuelManager.getInstance().sendHitSelf(finalDmg, true, player.getCurrentHp());
                    }
                    checkGameOver();
                } else if (player.getBodyBounds().contains(a.getX(), a.getY())) {
                    // Body hit on player
                    a.stick();
                    int finalDmg = a.getDamage();
                    player.takeDamage(finalDmg, false);
                    soundManager.playArrowHit();
                    screenShake = 3f;
                    addFloatingText(player.getX(), player.getY() - 80f, "-" + finalDmg, Color.parseColor("#FF5555"), 22f);
                    particleSystem.emitBurst(a.getX(), a.getY(), a.getElementColor(), 10, 100f);
                    if (eventListener != null) eventListener.onPlayerHpChanged(player.getCurrentHp(), player.getMaxHp());
                    if (isNetworkGame) {
                        NetworkDuelManager.getInstance().sendHitSelf(finalDmg, false, player.getCurrentHp());
                    }
                    checkGameOver();
                }
            }

            // Check collision with Enemy Archer
            if (a.getOwner() == Arrow.Owner.PLAYER && enemy != null && !enemy.isDead()) {
                if (enemy.getHeadBounds().contains(a.getX(), a.getY())) {
                    // Headshot on enemy!
                    a.stick();
                    soundManager.playHeadshot();
                    screenShake = 6f;
                    particleSystem.emitBurst(a.getX(), a.getY(), Color.parseColor("#FFCC00"), 25, 200f);
                    applyBowSpecialEffect(a, enemy, null, true);
                    int finalDmg = (int) (a.getDamage() * 1.5f);
                    enemy.takeDamage(a.getDamage(), true);
                    addFloatingText(enemy.getX(), enemy.getY() - 80f, "HEADSHOT! -" + finalDmg, Color.parseColor("#FFCC00"), 30f);
                    if (eventListener != null) eventListener.onEnemyHpChanged(enemy.getCurrentHp(), enemy.getMaxHp());
                    if (isNetworkGame) {
                        NetworkDuelManager.getInstance().sendHitYou(finalDmg, true, enemy.getCurrentHp());
                    }
                    checkGameOver();
                } else if (enemy.getBodyBounds().contains(a.getX(), a.getY())) {
                    // Body hit on enemy
                    a.stick();
                    soundManager.playArrowHit();
                    screenShake = 3f;
                    particleSystem.emitBurst(a.getX(), a.getY(), a.getElementColor(), 12, 120f);
                    applyBowSpecialEffect(a, enemy, null, false);
                    int finalDmg = a.getDamage();
                    enemy.takeDamage(finalDmg, false);
                    addFloatingText(enemy.getX(), enemy.getY() - 80f, "-" + finalDmg, Color.WHITE, 22f);
                    if (eventListener != null) eventListener.onEnemyHpChanged(enemy.getCurrentHp(), enemy.getMaxHp());
                    if (isNetworkGame) {
                        NetworkDuelManager.getInstance().sendHitYou(finalDmg, false, enemy.getCurrentHp());
                    }
                    checkGameOver();
                }
            }

            // Check collision with Boss Dark Lord
            if (a.getOwner() == Arrow.Owner.PLAYER && boss != null && !boss.isDead()) {
                if (boss.getHeadBounds().contains(a.getX(), a.getY())) {
                    a.stick();
                    boss.takeDamage(a.getDamage(), true);
                    soundManager.playHeadshot();
                    screenShake = 7f;
                    particleSystem.emitBurst(a.getX(), a.getY(), Color.parseColor("#FFCC00"), 30, 220f);
                    applyBowSpecialEffect(a, null, boss, true);
                    addFloatingText(boss.getX(), boss.getY() - 140f, "CRITICAL EYE STRIKE!", Color.parseColor("#FFCC00"), 30f);
                    checkGameOver();
                } else if (boss.getBodyBounds().contains(a.getX(), a.getY())) {
                    a.stick();
                    boss.takeDamage(a.getDamage(), false);
                    soundManager.playArrowHit();
                    screenShake = 4f;
                    particleSystem.emitBurst(a.getX(), a.getY(), a.getElementColor(), 15, 140f);
                    applyBowSpecialEffect(a, null, boss, false);
                    addFloatingText(boss.getX(), boss.getY() - 100f, "-" + a.getDamage(), Color.WHITE, 24f);
                    checkGameOver();
                }
            }
        }
        }
    }

    private void checkPlatformCollision(Arrow a, Platform p) {
        if (a.getX() >= p.getLeft() && a.getX() <= p.getRight() &&
                a.getY() >= p.getTop() && a.getY() <= p.getBottom() + 10f) {
            a.stick();
            soundManager.playArrowHit();
            particleSystem.emitBurst(a.getX(), p.getTop(), Color.parseColor("#556688"), 8, 80f);
        }
    }

    private void applyBowSpecialEffect(Arrow a, EnemyArcher targetEnemy, BossDarkLord targetBoss, boolean isHeadshot) {
        String bowId = a.getBowId();
        if (bowId == null) return;

        float targetX = (targetEnemy != null) ? targetEnemy.getX() : (targetBoss != null ? targetBoss.getX() : 0);
        float targetY = (targetEnemy != null) ? targetEnemy.getY() : (targetBoss != null ? targetBoss.getY() : 0);

        switch (bowId) {
            case "freeze_bow":
                if (targetEnemy != null) {
                    targetEnemy.freeze(2.5f);
                    soundManager.playPotion();
                    addFloatingText(targetX, targetY - 110f, "FROZEN \u2744\uFE0F (2.5s)", Color.parseColor("#80D8FF"), 28f);
                    particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#80D8FF"), 30, 180f);
                } else if (targetBoss != null) {
                    targetBoss.freeze(2.5f);
                    addFloatingText(targetX, targetY - 120f, "BOSS FROZEN \u2744\uFE0F (2.5s)", Color.parseColor("#80D8FF"), 30f);
                    particleSystem.emitBurst(targetX, targetY - 70f, Color.parseColor("#80D8FF"), 30, 200f);
                }
                break;

            case "titan_bow":
                screenShake = 7f;
                soundManager.playExplosion();
                addFloatingText(targetX, targetY - 110f, "TITAN SLAM \uD83D\uDCA5", Color.parseColor("#FF0055"), 30f);
                particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#FF0055"), 35, 240f);
                break;

            case "fire_bow":
                if (targetEnemy != null) {
                    targetEnemy.ignite(3.0f, 10);
                    soundManager.playExplosion();
                    addFloatingText(targetX, targetY - 110f, "INFERNO BURN \uD83D\uDD25", Color.parseColor("#FF4500"), 26f);
                    particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#FF4500"), 25, 200f);
                }
                break;

            case "golden_bow":
                prefs.addCoins(25);
                soundManager.playPotion();
                addFloatingText(targetX, targetY - 110f, "+25 MIDAS BONUS \uD83E\uDE99", Color.parseColor("#FFD700"), 26f);
                particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#FFD700"), 20, 150f);
                break;

            case "venom_bow":
                if (targetEnemy != null) {
                    targetEnemy.poison(3.0f, 8);
                    addFloatingText(targetX, targetY - 110f, "TOXIC POISON \uD83E\uDDEA", Color.parseColor("#00FF66"), 26f);
                    particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#00FF66"), 25, 170f);
                }
                break;

            case "lightning_bow":
                if (random.nextFloat() < 0.35f) {
                    if (targetEnemy != null) {
                        targetEnemy.freeze(1.2f);
                        addFloatingText(targetX, targetY - 110f, "SHOCKED \u26A1 (1.2s)", Color.parseColor("#00E5FF"), 26f);
                        particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#00E5FF"), 20, 160f);
                    }
                }
                break;

            case "void_bow":
                screenShake = 5f;
                addFloatingText(targetX, targetY - 110f, "VOID GRAVITY \uD83C\uDF00", Color.parseColor("#B829FF"), 28f);
                particleSystem.emitBurst(targetX, targetY - 50f, Color.parseColor("#B829FF"), 30, 220f);
                break;
        }
    }

    public void triggerDefeat() {
        if (isGameOver) return;
        isGameOver = true;
        isAutoFireActive = false;
        if (isNetworkGame) {
            NetworkDuelManager.getInstance().sendMatchResult("I_LOST");
        }
        post(() -> {
            if (eventListener != null) eventListener.onGameDefeat();
        });
    }

    public void triggerVictory() {
        if (isGameOver) return;
        isGameOver = true;
        isAutoFireActive = false;
        soundManager.playVictoryFanfare();
        if (isNetworkGame) {
            NetworkDuelManager.getInstance().sendMatchResult("I_WON");
        }
        int stars = (player != null && player.getCurrentHp() > 70) ? 3 : (player != null && player.getCurrentHp() > 30 ? 2 : 1);
        int coins = 120 + currentLevel * 10;
        if ("golden_bow".equals(prefs.getEquippedBow())) {
            coins = (int) (coins * 1.5f); // +50% Midas Gold bonus!
        }
        int gems = 10;
        prefs.addCoins(coins);
        prefs.addGems(gems);
        prefs.unlockNextLevel(currentLevel);
        prefs.setLevelStars(currentLevel, stars);
        final int finalCoins = coins;
        post(() -> {
            if (eventListener != null) eventListener.onGameVictory(stars, finalCoins, gems);
        });
    }

    private void checkGameOver() {
        if (isGameOver) return;

        if (player != null && player.isDead()) {
            triggerDefeat();
        } else if (enemy != null && enemy.isDead()) {
            triggerVictory();
        } else if (boss != null && boss.isDead()) {
            isGameOver = true;
            isAutoFireActive = false;
            soundManager.playVictoryFanfare();
            int coins = 500;
            if ("golden_bow".equals(prefs.getEquippedBow())) {
                coins = (int) (coins * 1.5f);
            }
            int gems = 50;
            prefs.addCoins(coins);
            prefs.addGems(gems);
            final int finalCoins = coins;
            post(() -> {
                if (eventListener != null) eventListener.onGameVictory(3, finalCoins, gems);
            });
        }
    }

    public void drawGame(Canvas canvas) {
        if (canvas == null) return;

        synchronized (gameLock) {
            boolean translated = false;
            // Apply screen shake
            if (screenShake > 0) {
                canvas.save();
                translated = true;
                float shakeX = (random.nextFloat() - 0.5f) * screenShake;
                float shakeY = (random.nextFloat() - 0.5f) * screenShake;
                canvas.translate(shakeX, shakeY);
            }

            try {
                // 1. Draw Boral River Backdrop & Dynamic Level Atmosphere
                gameRenderer.renderBackground(canvas, gameMode, currentLevel, wind);

                // 2. Draw Platforms (Multi-tier cliffs & ledges)
                for (Platform p : playerPlatforms) p.draw(canvas);
                for (Platform p : centerPlatforms) p.draw(canvas);
                for (Platform p : enemyPlatforms) p.draw(canvas);

                // 3. Draw Player Trajectory Preview
                if (player != null) {
                    player.drawTrajectory(canvas, wind, gravity);
                }

                // 4. Draw Characters
                if (player != null) player.draw(canvas);
                if (enemy != null) enemy.draw(canvas);
                if (boss != null) boss.draw(canvas);

                // 5. Draw Arrows & Particles
                for (Arrow a : arrows) a.draw(canvas);
                particleSystem.draw(canvas);

                // 6. Draw Floating Combat Text
                for (FloatingText ft : floatingTexts) {
                    ft.draw(canvas, textPaint);
                }
            } finally {
                if (translated) {
                    canvas.restore();
                }
            }
        }
    }
}
