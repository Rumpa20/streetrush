package online.arnitcreation.streetrush.engine;

/** Global gameplay/rendering constants and color helpers. */
public final class Cfg {
    private Cfg() {}

    public static final float LANE_W = 2.2f;
    public static final float ROAD_HALF = 3.8f;
    public static final float CHUNK_LEN = 40f;
    public static final int CHUNKS = 7;
    public static final float FOG_START = 55f;
    public static final float FOG_END = 125f;
    public static final float DRAW_DIST = 145f;

    public static final float PLAYER_W = 0.62f;
    public static final float PLAYER_D = 0.62f;
    public static final float PLAYER_H = 1.72f;
    public static final float SLIDE_H = 0.8f;
    public static final float JUMP_V = 7.6f;
    public static final float GRAVITY = 22f;
    public static final float SLIDE_TIME = 0.62f;

    public static final float BASE_SPEED = 8.5f;
    public static final float MAX_SPEED = 19f;
    public static final float ACCEL = 0.055f;

    public static final int POWER_TIME = 10;

    public static final int SWIPE_L = 0, SWIPE_R = 1, SWIPE_U = 2, SWIPE_D = 3;
    public static final int TAP = 4, DBLTAP = 5, BACK = 6;

    public static float laneX(int lane) {
        return (lane - 1) * LANE_W;
    }

    public static void col3(int argb, float[] out) {
        out[0] = ((argb >> 16) & 0xff) / 255f;
        out[1] = ((argb >> 8) & 0xff) / 255f;
        out[2] = (argb & 0xff) / 255f;
    }

    public static void col4(int argb, float[] out) {
        out[0] = ((argb >> 16) & 0xff) / 255f;
        out[1] = ((argb >> 8) & 0xff) / 255f;
        out[2] = (argb & 0xff) / 255f;
        out[3] = ((argb >> 24) & 0xff) / 255f;
    }
}
