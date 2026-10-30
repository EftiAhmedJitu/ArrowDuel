package com.arrowduel.game.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom View rendering the official "ARROW DUEL" circular emblem logo:
 * - Left: Blue Stickman Archer with glowing lightning bow
 * - Right: Red Stickman Archer with glowing fire bow
 * - Center: Moonlit sky, silhouette cliffs, torches
 * - Bottom/Center: Stylized "ARROW DUEL" emblem with target crosshair
 */
public class LogoView extends View {

    private Paint paint;
    private Paint textPaint;
    private Paint glowPaint;
    private RectF circleBounds;
    private Path clipPath;
    private float animPhase = 0f;

    public LogoView(Context context) {
        super(context);
        init();
    }

    public LogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LogoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleBounds = new RectF();
        clipPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        animPhase += 0.05f;
        float pulse = (float) Math.sin(animPhase) * 0.1f + 1.0f;

        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h) * 0.92f;
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = size / 2f;

        circleBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // 1. Draw outer circular glow
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(radius * 0.12f);
        glowPaint.setShader(new SweepGradient(cx, cy, 
                new int[]{Color.parseColor("#00E5FF"), Color.parseColor("#FF2A55"), Color.parseColor("#00E5FF")}, 
                new float[]{0f, 0.5f, 1f}));
        canvas.drawCircle(cx, cy, radius * 0.94f, glowPaint);

        // 2. Clip inside circular badge
        canvas.save();
        clipPath.reset();
        clipPath.addCircle(cx, cy, radius * 0.90f, Path.Direction.CW);
        canvas.clipPath(clipPath);

        // Background sky gradient
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx, cy - radius, cx, cy + radius,
                Color.parseColor("#0B132B"), Color.parseColor("#1C2541"), Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, paint);

        // Full Moon in center background
        paint.setShader(null);
        paint.setColor(Color.parseColor("#FFE8A3"));
        canvas.drawCircle(cx, cy - radius * 0.25f, radius * 0.38f, paint);

        // Moon glow
        paint.setColor(Color.parseColor("#33FFE8A3"));
        canvas.drawCircle(cx, cy - radius * 0.25f, radius * 0.46f, paint);

        // Distant mountains
        paint.setColor(Color.parseColor("#141D35"));
        Path mtn = new Path();
        mtn.moveTo(cx - radius, cy + radius * 0.4f);
        mtn.lineTo(cx - radius * 0.5f, cy - radius * 0.1f);
        mtn.lineTo(cx, cy + radius * 0.2f);
        mtn.lineTo(cx + radius * 0.6f, cy - radius * 0.15f);
        mtn.lineTo(cx + radius, cy + radius * 0.4f);
        mtn.lineTo(cx + radius, cy + radius);
        mtn.lineTo(cx - radius, cy + radius);
        mtn.close();
        canvas.drawPath(mtn, paint);

        // Left Cliff (Player side)
        paint.setColor(Color.parseColor("#090D18"));
        Path leftCliff = new Path();
        leftCliff.moveTo(cx - radius, cy - radius * 0.05f);
        leftCliff.lineTo(cx - radius * 0.22f, cy - radius * 0.05f);
        leftCliff.lineTo(cx - radius * 0.15f, cy + radius);
        leftCliff.lineTo(cx - radius, cy + radius);
        leftCliff.close();
        canvas.drawPath(leftCliff, paint);

        // Right Cliff (Enemy side)
        Path rightCliff = new Path();
        rightCliff.moveTo(cx + radius * 0.22f, cy - radius * 0.05f);
        rightCliff.lineTo(cx + radius, cy - radius * 0.05f);
        rightCliff.lineTo(cx + radius, cy + radius);
        rightCliff.lineTo(cx + radius * 0.15f, cy + radius);
        rightCliff.close();
        canvas.drawPath(rightCliff, paint);

        // Torch fires on cliffs
        drawTorch(canvas, cx - radius * 0.35f, cy - radius * 0.05f, radius * 0.08f, animPhase);
        drawTorch(canvas, cx + radius * 0.35f, cy - radius * 0.05f, radius * 0.08f, animPhase + 1.5f);

        // 3. Draw Blue Stickman Archer (Left)
        drawArcherStickman(canvas, cx - radius * 0.52f, cy - radius * 0.05f, radius * 0.55f, 
                true, Color.parseColor("#00E5FF"), Color.parseColor("#00B0FF"), animPhase);

        // 4. Draw Red Stickman Archer (Right)
        drawArcherStickman(canvas, cx + radius * 0.52f, cy - radius * 0.05f, radius * 0.55f, 
                false, Color.parseColor("#FF2A55"), Color.parseColor("#FF6600"), animPhase);

        // Restore clip
        canvas.restore();

        // 5. Outer Bezel Ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.06f);
        paint.setShader(new LinearGradient(cx - radius, cy, cx + radius, cy,
                new int[]{Color.parseColor("#00E5FF"), Color.parseColor("#FFCC00"), Color.parseColor("#FF2A55")},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 0.90f, paint);

        // 6. 3D Banner "ARROW DUEL"
        drawLogoBanner(canvas, cx, cy + radius * 0.52f, radius, pulse);

        // Redraw for dynamic glow loop
        postInvalidateOnAnimation();
    }

    private void drawTorch(Canvas canvas, float x, float y, float size, float phase) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        // Brazier base
        paint.setColor(Color.parseColor("#2A3C6B"));
        canvas.drawRect(x - size * 0.2f, y - size * 0.1f, x + size * 0.2f, y, paint);

        // Fire flame
        paint.setColor(Color.parseColor("#FFAA00"));
        float flameH = size * (0.8f + 0.2f * (float) Math.sin(phase * 3f));
        Path flame = new Path();
        flame.moveTo(x - size * 0.25f, y - size * 0.1f);
        flame.quadTo(x, y - flameH, x + size * 0.25f, y - size * 0.1f);
        flame.close();
        canvas.drawPath(flame, paint);

        paint.setColor(Color.parseColor("#FFEE55"));
        Path inner = new Path();
        inner.moveTo(x - size * 0.15f, y - size * 0.1f);
        inner.quadTo(x, y - flameH * 0.7f, x + size * 0.15f, y - size * 0.1f);
        inner.close();
        canvas.drawPath(inner, paint);
    }

    private void drawArcherStickman(Canvas canvas, float x, float y, float scale, 
                                    boolean facingRight, int glowColor, int bowColor, float phase) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setShader(null);

        float dir = facingRight ? 1f : -1f;
        float headR = scale * 0.16f;
        float headX = x;
        float headY = y - scale * 0.85f;

        // Cape blowing in wind
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(glowColor);
        paint.setAlpha(180);
        Path cape = new Path();
        cape.moveTo(headX - dir * 4f, headY + headR);
        cape.quadTo(headX - dir * scale * 0.35f, headY + scale * 0.4f + (float)Math.sin(phase * 4f) * 6f,
                headX - dir * scale * 0.5f, headY + scale * 0.7f);
        cape.lineTo(headX - dir * scale * 0.2f, headY + scale * 0.45f);
        cape.close();
        canvas.drawPath(cape, paint);
        paint.setAlpha(255);

        // Body Silhouette (Stickman)
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(headX, headY, headR, paint); // Head

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(scale * 0.08f);
        float neckY = headY + headR;
        float hipY = headY + scale * 0.45f;
        canvas.drawLine(headX, neckY, headX, hipY, paint); // Spine

        // Legs
        float foot1X = headX - dir * scale * 0.18f;
        float foot2X = headX + dir * scale * 0.18f;
        canvas.drawLine(headX, hipY, foot1X, y, paint); // Back leg
        canvas.drawLine(headX, hipY, foot2X, y, paint); // Forward leg

        // Arms drawing bow
        float shoulderY = neckY + scale * 0.08f;
        float bowHandX = headX + dir * scale * 0.32f;
        float bowHandY = shoulderY - scale * 0.05f;
        canvas.drawLine(headX, shoulderY, bowHandX, bowHandY, paint); // Bow arm
        
        float stringHandX = headX - dir * scale * 0.05f;
        float stringHandY = shoulderY;
        canvas.drawLine(headX, shoulderY, stringHandX, stringHandY, paint); // String arm

        // Glowing Bow
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(scale * 0.05f);
        paint.setColor(bowColor);

        // Neon Bow Arc
        Path bowArc = new Path();
        float bowTopX = bowHandX - dir * scale * 0.08f;
        float bowTopY = bowHandY - scale * 0.35f;
        float bowBotX = bowHandX - dir * scale * 0.08f;
        float bowBotY = bowHandY + scale * 0.35f;

        bowArc.moveTo(bowTopX, bowTopY);
        bowArc.quadTo(bowHandX + dir * scale * 0.15f, bowHandY, bowBotX, bowBotY);
        canvas.drawPath(bowArc, paint);

        // Bowstring
        paint.setStrokeWidth(scale * 0.02f);
        paint.setColor(Color.WHITE);
        canvas.drawLine(bowTopX, bowTopY, stringHandX, stringHandY, paint);
        canvas.drawLine(bowBotX, bowBotY, stringHandX, stringHandY, paint);

        // Arrow
        paint.setColor(glowColor);
        paint.setStrokeWidth(scale * 0.035f);
        float arrowTipX = bowHandX + dir * scale * 0.28f;
        canvas.drawLine(stringHandX, stringHandY, arrowTipX, bowHandY, paint);
    }

    private void drawLogoBanner(Canvas canvas, float cx, float cy, float radius, float pulse) {
        float bWidth = radius * 1.6f * pulse;
        float bHeight = radius * 0.52f * pulse;
        RectF bannerRect = new RectF(cx - bWidth / 2f, cy - bHeight / 2f, cx + bWidth / 2f, cy + bHeight / 2f);

        // Banner Dark Plate
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#090D18"));
        canvas.drawRoundRect(bannerRect, 18f, 18f, paint);

        // Banner Neon Border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.025f);
        paint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawRoundRect(bannerRect, 18f, 18f, paint);

        // "ARROW" Text
        textPaint.setTextSize(radius * 0.24f * pulse);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(8f, 0, 0, Color.parseColor("#00E5FF"));
        canvas.drawText("ARROW", cx, cy - bHeight * 0.05f, textPaint);

        // "DUEL" Text
        textPaint.setTextSize(radius * 0.22f * pulse);
        textPaint.setColor(Color.parseColor("#FF2A55"));
        textPaint.setShadowLayer(8f, 0, 0, Color.parseColor("#FF2A55"));
        canvas.drawText("DUEL", cx, cy + bHeight * 0.35f, textPaint);

        // Center Crosshair behind text
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#FFAA00"));
        paint.setStrokeWidth(radius * 0.018f);
        float chR = radius * 0.08f;
        canvas.drawCircle(cx, cy - bHeight * 0.12f, chR, paint);
        canvas.drawLine(cx - chR * 1.4f, cy - bHeight * 0.12f, cx + chR * 1.4f, cy - bHeight * 0.12f, paint);
        canvas.drawLine(cx, cy - bHeight * 0.12f - chR * 1.4f, cx, cy - bHeight * 0.12f + chR * 1.4f, paint);
    }
}
