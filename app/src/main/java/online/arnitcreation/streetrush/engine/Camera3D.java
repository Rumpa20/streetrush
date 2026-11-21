package online.arnitcreation.streetrush.engine;

import android.opengl.Matrix;

/** Perspective camera with precomputed view-projection matrix. */
public final class Camera3D {
    public final float[] proj = new float[16];
    public final float[] view = new float[16];
    public final float[] vp = new float[16];
    public float eyeX, eyeY, eyeZ;
    private final float[] tmpM = new float[16];

    public void perspective(float fovY, float aspect, float near, float far) {
        Matrix.perspectiveM(proj, 0, fovY, aspect, near, far);
    }

    public void lookAt(float ex, float ey, float ez,
                       float cx, float cy, float cz,
                       float ux, float uy, float uz) {
        eyeX = ex;
        eyeY = ey;
        eyeZ = ez;
        Matrix.setLookAtM(view, 0, ex, ey, ez, cx, cy, cz, ux, uy, uz);
    }

    public void refresh() {
        Matrix.multiplyMM(tmpM, 0, proj, 0, view, 0);
        System.arraycopy(tmpM, 0, vp, 0, 16);
    }
}
