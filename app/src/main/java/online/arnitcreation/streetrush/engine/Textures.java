package online.arnitcreation.streetrush.engine;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/** Runtime-generated textures (no binary assets): UI shapes, power-up icons, game logo. */
public final class Textures {
    private Textures() {}

    public static int white, round, circle, logo, coin;
    public static int[] power = new int[6];
    public static int board;

    public static final int P_MAGNET = 0, P_JET = 1, P_SJUMP = 2, P_MULT = 3, P_SHIELD = 4, P_BOOST = 5;

    private static final float[] TMP4 = new float[4];

    public static int fromBitmap(Bitmap bmp) {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return ids[0];
    }

    private static Bitmap bmp(int w, int h) {
        return Bitmap.createBitmap(w, h, Config.ARGB_8888);
    }

    public static void init() {
        white = texSolid(0xFFFFFFFF);
        round = rounded();
        circle = disc();
        coin = coinTex();
        board = boardTex();
        power[P_MAGNET] = magnetTex();
        power[P_JET] = jetTex();
        power[P_SJUMP] = jumpTex();
        power[P_MULT] = multTex();
        power[P_SHIELD] = shieldTex();
        power[P_BOOST] = boostTex();
        logo = logoTex();
    }

    private static int texSolid(int color) {
        Bitmap b = bmp(2, 2);
        b.eraseColor(color);
        int id = fromBitmap(b);
        b.recycle();
        return id;
    }

    private static Bitmap rounded() {
        int s = 64;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        c.drawRoundRect(new RectF(0, 0, s - 1, s - 1), s * 0.28f, s * 0.28f, p);
        return upload(b);
    }

    private static Bitmap disc() {
        int s = 64;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(s / 2f, s / 2f, s / 2f - 1, p);
        return upload(b);
    }

