package online.arnitcreation.streetrush.world;

import java.util.Random;
import online.arnitcreation.streetrush.audio.Audio;
import online.arnitcreation.streetrush.engine.Cfg;
import online.arnitcreation.streetrush.engine.Particles;
import online.arnitcreation.streetrush.game.Player;

/** Endless world: chunk recycling, difficulty progression, collisions, pickups. */
public final class World {
    public static final int POWER_COUNT = 6;

    public final Chunk[] chunks = new Chunk[Cfg.CHUNKS];
    private final Random rng = new Random();

    public float dist;
    public float runTime;
    public int coinsRun;
    public int jumpsRun;
    public int slidesRun;
    public int passedRun;
    public int powerRun;

    public float speed;
    public int themeIdx;

    public int crashEvent;
    public float shakeT, shakeAmp;
    public int coinStreak;

    private float nextPowerAt;
    private int powerKind;

    private final Particles particles;
    private final Audio audio;

    public World(Particles particles, Audio audio) {
        this.particles = particles;
        this.audio = audio;
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            chunks[i] = new Chunk(rng);
        }
        reset();
    }

    public void reset() {
        dist = 0;
        runTime = 0;
        coinsRun = 0;
        jumpsRun = 0;
        slidesRun = 0;
        passedRun = 0;
        powerRun = 0;
        speed = Cfg.BASE_SPEED;
        crashEvent = -1;
        coinStreak = 0;
        nextPowerAt = 260 + rng.nextInt(140);
        powerKind = rng.nextInt(POWER_COUNT);
        themeIdx = rng.nextInt(Themes.COUNT);
        Themes.reset(themeIdx);
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            c.offset = 4 - i * Cfg.CHUNK_LEN;
            c.generate(themeIdx, 0, true, i < 2);
        }
        chunks[1].coinLine(1, 8, 12f);
    }

    public int tier() {
        if (dist < 150) {
            return 0;
        }
        if (dist < 420) {
            return 1;
        }
        if (dist < 950) {
            return 2;
        }
        return 3;
    }

    public int currentTheme() {
        return (int) (dist / 650f) % Themes.COUNT;
    }

    public void update(float dt, Player player) {
        runTime += dt;
        dist += speed * dt;

        float target = Math.min(Cfg.MAX_SPEED, Cfg.BASE_SPEED + runTime * Cfg.ACCEL);
        if (player.boostT > 0) {
            target = Math.min(Cfg.MAX_SPEED + 4f, target * 1.4f);
        }
        speed = target;

        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            c.offset += speed * dt;
            if (c.offset >= 44) {
                float min = Float.MAX_VALUE;
                for (int j = 0; j < Cfg.CHUNKS; j++) {
                    if (chunks[j].offset < min) {
                        min = chunks[j].offset;
                    }
                }
                c.offset = min - Cfg.CHUNK_LEN;
                c.generate(currentTheme(), tier(), true, false);
                maybePlacePowerup(c);
            }
        }

        updateObstacles(dt);
        updateCoins(dt, player);
        updatePowerups(player);
        player.update(dt, speed, this);
        Themes.update(currentTheme(), 1f);
    }

    private void maybePlacePowerup(Chunk c) {
        if (dist < nextPowerAt) {
            return;
        }
        float attempt = 6f + rng.nextFloat() * (Chunk.LEN - 12f);
        int lane = rng.nextInt(3);
        for (int j = 0; j < c.obsCount; j++) {
            Obstacle o = c.obs[j];
            if (o.lane == lane && Math.abs(o.z - attempt) < 6f) {
                return;
            }
        }
        c.spawnLane = lane;
        c.spawnZ = attempt;
        c.powerTaken = false;
        powerKind = rng.nextInt(POWER_COUNT);
        nextPowerAt = dist + 300 + rng.nextInt(160);
    }

    private void updateObstacles(float dt) {
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            for (int j = 0; j < c.obsCount; j++) {
                Obstacle o = c.obs[j];
                if (o.moving) {
                    o.z += (speed - o.vz) * dt;
                }
                float wz = c.offset - o.z;
                if (!o.counted && wz > 2f) {
                    o.counted = true;
                    passedRun++;
                }
            }
        }
    }

    private void updateCoins(float dt, Player player) {
        float magnetR = player.magnetT > 0 ? 26f : 0f;
        boolean jet = player.jetT > 0;
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            for (int j = 0; j < c.coinCount; j++) {
                int o = j * 4;
                if (c.coins[o + 3] > 0) {
                    continue;
                }
                float lane = c.coins[o];
                float z = c.coins[o + 1];
                float y = c.coins[o + 2];
                float wz = c.offset - z;
                if (magnetR > 0 || jet) {
                    float dx = player.x - Cfg.laneX((int) lane);
                    float dz = -wz;
                    float dy = player.centerY() - y;
                    float d2 = dx * dx + dz * dz + dy * dy;
                    if (d2 < magnetR * magnetR) {
                        float pull = Math.min(1f, dt * 9f);
                        float nz = z - pull * dz;
                        c.coins[o + 1] = nz;
                        c.coins[o + 2] = y + pull * dy * 0.6f + 0.3f * dt;
                        z = nz;
                        y = c.coins[o + 2];
                    }
                }
                float wz2 = c.offset - z;
                if (wz2 > -1.1f && wz2 < 1.1f) {
                    float dx = Math.abs(player.x - Cfg.laneX((int) lane));
                    float dy = Math.abs(player.centerY() - y);
                    if (dx < 1.1f && dy < 1.5f) {
                        c.coins[o + 3] = 1;
                        coinsRun++;
                        coinStreak++;
                        audio.play(Audio.SFX_COIN);
                        particles.burst(player.x, y, wz2, 5, 0xFFF7B500, 3f);
                    }
                }
            }
        }
    }

    private void updatePowerups(Player player) {
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            if (c.spawnLane < 0 || c.powerTaken) {
                continue;
            }
            float wz = c.offset - c.spawnZ;
            if (wz > -1.2f && wz < 1.2f
                    && Math.abs(player.x - Cfg.laneX(c.spawnLane)) < 1.2f
                    && Math.abs(player.centerY() - 1.15f) < 1.9f) {
                c.powerTaken = true;
                powerRun++;
                player.applyPower(powerKind, this);
                audio.play(Audio.SFX_POWER);
                particles.burst(Cfg.laneX(c.spawnLane), 1.2f, wz, 18, 0xFF22E4C8, 6f);
            }
        }
    }

    /** Collision check; sets crashEvent (-1 = none) when the player hits a lethal box. */
    public void collide(Player player) {
        if (crashEvent >= 0) {
            return;
        }
        boolean jet = player.jetT > 0 && player.y > 2.2f;
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            if (c.offset < -6f || c.offset > Chunk.LEN) {
                continue;
            }
            for (int j = 0; j < c.obsCount; j++) {
                Obstacle o = c.obs[j];
                float near = c.offset - o.z;
                float far = near - o.length;
                if (near < -Cfg.PLAYER_D || far > Cfg.PLAYER_D) {
                    continue;
                }
                float px = player.x;
                float ox = Cfg.laneX(o.lane);
                if (Math.abs(px - ox) > o.halfW() + Cfg.PLAYER_W * 0.5f) {
                    continue;
                }
                if (o.kind == Obstacle.K_GAP) {
                    if (player.y < 0.09f) {
                        registerCrash(o.kind);
                        return;
                    }
                    continue;
                }
                if (jet) {
                    continue;
                }
                float pHi = player.y + (player.sliding ? Cfg.SLIDE_H : Cfg.PLAYER_H);
                if (pHi > o.yLow() && player.y < o.yHigh()) {
                    registerCrash(o.kind);
                    return;
                }
            }
        }
    }

    private void registerCrash(int kind) {
        crashEvent = kind;
        shakeT = 0.5f;
        shakeAmp = 0.35f;
    }

    /** Removes obstacles near the player (used by revive). */
    public void clearNear(float range) {
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = chunks[i];
            int idx = 0;
            while (idx < c.obsCount) {
                Obstacle o = c.obs[idx];
                float wz = c.offset - o.z - o.length * 0.5f;
                if (wz > -range && wz < range) {
                    for (int k = idx; k < c.obsCount - 1; k++) {
                        Obstacle tmp = c.obs[k + 1];
                        c.obs[k + 1] = c.obs[k];
                        c.obs[k] = tmp;
                    }
                    c.obsCount--;
                } else {
                    idx++;
                }
            }
        }
    }
}
