package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

public class Platform {
    private float left;
    private float top;
    private float right;
    private float bottom;
    private int tierIndex; // 0 = Bottom cliff, 1 = Mid ledge, 2 = High tower
    private Paint paint;
    private Paint topStrokePaint;
    private Paint torchPaint;
    private float animPhase = 0;

    public Platform(float left, float top, float right, float bottom, int tierIndex) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.tierIndex = tierIndex;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        topStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        topStrokePaint.setStyle(Paint.Style.STROKE);
        topStrokePaint.setStrokeWidth(4f);
        topStrokePaint.setColor(Color.parseColor("#00E5FF"));

        torchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public float getLeft() { return left; }
    public float getTop() { return top; }
    public float getRight() { return right; }
    public float getBottom() { return bottom; }
    public int getTierIndex() { return tierIndex; }

    public void update(float dt) {
        animPhase += dt * 4f;
    }

    public void draw(Canvas canvas) {
        // Platform dark rock body
        paint.setShader(new LinearGradient(left, top, left, bottom,
                Color.parseColor("#151D33"), Color.parseColor("#0A0E1A"), Shader.TileMode.CLAMP));
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, 8f, 8f, paint);

        // Glowing platform edge rim
        canvas.drawLine(left, top, right, top, topStrokePaint);

        // If high tier or mid tier, draw support beams or decorative torch
        if (tierIndex > 0) {
            // Brazier torch on outer corner
            float torchX = (left < canvas.getWidth() / 2f) ? right - 25f : left + 25f;
            drawTorch(canvas, torchX, top);
        }
    }

    private void drawTorch(Canvas canvas, float tx, float ty) {
        // Stand
        torchPaint.setStyle(Paint.Style.FILL);
        torchPaint.setColor(Color.parseColor("#2A3C6B"));
        canvas.drawRect(tx - 6, ty - 8, tx + 6, ty, torchPaint);

        // Animated Flame
        float flameH = 16f + 4f * (float) Math.sin(animPhase + tx);
        torchPaint.setColor(Color.parseColor("#FFAA00"));
        canvas.drawCircle(tx, ty - flameH * 0.6f, 8f, torchPaint);
        torchPaint.setColor(Color.parseColor("#FFEE55"));
        canvas.drawCircle(tx, ty - flameH * 0.5f, 4f, torchPaint);
    }
}
