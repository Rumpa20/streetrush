package online.arnitcreation.streetrush.engine;

import android.opengl.Matrix;

/** Pre-allocated matrix stack; avoids GC pressure during gameplay. */
public final class MatrixStack {
    private final float[][] stack = new float[48][16];
    private int top = 0;

    public MatrixStack() {
        for (float[] m : stack) {
            Matrix.setIdentityM(m, 0);
        }
    }

    public float[] top() {
        return stack[top];
    }

    public void reset() {
        top = 0;
        Matrix.setIdentityM(stack[0], 0);
    }

    public void push() {
        if (top < stack.length - 1) {
            System.arraycopy(stack[top], 0, stack[top + 1], 0, 16);
        }
        top++;
    }

    public void pop() {
        if (top > 0) {
            top--;
        }
    }

    public void identity() {
        Matrix.setIdentityM(stack[top], 0);
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(stack[top], 0, x, y, z);
    }

    public void rotateY(float deg) {
        Matrix.rotateM(stack[top], 0, deg, 0, 1, 0);
    }

    public void rotateX(float deg) {
        Matrix.rotateM(stack[top], 0, deg, 1, 0, 0);
    }

    public void rotateZ(float deg) {
        Matrix.rotateM(stack[top], 0, deg, 0, 0, 1);
    }

    public void scale(float x, float y, float z) {
        Matrix.scaleM(stack[top], 0, x, y, z);
    }
}
