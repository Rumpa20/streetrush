package online.arnitcreation.streetrush.game;

import online.arnitcreation.streetrush.engine.MatrixStack;
import online.arnitcreation.streetrush.engine.Primitives;
import online.arnitcreation.streetrush.engine.ShaderLib;
import online.arnitcreation.streetrush.meta.Catalog;

/** Blocky low-poly humanoid built from cubes, with run/jump/slide/board poses. */
public final class Humanoid {

    public static void draw(MatrixStack st, Catalog.Char ch, Player p, float t, boolean show) {
        if (!show) {
            return;
        }
        float swing;
        boolean air = p.airborne || p.jetT > 0;
        if (p.sliding) {
            swing = 0;
        } else if (air) {
            swing = -0.5f;
        } else {
            swing = (float) Math.sin(p.runPhase) * 1.05f;
        }

        st.push();
        // lean into lane changes
        st.rotateZ(p.tilt * 9f);
        if (p.boardActive) {
            drawBoard(st, ch, t);
            st.translate(0, 0.34f, 0);
        }
        if (p.sliding) {
            st.translate(0, 0.78f, 0);
            st.rotateX(-72f);
            st.translate(0, -0.7f, 0);
        }

        drawLeg(st, ch.pants, -0.16f, swing);
        drawLeg(st, ch.pants, 0.16f, -swing);

        // torso
        col(ch.shirt);
        st.push();
        st.translate(0, 1.02f, 0);
        st.scale(0.52f, 0.6f, 0.34f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();

        // arms
        drawArm(st, ch, -0.355f, -swing);
        drawArm(st, ch, 0.355f, swing);

        // head
        col(ch.skin);
        st.push();
        st.translate(0, 1.58f, 0);
        st.scale(0.42f, 0.42f, 0.42f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();

        // eyes (facing -z, away from camera)
        col(0xFF17191F);
        st.push();
        st.translate(-0.1f, 1.62f, -0.22f);
        st.scale(0.07f, 0.09f, 0.02f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();
        st.push();
        st.translate(0.1f, 1.62f, -0.22f);
        st.scale(0.07f, 0.09f, 0.02f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();

        drawHat(st, ch);
        if (p.jetT > 0) {
            drawJetpack(st, ch);
        }
        if (p.shieldOn) {
            col(0xFF3DC1FF);
            st.push();
            st.translate(0, 1.0f, 0);
            float s = 1.05f + (float) Math.sin(t * 6f) * 0.05f;
            st.scale(s, s * 1.5f, s);
            Primitives.cyl.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.65f);
            st.pop();
        }
        st.pop();
    }

    private static void drawLeg(MatrixStack st, int pants, float x, float swing) {
        col(pants);
        st.push();
        st.translate(x, 0.88f, 0);
        st.rotateX(swing * 38f);
        st.translate(0, -0.36f, 0);
        st.scale(0.19f, 0.78f, 0.2f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();
    }

    private static void drawArm(MatrixStack st, Catalog.Char ch, float x, float swing) {
        col(ch.shirt);
        st.push();
        st.translate(x, 1.26f, 0);
        st.rotateX(swing * 42f - 6f);
        st.translate(0, -0.26f, 0);
        st.scale(0.15f, 0.5f, 0.16f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();
        col(ch.skin);
        st.push();
        st.translate(x, 1.26f, 0);
        st.rotateX(swing * 42f - 6f);
        st.translate(0, -0.62f, 0);
        st.scale(0.14f, 0.2f, 0.15f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();
    }

    private static void drawHat(MatrixStack st, Catalog.Char ch) {
        if (ch.hat == 0) {
            return;
        }
        col(ch.hatColor);
        if (ch.hat == 1) { // cap
            st.push();
            st.translate(0, 1.84f, 0);
            st.scale(0.46f, 0.16f, 0.46f);
            Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
            st.pop();
            st.push();
            st.translate(0, 1.78f, -0.3f);
            st.scale(0.4f, 0.06f, 0.22f);
            Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
            st.pop();
        } else if (ch.hat == 2) { // hood
            st.push();
            st.translate(0, 1.62f, 0.04f);
            st.scale(0.5f, 0.5f, 0.5f);
            Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
            st.pop();
        } else if (ch.hat == 3) { // cyber visor
            st.push();
            st.translate(0, 1.68f, -0.2f);
            st.scale(0.4f, 0.12f, 0.12f);
            Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.85f);
            st.pop();
        } else if (ch.hat == 4) { // bandana
            st.push();
            st.translate(0, 1.7f, 0);
            st.scale(0.46f, 0.14f, 0.46f);
            Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
            st.pop();
        } else if (ch.hat == 5) { // legend crown
            col(0xFFFFC93C);
            st.push();
            st.translate(0, 1.88f, 0);
            st.scale(0.4f, 0.14f, 0.4f);
            Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.4f);
            st.pop();
        }
    }

    private static void drawJetpack(MatrixStack st, Catalog.Char ch) {
        col(0xFF78909C);
        st.push();
        st.translate(-0.16f, 1.1f, 0.28f);
        st.scale(0.18f, 0.6f, 0.18f);
        Primitives.cyl.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();
        st.push();
        st.translate(0.16f, 1.1f, 0.28f);
        st.scale(0.18f, 0.6f, 0.18f);
        Primitives.cyl.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        st.pop();
        col(0xFFFF7043);
        float fl = 0.25f + (float) Math.random() * 0.2f;
        st.push();
        st.translate(-0.16f, 0.7f - fl / 2f, 0.28f);
        st.scale(0.14f, fl, 0.14f);
        Primitives.cyl.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.7f);
        st.pop();
        st.push();
        st.translate(0.16f, 0.7f - fl / 2f, 0.28f);
        st.scale(0.14f, fl, 0.14f);
        Primitives.cyl.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.7f);
        st.pop();
    }

    private static void drawBoard(MatrixStack st, Catalog.Char ch, float t) {
        col(ch.accent);
        st.push();
        st.translate(0, 0.16f + (float) Math.sin(t * 7f) * 0.03f, 0);
        st.scale(0.7f, 0.12f, 1.7f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.4f);
        st.pop();
        col(0xFF7CFFE4);
        st.push();
        st.translate(0, 0.06f, 0);
        st.scale(0.95f, 0.05f, 0.2f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 1f);
        st.push();
        st.translate(0, 0, 0);
        st.scale(1f, 1f, 4f);
        Primitives.cube.draw(st.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 1f);
        st.pop();
        st.pop();
    }

    private static void col(int argb) {
        online.arnitcreation.streetrush.engine.Cfg.col3(argb, ShaderLib.C3);
    }
}
