package online.arnitcreation.streetrush.input;

import android.view.MotionEvent;
import online.arnitcreation.streetrush.StreetRushGame;
import online.arnitcreation.streetrush.engine.Cfg;

/** Touch gesture analysis: swipe directions, taps, double-taps. Sensitivity configurable. */
public final class Touch {
    private final StreetRushGame game;
    private float downX, downY, downT;
    private long lastTapAt;
    private float lastTapX, lastTapY;
    private int threshold = 52;

    /** Screen position of the most recent Enqueued tap/double-tap (read by the UI). */
    public float lastTapUpX, lastTapUpY;

    public Touch(StreetRushGame game) {
        this.game = game;
    }

    /** level: 0 low, 1 medium, 2 high sensitivity. */
    public void setSensitivity(int level) {
        threshold = level == 0 ? 78 : (level == 1 ? 48 : 30);
    }

    public boolean handle(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                downT = e.getEventTime();
                return true;
            case MotionEvent.ACTION_UP: {
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                float ax = Math.abs(dx);
                float ay = Math.abs(dy);
                long dt = e.getEventTime() - downT;
                if (Math.max(ax, ay) >= threshold) {
                    if (ax > ay) {
                        game.enqueue(dx > 0 ? Cfg.SWIPE_R : Cfg.SWIPE_L);
                    } else {
                        game.enqueue(dy > 0 ? Cfg.SWIPE_D : Cfg.SWIPE_U);
                    }
                } else if (dt < 300) {
                    long now = System.currentTimeMillis();
                    if (now - lastTapAt < 340
                            && Math.abs(e.getX() - lastTapX) < 90
                            && Math.abs(e.getY() - lastTapY) < 90) {
                        lastTapUpX = e.getX();
                        lastTapUpY = e.getY();
                        game.enqueue(Cfg.DBLTAP);
                        lastTapAt = 0;
                    } else {
                        lastTapUpX = e.getX();
                        lastTapUpY = e.getY();
                        game.enqueue(Cfg.TAP);
                        lastTapAt = now;
                        lastTapX = e.getX();
                        lastTapY = e.getY();
                    }
                }
                return true;
            }
            default:
                return false;
        }
    }
}
