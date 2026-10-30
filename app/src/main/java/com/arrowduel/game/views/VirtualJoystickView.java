package com.arrowduel.game.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 360-degree Virtual Joystick supporting:
 * - Left / Right: Forward & backward walking along platforms.
 * - Up / Down: Jumping to upper platforms or dropping to lower platforms.
 */
public class VirtualJoystickView extends View {

    public interface JoystickListener {
        void onJoystickMoved(float xPercent, float yPercent);
        void onJoystickReleased();
    }

    private JoystickListener listener;
    private Paint basePaint;
    private Paint knobPaint;
    private Paint strokePaint;
    private Paint glowPaint;

    private float centerX = 0f;
    private float centerY = 0f;
    private float baseRadius = 0f;
    private float knobRadius = 0f;
    private float knobX = 0f;
    private float knobY = 0f;
    private boolean isTouching = false;

    public VirtualJoystickView(Context context) {
        super(context);
        init();
    }

    public VirtualJoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VirtualJoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setColor(Color.parseColor("#990B132B"));

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2.5f * density);
        strokePaint.setColor(Color.parseColor("#00E5FF"));

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(6f * density);
        glowPaint.setColor(Color.parseColor("#3300E5FF"));

        knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(Color.parseColor("#00E5FF"));
    }

    private float defaultCenterX = 0f;
    private float defaultCenterY = 0f;
    private float scaleMultiplier = 1.0f;

    public void setJoystickListener(JoystickListener listener) {
        this.listener = listener;
    }

    public void setScaleMultiplier(float scale) {
        this.scaleMultiplier = Math.max(0.65f, Math.min(1.4f, scale));
        float density = getResources().getDisplayMetrics().density;
        this.baseRadius = 50f * density * scaleMultiplier;
        this.knobRadius = 22f * density * scaleMultiplier;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        defaultCenterX = w * 0.50f;
        defaultCenterY = h * 0.55f;
        centerX = defaultCenterX;
        centerY = defaultCenterY;
        float density = getResources().getDisplayMetrics().density;
        baseRadius = 50f * density * scaleMultiplier;
        knobRadius = 22f * density * scaleMultiplier;
        knobX = centerX;
        knobY = centerY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                // Snaps center directly to touch position
                centerX = tx;
                centerY = ty;
                knobX = tx;
                knobY = ty;
                isTouching = true;
                if (listener != null) {
                    listener.onJoystickMoved(0f, 0f);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                isTouching = true;
                float dx = tx - centerX;
                float dy = ty - centerY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist > baseRadius) {
                    knobX = centerX + (dx / dist) * baseRadius;
                    knobY = centerY + (dy / dist) * baseRadius;
                } else {
                    knobX = tx;
                    knobY = ty;
                }

                if (listener != null) {
                    float xPercent = (knobX - centerX) / baseRadius;
                    float yPercent = (knobY - centerY) / baseRadius;
                    listener.onJoystickMoved(xPercent, yPercent);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                centerX = defaultCenterX;
                centerY = defaultCenterY;
                knobX = defaultCenterX;
                knobY = defaultCenterY;
                if (listener != null) {
                    listener.onJoystickReleased();
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isTouching) {
            // Active: soft translucent cyan tint (completely see-through to background & enemy)
            basePaint.setColor(Color.parseColor("#2200E5FF"));
            canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
            glowPaint.setAlpha(120);
            canvas.drawCircle(centerX, centerY, baseRadius, glowPaint);
            strokePaint.setAlpha(240);
            canvas.drawCircle(centerX, centerY, baseRadius, strokePaint);

            // Active crosshairs
            Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            crossPaint.setColor(Color.parseColor("#5500E5FF"));
            crossPaint.setStrokeWidth(1.8f);
            canvas.drawLine(centerX - baseRadius * 0.5f, centerY, centerX + baseRadius * 0.5f, centerY, crossPaint);
            canvas.drawLine(centerX, centerY - baseRadius * 0.5f, centerX, centerY + baseRadius * 0.5f, crossPaint);

            // Active Knob
            knobPaint.setColor(Color.parseColor("#00FF88"));
            canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
            Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            corePaint.setColor(Color.WHITE);
            canvas.drawCircle(knobX, knobY, knobRadius * 0.38f, corePaint);
        } else {
            // Idle: minimal, elegant, non-intrusive ring (enemy is 100% visible)
            strokePaint.setAlpha(110);
            canvas.drawCircle(centerX, centerY, baseRadius, strokePaint);

            knobPaint.setColor(Color.parseColor("#00E5FF"));
            knobPaint.setAlpha(130);
            canvas.drawCircle(knobX, knobY, knobRadius * 0.65f, knobPaint);
        }
    }
}
