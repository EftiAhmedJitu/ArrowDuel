package com.arrowduel.game.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import com.arrowduel.game.R;
import com.arrowduel.game.engine.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated Loading Screen View:
 * 1. Displays the authentic high-res "ARROW DUEL" artwork logo.
 * 2. Continuously spins a glowing dual-color neon ring around the logo.
 * 3. Two animated archers on the left and right draw bows and shoot arrows at each other
 *    during the 4-second loading sequence with mid-air collisions, sparks, and SFX.
 */
public class AnimatedSplashView extends View {

    private Bitmap logoBitmap;
    private Paint paint;
    private Paint ringPaint;
    private Paint glowPaint;
    private Paint arrowPaint;
    private Paint sparkPaint;

    private float spinAngle = 0f;
    private float elapsedTime = 0f;
    private boolean soundPlayed1 = false;
    private boolean soundPlayed2 = false;
    private boolean clashSound1 = false;
    private boolean clashSound2 = false;

    // Sparks for collisions
    private static class Spark {
        float x, y, vx, vy, life = 0.4f, maxLife = 0.4f;
        int color;
        Spark(float x, float y, float vx, float vy, int color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color;
        }
    }
    private final List<Spark> sparks = new ArrayList<>();
    private final Random random = new Random();
    private SoundManager soundManager;

    public AnimatedSplashView(Context context) {
        super(context);
        init(context);
    }

