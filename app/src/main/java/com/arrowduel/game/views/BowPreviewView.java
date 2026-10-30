package com.arrowduel.game.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Renders glowing neon fantasy bows (Lightning Bow, Fire Bow, Golden Bow, Void Bow, etc.)
 * with dynamic neon energy aura and sparks.
 */
public class BowPreviewView extends View {

    private Paint bowPaint;
    private Paint stringPaint;
    private Paint glowPaint;
    private Paint sparkPaint;
    private int bowColor = Color.parseColor("#00E5FF");
    private float animPhase = 0f;
    private final Random random = new Random();

    public BowPreviewView(Context context) {
        super(context);
        init();
    }

    public BowPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BowPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bowPaint.setStyle(Paint.Style.STROKE);
        bowPaint.setStrokeCap(Paint.Cap.ROUND);

        stringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stringPaint.setStyle(Paint.Style.STROKE);
        stringPaint.setStrokeWidth(3f);
        stringPaint.setColor(Color.WHITE);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);

        sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparkPaint.setStyle(Paint.Style.FILL);
    }

    public void setBowColor(int color) {
        this.bowColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        animPhase += 0.08f;
        float w = getWidth();
        float h = getHeight();
        float cx = w * 0.45f;
        float cy = h * 0.5f;
        float bowHeight = h * 0.78f;
        float bowCurve = w * 0.32f;

        float topY = cy - bowHeight / 2f;
        float botY = cy + bowHeight / 2f;
        float midX = cx + bowCurve;

        // 1. Draw outer neon glow aura
        glowPaint.setColor(bowColor);
        glowPaint.setAlpha(70);
        glowPaint.setStrokeWidth(24f);

        Path glowPath = new Path();
        glowPath.moveTo(cx, topY);
        glowPath.cubicTo(midX + 20f, cy - bowHeight * 0.25f, midX + 20f, cy + bowHeight * 0.25f, cx, botY);
        canvas.drawPath(glowPath, glowPaint);

        // 2. Draw outer recurve limb limbs
        bowPaint.setColor(bowColor);
        bowPaint.setStrokeWidth(12f);

        Path limbPath = new Path();
        // Top recurve
        limbPath.moveTo(cx - 15f, topY + 10f);
        limbPath.cubicTo(cx + 10f, topY - 10f, midX - 10f, cy - bowHeight * 0.22f, midX, cy);
        // Bottom recurve
        limbPath.cubicTo(midX - 10f, cy + bowHeight * 0.22f, cx + 10f, botY + 10f, cx - 15f, botY - 10f);
        canvas.drawPath(limbPath, bowPaint);

        // Inner glowing core
        bowPaint.setColor(Color.WHITE);
        bowPaint.setStrokeWidth(4f);
        canvas.drawPath(limbPath, bowPaint);

        // 3. Bow String with slight vibration
        float stringDrawX = cx - 15f;
        canvas.drawLine(cx - 15f, topY + 10f, stringDrawX, cy, stringPaint);
        canvas.drawLine(stringDrawX, cy, cx - 15f, botY - 10f, stringPaint);

        // 4. Electric / Magical Energy Sparks
        sparkPaint.setColor(bowColor);
        for (int i = 0; i < 6; i++) {
            float sparkT = (animPhase * 0.3f + i * 0.2f) % 1.0f;
            float sparkX = cx + (float) Math.sin(sparkT * Math.PI) * bowCurve + (random.nextFloat() - 0.5f) * 20f;
            float sparkY = topY + sparkT * bowHeight + (random.nextFloat() - 0.5f) * 20f;
            float r = 2f + random.nextFloat() * 4f;
            sparkPaint.setAlpha((int) (150 + 100 * Math.sin(animPhase + i)));
            canvas.drawCircle(sparkX, sparkY, r, sparkPaint);
        }

        // Draw small lightning arcs
        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(Color.WHITE);
        arcPaint.setStrokeWidth(2f);
        float lx = midX - 5f;
        float ly = cy - 20f + (float) Math.sin(animPhase * 2f) * 40f;
        canvas.drawLine(lx, ly, lx + 12f, ly + 10f, arcPaint);
        canvas.drawLine(lx + 12f, ly + 10f, lx + 5f, ly + 25f, arcPaint);

        postInvalidateOnAnimation();
    }
}
