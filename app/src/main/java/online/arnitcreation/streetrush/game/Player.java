package online.arnitcreation.streetrush.game;

import online.arnitcreation.streetrush.engine.Cfg;
import online.arnitcreation.streetrush.world.World;

/** Player entity: lane movement, jump/slide physics, power-up timers, hoverboard. */
public final class Player {
    public static final String[] POWER_NAMES = {
            "COIN MAGNET", "JETPACK", "SUPER JUMP", "X2 SCORE", "SHIELD", "SPEED BOOST"
    };

    public int lane = 1;
    public float x = 0;
    public float y = 0;
    public float vy = 0;
    public boolean sliding;
    public float slideT;
    public float runPhase;
    public float tilt;
    public boolean airborne;
    public boolean dead;

    public float invulnT;
    public float magnetT, jetT, sjumpT, multT, boostT;
    public boolean shieldOn;
    public float shieldT;

    public boolean boardActive;
    public float boardT;
    public float boardCooldown;

    public int charId;
    public int trailId;
    public final int[] powerLv = {1, 1, 1, 1, 1, 1};
    public int boardDuration = 30;

    public void reset(int charIdx, int trailIdx, int[] levelUps, int boardDur) {
        lane = 1;
        x = 0;
        y = 0;
        vy = 0;
        sliding = false;
        slideT = 0;
        runPhase = 0;
        tilt = 0;
        airborne = false;
        dead = false;
        invulnT = 0;
        magnetT = 0;
        jetT = 0;
        sjumpT = 0;
        multT = 0;
        boostT = 0;
        shieldOn = false;
        shieldT = 0;
        boardActive = false;
        boardT = 0;
        boardCooldown = 0;
        charId = charIdx;
        trailId = trailIdx;
        for (int i = 0; i < 6; i++) {
            powerLv[i] = 1;
        }
        if (levelUps != null) {
            for (int i = 0; i < 6 && i < levelUps.length; i++) {
                powerLv[i] = 1 + levelUps[i];
            }
        }
        boardDuration = boardDur;
    }

    public float bodyHeight() {
        return sliding ? Cfg.SLIDE_H : Cfg.PLAYER_H;
    }

    public float centerY() {
        return y + bodyHeight() * 0.55f;
    }

    public boolean grounded() {
        return y <= 0.001f && !airborne;
    }

    public boolean doJump() {
        if (airborne || jetT > 0) {
            return false;
        }
        float v = Cfg.JUMP_V;
        if (sjumpT > 0) {
            v *= 1.35f;
        }
        vy = v;
        airborne = true;
        sliding = false;
        slideT = 0;
        return true;
    }

    public boolean doSlide() {
        if (jetT > 0) {
            return false;
        }
        if (airborne) {
            vy = Math.min(vy, -12f);
            return false;
        }
        if (sliding) {
            slideT = Cfg.SLIDE_TIME;
            return false;
        }
        sliding = true;
        slideT = Cfg.SLIDE_TIME;
        return true;
    }

    public boolean doLane(int delta) {
        int target = lane + delta;
        if (target < 0 || target > 2) {
            return false;
        }
        lane = target;
        return true;
    }

    public void applyPower(int kind, World world) {
        int lv = powerLv[kind];
        float dur = 8f + (lv - 1) * 3f;
        switch (kind) {
            case 0:
                magnetT = Math.max(magnetT, dur);
                break;
            case 1:
                jetT = Math.max(jetT, 5f + (lv - 1) * 2f);
                sliding = false;
                break;
            case 2:
                sjumpT = Math.max(sjumpT, dur);
                break;
            case 3:
                multT = Math.max(multT, dur);
                break;
            case 4:
                shieldOn = true;
                shieldT = Math.max(shieldT, dur);
                break;
            case 5:
                boostT = Math.max(boostT, 6f + (lv - 1) * 2f);
                break;
            default:
                break;
        }
    }

    public boolean activateBoard() {
        if (boardActive || boardCooldown > 0 || dead) {
            return false;
        }
        boardActive = true;
        boardT = boardDuration;
        return true;
    }

    /** Board breaks on a crash: survives the hit, brief invulnerability. */
    public boolean breakBoard() {
        if (!boardActive) {
            return false;
        }
        boardActive = false;
        boardT = 0;
        boardCooldown = 8f;
        invulnT = 1.6f;
        return true;
    }

    public void revive() {
        dead = false;
        y = 0;
        vy = 0;
        sliding = false;
        slideT = 0;
        airborne = false;
        invulnT = 2.5f;
        lane = 1;
    }

    public void consumeShield() {
        shieldOn = false;
        shieldT = 0;
        invulnT = 1.4f;
    }

    public void update(float dt, float speed, World world) {
        float targetX = Cfg.laneX(lane);
        float k = Math.min(1f, dt * 13f);
        tilt += ((targetX - x) * 0.25f - tilt) * Math.min(1f, dt * 10f);
        x += (targetX - x) * k;

        if (jetT > 0) {
            jetT -= dt;
            float targetY = 3.1f;
            y += (targetY - y) * Math.min(1f, dt * 4f);
            vy = 0;
            airborne = false;
            sliding = false;
        } else {
            float g = Cfg.GRAVITY * (sjumpT > 0 ? 0.8f : 1f);
            vy -= g * dt;
            y += vy * dt;
            if (y <= 0) {
                y = 0;
                vy = 0;
                airborne = false;
            }
        }
        if (sjumpT > 0) {
            sjumpT -= dt;
        }
        if (magnetT > 0) {
            magnetT -= dt;
        }
        if (multT > 0) {
            multT -= dt;
        }
        if (boostT > 0) {
            boostT -= dt;
        }
        if (shieldOn) {
            shieldT -= dt;
            if (shieldT <= 0) {
                shieldOn = false;
            }
        }
        if (invulnT > 0) {
            invulnT -= dt;
        }
        if (sliding) {
            slideT -= dt;
            if (slideT <= 0) {
                sliding = false;
            }
        }
        if (boardActive) {
            boardT -= dt;
            if (boardT <= 0) {
                boardActive = false;
                boardCooldown = 8f;
            }
        }
        if (boardCooldown > 0) {
            boardCooldown -= dt;
        }
        runPhase += speed * dt * 1.7f;
    }
}
