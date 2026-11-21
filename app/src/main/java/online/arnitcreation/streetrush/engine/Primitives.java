package online.arnitcreation.streetrush.engine;

import android.opengl.GLES20;
import java.util.ArrayList;

/** Factory of small low-poly primitives; each mesh is created once and reused forever. */
public final class Primitives {
    private Primitives() {}

    public static Mesh cube, cone, pyramid, cyl, coin, quad, pillar, rail, spike;

    public static void init() {
        cube = box();
        pyramid = pyramid();
        cyl = cylinder(10);
        coin = cylinder(14);
        quad = quadXZ();
        pillar = octaColumn();
        spike = cone6();
        rail = box();
        cone = cone6();
    }

    private static void face(ArrayList<Float> v,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        float[] p = {ax, ay, az, nx, ny, nz, bx, by, bz, nx, ny, nz, cx, cy, cz, nx, ny, nz,
                ax, ay, az, nx, ny, nz, cx, cy, cz, nx, ny, nz, dx, dy, dz, nx, ny, nz};
        for (float f : p) {
            v.add(f);
        }
    }

    /** Unit cube centered at origin, faces of unit length. */
    public static Mesh box() {
        ArrayList<Float> v = new ArrayList<Float>(24 * 36);
        float h = 0.5f;
        // +z
        face(v, -h, -h, h, h, -h, h, h, h, h, -h, h, h, 0, 0, 1);
        // -z
        face(v, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0, 0, -1);
        // +x
        face(v, h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1, 0, 0);
        // -x
        face(v, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1, 0, 0);
        // +y
        face(v, -h, h, h, h, h, h, h, h, -h, -h, h, -h, 0, 1, 0);
        // -y
        face(v, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0, -1, 0);
        return new Mesh(toArray(v), GLES20.GL_TRIANGLES);
    }

