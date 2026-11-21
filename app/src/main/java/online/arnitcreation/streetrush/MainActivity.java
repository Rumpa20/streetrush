package online.arnitcreation.streetrush;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.opengl.GLSurfaceView;
import online.arnitcreation.streetrush.engine.Cfg;

/**
 * Single-activity host: fullscreen immersive portrait GLSurfaceView.
 * All input is forwarded to the game on the UI thread; the game runs on the GL thread.
 */
public final class MainActivity extends Activity {

    private GLSurfaceView glView;
    private StreetRushGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        game = new StreetRushGame(this, glView);
        glView.setRenderer(game);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setContentView(glView);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        game.touch.handle(event);
        return true;
    }

    @Override
    public void onBackPressed() {
        game.enqueue(Cfg.BACK);
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
        game.onHostPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
        game.onHostResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
