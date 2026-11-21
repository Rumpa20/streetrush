package online.arnitcreation.streetrush.world;

import android.opengl.GLES20;
import online.arnitcreation.streetrush.engine.Cfg;
import online.arnitcreation.streetrush.engine.MatrixStack;
import online.arnitcreation.streetrush.engine.Primitives;
import online.arnitcreation.streetrush.engine.ShaderLib;

/** Draws one chunk: road, shoulders, lane marks, decorations, obstacles, coins, pickups. */
public final class ChunkDraw {
    private ChunkDraw() {}

    public static void draw(Chunk c, MatrixStack stack, float t, boolean dense) {
        if (c.offset < -Chunk.LEN - 10 || c.offset > Chunk.LEN) {
            return;
        }
        float base = c.offset;
        int[] curT = Themes.cur;

        // road surface
        col(curT[Themes.I_ROAD]);
        stack.push();
        stack.translate(0, 0, base - Chunk.LEN / 2f);
        stack.scale(Cfg.ROAD_HALF * 2f, 1f, Chunk.LEN);
        stack.translate(0, -0.5f, 0);
        Primitives.quad.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        stack.pop();

        // shoulders
        col(curT[Themes.I_SHOULDER]);
        stack.push();
        stack.translate(-(Cfg.ROAD_HALF + 1.6f), -0.2f, base - Chunk.LEN / 2f);
        stack.scale(2.6f, 0.4f, Chunk.LEN);
        Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        stack.pop();
        stack.push();
        stack.translate(Cfg.ROAD_HALF + 1.6f, -0.2f, base - Chunk.LEN / 2f);
        stack.scale(2.6f, 0.4f, Chunk.LEN);
        Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
        stack.pop();

        // lane divider dashes
        col(curT[Themes.I_MARK]);
        float mr = ShaderLib.C3[0], mg = ShaderLib.C3[1], mb = ShaderLib.C3[2];
        for (int k = 0; k < Chunk.LEN / 5; k++) {
            for (int s = -1; s <= 1; s += 2) {
                stack.push();
                stack.translate(s * Cfg.LANE_W / 2f, 0.02f, base - k * 5f - 1.25f);
                stack.scale(0.14f, 1f, 2.2f);
                stack.translate(0, -0.5f, 0);
                Primitives.quad.draw(stack.top(), mr, mg, mb, 0.4f);
                stack.pop();
            }
        }

        // decorations
        int decLimit = dense ? c.decCount : Math.min(c.decCount, 9);
        for (int i = 0; i < decLimit; i++) {
            drawDecoration(c, stack, i * 5, t, base);
        }

        // obstacles
        for (int i = 0; i < c.obsCount; i++) {
            drawObstacle(c, stack, c.obs[i], t, base);
        }

        // coins
        for (int j = 0; j < c.coinCount; j++) {
            int o = j * 4;
            if (c.coins[o + 3] > 0) {
                continue;
            }
            stack.push();
            stack.translate(Cfg.laneX((int) c.coins[o]),
                    c.coins[o + 2] + (float) Math.sin(t * 3f + j) * 0.07f,
                    base - c.coins[o + 1]);
            stack.rotateY(t * 160f);
            stack.scale(0.5f, 0.5f, 0.12f);
            col(0xFFF7B500);
            Primitives.coin.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.55f);
            stack.pop();
        }

