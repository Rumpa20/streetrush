package online.arnitcreation.streetrush.meta;

import java.util.ArrayList;
import online.arnitcreation.streetrush.world.World;

/** Mission definitions and progress updating. Progress persists in Save. */
public final class Missions {
    private Missions() {}

    public static final int COUNT = 12;

    public static final int M_COINS = 0;      // lifetime coins collected
    public static final int M_DIST = 1;       // lifetime meters
    public static final int M_JUMPS = 2;      // lifetime jumps
    public static final int M_SLIDES = 3;     // lifetime slides
    public static final int M_PASSED = 4;     // lifetime obstacles dodged
    public static final int M_POWER = 5;      // lifetime power-ups
    public static final int M_RUN_DIST = 6;   // single run distance
    public static final int M_RUN_SCORE = 7;  // single run score

    public static final int[] METRIC = {
            M_COINS, M_COINS, M_DIST, M_DIST, M_JUMPS, M_SLIDES,
            M_PASSED, M_POWER, M_RUN_DIST, M_DIST, M_RUN_SCORE, M_COINS
    };
    public static final int[] TARGET = {100, 500, 2000, 6000, 25, 20, 50, 8, 500, 12000, 15000, 2000};
    public static final int[] REWARD = {100, 250, 200, 500, 150, 150, 200, 250, 300, 800, 1000, 700};
    public static final String[] TITLE = {
            "ROOKIE COLLECTOR",
            "COIN HUNTER",
            "STREET WARMUP",
            "MARATHON LEGEND",
            "AIR TIME",
            "LOW RIDER",
            "NEAR MISS KING",
            "POWER JUNKIE",
            "500 CLUB",
            "STREET PRO",
            "HIGH SCORER",
            "GOLD RUSH",
    };
    public static final String[] DESC = {
            "COLLECT 100 COINS",
            "COLLECT 500 COINS",
            "RUN 2,000 METERS",
            "RUN 6,000 METERS",
            "JUMP 25 TIMES",
            "SLIDE 20 TIMES",
            "DODGE 50 OBSTACLES",
            "GRAB 8 POWER-UPS",
            "RUN 500M IN ONE RUN",
            "RUN 12,000 METERS",
            "SCORE 15,000 IN ONE RUN",
            "COLLECT 2,000 COINS",
    };

    /** Applies a finished run to mission progress; returns titles completed this run. */
    public static ArrayList<String> afterRun(Save s, World w, int score) {
        ArrayList<String> done = new ArrayList<String>();
        int coins = w.coinsRun;
        int dist = (int) w.dist;
        int[] values = {
                s.lifetimeCoins, s.totalDist, s.jumps, s.slides, s.passed, s.powerTaken,
                Math.max(s.bestRunDist, dist), Math.max(s.bestScore, score)
        };
        for (int i = 0; i < COUNT; i++) {
            if (s.missionDone[i]) {
                continue;
            }
            int metric = METRIC[i];
            int target = TARGET[i];
            int progress;
            if (metric == M_RUN_DIST || metric == M_RUN_SCORE) {
                progress = values[metric];
                s.missions[i] = Math.max(s.missions[i], progress);
            } else if (metric == M_COINS || metric == M_DIST || metric == M_JUMPS
                    || metric == M_SLIDES || metric == M_PASSED || metric == M_POWER) {
                s.missions[i] = values[metric];
                progress = s.missions[i];
            } else {
                progress = values[metric];
                s.missions[i] = progress;
            }
            if (progress >= target) {
                s.missionDone[i] = true;
                s.earn(REWARD[i]);
                done.add(TITLE[i]);
            }
        }
        return done;
    }

    public static int currentProgress(Save s, int i) {
        return Math.min(s.missions[i], TARGET[i]);
    }
}