    public AnimatedSplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AnimatedSplashView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);

        sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparkPaint.setStyle(Paint.Style.FILL);

        soundManager = SoundManager.getInstance(context);

        try {
            logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo_arrow_duel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float dt = 0.016f;
        elapsedTime += dt;
        spinAngle = (spinAngle + 120f * dt) % 360f;

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.45f;
        float logoSize = Math.min(w * 0.50f, h * 0.65f);

        // 1. Draw High-Res Circular Logo in Center
        if (logoBitmap != null) {
            RectF dst = new RectF(cx - logoSize / 2f, cy - logoSize / 2f, cx + logoSize / 2f, cy + logoSize / 2f);
            canvas.drawBitmap(logoBitmap, null, dst, paint);
        }

        // 2. Draw Rotating Glowing Neon Ring around the Logo ("goal sircle ta ghurbe")
        float ringRadius = logoSize * 0.51f;
        canvas.save();
        canvas.rotate(spinAngle, cx, cy);

        ringPaint.setStrokeWidth(8f);
        ringPaint.setShader(new SweepGradient(cx, cy,
                new int[]{Color.parseColor("#00E5FF"), Color.parseColor("#FF2A55"),
                        Color.parseColor("#FFD700"), Color.parseColor("#00E5FF")}, null));
        canvas.drawCircle(cx, cy, ringRadius, ringPaint);

        // Outer soft glow
        glowPaint.setStrokeWidth(18f);
        glowPaint.setColor(Color.parseColor("#4000E5FF"));
        canvas.drawCircle(cx, cy, ringRadius, glowPaint);
        canvas.restore();

        // 3. Archers on Left & Right Shooting Arrows ("2 ta archar akjon arek jon re bow marbe 4s er jonne")
        float groundY = h * 0.82f;
        float leftArcherX = w * 0.12f;
        float rightArcherX = w * 0.88f;

        // Draw Left Blue Archer
        drawArcher(canvas, leftArcherX, groundY, true, Color.parseColor("#00E5FF"), elapsedTime);

        // Draw Right Red Archer
        drawArcher(canvas, rightArcherX, groundY, false, Color.parseColor("#FF2A55"), elapsedTime);

        // Volley 1: Shoots at 1.0s, clashes at 1.6s
        // Volley 2: Shoots at 2.6s, clashes at 3.2s
        animateArrowVolley(canvas, leftArcherX, rightArcherX, groundY - 40f, 1.0f, 1.6f, elapsedTime, 1);
        animateArrowVolley(canvas, leftArcherX, rightArcherX, groundY - 40f, 2.6f, 3.2f, elapsedTime, 2);

        // 4. Update and Draw Clash Sparks
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.life -= dt;
            if (s.life <= 0) {
                sparks.remove(i);
            } else {
                sparkPaint.setColor(s.color);
                sparkPaint.setAlpha((int) ((s.life / s.maxLife) * 255));
                canvas.drawCircle(s.x, s.y, 4f, sparkPaint);
            }
        }

        postInvalidateOnAnimation();
    }

    private void animateArrowVolley(Canvas canvas, float x1, float x2, float y, float shootTime, float clashTime, float time, int volleyIndex) {
        if (time >= shootTime && time <= clashTime) {
            float t = (time - shootTime) / (clashTime - shootTime);

            // Play shoot sound once
            if (volleyIndex == 1 && !soundPlayed1) {
                soundPlayed1 = true;
                soundManager.playArrowShoot();
            } else if (volleyIndex == 2 && !soundPlayed2) {
                soundPlayed2 = true;
                soundManager.playArrowShoot();
            }

            // Blue Arrow flying from left to center
            float blueArrowTipX = x1 + (x2 - x1) * 0.5f * t;
            arrowPaint.setColor(Color.parseColor("#00E5FF"));
            arrowPaint.setStrokeWidth(5f);
            canvas.drawLine(blueArrowTipX - 35f, y, blueArrowTipX, y, arrowPaint);

            // Red Arrow flying from right to center
            float redArrowTipX = x2 - (x2 - x1) * 0.5f * t;
            arrowPaint.setColor(Color.parseColor("#FF2A55"));
            canvas.drawLine(redArrowTipX + 35f, y, redArrowTipX, y, arrowPaint);

            // Draw arrow tips
            arrowPaint.setColor(Color.WHITE);
            arrowPaint.setStrokeWidth(2f);
            canvas.drawLine(blueArrowTipX, y, blueArrowTipX - 8f, y - 4f, arrowPaint);
            canvas.drawLine(blueArrowTipX, y, blueArrowTipX - 8f, y + 4f, arrowPaint);

            canvas.drawLine(redArrowTipX, y, redArrowTipX + 8f, y - 4f, arrowPaint);
            canvas.drawLine(redArrowTipX, y, redArrowTipX + 8f, y + 4f, arrowPaint);

        } else if (time > clashTime && time < clashTime + 0.35f) {
            // Mid-air collision burst!
            float clashX = (x1 + x2) / 2f;
            if ((volleyIndex == 1 && !clashSound1) || (volleyIndex == 2 && !clashSound2)) {
                if (volleyIndex == 1) clashSound1 = true;
                else clashSound2 = true;

                soundManager.playHeadshot();
                soundManager.playExplosion();

                for (int i = 0; i < 25; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    float speed = 80f + random.nextFloat() * 160f;
                    int col = random.nextBoolean() ? Color.parseColor("#00E5FF") : Color.parseColor("#FF2A55");
                    sparks.add(new Spark(clashX, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, col));
                }
            }
        }
    }

    private void drawArcher(Canvas canvas, float x, float y, boolean facingRight, int themeColor, float time) {
        float dir = facingRight ? 1f : -1f;
        float h = 65f;

        // Flowing Cape
        Paint cPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cPaint.setColor(themeColor);
        cPaint.setStyle(Paint.Style.FILL);
        Path cape = new Path();
        float wave = (float) Math.sin(time * 6f) * 6f;
        cape.moveTo(x - dir * 4f, y - h * 0.75f);
        cape.quadTo(x - dir * 25f, y - h * 0.4f, x - dir * (32f + wave), y - 5f);
        cape.lineTo(x - dir * 15f, y - h * 0.3f);
        cape.close();
        canvas.drawPath(cape, cPaint);

        // Body Silhouette
        Paint bPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bPaint.setColor(Color.parseColor("#121829"));
        bPaint.setStyle(Paint.Style.FILL);

        // Head
        canvas.drawCircle(x, y - h * 0.85f, 10f, bPaint);

        // Glowing visor/eyes
        Paint vPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vPaint.setColor(themeColor);
        vPaint.setStrokeWidth(2.5f);
        canvas.drawLine(x, y - h * 0.85f, x + dir * 8f, y - h * 0.85f, vPaint);

        // Spine & Legs
        bPaint.setStyle(Paint.Style.STROKE);
        bPaint.setStrokeWidth(6f);
        bPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x, y - h * 0.75f, x, y - h * 0.45f, bPaint); // Spine
        canvas.drawLine(x, y - h * 0.45f, x - dir * 14f, y, bPaint); // Back leg
        canvas.drawLine(x, y - h * 0.45f, x + dir * 14f, y, bPaint); // Forward leg

        // Arms drawing bow
        float bowHandX = x + dir * 22f;
        float bowHandY = y - h * 0.60f;
        canvas.drawLine(x, y - h * 0.70f, bowHandX, bowHandY, bPaint);

        // Glowing Recurve Bow
        Paint bowP = new Paint(Paint.ANTI_ALIAS_FLAG);
        bowP.setStyle(Paint.Style.STROKE);
        bowP.setStrokeWidth(4.5f);
        bowP.setColor(themeColor);
        Path bowArc = new Path();
        bowArc.moveTo(bowHandX - dir * 6f, bowHandY - 26f);
        bowArc.quadTo(bowHandX + dir * 12f, bowHandY, bowHandX - dir * 6f, bowHandY + 26f);
        canvas.drawPath(bowArc, bowP);

        // Bowstring
        Paint strP = new Paint(Paint.ANTI_ALIAS_FLAG);
        strP.setColor(Color.WHITE);
        strP.setStrokeWidth(2f);
        canvas.drawLine(bowHandX - dir * 6f, bowHandY - 26f, x - dir * 4f, bowHandY, strP);
        canvas.drawLine(bowHandX - dir * 6f, bowHandY + 26f, x - dir * 4f, bowHandY, strP);
    }
}
