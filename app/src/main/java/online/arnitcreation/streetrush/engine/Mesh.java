package online.arnitcreation.streetrush.engine;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** Static interleaved vertex mesh (pos + normal), uploaded once to a VBO. */
public final class Mesh {
    private final int vbo;
    private final int vertexCount;
    private final int mode;

    /** verts: 6 floats per vertex [x,y,z,nx,ny,nz]; mode: GL_TRIANGLES, GL_LINES... */
    public Mesh(float[] verts, int mode) {
        this.mode = mode;
        vertexCount = verts.length / 6;
        int[] b = new int[1];
        GLES20.glGenBuffers(1, b, 0);
        vbo = b[0];
        FloatBuffer fb = ByteBuffer.allocateDirect(verts.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(verts);
        fb.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.length * 4, fb, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void draw(float[] model, float r, float g, float b, float glow) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glEnableVertexAttribArray(ShaderLib.aPos);
        GLES20.glVertexAttribPointer(ShaderLib.aPos, 3, GLES20.GL_FLOAT, false, 24, 0);
        GLES20.glEnableVertexAttribArray(ShaderLib.aNormal);
        GLES20.glVertexAttribPointer(ShaderLib.aNormal, 3, GLES20.GL_FLOAT, false, 24, 12);
        GLES20.glUniformMatrix4fv(ShaderLib.uModel, 1, false, model, 0);
        GLES20.glUniform3f(ShaderLib.uColor, r, g, b);
        GLES20.glUniform1f(ShaderLib.uGlow, glow);
        GLES20.glDrawArrays(mode, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(ShaderLib.aPos);
        GLES20.glDisableVertexAttribArray(ShaderLib.aNormal);
    }
}
