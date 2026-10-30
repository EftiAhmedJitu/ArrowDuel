package com.arrowduel.game.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arrowduel.game.utils.FullScreenHelper;

/**
 * Base activity for all game screens ensuring true full-screen immersive mode.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.arrowduel.game.utils.AppCrashHandler.install(this);
        FullScreenHelper.enableFullScreen(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenHelper.enableFullScreen(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            FullScreenHelper.enableFullScreen(this);
        }
    }
}