        // power-up pickup
        if (c.spawnLane >= 0 && !c.powerTaken) {
            stack.push();
            stack.translate(Cfg.laneX(c.spawnLane),
                    1.15f + (float) Math.sin(t * 4f) * 0.25f, base - c.spawnZ);
            stack.rotateY(t * 120f);
            stack.rotateX(25f + (float) Math.sin(t * 2f) * 15f);
            stack.scale(0.62f, 0.62f, 0.62f);
            col(0xFF22E4C8);
            Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.8f);
            stack.pop();
        }
    }

    private static void drawDecoration(Chunk c, MatrixStack stack, int o, float t, float base) {
        int type = (int) c.dec[o];
        float x = c.dec[o + 1];
        float z = c.offset - c.dec[o + 2];
        float sizeA = c.dec[o + 3];
        float sizeB = c.dec[o + 4];
        int ci = c.decColor[o / 5];
        switch (type) {
            case Chunk.D_BUILDING: {
                int[] pal = {Themes.get(c.theme, Themes.I_BLD_A),
                        Themes.get(c.theme, Themes.I_BLD_B), Themes.get(c.theme, Themes.I_BLD_C)};
                col(pal[ci]);
                stack.push();
                stack.translate(x, sizeB / 2f, z);
                stack.scale(sizeA, sizeB, sizeA);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                if (c.theme == Themes.NEON || c.theme == Themes.METRO) {
                    col(Themes.get(c.theme, Themes.I_GLOW));
                    stack.push();
                    stack.translate(x, sizeB * 0.25f, z + sizeA / 2f + 0.02f);
                    stack.scale(sizeA * 0.25f, sizeB * 0.12f, 0.05f);
                    Primitives.quad.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.95f);
                    stack.pop();
                }
                break;
            }
            case Chunk.D_TREE: {
                col(0xFF6B4B2A);
                stack.push();
                stack.translate(x, 0.7f, z);
                stack.scale(0.28f, 1.4f, 0.28f);
                Primitives.cyl.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                col(0xFF2E7D32);
                stack.push();
                stack.translate(x, 1.7f, z);
                stack.scale(1.5f, 1.6f, 1.5f);
                Primitives.cone.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                break;
            }
            case Chunk.D_LIGHT: {
                col(0xFF54617A);
                stack.push();
                stack.translate(x, 2f, z);
                stack.scale(0.14f, 4f, 0.14f);
                Primitives.cyl.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                stack.push();
                stack.translate(x > 0 ? x - 0.8f : x + 0.8f, 4f, z);
                stack.scale(1.7f, 0.14f, 0.14f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                float blink = 0.75f + 0.25f * (float) Math.sin(t * 2.2f + z);
                col(0xFFF7E9B0);
                stack.push();
                stack.translate(x > 0 ? x - 1.5f : x + 1.5f, 3.85f, z);
                stack.scale(0.35f, 0.2f, 0.35f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], blink);
                stack.pop();
                break;
            }
            case Chunk.D_SIGN: {
                col(0xFF464C63);
                stack.push();
                stack.translate(x, 2.8f, z);
                stack.scale(0.16f, 2.8f, 0.16f);
                Primitives.cyl.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                int col = 0xFFFF3D8A;
                if (((int) c.dec[o + 2]) % 3 == 0) {
                    col = 0xFF22E4C8;
                } else if (((int) c.dec[o + 2]) % 3 == 1) {
                    col = 0xFFFFC93C;
                }
                col(col);
                stack.push();
                stack.translate(x > 0 ? x - 0.05f : x + 0.05f, 2.2f + sizeA * 0.4f, z);
                stack.rotateY(x > 0 ? -78f : 78f);
                stack.scale(0.1f, sizeA, sizeA * 1.6f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.65f);
                stack.pop();
                break;
            }
            case Chunk.D_PILLAR: {
                col(0xFF38415C);
                stack.push();
                stack.translate(x, sizeA / 2f, z);
                stack.scale(0.5f, sizeA, 0.5f);
                Primitives.pillar.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                break;
            }
            case Chunk.D_AWNING: {
                col(ci == 0 ? 0xFFB4432E : (ci == 1 ? 0xFF2E75B4 : 0xFF2E9E54));
                stack.push();
                stack.translate(x, 2.1f, z);
                stack.scale(1.7f, 0.14f, 2.2f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                break;
            }
            default:
                break;
        }
    }

    private static void drawObstacle(Chunk c, MatrixStack stack, Obstacle ob, float t, float base) {
        float near = base - ob.z;
        float cz = near - ob.length / 2f;
        float x = Cfg.laneX(ob.lane);
        float r, g, b;
        switch (ob.kind) {
            case Obstacle.K_LOW: {
                col(ob.variant == 0 ? 0xFFE6432B : 0xFFFF8A2B);
                r = ShaderLib.C3[0];
                g = ShaderLib.C3[1];
                b = ShaderLib.C3[2];
                if (ob.variant == 0) {
                    stack.push();
                    stack.translate(x, 0.35f, cz);
                    stack.scale(1.7f, 0.7f, 0.34f);
                    Primitives.cube.draw(stack.top(), r, g, b, 0f);
                    stack.pop();
                    col(0xFFF1F2F6);
                    stack.push();
                    stack.translate(x, 0.5f, cz + 0.18f);
                    stack.scale(1.7f, 0.22f, 0.02f);
                    Primitives.quad.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.4f);
                    stack.pop();
                } else {
                    stack.push();
                    stack.translate(x, 0.7f, cz);
                    stack.scale(1.0f, 1.4f, 1.0f);
                    Primitives.cone.draw(stack.top(), r, g, b, 0f);
                    stack.pop();
                    col(0xFFF1F2F6);
                    stack.push();
                    stack.translate(x, 0.42f, cz);
                    stack.scale(0.95f, 0.34f, 0.95f);
                    Primitives.cyl.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.2f);
                    stack.pop();
                }
                break;
            }
            case Obstacle.K_HIGH: {
                col(0xFF464C63);
                r = ShaderLib.C3[0];
                g = ShaderLib.C3[1];
                b = ShaderLib.C3[2];
                stack.push();
                stack.translate(x - 1.7f, 1.72f, cz);
                stack.scale(0.18f, 1.4f, 0.18f);
                Primitives.cube.draw(stack.top(), r, g, b, 0f);
                stack.pop();
                stack.push();
                stack.translate(x + 1.7f, 1.72f, cz);
                stack.scale(0.18f, 1.4f, 0.18f);
                Primitives.cube.draw(stack.top(), r, g, b, 0f);
                stack.pop();
                col(0xFFFF3D8A);
                stack.push();
                stack.translate(x, 2.35f, cz);
                stack.scale(3.6f, 0.34f, 0.4f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.4f);
                stack.pop();
                break;
            }
            case Obstacle.K_WALL: {
                col(0xFF9A5B33);
                r = ShaderLib.C3[0];
                g = ShaderLib.C3[1];
                b = ShaderLib.C3[2];
                for (int k = 0; k < 3; k++) {
                    stack.push();
                    stack.translate(x, 0.44f + k * 0.88f, cz);
                    stack.scale(1.7f, 0.8f, 0.8f);
                    stack.rotateY(k * 7f - 7f);
                    Primitives.cube.draw(stack.top(), r - k * 0.05f, g, b, 0f);
                    stack.pop();
                }
                break;
            }
            case Obstacle.K_VEHICLE: {
                drawVehicle(stack, x, cz, ob, t);
                break;
            }
            case Obstacle.K_TRAIN: {
                col(0xFFB9C4D0);
                r = ShaderLib.C3[0];
                g = ShaderLib.C3[1];
                b = ShaderLib.C3[2];
                stack.push();
                stack.translate(x, 1.5f, cz);
                stack.scale(1.9f, 2.6f, ob.length);
                Primitives.cube.draw(stack.top(), r, g, b, 0f);
                stack.pop();
                col(0xFF2B3A4A);
                stack.push();
                stack.translate(x, 2.1f, cz);
                stack.scale(1.94f, 0.7f, ob.length * 0.86f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.15f);
                stack.pop();
                col(0xFFF7C948);
                stack.push();
                stack.translate(x, 0.8f, cz + ob.length / 2f + 0.05f);
                stack.scale(1.4f, 0.3f, 0.06f);
                Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 1f);
                stack.pop();
                break;
            }
            case Obstacle.K_GAP: {
                col(0xFF05060C);
                stack.push();
                stack.translate(x, 0.01f, cz);
                stack.scale(2.2f, 1f, ob.length);
                Primitives.quad.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
                stack.pop();
                break;
            }
            default:
                break;
        }
    }

    private static void drawVehicle(MatrixStack stack, float x, float cz, Obstacle ob, float t) {
        int[] body = {0xFFD64545, 0xFF4577D6, 0xFFE0B33C, 0xFF63C56B};
        col(body[ob.variant & 3]);
        float r = ShaderLib.C3[0], g = ShaderLib.C3[1], b = ShaderLib.C3[2];
        float bounce = ob.moving ? (float) Math.sin(t * 18f) * 0.02f : 0f;
        stack.push();
        stack.translate(x, 0.34f + bounce, cz);
        stack.scale(1.8f, 0.7f, ob.length);
        Primitives.cube.draw(stack.top(), r, g, b, 0f);
        stack.pop();
        col(0xFF2B3A4A);
        stack.push();
        stack.translate(x, 1.1f + bounce, cz - (ob.moving ? 0.35f : -0.35f));
        stack.scale(1.5f, 0.7f, ob.length * 0.5f);
        Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0.1f);
        stack.pop();
        col(0xFF17191F);
        for (int w = 0; w < 4; w++) {
            float wx = (w & 1) == 0 ? x - 0.85f : x + 0.85f;
            float wz = (w & 2) == 0 ? cz - 1.3f : cz + 1.3f;
            stack.push();
            stack.translate(wx, 0.34f, wz);
            stack.scale(0.22f, 0.7f, 0.7f);
            Primitives.cyl.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 0f);
            stack.pop();
        }
        if (ob.moving) {
            col(0xFFF7C948);
            stack.push();
            stack.translate(x, 0.7f, cz + ob.length / 2f + 0.03f);
            stack.scale(1.3f, 0.2f, 0.05f);
            Primitives.cube.draw(stack.top(), ShaderLib.C3[0], ShaderLib.C3[1], ShaderLib.C3[2], 1f);
            stack.pop();
        }
    }

    private static void col(int argb) {
        Cfg.col3(argb, ShaderLib.C3);
    }
}
