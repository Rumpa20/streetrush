package online.arnitcreation.streetrush.meta;

/** Achievement badges, checked after each run. */
public final class Achievements {
    private Achievements() {}

    public static final int COUNT = 8;

    public static final String[] TITLE = {
            "FIRST STEPS",
            "STREET REGULAR",
            "SCORE SENSATION",
            "COIN MAGNATE",
            "CREW BUILDER",
            "MISSION MACHINE",
            "CITY HOPPER",
            "ENDLESS DEVOTEE",
    };
    public static final String[] DESC = {
            "PLAY YOUR FIRST RUN",
            "PLAY 10 RUNS",
            "REACH A 20,000 BEST SCORE",
            "EARN 10,000 COINS TOTAL",
            "OWN 3 CHARACTERS",
            "COMPLETE 5 MISSIONS",
            "SEE ALL 5 THEMES IN ONE RUN? NO - VISIT THE METRO THEME",
            "PLAY 50 RUNS",
    };

    /** Marks achievements; returns indexes newly unlocked. */
    public static ArrayList<Integer> check(Save s) {
        ArrayList<Integer> got = new ArrayList<Integer>();
        boolean[] a = s.achievements;
        boolean[] now = new boolean[COUNT];
        now[0] = s.totalRuns >= 1;
        now[1] = s.totalRuns >= 10;
        now[2] = s.bestScore >= 20000;
        now[3] = s.lifetimeCoins >= 10000;
        now[4] = s.ownedChars.length() >= 3;
        now[5] = s.missionsCompleted() >= 5;
        now[6] = s.totalDist >= 3250;
        now[7] = s.totalRuns >= 50;
        for (int i = 0; i < COUNT; i++) {
            if (now[i] && !a[i]) {
                a[i] = true;
                got.add(i);
            }
        }
        return got;
    }
}
