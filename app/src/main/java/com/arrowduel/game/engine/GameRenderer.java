package com.arrowduel.game.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import com.arrowduel.game.R;
import com.arrowduel.game.model.GameMode;

import java.util.Random;

/**
 * Multi-Map Environment Renderer featuring iconic real locations of Bangladesh:
 * - Level 1: Boral River (বড়াল নদী) - Real Photo Bank
 * - Level 2: Sajek Valley (সাজেক ভ্যালি) - Cloud Mountain Peaks & Green Ridges
 * - Level 3: Sundarbans (সুন্দরবন) - Ancient Mangrove Canopy & Emerald Waters
 * - Level 4: Lalbagh Fort (লালবাগ কেল্লা) - Historic Mughal Citadel & Sunset Battlements
 * - Level 5: Cox's Bazar (কক্সবাজার) - Ocean Waves & Coastal Sunset Cliffs
 * - Level 6+ / Boss: Jaflong Canyon (জাফলং) - Rocky Mountain River & Cascading Waterfalls
 */
public class GameRenderer {

    private final Context context;

    // Bitmap Cache (Lazy loads active map for optimal memory & 60fps performance)
    private int currentMapIndex = -1;
    private Bitmap rawActiveMapBitmap;
    private Bitmap scaledActiveMapBitmap;
    private int cachedW = 0;
    private int cachedH = 0;

    // Atmospheric Paints
    private final Paint basePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mistPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sporePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint terrainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path terrainPath = new Path();

    // Pre-allocated reusable graphics objects for Skia leak prevention
    private final RectF windPillRect = new RectF();
    private final RectF bannerPillRect = new RectF();
    private final Path birdPath = new Path();
    private final Path rayPath = new Path();
    private final Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float animPhase = 0f;
    private float levelBannerTimer = 4.0f; // Shows location banner for first 4s of each level

    public void resetLevelBannerTimer() {
        this.levelBannerTimer = 4.0f;
    }

    // Procedural Particle Systems
    private final int NUM_MIST = 14;
    private float[] mistX, mistY, mistSpeed, mistSize;

    private final int NUM_SPORES = 25;
    private float[] sporeX, sporeY, sporePhase;

    private final int NUM_EMBERS = 22;
    private float[] emberX, emberY, emberSpeed;

    private final Random rand = new Random(77);
    private boolean particlesInitialized = false;

    public GameRenderer() {
        this(null);
    }

    public GameRenderer(Context context) {
        this.context = context;
        initPaints();
    }

    private void initPaints() {
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(2.2f);

        bannerPaint.setTextAlign(Paint.Align.CENTER);
        bannerPaint.setFakeBoldText(true);

        birdPaint.setColor(Color.parseColor("#66263238"));
        birdPaint.setStyle(Paint.Style.STROKE);
        birdPaint.setStrokeWidth(2.2f);

        rayPaint.setColor(Color.parseColor("#18FFF9C4"));

        pillBg.setStyle(Paint.Style.FILL);
        pillBg.setColor(Color.parseColor("#850A1124"));

        pillBorder.setStyle(Paint.Style.STROKE);
        pillBorder.setStrokeWidth(1.5f);
        pillBorder.setColor(Color.parseColor("#4000E5FF"));

        bannerBg.setStyle(Paint.Style.FILL);
        bannerBg.setColor(Color.parseColor("#D90A1124"));

        bannerBorder.setStyle(Paint.Style.STROKE);
        bannerBorder.setStrokeWidth(2f);
        bannerBorder.setColor(Color.parseColor("#00E5FF"));

        terrainPaint.setStyle(Paint.Style.FILL);
    }

    private void initParticles(int w, int h) {
        // Floating Cloud / River Mist
        mistX = new float[NUM_MIST];
        mistY = new float[NUM_MIST];
        mistSpeed = new float[NUM_MIST];
        mistSize = new float[NUM_MIST];
        for (int i = 0; i < NUM_MIST; i++) {
            mistX[i] = rand.nextFloat() * w;
            mistY[i] = h * 0.35f + rand.nextFloat() * (h * 0.40f);
            mistSpeed[i] = 18f + rand.nextFloat() * 32f;
            mistSize[i] = 70f + rand.nextFloat() * 90f;
        }

        // Green Forest Spores / Fireflies
        sporeX = new float[NUM_SPORES];
        sporeY = new float[NUM_SPORES];
        sporePhase = new float[NUM_SPORES];
        for (int i = 0; i < NUM_SPORES; i++) {
            sporeX[i] = rand.nextFloat() * w;
            sporeY[i] = h * 0.30f + rand.nextFloat() * (h * 0.55f);
            sporePhase[i] = rand.nextFloat() * 6.28f;
        }

        // Boss / Fort Embers
        emberX = new float[NUM_EMBERS];
        emberY = new float[NUM_EMBERS];
        emberSpeed = new float[NUM_EMBERS];
        for (int i = 0; i < NUM_EMBERS; i++) {
            emberX[i] = rand.nextFloat() * w;
            emberY[i] = h * 0.5f + rand.nextFloat() * (h * 0.5f);
            emberSpeed[i] = 90f + rand.nextFloat() * 150f;
        }

        particlesInitialized = true;
    }

    public void update(float dt) {
        animPhase += dt * 2.5f;
        if (levelBannerTimer > 0) {
            levelBannerTimer -= dt;
        }
    }

    /**
     * Backward-compatible overload
     */
    public void renderBackground(Canvas canvas, GameMode mode, float wind) {
        renderBackground(canvas, mode, 1, wind);
    }

    /**
     * Main background rendering pipeline
     */
    public void renderBackground(Canvas canvas, GameMode mode, int level, float wind) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        if (!particlesInitialized) {
            initParticles(w, h);
        }

        // 1. Determine active map index based on level or boss mode
        int targetMapIndex;
        if (mode == GameMode.BOSS_FIGHT) {
            targetMapIndex = 5; // Jaflong Canyon / Dragon Mountain
        } else {
            // Support 16 stunning maps of Bangladesh!
            // Level 1..16 -> mapIndex 0..15
            targetMapIndex = Math.abs((level - 1) % 16);
        }

        // 2. Load and cache bitmap if map changed or canvas resized
        ensureMapLoaded(targetMapIndex, w, h);

