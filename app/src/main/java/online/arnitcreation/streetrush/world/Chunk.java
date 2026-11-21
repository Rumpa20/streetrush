package online.arnitcreation.streetrush.world;

import java.util.Random;

/**
 * One modular section of the running world. Holds obstacles, coins and decorations in
 * fixed-capacity pooled arrays. Generation is fairness-checked so the player always has
 * a survivable path through every row.
 */
public final class Chunk {
    public static final int LEN = (int) online.arnitcreation.streetrush.engine.Cfg.CHUNK_LEN;
    public static final int DEC_CAP = 24;
    public static final int OBS_CAP = 20;
    public static final int COIN_CAP = 96;

    public static final int D_BUILDING = 0, D_TREE = 1, D_LIGHT = 2, D_SIGN = 3,
            D_PILLAR = 4, D_AWNING = 5;

    public float offset;          // world z of the chunk's NEAR edge
    public int theme;

    public final Obstacle[] obs = new Obstacle[OBS_CAP];
    public int obsCount;

    // coins: [laneIndex, localZ, centerY, takenFlag] per coin
    public final float[] coins = new float[COIN_CAP * 4];
    public int coinCount;

    // decorations: [type, x, localZ, sizeA, sizeB]
    public final float[] dec = new float[DEC_CAP * 5];
    public final int[] decColor = new int[DEC_CAP];
    public int decCount;

    public int spawnLane = -1;    // power-up pickup lane in this chunk (-1 = none)
    public float spawnZ;
    public boolean powerTaken;

    private final Random rng;
    private final int[] rowKinds = new int[3];

    public Chunk(Random rng) {
        this.rng = rng;
        for (int i = 0; i < OBS_CAP; i++) {
            obs[i] = new Obstacle();
        }
    }

    /** tier: 0 tutorial, 1 easy, 2 medium, 3 hard. decDense: high graphics quality. */
    public void generate(int themeIdx, int tier, boolean decDense, boolean safe) {
        theme = themeIdx;
        obsCount = 0;
        coinCount = 0;
        decCount = 0;
        spawnLane = -1;
        powerTaken = false;

        generateDecorations(decDense);
        if (safe) {
            if (rng.nextFloat() < 0.7f) {
                coinLine(1, 5, 10f);
            }
            return;
        }
        generateObstacles(tier);
    }

    private void generateDecorations(boolean dense) {
        int target = dense ? 20 : 11;
        for (int j = 0; j < target && decCount < DEC_CAP; j++) {
            float z = rng.nextFloat() * LEN;
            boolean left = rng.nextBoolean();
            int r = rng.nextInt(100);
            int type;
            float sizeA, sizeB;
            float x;
            if (theme == Themes.JUNGLE) {
                type = r < 25 ? D_BUILDING : (r < 88 ? D_TREE : D_LIGHT);
            } else if (theme == Themes.NEON || theme == Themes.METRO) {
                type = r < 42 ? D_BUILDING : (r < 68 ? D_LIGHT : (r < 86 ? D_SIGN : D_PILLAR));
            } else if (theme == Themes.INDIAN) {
                type = r < 30 ? D_BUILDING : (r < 55 ? D_TREE : (r < 78 ? D_AWNING : D_SIGN));
            } else {
                type = r < 35 ? D_BUILDING : (r < 60 ? D_LIGHT : (r < 88 ? D_SIGN : D_TREE));
            }
            if (type == D_BUILDING) {
                x = left ? -(8f + rng.nextFloat() * 6f) : (8f + rng.nextFloat() * 6f);
                sizeA = 4f + rng.nextFloat() * 4f;
                sizeB = 4f + rng.nextFloat() * 6f;
            } else {
                x = left ? -(4.9f + rng.nextFloat() * 1.4f) : (4.9f + rng.nextFloat() * 1.4f);
                sizeA = 1f;
                sizeB = 0f;
                if (type == D_PILLAR) {
                    x = left ? -5.6f : 5.6f;
                    sizeA = 4.4f;
                }
            }
            decColor[decCount] = rng.nextInt(3);
            int o = decCount * 5;
            dec[o] = type;
            dec[o + 1] = x;
            dec[o + 2] = z;
            dec[o + 3] = sizeA;
            dec[o + 4] = sizeB;
            decCount++;
        }
    }

    private void generateObstacles(int tier) {
        float z = 4f;
        while (z < LEN - 5f && obsCount < OBS_CAP - 2) {
            int roll = rng.nextInt(100);
            if (roll < 55) {
                z += buildRow(z, tier) + rowGap(tier);
            } else if (roll < 75 && tier >= 1) {
                z += buildLong(z, tier) + rowGap(tier);
            } else {
                if (coinCount < COIN_CAP - 18 && rng.nextFloat() < 0.6f) {
                    coinZigzag(z);
                }
                z += 7f + rng.nextFloat() * 5f;
            }
        }
    }

    private float rowGap(int tier) {
        return 9f + (3 - tier) * 1.2f + rng.nextFloat() * 4f;
    }