    private static Bitmap coinTex() {
        int s = 64;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFF7B500);
        c.drawCircle(s / 2f, s / 2f, s / 2f - 2, p);
        p.setColor(0xFFFFE082);
        c.drawCircle(s / 2f, s / 2f, s / 2f - 8, p);
        p.setColor(0xFFD68A00);
        p.setTextSize(s * 0.5f);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        c.drawText("A", s / 2f, s * 0.68f, p);
        return upload(b);
    }

    private static Bitmap boardTex() {
        int w = 96, h = 96;
        Bitmap b = bmp(w, h);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF26D6C2);
        c.drawRoundRect(new RectF(w * 0.25f, h * 0.30f, w * 0.75f, h * 0.48f), 8, 8, p);
        p.setShader(null);
        p.setColor(0xFF7CFFE4);
        c.drawRoundRect(new RectF(w * 0.25f, h * 0.30f, w * 0.75f, h * 0.36f), 6, 6, p);
        p.setColor(0x6626D6C2);
        c.drawCircle(w * 0.5f, h * 0.68f, w * 0.13f, p);
        c.drawCircle(w * 0.5f, h * 0.68f, w * 0.07f, p);
        return upload(b);
    }

    private static Bitmap magnetTex() {
        int s = 96;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.16f);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setColor(0xFFFF4757);
        c.drawArc(new RectF(s * 0.18f, s * 0.12f, s * 0.82f, s * 0.76f), 180, 180, false, p);
        p.setColor(0xFFF1F2F6);
        c.drawRect(s * 0.25f - s * 0.08f, s * 0.25f, s * 0.25f + s * 0.25f, s * 0.44f, p);
        c.drawRect(s * 0.75f - s * 0.25f, s * 0.25f, s * 0.75f + s * 0.08f, s * 0.44f, p);
        return upload(b);
    }

    private static Bitmap jetTex() {
        int s = 96;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path path = new Path();
        path.moveTo(s * 0.5f, s * 0.08f);
        path.quadTo(s * 0.72f, s * 0.4f, s * 0.62f, s * 0.72f);
        path.lineTo(s * 0.38f, s * 0.72f);
        path.quadTo(s * 0.28f, s * 0.4f, s * 0.5f, s * 0.08f);
        p.setColor(0xFFDDE4EC);
        c.drawPath(path, p);
        p.setColor(0xFFFF7043);
        Path flame = new Path();
        flame.moveTo(s * 0.38f, s * 0.74f);
        flame.lineTo(s * 0.62f, s * 0.74f);
        flame.lineTo(s * 0.5f, s * 0.95f);
        flame.close();
        c.drawPath(flame, p);
        p.setColor(0xFF42A5F5);
        c.drawCircle(s * 0.5f, s * 0.4f, s * 0.07f, p);
        return upload(b);
    }

    private static Bitmap jumpTex() {
        int s = 96;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF9C27FF);
        Path up = new Path();
        up.moveTo(s * 0.5f, s * 0.1f);
        up.lineTo(s * 0.8f, s * 0.48f);
        up.lineTo(s * 0.63f, s * 0.48f);
        up.lineTo(s * 0.63f, s * 0.9f);
        up.lineTo(s * 0.37f, s * 0.9f);
        up.lineTo(s * 0.37f, s * 0.48f);
        up.lineTo(s * 0.2f, s * 0.48f);
        up.close();
        c.drawPath(up, p);
        return upload(b);
    }

    private static Bitmap multTex() {
        int s = 96;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFC93C);
        p.setTextSize(s * 0.52f);
        p.setFakeBoldText(true);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("X2", s * 0.5f, s * 0.68f, p);
        return upload(b);
    }

    private static Bitmap shieldTex() {
        int s = 96;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path sp = new Path();
        sp.moveTo(s * 0.5f, s * 0.08f);
        sp.lineTo(s * 0.84f, s * 0.22f);
        sp.quadTo(s * 0.86f, s * 0.6f, s * 0.5f, s * 0.92f);
        sp.quadTo(s * 0.14f, s * 0.6f, s * 0.16f, s * 0.22f);
        sp.close();
        p.setColor(0xFF3DC1FF);
        c.drawPath(sp, p);
        p.setColor(0xFFD9F3FF);
        p.setStrokeWidth(s * 0.06f);
        p.setStyle(Paint.Style.STROKE);
        c.drawPath(sp, p);
        return upload(b);
    }

    private static Bitmap boostTex() {
        int s = 96;
        Bitmap b = bmp(s, s);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF00E676);
        Path a1 = new Path();
        a1.moveTo(s * 0.18f, s * 0.2f); a1.lineTo(s * 0.48f, s * 0.5f);
        a1.lineTo(s * 0.18f, s * 0.8f); a1.close();
        c.drawPath(a1, p);
        Path a2 = new Path();
        a2.moveTo(s * 0.5f, s * 0.2f); a2.lineTo(s * 0.8f, s * 0.5f);
        a2.lineTo(s * 0.5f, s * 0.8f); a2.close();
        c.drawPath(a2, p);
        return upload(b);
    }

    private static Bitmap logoTex() {
        int w = 1024, h = 512;
        Bitmap b = bmp(w, h);
        Canvas c = new Canvas(b);
        float cx = w / 2f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        p.setShadowLayer(18, 0, 0, 0xFF22E4C8);
        p.setColor(0xFF22E4C8);
        p.setTextSize(150);
        c.drawText("AR NIT", cx, 165, p);
        p.setShadowLayer(18, 0, 0, 0xFFFF3D8A);
        p.setColor(0xFFFF3D8A);
        p.setTextSize(190);
        c.drawText("STREET", cx, 340, p);
        p.setShadowLayer(18, 0, 0, 0xFFFFC93C);
        p.setColor(0xFFFFC93C);
        p.setTextSize(190);
        c.drawText("RUSH", cx, 480, p);
        return upload(b);
    }

    private static int upload(Bitmap b) {
        int id = fromBitmap(b);
        b.recycle();
        return id;
    }

    /** Tint helper used by UiBatch / Font. */
    public static void rgba(int argb, float[] out) {
        Cfg.col4(argb, out);
    }
}