        // 3. Render base map photo / graphic
        if (scaledActiveMapBitmap != null && !scaledActiveMapBitmap.isRecycled()) {
            canvas.drawBitmap(scaledActiveMapBitmap, 0, 0, basePaint);
        } else {
            drawProceduralFallback(canvas, w, h, targetMapIndex);
        }

        // 4. Render map-specific dynamic atmospheric animations
        switch (targetMapIndex) {
            case 0:
                renderBoralRiverAmbiance(canvas, w, h, wind);
                break;
            case 1:
                renderSajekValleyAmbiance(canvas, w, h, wind);
                break;
            case 2:
                renderSundarbansAmbiance(canvas, w, h, wind);
                break;
            case 3:
                renderLalbaghFortAmbiance(canvas, w, h, wind);
                break;
            case 4:
                renderCoxsBazarAmbiance(canvas, w, h, wind);
                break;
            case 5:
                renderJaflongAmbiance(canvas, w, h, wind, mode == GameMode.BOSS_FIGHT);
                break;
            case 6:
                renderSreemangalAmbiance(canvas, w, h, wind);
                break;
            case 7:
                renderSaintMartinsAmbiance(canvas, w, h, wind);
                break;
            case 8:
                renderNilgiriAmbiance(canvas, w, h, wind);
                break;
            case 9:
                renderMahasthangarhAmbiance(canvas, w, h, wind);
                break;
            case 10:
                renderKaptaiAmbiance(canvas, w, h, wind);
                break;
            case 11:
                renderKuakataAmbiance(canvas, w, h, wind);
                break;
            case 12:
                renderBichanakandiAmbiance(canvas, w, h, wind);
                break;
            case 13:
                renderTanguarHaorAmbiance(canvas, w, h, wind);
                break;
            case 14:
                renderSomapuraAmbiance(canvas, w, h, wind);
                break;
            case 15:
            default:
                renderInaniAmbiance(canvas, w, h, wind);
                break;
        }

        // 5. Render Wind Indicator (Pill badge safely on right side - zero overlap with Level Title)
        drawWindIndicator(canvas, w * 0.73f, 32f, wind);

