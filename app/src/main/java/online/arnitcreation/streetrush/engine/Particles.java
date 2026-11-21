package online.arnitcreation.streetrush.engine;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** Pooled 3D point particle system (dust, sparks, crashes, power-up auras). */
public final class Particles {
    public static final int CAP = 384;

    private final float[] px = new float[CAP];
    private final float[] py = new float[CAP];
    private final float[] pz = new float[CAP];
    private final float[] vx = new float[CAP];
    private final float[] vy = new float[CAP];
    private final float[] vz = new float[CAP];
    private final float[] life = new float[CAP];
    private final float[] maxLife = new float[CAP];
    private final int[] col = new int[CAP];
    private final float[] size = new float[CAP];
    private int cursor;
    private FloatBuffer buf;
    private final float[] tmp = new float[4];

    public Particles() {
        buf = ByteBuffer.allocateDirect(CAP * 8 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void spawn(float x, float y, float z, float spread, float up,
                      int argb, float lifeSec, float sizePx) {
        int i = cursor;
        cursor = (cursor + 1) % CAP;
        px[i] = x;
        py[i] = y;
        pz[i] = z;
        vx[i] = (rnd() - 0.5f) * spread;
        vy[i] = rnd() * up;
        vz[i] = (rnd() - 0.5f) * spread;
        life[i] = lifeSec;
        maxLife[i] = lifeSec;
        col[i] = argb;
        size[i] = sizePx * (0.6f + rnd() * 0.8f);
    }

    public void burst(float x, float y, float z, int n, int argb, float power) {
        for (int j = 0; j < n; j++) {
            int i = cursor;
            cursor = (cursor + 1) % CAP;
            px[i] = x;
            py[i] = y;
            pz[i] = z;
            vx[i] = (rnd() - 0.5f) * power;
            vy[i] = rnd() * power * 0.8f + 1f;
            vz[i] = (rnd() - 0.5f) * power;
            life[i] = 0.5f + rnd() * 0.5f;
            maxLife[i] = life[i];
            col[i] = argb;
            size[i] = 6f + rnd() * 10f;
        }
    }

    public void update(float dt) {
        for (int i = 0; i < CAP; i++) {
            if (life[i] > 0) {
                life[i] -= dt;
                px[i] += vx[i] * dt;
                py[i] += vy[i] * dt;
                pz[i] += vz[i] * dt;
                vy[i] -= 4f * dt;
            }
        }
    }

    public void draw(float[] vp, float camZ, boolean dense) {
        buf.position(0);
        int n = 0;
        for (int i = 0; i < CAP; i++) {
            if (life[i] <= 0) {
                continue;
            }
            if (!dense && (n & 1) == 1) {
                n++;
                continue;
            }
            Cfg.col4(col[i], tmp);
            float a = tmp[3] * Math.min(1f, life[i] / (maxLife[i] * 0.5f + 0.01f));
            float dist = Math.abs(pz[i] - camZ);
            float s = size[i] * 140f / (50f + dist * 42f);
            if (s < 1.5f) {
                s = 1.5f;
            }
            buf.put(px[i]); buf.put(py[i]); buf.put(pz[i]);
            buf.put(tmp[0]); buf.put(tmp[1]); buf.put(tmp[2]); buf.put(a);
            buf.put(s);
            n++;
        }
        if (n == 0) {
            return;
        }
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);
        GLES20.glUseProgram(ShaderLib.progPts);
        GLES20.glUniformMatrix4fv(ShaderLib.qVP, 1, false, vp, 0);
        GLES20.glEnableVertexAttribArray(ShaderLib.qPos);
        buf.position(0);
        GLES20.glVertexAttribPointer(ShaderLib.qPos, 3, GLES20.GL_FLOAT, false, 32, buf);
        buf.position(3);
        GLES20.glEnableVertexAttribArray(ShaderLib.qColor);
        GLES20.glVertexAttribPointer(ShaderLib.qColor, 4, GLES20.GL_FLOAT, false, 32, buf);
        buf.position(7);
        GLES20.glEnableVertexAttribArray(ShaderLib.qSize);
        GLES20.glVertexAttribPointer(ShaderLib.qSize, 1, GLES20.GL_FLOAT, false, 32, buf);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, n);
        GLES20.glDisableVertexAttribArray(ShaderLib.qPos);
        GLES20.glDisableVertexAttribArray(ShaderLib.qColor);
        GLES20.glDisableVertexAttribArray(ShaderLib.qSize);
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private float rnd() {
        return (float) Math.random();
    }
}
