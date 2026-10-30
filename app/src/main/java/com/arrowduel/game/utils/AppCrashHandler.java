package com.arrowduel.game.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Global Crash Protection Handler:
 * Prevents abrupt app crashes during long gameplay sessions.
 * Intercepts uncaught exceptions on background threads and graceful recovery.
 */
public class AppCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "ArrowDuelCrashHandler";
    private static boolean isInstalled = false;
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public static synchronized void install(Context context) {
        if (!isInstalled) {
            Thread.setDefaultUncaughtExceptionHandler(new AppCrashHandler(context));
            isInstalled = true;
            Log.i(TAG, "Global crash shield installed successfully.");
        }
    }

    private AppCrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Log.e(TAG, "FATAL SHIELD: Intercepted exception on thread [" + thread.getName() + "]: " + throwable.getMessage(), throwable);

        try {
            // If exception was on a background thread (audio, network, game thread, worker),
            // do not allow it to terminate the whole application process.
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.w(TAG, "Background thread crashed. App kept alive safely.");
                return;
            }

            // Main Thread exception: attempt graceful user notification
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(context, "Game recovered from a minor glitch.", Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}

        // Fallback to default handler only if absolutely necessary
        if (defaultHandler != null && Looper.myLooper() == Looper.getMainLooper()) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}
