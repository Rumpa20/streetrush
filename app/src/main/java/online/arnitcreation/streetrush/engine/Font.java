package online.arnitcreation.streetrush.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.Map;

/** Runtime-baked glyph atlas; all in-game text renders from it. Caps only, keeps atlas small. */
public final class Font {
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ.,:!?+-()%/:*' ";

    private static int atlas;
    private static final float[] ADV = new float[CHARS.length()];
    private static final float[] GLYPH_U = new float[CHARS.length()];
    private static final float[] GLYPH_V = new float[CHARS.length()];
    private static final float[] GLYPH_W = new float[CHARS.length()];
    private static final float[] GLYPH_H = new float[CHARS.length()];
    private static final float BAKE = 64f;
    private static int atlasW, atlasH;
    private static final Map<String, Float> WIDTH_CACHE = new HashMap<String, Float>(64);

    public static void init() {
        atlasW = 1024;
        atlasH = 128;
        Bitmap bmp = Bitmap.createBitmap(atlasW, atlasH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        p.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        p.setTextSize(BAKE);
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = -fm.ascent;
        float pen = 2f;
        for (int i = 0; i < CHARS.length(); i++) {
            String ch = String.valueOf(CHARS.charAt(i));
            float w = p.measureText(ch);
            if (pen + w + 2 > atlasW) {
                break;
            }
            c.drawText(ch, pen, baseline, p);
            GLYPH_U[i] = pen / atlasW;
            GLYPH_V[i] = 0f;
            GLYPH_W[i] = w / atlasW;
            GLYPH_H[i] = (baseline + fm.descent) / atlasH;
            ADV[i] = w;
            pen += w + 3f;
        }
        atlas = Textures.fromBitmap(bmp);
        bmp.recycle();
    }

    private static int idx(char ch) {
        int i = CHARS.indexOf(ch);
        return i;
    }

    public static float width(String s, float size) {
        Float cached = WIDTH_CACHE.get(s);
        float w;
        if (cached != null) {
            w = cached;
        } else {
            w = 0;
            for (int i = 0; i < s.length(); i++) {
                int gi = idx(s.charAt(i));
                w += gi >= 0 ? ADV[gi] : BAKE * 0.25f;
            }
            if (WIDTH_CACHE.size() < 256) {
                WIDTH_CACHE.put(s, w);
            }
        }
        return w * (size / BAKE);
    }

    public static float height(float size) {
        return size * 0.93f;
    }

    public static void draw(UiBatch b, String s, float x, float y, float size, int argb) {
        float scale = size / BAKE;
        float baseline = y + size * 0.25f;
        for (int i = 0; i < s.length(); i++) {
            int gi = idx(s.charAt(i));
            if (gi < 0) {
                x += BAKE * 0.25f * scale;
                continue;
            }
            float gw = ADV[gi] * scale;
            b.quad(atlas, x, y, gw, GLYPH_H[gi] * atlasH * scale,
                    GLYPH_U[gi], 0f, GLYPH_U[gi] + GLYPH_W[gi], GLYPH_H[gi], argb);
            x += gw;
            x += 1f * scale;
        }
    }

    public static void drawCentered(UiBatch b, String s, float cx, float y, float size, int argb) {
        draw(b, s, cx - width(s, size) / 2f, y, size, argb);
    }
}
