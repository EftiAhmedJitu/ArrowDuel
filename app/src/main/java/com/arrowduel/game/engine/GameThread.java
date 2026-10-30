package com.arrowduel.game.engine;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean isRunning = false;
    private volatile int targetFps = 60;
    private volatile long optimalTime = 1000000000L / 60;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this(surfaceHolder, gameView, 60);
    }

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView, int targetFps) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
        setTargetFps(targetFps);
    }

    public void setTargetFps(int fps) {
        this.targetFps = (fps == 120) ? 120 : 60;
        this.optimalTime = 1000000000L / this.targetFps;
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    @Override
    public void run() {
        long lastLoopTime = System.nanoTime();

        while (isRunning) {
            try {
                long now = System.nanoTime();
                long updateLength = now - lastLoopTime;
                lastLoopTime = now;
                float delta = updateLength / 1000000000.0f;

                // Cap delta to prevent huge jumps (max 33ms if backgrounded)
                if (delta > 0.033f) delta = 0.033f;

                try {
                    gameView.update(delta);
                } catch (Throwable t) {
                    android.util.Log.e("GameThread", "Recovered from update error: " + t.getMessage(), t);
                }

                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        synchronized (surfaceHolder) {
                            gameView.drawGame(canvas);
                        }
                    }
                } catch (Throwable t) {
                    android.util.Log.e("GameThread", "Recovered from draw error: " + t.getMessage(), t);
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        } catch (Throwable t) {
                            android.util.Log.e("GameThread", "Error in unlockCanvasAndPost", t);
                        }
                    }
                }

                long sleepTime = (lastLoopTime - System.nanoTime() + optimalTime) / 1000000L;
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ignored) {}
                }
            } catch (Throwable t) {
                android.util.Log.e("GameThread", "Loop protection caught error: " + t.getMessage(), t);
            }
        }
    }
}
