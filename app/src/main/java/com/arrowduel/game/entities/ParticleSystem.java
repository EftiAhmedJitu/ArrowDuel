package com.arrowduel.game.entities;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private static final int MAX_PARTICLES = 300;
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    public ParticleSystem() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles[i] = new Particle();
        }
    }

    public void emit(float x, float y, float vx, float vy, float size, int color, float life) {
        for (Particle p : particles) {
            if (!p.active) {
                p.init(x, y, vx, vy, size, color, life);
                break;
            }
        }
    }

    public void emitBurst(float x, float y, int color, int count, float speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            float spd = (0.3f + 0.7f * random.nextFloat()) * speed;
            float vx = (float) Math.cos(angle) * spd;
            float vy = (float) Math.sin(angle) * spd;
            float size = 3f + random.nextFloat() * 6f;
            float life = 0.3f + random.nextFloat() * 0.4f;
            emit(x, y, vx, vy, size, color, life);
        }
    }

    public void emitArrowTrail(float x, float y, int color) {
        float vx = (random.nextFloat() - 0.5f) * 20f;
        float vy = (random.nextFloat() - 0.5f) * 20f;
        float size = 3f + random.nextFloat() * 3f;
        float life = 0.2f + random.nextFloat() * 0.2f;
        emit(x, y, vx, vy, size, color, life);
    }

    public void update(float dt) {
        for (Particle p : particles) {
            if (p.active) {
                p.update(dt);
            }
        }
    }

    public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        for (Particle p : particles) {
            if (p.active) {
                p.draw(canvas, paint);
            }
        }
    }
}
