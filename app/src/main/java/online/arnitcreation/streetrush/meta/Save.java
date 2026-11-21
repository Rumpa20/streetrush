package online.arnitcreation.streetrush.meta;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** All persistent game data, stored locally as JSON in SharedPreferences. */
public final class Save {
    private static final String PREF = "streetrush_save";
    private static final String KEY = "state";

    public int totalCoins;
    public int bestScore;
    public int bestRunDist;
    public int totalDist;
    public int totalRuns;
    public int lifetimeCoins;
    public int jumps;
    public int slides;
    public int passed;
    public int powerTaken;

    public final int[] missions = new int[Missions.COUNT];
    public final boolean[] missionDone = new boolean[Missions.COUNT];
    public final boolean[] achievements = new boolean[Achievements.COUNT];

    public int selChar;
    public int selBoard;
    public int selTrail;
    public JSONArray ownedChars;
    public JSONArray ownedBoards;
    public JSONArray ownedTrails;

    public final int[] powerLv = new int[6];

    public long dailyLastDay;
    public int dailyStreak;

    public boolean musicOn = true;
    public boolean sfxOn = true;
    public boolean vibOn = true;
    public int quality;      // 0 auto, 1 high, 2 low
    public int fpsMode;      // 0 60fps, 1 30fps
    public int sens = 1;     // 0 low, 1 med, 2 high
    public int volume = 80;  // 0..100
    public boolean tutorialShown;

    public Save() {
        for (int i = 0; i < 6; i++) {
            powerLv[i] = 1;
        }
        ownedChars = new JSONArray();
        ownedBoards = new JSONArray();
        ownedTrails = new JSONArray();
    }

    public int missionsCompleted() {
        int n = 0;
        for (int i = 0; i < missionDone.length; i++) {
            if (missionDone[i]) {
                n++;
            }
        }
        return n;
    }

    public boolean owned(JSONArray arr, int idx) {
        for (int i = 0; i < arr.length(); i++) {
            if (arr.optInt(i, -1) == idx) {
                return true;
            }
        }
        return false;
    }

    public void own(JSONArray arr, int idx) {
        if (!owned(arr, idx)) {
            arr.put(idx);
        }
    }

    public boolean spend(int amount) {
        if (totalCoins < amount) {
            return false;
        }
        totalCoins -= amount;
        return true;
    }

    public void earn(int amount) {
        totalCoins += amount;
        lifetimeCoins += amount;
    }

    private SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void load(Context ctx) {
        String raw = prefs(ctx).getString(KEY, null);
        if (raw == null) {
            for (int i = 0; i < 6; i++) {
                powerLv[i] = 1;
            }
            own(ownedChars, 0);
            own(ownedBoards, 0);
            own(ownedTrails, 0);
            return;
        }
        try {
            JSONObject o = new JSONObject(raw);
            totalCoins = o.optInt("coins", 0);
            bestScore = o.optInt("best", 0);
            bestRunDist = o.optInt("brd", 0);
            totalDist = o.optInt("tdist", 0);
            totalRuns = o.optInt("runs", 0);
            lifetimeCoins = o.optInt("lcoins", 0);
            jumps = o.optInt("jumps", 0);
            slides = o.optInt("slides", 0);
            passed = o.optInt("passed", 0);
            powerTaken = o.optInt("power", 0);
            JSONArray m = o.optJSONArray("missions");
            for (int i = 0; i < Missions.COUNT; i++) {
                missions[i] = m != null ? m.optInt(i, 0) : 0;
            }
            JSONArray md = o.optJSONArray("missionDone");
            for (int i = 0; i < Missions.COUNT; i++) {
                missionDone[i] = md != null && md.optBoolean(i, false);
            }
            JSONArray ac = o.optJSONArray("ach");
            for (int i = 0; i < Achievements.COUNT; i++) {
                achievements[i] = ac != null && ac.optBoolean(i, false);
            }
            selChar = o.optInt("selChar", 0);
            selBoard = o.optInt("selBoard", 0);
            selTrail = o.optInt("selTrail", 0);
            JSONArray oc = o.optJSONArray("ownedChars");
            if (oc != null) {
                ownedChars = oc;
            } else {
                ownedChars = new JSONArray();
            }
            JSONArray ob = o.optJSONArray("ownedBoards");
            if (ob != null) {
                ownedBoards = ob;
            } else {
                ownedBoards = new JSONArray();
            }
            JSONArray ot = o.optJSONArray("ownedTrails");
            if (ot != null) {
                ownedTrails = ot;
            } else {
                ownedTrails = new JSONArray();
            }
            own(ownedChars, 0);
            own(ownedBoards, 0);
            own(ownedTrails, 0);
            JSONArray pl = o.optJSONArray("powerLv");
            for (int i = 0; i < 6; i++) {
                powerLv[i] = pl != null ? pl.optInt(i, 1) : 1;
                if (powerLv[i] < 1) {
                    powerLv[i] = 1;
                }
                if (powerLv[i] > 3) {
                    powerLv[i] = 3;
                }
            }
            dailyLastDay = o.optLong("dailyDay", 0);
            dailyStreak = o.optInt("dailyStreak", 0);
            musicOn = o.optBoolean("music", true);
            sfxOn = o.optBoolean("sfx", true);
            vibOn = o.optBoolean("vib", true);
            quality = o.optInt("quality", 0);
            fpsMode = o.optInt("fps", 0);
            sens = o.optInt("sens", 1);
            volume = o.optInt("vol", 80);
            tutorialShown = o.optBoolean("tut", false);
        } catch (Exception ignored) {
            own(ownedChars, 0);
            own(ownedBoards, 0);
            own(ownedTrails, 0);
        }
        if (!owned(ownedChars, selChar)) {
            selChar = 0;
        }
        if (!owned(ownedBoards, selBoard)) {
            selBoard = 0;
        }
        if (!owned(ownedTrails, selTrail)) {
            selTrail = 0;
        }
    }