        // 6. Render Stage & Map Location HUD Banner for first few seconds
        drawLocationBanner(canvas, w, h, level, targetMapIndex, mode == GameMode.BOSS_FIGHT);
    }

    /**
     * Efficient lazy-loader: Keeps only 1 active map bitmap in memory
     */
    private void ensureMapLoaded(int mapIndex, int w, int h) {
        if (context == null) return;

        boolean mapChanged = (currentMapIndex != mapIndex);
        boolean sizeChanged = (cachedW != w || cachedH != h);

        if (mapChanged || sizeChanged || (mapIndex < 6 && scaledActiveMapBitmap == null)) {
            currentMapIndex = mapIndex;
            cachedW = w;
            cachedH = h;
            levelBannerTimer = 4.0f; // Reset banner on map load to exactly 4s

            if (mapIndex >= 6) {
                // Procedural Bangladesh landscapes (zero bitmap overhead, 120 FPS high refresh rate!)
                if (scaledActiveMapBitmap != null && !scaledActiveMapBitmap.isRecycled()) {
                    scaledActiveMapBitmap.recycle();
                    scaledActiveMapBitmap = null;
                }
                if (rawActiveMapBitmap != null && !rawActiveMapBitmap.isRecycled()) {
                    rawActiveMapBitmap.recycle();
                    rawActiveMapBitmap = null;
                }
                return;
            }

            int resId;
            switch (mapIndex) {
                case 0:
                    resId = R.drawable.bg_boral_river;
                    break;
                case 1:
                    resId = R.drawable.bg_map_sajek;
                    break;
                case 2:
                    resId = R.drawable.bg_map_sundarbans;
                    break;
                case 3:
                    resId = R.drawable.bg_map_lalbagh;
                    break;
                case 4:
                    resId = R.drawable.bg_map_coxs_bazar;
                    break;
                case 5:
                default:
                    resId = R.drawable.bg_map_jaflong;
                    break;
            }

            try {
                if (scaledActiveMapBitmap != null && !scaledActiveMapBitmap.isRecycled()) {
                    scaledActiveMapBitmap.recycle();
                    scaledActiveMapBitmap = null;
                }
                if (rawActiveMapBitmap != null && !rawActiveMapBitmap.isRecycled()) {
                    rawActiveMapBitmap.recycle();
                    rawActiveMapBitmap = null;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565; // High performance & low memory
                rawActiveMapBitmap = BitmapFactory.decodeResource(context.getResources(), resId, opts);

                if (rawActiveMapBitmap != null) {
                    if (rawActiveMapBitmap.getWidth() == w && rawActiveMapBitmap.getHeight() == h) {
                        scaledActiveMapBitmap = rawActiveMapBitmap;
                        rawActiveMapBitmap = null;
                    } else {
                        scaledActiveMapBitmap = Bitmap.createScaledBitmap(rawActiveMapBitmap, w, h, true);
                        if (scaledActiveMapBitmap != rawActiveMapBitmap) {
                            rawActiveMapBitmap.recycle();
                            rawActiveMapBitmap = null;
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                scaledActiveMapBitmap = null;
            }
        }
    }

    /**
     * Map 0: Boral River Ambiance (শান্ত বড়াল নদী)
     */
    private void renderBoralRiverAmbiance(Canvas canvas, int w, int h, float wind) {
        // Soft blue sky tint
        overlayPaint.setShader(new LinearGradient(0, 0, 0, h,
                Color.parseColor("#120077B6"), Color.parseColor("#180096C7"), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, overlayPaint);

        // Glistening river water ripples
        ripplePaint.setColor(Color.parseColor("#44E0F7FA"));
        for (int i = 0; i < 6; i++) {
            float ry = h * 0.58f + i * (h * 0.06f);
            float offset = (float) Math.sin(animPhase + i * 1.5f) * 20f;
            canvas.drawLine(w * 0.28f + offset, ry, w * 0.62f + offset + 60f, ry, ripplePaint);
        }

        // Distant flying swallows/birds
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 1: Sajek Valley Ambiance (সাজেক ভ্যালি - পাহাড় ও মেঘের রাজ্য)
     */
    private void renderSajekValleyAmbiance(Canvas canvas, int w, int h, float wind) {
        // Mountain morning sun glow
        sunGlowPaint.setColor(Color.parseColor("#22FFE082"));
        canvas.drawCircle(w * 0.68f, h * 0.18f, 130f, sunGlowPaint);
        sunGlowPaint.setColor(Color.parseColor("#44FFF9C4"));
        canvas.drawCircle(w * 0.68f, h * 0.18f, 70f, sunGlowPaint);

        // Floating sea of clouds / valley mist
        mistPaint.setColor(Color.parseColor("#25FFFFFF"));
        for (int i = 0; i < NUM_MIST; i++) {
            float mx = (mistX[i] + animPhase * mistSpeed[i] * 0.4f) % (w + mistSize[i] * 2);
            float my = mistY[i] + (float) Math.sin(animPhase * 0.6f + i) * 10f;
            canvas.drawCircle(mx - mistSize[i], my, mistSize[i], mistPaint);
        }

        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 2: Sundarbans Forest Ambiance (সুন্দরবন - প্রাচীন ম্যানগ্রোভ বন)
     */
    private void renderSundarbansAmbiance(Canvas canvas, int w, int h, float wind) {
        // God rays / canopy light shafts
        rayPath.reset();
        rayPath.moveTo(w * 0.42f, 0);
        rayPath.lineTo(w * 0.54f, 0);
        rayPath.lineTo(w * 0.62f, h);
        rayPath.lineTo(w * 0.38f, h);
        rayPath.close();
        canvas.drawPath(rayPath, rayPaint);

        // Emerald jungle water sheen
        ripplePaint.setColor(Color.parseColor("#33A7F3D0"));
        for (int i = 0; i < 6; i++) {
            float ry = h * 0.62f + i * (h * 0.055f);
            float wave = (float) Math.sin(animPhase * 1.8f + i) * 18f;
            canvas.drawLine(w * 0.22f + wave, ry, w * 0.55f + wave, ry, ripplePaint);
        }

        // Drifting green forest spores & fireflies
        for (int i = 0; i < NUM_SPORES; i++) {
            float pulse = 0.4f + 0.6f * (float) Math.sin(animPhase * 2.5f + sporePhase[i]);
            float sx = (sporeX[i] + (float) Math.sin(animPhase + i) * 15f) % w;
            float sy = sporeY[i] + (float) Math.cos(animPhase + i) * 12f;

            sporePaint.setColor(Color.parseColor("#80CCFF00"));
            canvas.drawCircle(sx, sy, 4f * pulse, sporePaint);
        }
    }

    /**
     * Map 3: Lalbagh Fort Citadel Ambiance (লালবাগ কেল্লা - ঐতিহাসিক দুর্গ)
     */
    private void renderLalbaghFortAmbiance(Canvas canvas, int w, int h, float wind) {
        // Fiery sunset wash over the red bricks
        overlayPaint.setShader(new LinearGradient(0, 0, 0, h,
                Color.parseColor("#35FF6F00"), Color.parseColor("#35D84315"), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, overlayPaint);

        // Setting sun behind the minarets
        sunGlowPaint.setColor(Color.parseColor("#44FFB74D"));
        canvas.drawCircle(w * 0.51f, h * 0.42f, 85f, sunGlowPaint);
        sunGlowPaint.setColor(Color.parseColor("#88FFE082"));
        canvas.drawCircle(w * 0.51f, h * 0.42f, 40f, sunGlowPaint);

        // Torch flame embers from the ramparts
        for (int i = 0; i < NUM_EMBERS; i++) {
            float ex = (emberX[i] + (float) Math.sin(animPhase + i) * 20f + (wind * 0.08f)) % w;
            if (ex < 0) ex += w;
            float ey = (emberY[i] - (animPhase * emberSpeed[i])) % h;
            if (ey < 0) ey += h;

            emberPaint.setColor(Color.parseColor("#CCFF5722"));
            canvas.drawCircle(ex, ey, 3.5f, emberPaint);
        }
    }

    /**
     * Map 4: Cox's Bazar Coastal Cliffs Ambiance (কক্সবাজার - সমুদ্র সৈকত)
     */
    private void renderCoxsBazarAmbiance(Canvas canvas, int w, int h, float wind) {
        // Twilight coastal gradient
        overlayPaint.setShader(new LinearGradient(0, 0, 0, h,
                Color.parseColor("#155E35B1"), Color.parseColor("#20D81B60"), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, overlayPaint);

        // Crashing wave foam highlights
        ripplePaint.setColor(Color.parseColor("#55FFFFFF"));
        for (int i = 0; i < 8; i++) {
            float ry = h * 0.54f + i * (h * 0.05f);
            float wave = (float) Math.sin(animPhase * 3.0f + i * 1.5f) * 30f;
            canvas.drawLine(w * 0.05f + wave, ry, w * 0.52f + wave, ry, ripplePaint);
        }

        // Coastal seabirds
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 5: Jaflong Canyon & Waterfall Ambiance (জাফলং - পাথুরে নদী ও জলপ্রপাত)
     */
    private void renderJaflongAmbiance(Canvas canvas, int w, int h, float wind, boolean isBoss) {
        if (isBoss) {
            // Apocalyptic Blood Moon overlay for Boss Fight
            overlayPaint.setShader(new LinearGradient(0, 0, 0, h,
                    Color.parseColor("#8850020A"), Color.parseColor("#99320006"), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, overlayPaint);

            // Blood moon
            Paint bm = new Paint(Paint.ANTI_ALIAS_FLAG);
            bm.setColor(Color.parseColor("#66FF0033"));
            canvas.drawCircle(w * 0.50f, h * 0.22f, 75f, bm);
            bm.setColor(Color.parseColor("#FFFF1744"));
            canvas.drawCircle(w * 0.50f, h * 0.22f, 38f, bm);

            // Embers
            for (int i = 0; i < NUM_EMBERS; i++) {
                float ex = (emberX[i] + (float) Math.sin(animPhase + i) * 22f) % w;
                float ey = (emberY[i] - animPhase * emberSpeed[i]) % h;
                if (ey < 0) ey += h;
                emberPaint.setColor(Color.parseColor("#FFFF3D00"));
                canvas.drawCircle(ex, ey, 4f, emberPaint);
            }
        } else {
            // Waterfall spray mist
            mistPaint.setColor(Color.parseColor("#20FFFFFF"));
            for (int i = 0; i < 6; i++) {
                float my = h * 0.40f + i * 22f + (float) Math.sin(animPhase + i) * 8f;
                canvas.drawCircle(w * 0.55f + (float) Math.sin(animPhase + i) * 15f, my, 45f + i * 8f, mistPaint);
            }

            // River pebble ripples
            ripplePaint.setColor(Color.parseColor("#44B2EBF2"));
            for (int i = 0; i < 7; i++) {
                float ry = h * 0.65f + i * (h * 0.045f);
                float ripple = (float) Math.sin(animPhase * 3.5f + i * 1.8f) * 25f;
                canvas.drawLine(w * 0.25f + ripple, ry, w * 0.75f + ripple, ry, ripplePaint);
            }
        }
    }

    /**
     * Soaring distant birds across the horizon
     */
    private void drawSoaringBirds(Canvas canvas, int w, int h) {
        for (int i = 0; i < 4; i++) {
            float bx = ((animPhase * 55f + i * 95f) % (w + 220f)) - 110f;
            float by = h * 0.12f + i * 20f + (float) Math.sin(animPhase + i) * 12f;
            float flap = (float) Math.sin(animPhase * 6f + i) * 6f;

            birdPath.reset();
            birdPath.moveTo(bx - 12f, by + flap);
            birdPath.quadTo(bx - 6f, by - 6f, bx, by);
            birdPath.quadTo(bx + 6f, by - 6f, bx + 12f, by + flap);
            canvas.drawPath(birdPath, birdPaint);
        }
    }

    /**
     * Procedural landscape fallback and dedicated procedural Bangladesh maps (Maps 6 to 15)
     */
    private void drawProceduralFallback(Canvas canvas, int w, int h, int mapIndex) {
        switch (mapIndex) {
            case 6: // Sreemangal Tea Gardens (শ্রীমঙ্গল চা বাগান)
                // Sky: Emerald morning sunrise
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.65f,
                        Color.parseColor("#061A14"), Color.parseColor("#1B4D3E"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, overlayPaint);
                // Morning Golden Sun
                sunGlowPaint.setColor(Color.parseColor("#33FFF59D"));
                canvas.drawCircle(w * 0.76f, h * 0.20f, 90f, sunGlowPaint);
                sunGlowPaint.setColor(Color.parseColor("#AAFFF9C4"));
                canvas.drawCircle(w * 0.76f, h * 0.20f, 40f, sunGlowPaint);
                // Layer 1: Distant rolling tea hills
                terrainPaint.setColor(Color.parseColor("#0D3321"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.50f);
                terrainPath.cubicTo(w * 0.25f, h * 0.44f, w * 0.50f, h * 0.54f, w, h * 0.46f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Layer 2: Contoured green tea slopes
                terrainPaint.setColor(Color.parseColor("#14452B"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.62f);
                terrainPath.cubicTo(w * 0.35f, h * 0.55f, w * 0.65f, h * 0.68f, w, h * 0.58f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Layer 3: Foreground lush tea terraces
                terrainPaint.setColor(Color.parseColor("#1E5F38"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.75f);
                terrainPath.cubicTo(w * 0.30f, h * 0.70f, w * 0.70f, h * 0.80f, w, h * 0.72f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                break;

            case 7: // Saint Martin's Coral Island (সেন্ট মার্টিন প্রবাল দ্বীপ)
                // Sky: Tropical azure
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.55f,
                        Color.parseColor("#01579B"), Color.parseColor("#80DEEA"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h * 0.55f, overlayPaint);
                // Brilliant tropical sun
                sunGlowPaint.setColor(Color.parseColor("#44FFFDE7"));
                canvas.drawCircle(w * 0.72f, h * 0.18f, 110f, sunGlowPaint);
                sunGlowPaint.setColor(Color.parseColor("#DDFFFF8D"));
                canvas.drawCircle(w * 0.72f, h * 0.18f, 45f, sunGlowPaint);
                // Deep turquoise ocean
                terrainPaint.setShader(new LinearGradient(0, h * 0.50f, 0, h,
                        Color.parseColor("#006064"), Color.parseColor("#00838F"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * 0.50f, w, h, terrainPaint);
                terrainPaint.setShader(null);
                // Distant coral atoll strip
                terrainPaint.setColor(Color.parseColor("#004D40"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.55f);
                terrainPath.cubicTo(w * 0.30f, h * 0.52f, w * 0.70f, h * 0.56f, w, h * 0.53f);
                terrainPath.lineTo(w, h * 0.60f);
                terrainPath.lineTo(0, h * 0.60f);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Coral sands
                terrainPaint.setColor(Color.parseColor("#D4AC0D"));
                canvas.drawRect(0, h * 0.85f, w, h, terrainPaint);
                break;

            case 8: // Bandarban Nilgiri Cloud Peaks (বান্দরবান নীলগিরি)
                // Sky: High altitude mountain twilight
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.70f,
                        Color.parseColor("#1A1423"), Color.parseColor("#EBBAB9"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, overlayPaint);
                // High altitude star glow
                sunGlowPaint.setColor(Color.parseColor("#55FFF9C4"));
                canvas.drawCircle(w * 0.22f, h * 0.15f, 50f, sunGlowPaint);
                // Peak Ridge 1 (Far violet mountain)
                terrainPaint.setColor(Color.parseColor("#2B2240"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.48f);
                terrainPath.lineTo(w * 0.25f, h * 0.30f);
                terrainPath.lineTo(w * 0.55f, h * 0.45f);
                terrainPath.lineTo(w * 0.80f, h * 0.28f);
                terrainPath.lineTo(w, h * 0.42f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Peak Ridge 2 (Mid blue mountain)
                terrainPaint.setColor(Color.parseColor("#1B2E4B"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.58f);
                terrainPath.lineTo(w * 0.38f, h * 0.40f);
                terrainPath.lineTo(w * 0.72f, h * 0.52f);
                terrainPath.lineTo(w, h * 0.38f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Peak Ridge 3 (Near dark navy mountain)
                terrainPaint.setColor(Color.parseColor("#0F1D30"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.65f);
                terrainPath.lineTo(w * 0.48f, h * 0.52f);
                terrainPath.lineTo(w, h * 0.68f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                break;

            case 9: // Mahasthangarh Ancient Citadel (মহাস্থানগড়)
                // Sky: Sunset terracotta & ember
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.65f,
                        Color.parseColor("#2D1500"), Color.parseColor("#F39C12"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, overlayPaint);
                // Sunset sun ball
                sunGlowPaint.setColor(Color.parseColor("#44FF6D00"));
                canvas.drawCircle(w * 0.50f, h * 0.38f, 100f, sunGlowPaint);
                sunGlowPaint.setColor(Color.parseColor("#CCFFA726"));
                canvas.drawCircle(w * 0.50f, h * 0.38f, 55f, sunGlowPaint);
                // Ancient terracotta ramparts silhouette
                terrainPaint.setColor(Color.parseColor("#2B1204"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.52f);
                terrainPath.lineTo(w * 0.18f, h * 0.52f);
                terrainPath.lineTo(w * 0.18f, h * 0.44f);
                terrainPath.lineTo(w * 0.28f, h * 0.44f);
                terrainPath.lineTo(w * 0.28f, h * 0.52f);
                terrainPath.lineTo(w * 0.45f, h * 0.52f);
                terrainPath.lineTo(w * 0.48f, h * 0.40f);
                terrainPath.lineTo(w * 0.58f, h * 0.40f);
                terrainPath.lineTo(w * 0.62f, h * 0.52f);
                terrainPath.lineTo(w * 0.75f, h * 0.52f);
                terrainPath.lineTo(w * 0.75f, h * 0.46f);
                terrainPath.lineTo(w * 0.85f, h * 0.46f);
                terrainPath.lineTo(w * 0.85f, h * 0.52f);
                terrainPath.lineTo(w, h * 0.52f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                break;

            case 10: // Kaptai Lake Sapphire Basin (কাপ্তাই হ্রদ)
                // Sky: Serene mountain-lake dawn
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.60f,
                        Color.parseColor("#0F2027"), Color.parseColor("#2C5364"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h * 0.60f, overlayPaint);
                // Surrounding lush green hills
                terrainPaint.setColor(Color.parseColor("#143D2A"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.48f);
                terrainPath.cubicTo(w * 0.30f, h * 0.38f, w * 0.70f, h * 0.52f, w, h * 0.42f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Mirrored sapphire lake water
                terrainPaint.setShader(new LinearGradient(0, h * 0.52f, 0, h,
                        Color.parseColor("#0A2338"), Color.parseColor("#00738C"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * 0.52f, w, h, terrainPaint);
                terrainPaint.setShader(null);
                break;

            case 11: // Kuakata Sunrise Beach (কুয়াকাটা সূর্যোদয় সৈকত)
                // Sky: Spectacular sunrise horizon
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.62f,
                        Color.parseColor("#3A1C71"), Color.parseColor("#FFAF7B"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h * 0.62f, overlayPaint);
                // Giant Rising Sun at horizon
                sunGlowPaint.setColor(Color.parseColor("#55FF5722"));
                canvas.drawCircle(w * 0.50f, h * 0.48f, 130f, sunGlowPaint);
                sunGlowPaint.setColor(Color.parseColor("#EEFFD54F"));
                canvas.drawCircle(w * 0.50f, h * 0.48f, 65f, sunGlowPaint);
                // Bay of Bengal morning ocean
                terrainPaint.setShader(new LinearGradient(0, h * 0.55f, 0, h,
                        Color.parseColor("#15222E"), Color.parseColor("#1F3347"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * 0.55f, w, h, terrainPaint);
                terrainPaint.setShader(null);
                break;

            case 12: // Bichanakandi Stone River (বিছনাকান্দি পাথুরে নদী)
                // Sky: Misty mountain sky
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.55f,
                        Color.parseColor("#232526"), Color.parseColor("#757F9A"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h * 0.55f, overlayPaint);
                // Towering green Meghalaya mountain wall backdrop
                terrainPaint.setColor(Color.parseColor("#1A2F23"));
                terrainPath.reset();
                terrainPath.moveTo(0, h * 0.42f);
                terrainPath.lineTo(w * 0.22f, h * 0.26f);
                terrainPath.lineTo(w * 0.50f, h * 0.36f);
                terrainPath.lineTo(w * 0.78f, h * 0.22f);
                terrainPath.lineTo(w, h * 0.38f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                // Mountain stream riverbed
                terrainPaint.setShader(new LinearGradient(0, h * 0.55f, 0, h,
                        Color.parseColor("#1C3144"), Color.parseColor("#385973"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * 0.55f, w, h, terrainPaint);
                terrainPaint.setShader(null);
                break;

            case 13: // Tanguar Haor Wetland Sanctuary (টাঙ্গুয়ার হাওর)
                // Sky: Dramatic monsoon-autumn wetland sky
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.60f,
                        Color.parseColor("#141E30"), Color.parseColor("#4B6584"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h * 0.60f, overlayPaint);
                // Vast mirrored wetland water
                terrainPaint.setShader(new LinearGradient(0, h * 0.52f, 0, h,
                        Color.parseColor("#102A43"), Color.parseColor("#1D3B58"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * 0.52f, w, h, terrainPaint);
                terrainPaint.setShader(null);
                // Swamp Koroch tree silhouettes in water
                terrainPaint.setColor(Color.parseColor("#0C1B2A"));
                terrainPath.reset();
                // Tree cluster on left
                terrainPath.moveTo(w * 0.15f, h * 0.56f);
                terrainPath.lineTo(w * 0.14f, h * 0.46f);
                terrainPath.lineTo(w * 0.17f, h * 0.42f);
                terrainPath.lineTo(w * 0.18f, h * 0.46f);
                terrainPath.lineTo(w * 0.19f, h * 0.56f);
                terrainPath.close();
                // Tree cluster on right
                terrainPath.moveTo(w * 0.82f, h * 0.56f);
                terrainPath.lineTo(w * 0.81f, h * 0.44f);
                terrainPath.lineTo(w * 0.84f, h * 0.40f);
                terrainPath.lineTo(w * 0.86f, h * 0.45f);
                terrainPath.lineTo(w * 0.87f, h * 0.56f);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                break;

            case 14: // Somapura Mahavihara Temple (পাহাড়পুর বৌদ্ধ বিহার)
                // Sky: Spiritual bronze-golden dusk
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.65f,
                        Color.parseColor("#1E130B"), Color.parseColor("#8D6E63"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, overlayPaint);
                // Central tiered Buddhist Stupa pyramid silhouette
                terrainPaint.setColor(Color.parseColor("#26150D"));
                terrainPath.reset();
                float stupaMid = w * 0.50f;
                terrainPath.moveTo(stupaMid - 180f, h * 0.65f);
                terrainPath.lineTo(stupaMid - 140f, h * 0.58f);
                terrainPath.lineTo(stupaMid - 100f, h * 0.58f);
                terrainPath.lineTo(stupaMid - 70f, h * 0.50f);
                terrainPath.lineTo(stupaMid - 40f, h * 0.50f);
                terrainPath.lineTo(stupaMid, h * 0.38f); // Apex
                terrainPath.lineTo(stupaMid + 40f, h * 0.50f);
                terrainPath.lineTo(stupaMid + 70f, h * 0.50f);
                terrainPath.lineTo(stupaMid + 100f, h * 0.58f);
                terrainPath.lineTo(stupaMid + 140f, h * 0.58f);
                terrainPath.lineTo(stupaMid + 180f, h * 0.65f);
                terrainPath.lineTo(w, h * 0.65f);
                terrainPath.lineTo(w, h);
                terrainPath.lineTo(0, h);
                terrainPath.lineTo(0, h * 0.65f);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                break;

            case 15: // Inani Beach Coral Sands (ইনানী সৈকত)
            default:
                // Sky: Twilight violet ocean
                overlayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.60f,
                        Color.parseColor("#240046"), Color.parseColor("#9D4EDD"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h * 0.60f, overlayPaint);
                // Night twilight stars
                sunGlowPaint.setColor(Color.parseColor("#44E0AAFF"));
                canvas.drawCircle(w * 0.78f, h * 0.16f, 60f, sunGlowPaint);
                // Deep navy ocean
                terrainPaint.setShader(new LinearGradient(0, h * 0.54f, 0, h,
                        Color.parseColor("#080E21"), Color.parseColor("#152042"), Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * 0.54f, w, h, terrainPaint);
                terrainPaint.setShader(null);
                // Coral rock jagged boulders in surf
                terrainPaint.setColor(Color.parseColor("#111726"));
                terrainPath.reset();
                terrainPath.moveTo(w * 0.20f, h * 0.72f);
                terrainPath.lineTo(w * 0.28f, h * 0.62f);
                terrainPath.lineTo(w * 0.35f, h * 0.70f);
                terrainPath.lineTo(w * 0.42f, h * 0.58f);
                terrainPath.lineTo(w * 0.52f, h * 0.72f);
                terrainPath.close();
                canvas.drawPath(terrainPath, terrainPaint);
                break;
        }
    }

    /**
     * Map 6: Sreemangal Tea Estate Ambiance (শ্রীমঙ্গল চা বাগান)
     */
    private void renderSreemangalAmbiance(Canvas canvas, int w, int h, float wind) {
        // Morning dew mist drifting over tea slopes
        mistPaint.setColor(Color.parseColor("#20FFFFFF"));
        for (int i = 0; i < 5; i++) {
            float mx = (mistX[i] + animPhase * mistSpeed[i] * 0.3f) % (w + mistSize[i] * 2);
            float my = h * 0.50f + (float) Math.sin(animPhase * 0.8f + i) * 12f;
            canvas.drawCircle(mx - mistSize[i], my, mistSize[i] * 0.8f, mistPaint);
        }
        // Glowing golden tea pollen spores
        for (int i = 0; i < 14; i++) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(animPhase * 2.0f + sporePhase[i]);
            float sx = (sporeX[i] + (float) Math.sin(animPhase + i) * 16f) % w;
            float sy = sporeY[i] + (float) Math.cos(animPhase + i) * 10f;
            sporePaint.setColor(Color.parseColor("#90D4E157"));
            canvas.drawCircle(sx, sy, 3.5f * pulse, sporePaint);
        }
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 7: Saint Martin's Coral Island Ambiance (সেন্ট মার্টিন প্রবাল দ্বীপ)
     */
    private void renderSaintMartinsAmbiance(Canvas canvas, int w, int h, float wind) {
        // Turquoise tropical ocean wave foam ripples
        ripplePaint.setColor(Color.parseColor("#55E0F7FA"));
        for (int i = 0; i < 7; i++) {
            float ry = h * 0.56f + i * (h * 0.05f);
            float wave = (float) Math.sin(animPhase * 2.8f + i * 1.3f) * 28f;
            canvas.drawLine(w * 0.06f + wave, ry, w * 0.65f + wave, ry, ripplePaint);
        }
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 8: Bandarban Nilgiri Cloud Peaks Ambiance (বান্দরবান নীলগিরি)
     */
    private void renderNilgiriAmbiance(Canvas canvas, int w, int h, float wind) {
        // Majestic sea of clouds sweeping between peaks
        mistPaint.setColor(Color.parseColor("#35FFFFFF"));
        for (int i = 0; i < NUM_MIST; i++) {
            float mx = (mistX[i] + animPhase * mistSpeed[i] * 0.6f + wind * 0.15f) % (w + mistSize[i] * 2);
            float my = h * 0.52f + (float) Math.sin(animPhase * 0.5f + i) * 14f;
            canvas.drawCircle(mx - mistSize[i], my, mistSize[i] * 1.1f, mistPaint);
        }
    }

    /**
     * Map 9: Mahasthangarh Ancient Citadel Ambiance (মহাস্থানগড়)
     */
    private void renderMahasthangarhAmbiance(Canvas canvas, int w, int h, float wind) {
        // Ancient citadel torch embers
        for (int i = 0; i < NUM_EMBERS; i++) {
            float ex = (emberX[i] + (float) Math.sin(animPhase + i) * 18f + (wind * 0.06f)) % w;
            if (ex < 0) ex += w;
            float ey = (emberY[i] - (animPhase * emberSpeed[i])) % h;
            if (ey < 0) ey += h;
            emberPaint.setColor(Color.parseColor("#DDFF6D00"));
            canvas.drawCircle(ex, ey, 3.2f, emberPaint);
        }
    }

    /**
     * Map 10: Kaptai Lake Sapphire Basin Ambiance (কাপ্তাই হ্রদ)
     */
    private void renderKaptaiAmbiance(Canvas canvas, int w, int h, float wind) {
        // Mirrored lake ripples
        ripplePaint.setColor(Color.parseColor("#4480DEEA"));
        for (int i = 0; i < 6; i++) {
            float ry = h * 0.58f + i * (h * 0.055f);
            float wave = (float) Math.sin(animPhase * 1.6f + i * 1.4f) * 22f;
            canvas.drawLine(w * 0.20f + wave, ry, w * 0.75f + wave, ry, ripplePaint);
        }
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 11: Kuakata Sunrise Beach Ambiance (কুয়াকাটা সূর্যোদয় সৈকত)
     */
    private void renderKuakataAmbiance(Canvas canvas, int w, int h, float wind) {
        // Golden sunrise reflection path down the water
        ripplePaint.setColor(Color.parseColor("#66FFE082"));
        for (int i = 0; i < 8; i++) {
            float ry = h * 0.52f + i * (h * 0.045f);
            float spread = 30f + i * 25f;
            float wave = (float) Math.sin(animPhase * 3.2f + i * 1.2f) * 15f;
            canvas.drawLine(w * 0.50f - spread + wave, ry, w * 0.50f + spread + wave, ry, ripplePaint);
        }
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 12: Bichanakandi Stone River Ambiance (বিছনাকান্দি পাথুরে নদী)
     */
    private void renderBichanakandiAmbiance(Canvas canvas, int w, int h, float wind) {
        // Waterfall spray mist
        mistPaint.setColor(Color.parseColor("#22E0F2F1"));
        for (int i = 0; i < 5; i++) {
            float my = h * 0.44f + i * 20f + (float) Math.sin(animPhase + i) * 8f;
            canvas.drawCircle(w * 0.48f + (float) Math.sin(animPhase + i) * 15f, my, 40f + i * 8f, mistPaint);
        }
        // Rapid mountain water ripples over stones
        ripplePaint.setColor(Color.parseColor("#55B2EBF2"));
        for (int i = 0; i < 7; i++) {
            float ry = h * 0.60f + i * (h * 0.048f);
            float wave = (float) Math.sin(animPhase * 4.0f + i * 1.7f) * 25f;
            canvas.drawLine(w * 0.15f + wave, ry, w * 0.85f + wave, ry, ripplePaint);
        }
    }

    /**
     * Map 13: Tanguar Haor Wetland Sanctuary Ambiance (টাঙ্গুয়ার হাওর)
     */
    private void renderTanguarHaorAmbiance(Canvas canvas, int w, int h, float wind) {
        // Vast wetland calm ripples
        ripplePaint.setColor(Color.parseColor("#44B0BEC5"));
        for (int i = 0; i < 6; i++) {
            float ry = h * 0.56f + i * (h * 0.055f);
            float wave = (float) Math.sin(animPhase * 1.5f + i) * 20f;
            canvas.drawLine(w * 0.10f + wave, ry, w * 0.90f + wave, ry, ripplePaint);
        }
        // Migratory wild birds soaring
        drawSoaringBirds(canvas, w, h);
    }

    /**
     * Map 14: Somapura Mahavihara Temple Ambiance (পাহাড়পুর বৌদ্ধ বিহার)
     */
    private void renderSomapuraAmbiance(Canvas canvas, int w, int h, float wind) {
        // Golden spiritual halo over apex
        sunGlowPaint.setColor(Color.parseColor("#22FFE082"));
        canvas.drawCircle(w * 0.50f, h * 0.38f, 100f, sunGlowPaint);
        sunGlowPaint.setColor(Color.parseColor("#44FFF9C4"));
        canvas.drawCircle(w * 0.50f, h * 0.38f, 45f, sunGlowPaint);
        // Sacred incense motes
        for (int i = 0; i < 12; i++) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(animPhase * 2.0f + sporePhase[i]);
            float sx = (sporeX[i] + (float) Math.sin(animPhase + i) * 12f) % w;
            float sy = sporeY[i] + (float) Math.cos(animPhase + i) * 12f;
            sporePaint.setColor(Color.parseColor("#80FFE082"));
            canvas.drawCircle(sx, sy, 3.2f * pulse, sporePaint);
        }
    }

    /**
     * Map 15: Inani Beach Coral Sands Ambiance (ইনানী সৈকত)
     */
    private void renderInaniAmbiance(Canvas canvas, int w, int h, float wind) {
        // Bioluminescent surf ripples crashing against coral
        ripplePaint.setColor(Color.parseColor("#5500E5FF"));
        for (int i = 0; i < 7; i++) {
            float ry = h * 0.58f + i * (h * 0.048f);
            float wave = (float) Math.sin(animPhase * 2.8f + i * 1.5f) * 26f;
            canvas.drawLine(w * 0.12f + wave, ry, w * 0.70f + wave, ry, ripplePaint);
        }
        // Twilight ocean mist
        mistPaint.setColor(Color.parseColor("#18B388FF"));
        for (int i = 0; i < 4; i++) {
            float my = h * 0.65f + i * 20f;
            canvas.drawCircle(w * 0.35f + (float) Math.sin(animPhase + i) * 20f, my, 55f, mistPaint);
        }
    }

    /**
     * Wind Indicator: Positioned safely at top right (cx = w * 0.73f, cy = 32f)
     * Zero overlap with LEVEL 1, 2, 3 and the 3 stars!
     */
    private void drawWindIndicator(Canvas canvas, float cx, float cy, float wind) {
        // Pill background
        windPillRect.set(cx - 56f, cy - 14f, cx + 56f, cy + 14f);
        canvas.drawRoundRect(windPillRect, 14f, 14f, pillBg);
        canvas.drawRoundRect(windPillRect, 14f, 14f, pillBorder);

        // Wind Text
        windPaint.setColor(Color.WHITE);
        windPaint.setTextSize(13f);
        windPaint.setTextAlign(Paint.Align.CENTER);
        windPaint.setFakeBoldText(true);

        String text = (wind >= 0 ? "Wind +" : "Wind ") + ((int) wind) + " mph";
        canvas.drawText(text, cx - 10f, cy + 5f, windPaint);

        // Direction Arrow
        windPaint.setStyle(Paint.Style.STROKE);
        windPaint.setStrokeWidth(2.2f);
        windPaint.setColor(wind >= 0 ? Color.parseColor("#00E5FF") : Color.parseColor("#FF5252"));
        float dir = wind >= 0 ? 1f : -1f;
        float ax = cx + 32f;
        canvas.drawLine(ax - dir * 10f, cy, ax + dir * 10f, cy, windPaint);
        canvas.drawLine(ax + dir * 10f, cy, ax + dir * 5f, cy - 4f, windPaint);
        canvas.drawLine(ax + dir * 10f, cy, ax + dir * 5f, cy + 4f, windPaint);
    }

    /**
     * Stage & Map Location HUD Banner (Shown for 4s at level start, prominent & large!)
     */
    private void drawLocationBanner(Canvas canvas, int w, int h, int level, int mapIndex, boolean isBoss) {
        if (levelBannerTimer <= 0) return;

        float alpha = Math.min(1.0f, levelBannerTimer / 1.0f);
        int alpha255 = (int) (alpha * 240);

        float cx = w * 0.5f;
        float cy = h * 0.22f;

        // Expanded, prominent badge pill
        float pillHalfWidth = Math.min(w * 0.44f, 340f);
        float pillHalfHeight = 45f;
        bannerBg.setAlpha(alpha255);
        bannerPillRect.set(cx - pillHalfWidth, cy - pillHalfHeight, cx + pillHalfWidth, cy + pillHalfHeight);
        canvas.drawRoundRect(bannerPillRect, 22f, 22f, bannerBg);

        bannerBorder.setColor(isBoss ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF"));
        bannerBorder.setAlpha((int) (alpha * 220));
        bannerBorder.setStrokeWidth(2.5f);
        canvas.drawRoundRect(bannerPillRect, 22f, 22f, bannerBorder);

        String title;
        String bnTitle;

        if (isBoss) {
            title = "BOSS BATTLE • DRAGON MOUNTAIN";
            bnTitle = "চূড়ান্ত যুদ্ধ - ড্রাগন পর্বত ও জলপ্রপাত";
        } else {
            switch (mapIndex) {
                case 0:
                    title = "BORAL RIVER • DAYLIGHT BANK";
                    bnTitle = "বড়াল নদী - স্নিগ্ধ প্রভাত (Level " + level + ")";
                    break;
                case 1:
                    title = "SAJEK VALLEY • CLOUD MOUNTAIN";
                    bnTitle = "সাজেক ভ্যালি - মেঘের রাজ্য (Level " + level + ")";
                    break;
                case 2:
                    title = "SUNDARBANS • ANCIENT MANGROVE";
                    bnTitle = "সুন্দরবন - প্রাচীন ম্যানগ্রোভ বন (Level " + level + ")";
                    break;
                case 3:
                    title = "LALBAGH FORT • MUGHAL CITADEL";
                    bnTitle = "লালবাগ কেল্লা - ঐতিহাসিক দুর্গ (Level " + level + ")";
                    break;
                case 4:
                    title = "COX'S BAZAR • OCEAN COAST";
                    bnTitle = "কক্সবাজার - সমুদ্র সৈকত ও পাহাড় (Level " + level + ")";
                    break;
                case 5:
                    title = "JAFLONG • ROCKY CANYON";
                    bnTitle = "জাফলং - পাথুরে নদী ও ঝরনা (Level " + level + ")";
                    break;
                case 6:
                    title = "SREEMANGAL • TEA ESTATE";
                    bnTitle = "শ্রীমঙ্গল - সবুজ চা বাগান ও কুয়াশা (Level " + level + ")";
                    break;
                case 7:
                    title = "SAINT MARTIN'S • CORAL ISLAND";
                    bnTitle = "সেন্ট মার্টিন - নীল জলরাশি ও প্রবাল দ্বীপ (Level " + level + ")";
                    break;
                case 8:
                    title = "NILGIRI • CLOUD PEAKS";
                    bnTitle = "বান্দরবান নীলগিরি - মেঘ ছুঁয়ে যাওয়া চূড়া (Level " + level + ")";
                    break;
                case 9:
                    title = "MAHASTHANGARH • ANCIENT CITADEL";
                    bnTitle = "মহাস্থানগড় - প্রাচীন ঐতিহাসিক দুর্গ (Level " + level + ")";
                    break;
                case 10:
                    title = "KAPTAI LAKE • SAPPHIRE BASIN";
                    bnTitle = "কাপ্তাই হ্রদ - নীল পাহাড় ও জলরাশি (Level " + level + ")";
                    break;
                case 11:
                    title = "KUAKATA • SUNRISE BEACH";
                    bnTitle = "কুয়াকাটা - সাগরকন্যা ও সূর্যোদয় সৈকত (Level " + level + ")";
                    break;
                case 12:
                    title = "BICHANAKANDI • STONE RIVER";
                    bnTitle = "বিছনাকান্দি - পাথুরে নদী ও পাহাড়ের ঝরনা (Level " + level + ")";
                    break;
                case 13:
                    title = "TANGUAR HAOR • WETLAND SANCTUARY";
                    bnTitle = "টাঙ্গুয়ার হাওর - অথৈ জল ও জলজ অরণ্য (Level " + level + ")";
                    break;
                case 14:
                    title = "SOMAPURA • BUDDHIST VIHARA";
                    bnTitle = "পাহাড়পুর বৌদ্ধ বিহার - প্রাচীন প্রত্নতত্ত্ব (Level " + level + ")";
                    break;
                case 15:
                default:
                    title = "INANI BEACH • CORAL SANDS";
                    bnTitle = "ইনানী সৈকত - পাথুরে প্রবাল ও সোনালী বালুকা (Level " + level + ")";
                    break;
            }
        }

        // Title text (Prominent, large, readable)
        bannerPaint.setTextSize(20f);
        bannerPaint.setColor(isBoss ? Color.parseColor("#FF5252") : Color.parseColor("#00E5FF"));
        bannerPaint.setAlpha((int) (alpha * 255));
        canvas.drawText("📍 " + title, cx, cy - 6f, bannerPaint);

        // Bengali subtitle (Prominent, crisp white)
        bannerPaint.setTextSize(16.5f);
        bannerPaint.setColor(Color.WHITE);
        bannerPaint.setAlpha((int) (alpha * 250));
        canvas.drawText(bnTitle, cx, cy + 22f, bannerPaint);
    }
}
