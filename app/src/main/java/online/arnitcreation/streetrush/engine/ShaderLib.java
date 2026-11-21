package online.arnitcreation.streetrush.engine;

import android.opengl.GLES20;
import javax.microedition.khronos.opengles.GLException;

/** Static GL shader programs and attribute/uniform locations for the whole game. */
public final class ShaderLib {
    private ShaderLib() {}

    public static final float[] C3 = new float[3];

    public static int prog3D, progUI, progPts;

    public static int uVP, uModel, uColor, uLight, uAmb, uFogCol, uFog, uCam, uGlow;
    public static int aPos, aNormal;

    public static int uM;
    public static int uiPos, uiUV, uiCol;

    public static int qVP, qPos, qColor, qSize;

    private static final String VS3D =
            "uniform mat4 uVP;\n" +
            "uniform mat4 uModel;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec3 aNormal;\n" +
            "varying vec3 vN;\n" +
            "varying vec3 vW;\n" +
            "void main() {\n" +
            "  vec4 w = uModel * vec4(aPos, 1.0);\n" +
            "  vW = w.xyz;\n" +
            "  vN = normalize(uModel[0].xyz * aNormal.x + uModel[1].xyz * aNormal.y + uModel[2].xyz * aNormal.z);\n" +
            "  gl_Position = uVP * w;\n" +
            "}\n";

    private static final String FS3D =
            "precision mediump float;\n" +
            "uniform vec3 uColor;\n" +
            "uniform vec3 uLight;\n" +
            "uniform vec3 uAmb;\n" +
            "uniform vec3 uFogCol;\n" +
            "uniform vec2 uFog;\n" +
            "uniform vec3 uCam;\n" +
            "uniform float uGlow;\n" +
            "varying vec3 vN;\n" +
            "varying vec3 vW;\n" +
            "void main() {\n" +
            "  float d = max(dot(normalize(vN), uLight), 0.0);\n" +
            "  vec3 c = uColor * (uAmb + d * 0.85);\n" +
            "  c = mix(c, uColor, uGlow);\n" +
            "  float f = clamp((distance(vW, uCam) - uFog.x) / (uFog.y - uFog.x), 0.0, 1.0);\n" +
            "  gl_FragColor = vec4(mix(c, uFogCol, f), 1.0);\n" +
            "}\n";

    private static final String VSUI =
            "uniform mat4 uM;\n" +
            "attribute vec2 aPos;\n" +
            "attribute vec2 aUV;\n" +
            "attribute vec4 aColor;\n" +
            "varying vec2 vUV;\n" +
            "varying vec4 vC;\n" +
            "void main() {\n" +
            "  gl_Position = uM * vec4(aPos, 0.0, 1.0);\n" +
            "  vUV = aUV;\n" +
            "  vC = aColor;\n" +
            "}\n";

    private static final String FSUI =
            "precision mediump float;\n" +
            "uniform sampler2D uTex;\n" +
            "varying vec2 vUV;\n" +
            "varying vec4 vC;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTex, vUV) * vC;\n" +
            "}\n";

    private static final String VSPTS =
            "uniform mat4 uVP;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec4 aColor;\n" +
            "attribute float aSize;\n" +
            "varying vec4 vC;\n" +
            "void main() {\n" +
            "  gl_Position = uVP * vec4(aPos, 1.0);\n" +
            "  gl_PointSize = aSize;\n" +
            "  vC = aColor;\n" +
            "}\n";

    private static final String FSPTS =
            "precision mediump float;\n" +
            "varying vec4 vC;\n" +
            "void main() {\n" +
            "  gl_FragColor = vC;\n" +
            "}\n";

    private static int compile(int type, String src, String tag) throws GLException {
        int sh = GLES20.glCreateShader(type);
        GLES20.glShaderSource(sh, src);
        GLES20.glCompileShader(sh);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(sh);
            throw new GLException("Shader compile failed (" + tag + "): " + log);
        }
        return sh;
    }

    private static int link(String vs, String fs, String tag, String[] attrNames) throws GLException {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, compile(GLES20.GL_VERTEX_SHADER, vs, tag));
        GLES20.glAttachShader(p, compile(GLES20.GL_FRAGMENT_SHADER, fs, tag));
        for (int i = 0; i < attrNames.length; i++) {
            GLES20.glBindAttribLocation(p, i, attrNames[i]);
        }
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(p);
            throw new GLException("Program link failed (" + tag + "): " + log);
        }
        return p;
    }

    public static void init() throws GLException {
        prog3D = link(VS3D, FS3D, "3d", new String[]{"aPos", "aNormal"});
        uVP = GLES20.glGetUniformLocation(prog3D, "uVP");
        uModel = GLES20.glGetUniformLocation(prog3D, "uModel");
        uColor = GLES20.glGetUniformLocation(prog3D, "uColor");
        uLight = GLES20.glGetUniformLocation(prog3D, "uLight");
        uAmb = GLES20.glGetUniformLocation(prog3D, "uAmb");
        uFogCol = GLES20.glGetUniformLocation(prog3D, "uFogCol");
        uFog = GLES20.glGetUniformLocation(prog3D, "uFog");
        uCam = GLES20.glGetUniformLocation(prog3D, "uCam");
        uGlow = GLES20.glGetUniformLocation(prog3D, "uGlow");
        aPos = GLES20.glGetAttribLocation(prog3D, "aPos");
        aNormal = GLES20.glGetAttribLocation(prog3D, "aNormal");

        progUI = link(VSUI, FSUI, "ui", new String[]{"aPos", "aUV", "aColor"});
        uM = GLES20.glGetUniformLocation(progUI, "uM");
        uiPos = GLES20.glGetAttribLocation(progUI, "aPos");
        uiUV = GLES20.glGetAttribLocation(progUI, "aUV");
        uiCol = GLES20.glGetAttribLocation(progUI, "aColor");

        progPts = link(VSPTS, FSPTS, "pts", new String[]{"aPos", "aColor", "aSize"});
        qVP = GLES20.glGetUniformLocation(progPts, "uVP");
        qPos = GLES20.glGetAttribLocation(progPts, "aPos");
        qColor = GLES20.glGetAttribLocation(progPts, "aColor");
        qSize = GLES20.glGetAttribLocation(progPts, "aSize");
    }
}