    public void flush(Context ctx) {
        try {
            JSONObject o = new JSONObject();
            o.put("coins", totalCoins);
            o.put("best", bestScore);
            o.put("brd", bestRunDist);
            o.put("tdist", totalDist);
            o.put("runs", totalRuns);
            o.put("lcoins", lifetimeCoins);
            o.put("jumps", jumps);
            o.put("slides", slides);
            o.put("passed", passed);
            o.put("power", powerTaken);
            o.put("missions", new JSONArray(missions));
            o.put("missionDone", new JSONArray(missionDone));
            o.put("ach", new JSONArray(achievements));
            o.put("selChar", selChar);
            o.put("selBoard", selBoard);
            o.put("selTrail", selTrail);
            o.put("ownedChars", ownedChars);
            o.put("ownedBoards", ownedBoards);
            o.put("ownedTrails", ownedTrails);
            o.put("powerLv", new JSONArray(powerLv));
            o.put("dailyDay", dailyLastDay);
            o.put("dailyStreak", dailyStreak);
            o.put("music", musicOn);
            o.put("sfx", sfxOn);
            o.put("vib", vibOn);
            o.put("quality", quality);
            o.put("fps", fpsMode);
            o.put("sens", sens);
            o.put("vol", volume);
            o.put("tut", tutorialShown);
            prefs(ctx).edit().putString(KEY, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void wipe(Context ctx) {
        prefs(ctx).edit().remove(KEY).apply();
        Save fresh = new Save();
        fresh.own(fresh.ownedChars, 0);
        fresh.own(fresh.ownedBoards, 0);
        fresh.own(fresh.ownedTrails, 0);
        apply(fresh, ctx);
    }

    private void apply(Save s, Context ctx) {
        totalCoins = s.totalCoins;
        bestScore = 0;
        bestRunDist = 0;
        totalDist = 0;
        totalRuns = 0;
        lifetimeCoins = 0;
        jumps = 0;
        slides = 0;
        passed = 0;
        powerTaken = 0;
        for (int i = 0; i < Missions.COUNT; i++) {
            missions[i] = 0;
            missionDone[i] = false;
        }
        for (int i = 0; i < Achievements.COUNT; i++) {
            achievements[i] = false;
        }
        ownedChars = new JSONArray();
        ownedBoards = new JSONArray();
        ownedTrails = new JSONArray();
        own(ownedChars, 0);
        own(ownedBoards, 0);
        own(ownedTrails, 0);
        selChar = 0;
        selBoard = 0;
        selTrail = 0;
        for (int i = 0; i < 6; i++) {
            powerLv[i] = 1;
        }
        dailyLastDay = 0;
        dailyStreak = 0;
        musicOn = true;
        sfxOn = true;
        vibOn = true;
        quality = 0;
        fpsMode = 0;
        sens = 1;
        volume = 80;
        tutorialShown = false;
        flush(ctx);
    }
}
