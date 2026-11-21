package online.arnitcreation.streetrush.meta;

/** 7-day daily reward cycle with streak reset logic. */
public final class Daily {
    private Daily() {}

    public static final int[] COIN_REWARDS = {100, 200, -1, 300, 500, -2, 1000};

    public static String rewardText(int day) {
        int r = COIN_REWARDS[(day - 1) % 7];
        if (r >= 0) {
            return r + " COINS";
        }
        if (r == -1) {
            return "POWER-UP LEVEL";
        }
        return "MEGA COIN HAUL";
    }

    public static long today() {
        return System.currentTimeMillis() / 86400000L;
    }

    /** True when the player can claim today's reward right now. */
    public static boolean claimable(Save s) {
        return s.dailyLastDay != today();
    }

    /**
     * Claims today's reward. Returns a description of what was granted,
     * or null when already claimed today.
     */
    public static String claim(Save s, java.util.Random rng) {
        long t = today();
        if (s.dailyLastDay == t) {
            return null;
        }
        int day;
        if (s.dailyLastDay == t - 1) {
            day = s.dailyStreak;
            if (day < 1 || day > 6) {
                day = 1;
            } else {
                day = day + 1;
                if (day > 7) {
                    day = 7;
                }
            }
        } else {
            day = 1;
        }
        s.dailyStreak = day;
        s.dailyLastDay = t;
        int r = COIN_REWARDS[(day - 1) % 7];
        String what;
        if (r >= 0) {
            s.earn(r);
            what = "+" + r + " COINS";
        } else {
            int lowest = 0;
            for (int i = 1; i < 6; i++) {
                if (s.powerLv[i] < s.powerLv[lowest]) {
                    lowest = i;
                }
            }
            s.powerLv[lowest] = Math.min(3, s.powerLv[lowest] + 1);
            what = "UPGRADED " + "A POWER-UP";
        }
        return "DAY " + day + ": " + what;
    }
}