    /**
     * One z-row. Up to (1 + tier) lanes carry obstacles. Rows with 3 blockers are always
     * a single uniform action (all jump / all slide / all gap). Hard blockers (walls,
     * vehicles, trains) never exceed 2 per row, so at least one safe lane always exists.
     */
    private float buildRow(float z, int tier) {
        for (int j = 0; j < 3; j++) {
            rowKinds[j] = -1;
        }
        int roll = rng.nextInt(100);
        boolean gapRow = tier >= 1 && roll < 8;
        if (gapRow) {
            for (int j = 0; j < 3; j++) {
                rowKinds[j] = Obstacle.K_GAP;
                place(j, Obstacle.K_GAP, z, 2.6f);
            }
            coinArc(1, z + 1.3f);
            return 2.6f;
        }
        int maxBlocked = Math.min(3, 1 + tier);
        if (tier >= 2 && roll >= 8 && roll < 24) {
            maxBlocked = 3;
        }
        if (maxBlocked == 3) {
            int k = rng.nextBoolean() ? Obstacle.K_LOW : Obstacle.K_HIGH;
            for (int j = 0; j < 3; j++) {
                rowKinds[j] = k;
                place(j, k, z, 2.2f);
            }
            coinArc(rng.nextInt(3), z + 1.1f);
        } else {
            int blocked = 1 + rng.nextInt(maxBlocked);
            int act = -1;
            for (int b = 0; b < blocked; b++) {
                int lane = rng.nextInt(3);
                int guard = 0;
                while (rowKinds[lane] != -1 && guard++ < 4) {
                    lane = (lane + 1) % 3;
                }
                if (rowKinds[lane] != -1) {
                    break;
                }
                int k = pickKind(tier);
                if (k == Obstacle.K_LOW || k == Obstacle.K_HIGH) {
                    if (act != -1) {
                        k = act;
                    } else {
                        act = k;
                    }
                }
                rowKinds[lane] = k;
                place(lane, k, z, 2.2f);
                if (k == Obstacle.K_LOW && rng.nextFloat() < 0.5f) {
                    coinArc(lane, z + 1.1f);
                }
            }
            if (coinCount < COIN_CAP - 12 && rng.nextFloat() < 0.55f) {
                int free = 1;
                for (int j = 0; j < 3; j++) {
                    if (rowKinds[j] == -1) {
                        free = j;
                        break;
                    }
                }
                coinLine(free, 4, Math.max(2f, z - 1f));
            }
        }
        return 2.2f;
    }

    /** Long structure: train car, optionally with a parked vehicle beside it. */
    private float buildLong(float z, int tier) {
        float len = 12f + rng.nextFloat() * 4f;
        int lane = rng.nextInt(3);
        place(lane, Obstacle.K_TRAIN, z, len);
        if (tier >= 1 && rng.nextFloat() < 0.4f && (lane == 0 || lane == 2)) {
            int lane2 = lane == 0 ? 1 : 1;
            place(lane2, Obstacle.K_VEHICLE, z + 2f, 4.2f);
        }
        int free = 2;
        for (int j = 0; j < 3; j++) {
            if (j != lane && j != (lane == 0 ? 1 : 1)) {
                free = j;
                break;
            }
        }
        if (coinCount < COIN_CAP - 12) {
            coinLine(free, (int) (len / 1.8f), z + 1f);
        } else if (coinCount < COIN_CAP - 6) {
            coinLine(free, 3, z + 1f);
        }
        return len;
    }

    private int pickKind(int tier) {
        int r = rng.nextInt(100);
        if (tier <= 1) {
            if (r < 40) {
                return Obstacle.K_LOW;
            }
            if (r < 70) {
                return Obstacle.K_HIGH;
            }
            if (r < 88) {
                return Obstacle.K_WALL;
            }
            return Obstacle.K_VEHICLE;
        }
        if (r < 30) {
            return Obstacle.K_LOW;
        }
        if (r < 55) {
            return Obstacle.K_HIGH;
        }
        if (r < 78) {
            return Obstacle.K_WALL;
        }
        return Obstacle.K_VEHICLE;
    }

    private void place(int lane, int kind, float z, float len) {
        if (obsCount >= OBS_CAP) {
            return;
        }
        Obstacle o = obs[obsCount++];
        o.set(kind, lane, rng.nextInt(4), z, len);
        if (kind == Obstacle.K_VEHICLE && (lane == 0 || lane == 2) && rng.nextFloat() < 0.45f) {
            o.moving = true;
            o.vz = 2.5f + rng.nextFloat() * 2.5f;
        }
    }

    public void coinLine(int lane, int count, float startZ) {
        if (startZ < 1.5f) {
            startZ = 1.5f;
        }
        for (int j = 0; j < count && coinCount < COIN_CAP; j++) {
            addCoin(lane, startZ + j * 1.6f);
        }
    }

    /** Jump-arc coin trail centered on row z. */
    public void coinArc(int lane, float z) {
        for (int j = 0; j < 7 && coinCount < COIN_CAP; j++) {
            float f = j / 6f;
            float y = 1.0f + (float) Math.sin(f * Math.PI) * 1.5f;
            addCoin(lane, z - 4.8f + f * 9.6f, y);
        }
    }

    private void coinZigzag(float z) {
        int lane = rng.nextInt(3);
        for (int j = 0; j < 9 && coinCount < COIN_CAP; j++) {
            if (j == 3) {
                lane = Math.max(0, lane - 1);
            }
            if (j == 6) {
                lane = Math.min(2, lane + 1);
            }
            addCoin(lane, z + j * 1.6f);
        }
    }

    private void addCoin(int lane, float z) {
        addCoin(lane, z, 1.0f);
    }

    private void addCoin(int lane, float z, float y) {
        if (coinCount >= COIN_CAP || z < 0.5f || z > LEN) {
            return;
        }
        int o = coinCount * 4;
        coins[o] = lane;
        coins[o + 1] = z;
        coins[o + 2] = y;
        coins[o + 3] = 0;
        coinCount++;
    }
}