    /** Cylinder with radius 0.5, height 1, centered, axis Y. */
    public static Mesh cylinder(int seg) {
        ArrayList<Float> v = new ArrayList<Float>();
        float r = 0.5f;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * 2 * Math.PI / seg);
            float a1 = (float) ((i + 1) * 2 * Math.PI / seg);
            float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r;
            float x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
            float n0x = (float) Math.cos(a0), n0z = (float) Math.sin(a0);
            float n1x = (float) Math.cos(a1), n1z = (float) Math.sin(a1);
            // side quads (two triangles)
            v.add(x0); v.add(-0.5f); v.add(z0); v.add(n0x); v.add(0f); v.add(n0z);
            v.add(x1); v.add(-0.5f); v.add(z1); v.add(n1x); v.add(0f); v.add(n1z);
            v.add(x1); v.add(0.5f); v.add(z1); v.add(n1x); v.add(0f); v.add(n1z);
            v.add(x0); v.add(-0.5f); v.add(z0); v.add(n0x); v.add(0f); v.add(n0z);
            v.add(x1); v.add(0.5f); v.add(z1); v.add(n1x); v.add(0f); v.add(n1z);
            v.add(x0); v.add(0.5f); v.add(z0); v.add(n0x); v.add(0f); v.add(n0z);
            // caps
            v.add(x0); v.add(0.5f); v.add(z0); v.add(0f); v.add(1f); v.add(0f);
            v.add(x1); v.add(0.5f); v.add(z1); v.add(0f); v.add(1f); v.add(0f);
            v.add(0f); v.add(0.5f); v.add(0f); v.add(0f); v.add(1f); v.add(0f);
            v.add(0f); v.add(-0.5f); v.add(0f); v.add(0f); v.add(-1f); v.add(0f);
            v.add(x1); v.add(-0.5f); v.add(z1); v.add(0f); v.add(-1f); v.add(0f);
            v.add(x0); v.add(-0.5f); v.add(z0); v.add(0f); v.add(-1f); v.add(0f);
        }
        return new Mesh(toArray(v), GLES20.GL_TRIANGLES);
    }

    /** 6-sided cone with radius 0.5 and height 1 (base at -0.5). */
    public static Mesh cone6() {
        ArrayList<Float> v = new ArrayList<Float>();
        int seg = 8;
        float r = 0.5f;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * 2 * Math.PI / seg);
            float a1 = (float) ((i + 1) * 2 * Math.PI / seg);
            float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r;
            float x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
            float mx = (x0 + x1) * 0.5f, mz = (z0 + z1) * 0.5f;
            float ml = (float) Math.sqrt(mx * mx + mz * mz);
            float nx = mx / ml, nz = mz / ml;
            v.add(x0); v.add(-0.5f); v.add(z0); v.add(nx); v.add(0.35f); v.add(nz);
            v.add(x1); v.add(-0.5f); v.add(z1); v.add(nx); v.add(0.35f); v.add(nz);
            v.add(0f); v.add(0.5f); v.add(0f); v.add(nx); v.add(0.35f); v.add(nz);
            // base disc segment
            v.add(0f); v.add(-0.5f); v.add(0f); v.add(0f); v.add(-1f); v.add(0f);
            v.add(x1); v.add(-0.5f); v.add(z1); v.add(0f); v.add(-1f); v.add(0f);
            v.add(x0); v.add(-0.5f); v.add(z0); v.add(0f); v.add(-1f); v.add(0f);
        }
        return new Mesh(toArray(v), GLES20.GL_TRIANGLES);
    }

    /** 4-sided pyramid for roofs. */
    public static Mesh pyramid() {
        ArrayList<Float> v = new ArrayList<Float>();
        float h = 0.5f, r = 0.5f;
        // four sloped faces
        float[][] pts = {{-r, -h, r}, {r, -h, r}, {r, -h, -r}, {-r, -h, -r}};
        for (int i = 0; i < 4; i++) {
            float[] a = pts[i];
            float[] b = pts[(i + 1) % 4];
            float mx = (a[0] + b[0]) * 0.5f, mz = (a[2] + b[2]) * 0.5f;
            float ml = (float) Math.sqrt(mx * mx + mz * mz);
            float nx = mx / ml, nz = mz / ml;
            v.add(a[0]); v.add(a[1]); v.add(a[2]); v.add(nx); v.add(0.6f); v.add(nz);
            v.add(b[0]); v.add(b[1]); v.add(b[2]); v.add(nx); v.add(0.6f); v.add(nz);
            v.add(0f); v.add(h); v.add(0f); v.add(nx); v.add(0.6f); v.add(nz);
        }
        // base
        v.add(-r); v.add(-h); v.add(r); v.add(0f); v.add(-1f); v.add(0f);
        v.add(r); v.add(-h); v.add(-r); v.add(0f); v.add(-1f); v.add(0f);
        v.add(r); v.add(-h); v.add(r); v.add(0f); v.add(-1f); v.add(0f);
        v.add(-r); v.add(-h); v.add(r); v.add(0f); v.add(-1f); v.add(0f);
        v.add(-r); v.add(-h); v.add(-r); v.add(0f); v.add(-1f); v.add(0f);
        v.add(r); v.add(-h); v.add(-r); v.add(0f); v.add(-1f); v.add(0f);
        return new Mesh(toArray(v), GLES20.GL_TRIANGLES);
    }

    /** Flat 1x1 horizontal quad facing up (roads, pits, markings). */
    public static Mesh quadXZ() {
        ArrayList<Float> v = new ArrayList<Float>();
        float h = 0.5f;
        face(v, -h, 0, h, h, 0, h, h, 0, -h, -h, 0, -h, 0, 1, 0);
        return new Mesh(toArray(v), GLES20.GL_TRIANGLES);
    }

    /** Slim octagonal column (streetlights, metro pillars). */
    public static Mesh octaColumn() {
        return cylinder(8);
    }

    private static float[] toArray(ArrayList<Float> v) {
        float[] out = new float[v.size()];
        for (int i = 0; i < v.size(); i++) {
            out[i] = v.get(i);
        }
        return out;
    }
}
