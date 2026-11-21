package online.arnitcreation.streetrush.engine;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** 2D textured/solid quad batcher with y-down screen coordinates and alpha blending. */
public final class UiBatch {
    private final float[] ortho = new float[16];
    private FloatBuffer posBuf;
    private FloatBuffer uvBuf;
    private FloatBuffer colBuf;
    private IntBuffer texIds;
    private int count;
    private int capacity;
    private final float[] tmp4 = new float[4];
    private final float[] tmpPos = new float[12];
    private final float[] tmpUv = new float[12];
    private final float[] tmpCol = new float[24];

    public UiBatch(int screenW, int screenH) {
        capacity = 2048;
        alloc();
        resize(screenW, screenH);
    }

    private void alloc() {
        posBuf = ByteBuffer.allocateDirect(capacity * 12 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        uvBuf = ByteBuffer.allocateDirect(capacity * 12 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        colBuf = ByteBuffer.allocateDirect(capacity * 24 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texIds = ByteBuffer.allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public void resize(int screenW, int screenH) {
        android.opengl.Matrix.orthoM(ortho, 0, 0, screenW, screenH, 0, -1, 1);
    }

    public void begin() {
        count = 0;
    }

    private void ensure(int extra) {
        if (count + extra < capacity) {
            return;
        }
        while (capacity < count + extra) {
            capacity *= 2;
        }
        FloatBuffer p = posBuf;
        FloatBuffer u = uvBuf;
        FloatBuffer c = colBuf;
        IntBuffer t = texIds;
        alloc();
        p.rewind();
        u.rewind();
        c.rewind();
        t.rewind();
        posBuf.put(p);
        uvBuf.put(u);
        colBuf.put(c);
        texIds.put(t);
        posBuf.position(0);
        uvBuf.position(0);
        colBuf.position(0);
        texIds.position(0);
    }

    public void quad(int tex, float x, float y, float wq, float hq,
                     float u0, float v0, float u1, float v1, int argb) {
        ensure(1);
        Cfg.col4(argb, tmp4);
        final float r = tmp4[0], g = tmp4[1], b = tmp4[2], a = tmp4[3];
        final float x2 = x + wq, y2 = y + hq;
        tmpPos[0] = x;  tmpPos[1] = y;   tmpUv[0] = u0; tmpUv[1] = v0;
        tmpPos[2] = x2; tmpPos[3] = y;   tmpUv[2] = u1; tmpUv[3] = v0;
        tmpPos[4] = x2; tmpPos[5] = y2;  tmpUv[4] = u1; tmpUv[5] = v1;
        tmpPos[6] = x;  tmpPos[7] = y;   tmpUv[6] = u0; tmpUv[7] = v0;
        tmpPos[8] = x2; tmpPos[9] = y2;  tmpUv[8] = u1; tmpUv[9] = v1;
        tmpPos[10] = x; tmpPos[11] = y2; tmpUv[10] = u0; tmpUv[11] = v1;
        for (int i = 0; i < 6; i++) {
            int co = i * 4;
            tmpCol[co] = r;
            tmpCol[co + 1] = g;
            tmpCol[co + 2] = b;
            tmpCol[co + 3] = a;
        }
        int vo = count * 12;
        posBuf.position(0);
        uvBuf.position(0);
        colBuf.position(0);
        posBuf.put(vo, tmpPos, 0, 12);
        uvBuf.put(vo, tmpUv, 0, 12);
        colBuf.put(vo * 2, tmpCol, 0, 24);
        texIds.put(count, tex);
        count++;
    }

    public void solid(float x, float y, float wq, float hq, int argb) {
        quad(Textures.white, x, y, wq, hq, 0, 0, 1, 1, argb);
    }

    public void roundRect(float x, float y, float wq, float hq, int argb) {
        quad(Textures.round, x, y, wq, hq, 0, 0, 1, 1, argb);
    }

    public void disc(float x, float y, float d, int argb) {
        quad(Textures.circle, x, y, d, d, 0, 0, 1, 1, argb);
    }

    public void tex(int tex, float x, float y, float wq, float hq, int argb) {
        quad(tex, x, y, wq, hq, 0, 0, 1, 1, argb);
    }

    public void coinIcon(float x, float y, float d) {
        quad(Textures.coin, x, y, d, d, 0, 0, 1, 1, 0xFFFFFFFF);
    }

    public void powerIcon(int type, float x, float y, float d, int argb) {
        quad(Textures.power[type], x, y, d, d, 0, 0, 1, 1, argb);
    }

    public void flush() {
        if (count == 0) {
            return;
        }
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(ShaderLib.progUI);
        GLES20.glUniformMatrix4fv(ShaderLib.uM, 1, false, ortho, 0);
        GLES20.glEnableVertexAttribArray(ShaderLib.uiPos);
        GLES20.glEnableVertexAttribArray(ShaderLib.uiUV);
        GLES20.glEnableVertexAttribArray(ShaderLib.uiCol);
        int start = 0;
        int curTex = texIds.get(0);
        for (int i = 1; i <= count; i++) {
            int texNow = i < count ? texIds.get(i) : -1;
            if (texNow != curTex) {
                int len = i - start;
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, curTex);
                posBuf.position(start * 12);
                GLES20.glVertexAttribPointer(ShaderLib.uiPos, 2, GLES20.GL_FLOAT, false, 8, posBuf);
                uvBuf.position(start * 12);
                GLES20.glVertexAttribPointer(ShaderLib.uiUV, 2, GLES20.GL_FLOAT, false, 8, uvBuf);
                colBuf.position(start * 24);
                GLES20.glVertexAttribPointer(ShaderLib.uiCol, 4, GLES20.GL_FLOAT, false, 16, colBuf);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, len * 6);
                start = i;
                curTex = texNow;
            }
        }
        GLES20.glDisableVertexAttribArray(ShaderLib.uiPos);
        GLES20.glDisableVertexAttribArray(ShaderLib.uiUV);
        GLES20.glDisableVertexAttribArray(ShaderLib.uiCol);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        count = 0;
    }
}
