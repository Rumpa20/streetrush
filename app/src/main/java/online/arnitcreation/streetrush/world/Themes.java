package online.arnitcreation.streetrush.world;

/** Theme definitions and a smoothly blended current-theme state. */
public final class Themes {
    private Themes() {}

    public static final int NEON = 0, INDIAN = 1, SUNSET = 2, METRO = 3, JUNGLE = 4;
    public static final int COUNT = 5;

    public static final String[] NAMES = {
            "NEON CITY", "INDIAN STREET", "SUNSET HIGHWAY", "FUTURISTIC METRO", "JUNGLE RUN"
    };

    // { sky, fog, road, shoulder, mark, buildingA, buildingB, buildingC, glowWindow }
    public static final int I_SKY = 0, I_FOG = 1, I_ROAD = 2, I_SHOULDER = 3, I_MARK = 4,
            I_BLD_A = 5, I_BLD_B = 6, I_BLD_C = 7, I_GLOW = 8;

    private static final int[][] DEF = {
            // NEON CITY
            {0xFF0E1023, 0xFF0E1023, 0xFF1D2246, 0xFF272B58, 0xFF22E4C8,
                    0xFF35256B, 0xFF4C3FA0, 0xFF6C5CE7, 0xFF22E4C8},
            // INDIAN STREET
            {0xFF7EC8E3, 0xFFB8DCE8, 0xFF8A8F98, 0xFFB9976B, 0xFFFFE9B8,
                    0xFFF2A65A, 0xFFE76F51, 0xFF2A9D8F, 0xFFFBE7A1},
            // SUNSET HIGHWAY
            {0xFFE0653C, 0xFFF0906A, 0xFF3A3D4D, 0xFF6B6E80, 0xFFFFD97D,
                    0xFF7A4F6D, 0xFF9C6B70, 0xFFC98C60, 0xFFFFD97D},
            // FUTURISTIC METRO
            {0xFF101426, 0xFF101426, 0xFF232A3E, 0xFF2E3752, 0xFF3DDCFF,
                    0xFF1E2A4A, 0xFF273B66, 0xFF3A5A8C, 0xFF3DDCFF},
            // JUNGLE RUN
            {0xFF59B36B, 0xFF86C98F, 0xFF8A6B4A, 0xFF6E8B3D, 0xFFC8B08A,
                    0xFF588157, 0xFF6E8B3D, 0xFF815536, 0xFFB5E48C},
    };

    private static final float[] AC = new float[4];
    private static final float[] BC = new float[4];

    public static int get(int theme, int idx) {
        return DEF[theme][idx];
    }

    /** Current blended theme colors (indices as in DEF). */
    public static final int CUR_SLOTS = 9;
    public static final int[] cur = new int[CUR_SLOTS];

    public static void reset(int theme) {
        for (int j = 0; j < CUR_SLOTS; j++) {
            cur[j] = DEF[theme][j];
        }
    }

    public static void update(int target, float k) {
        for (int j = 0; j < CUR_SLOTS; j++) {
            int a = cur[j];
            int b = DEF[target][j];
            if (a == b) {
                continue;
            }
            Cfg.col4(a, AC);
            Cfg.col4(b, BC);
            int r = (int) (AC[0] + (BC[0] - AC[0]) * k) & 0xff;
            int g = (int) (AC[1] + (BC[1] - AC[1]) * k) & 0xff;
            int bl = (int) (AC[2] + (BC[2] - AC[2]) * k) & 0xff;
            cur[j] = 0xFF000000 | (r << 16) | (g << 8) | bl;
        }
    }
}
