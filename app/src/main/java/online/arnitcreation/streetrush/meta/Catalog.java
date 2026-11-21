package online.arnitcreation.streetrush.meta;

/** Catalog of purchasable/unlockable content: characters, hoverboards, cosmetic trails. */
public final class Catalog {
    private Catalog() {}

    public static final int CHARS = 6;
    public static final int BOARDS = 4;
    public static final int TRAILS = 4;

    public static final class Char {
        public final String name;
        public final String desc;
        public final int cost;         // 0 = free starter
        public final int reqType;      // 0 none, 1 missions completed, 2 best score
        public final int reqVal;
        public final int skin, shirt, pants, hatColor, accent;
        public final int hat;          // 0 none, 1 cap, 2 hood, 3 visor, 4 bandana, 5 crown

        public Char(String name, String desc, int cost, int reqType, int reqVal,
                    int skin, int shirt, int pants, int hatColor, int accent, int hat) {
            this.name = name;
            this.desc = desc;
            this.cost = cost;
            this.reqType = reqType;
            this.reqVal = reqVal;
            this.skin = skin;
            this.shirt = shirt;
            this.pants = pants;
            this.hatColor = hatColor;
            this.accent = accent;
            this.hat = hat;
        }

        public boolean requirementMet(Save s) {
            if (reqType == 1) {
                return s.missionsCompleted() >= reqVal;
            }
            if (reqType == 2) {
                return s.bestScore >= reqVal;
            }
            return true;
        }

        public String reqText() {
            if (reqType == 1) {
                return "FINISH " + reqVal + " MISSIONS";
            }
            if (reqType == 2) {
                return "BEST SCORE " + reqVal;
            }
            return "";
        }
    }

    public static final class Board {
        public final String name;
        public final int cost;
        public final int deckColor, glowColor;
        public final int duration;   // seconds per activation

        public Board(String name, int cost, int deckColor, int glowColor, int duration) {
            this.name = name;
            this.cost = cost;
            this.deckColor = deckColor;
            this.glowColor = glowColor;
            this.duration = duration;
        }
    }

    public static final class Trail {
        public final String name;
        public final int cost;
        public final int dustColor;   // argb; animated rainbow uses alpha < ff as flag

        public Trail(String name, int cost, int dustColor) {
            this.name = name;
            this.cost = cost;
            this.dustColor = dustColor;
        }
    }

    public static final Char[] CHARS_DATA = {
            new Char("REX", " STREET RUNNER", 0, 0, 0,
                    0xFFD9A066, 0xFF22B7A5, 0xFF2B3A67, 0xFFE6432B, 0xFFFFC93C, 1),
            new Char("KAGE", " NEON NINJA", 1500, 0, 0,
                    0xFFC8A888, 0xFF1F2430, 0xFF14161F, 0xFF22E4C8, 0xFF22E4C8, 4),
            new Char("MAYA", " URBAN EXPLORER", 2500, 0, 0,
                    0xFF8C5A2B, 0xFFF2A65A, 0xFF4A4A58, 0xFF2A9D8F, 0xFFF4A261, 2),
            new Char("VOLT", " CYBER RACER", 4000, 0, 0,
                    0xFFE8DCC8, 0xFF3DDCFF, 0xFF273B66, 0xFF3DDCFF, 0xFF3DDCFF, 3),
            new Char("KIRAN", " JUNGLE ADVENTURER", 6000, 0, 0,
                    0xFFC68642, 0xFF588157, 0xFF6B4B2A, 0xFF815536, 0xFFB5E48C, 0),
            new Char("BLAZE", " STREET LEGEND", 9000, 1, 10,
                    0xFFB97A57, 0xFFFFC93C, 0xFFB3261E, 0xFFFFC93C, 0xFFFFC93C, 5),
    };

    public static final Board[] BOARDS_DATA = {
            new Board("STREET GLIDER", 0, 0xFF26D6C2, 0xFF7CFFE4, 25),
            new Board("NEON WAVE", 2000, 0xFFFF3D8A, 0xFFFF9EC6, 30),
            new Board("SOLAR FLARE", 4000, 0xFFFFC93C, 0xFFFFE082, 35),
            new Board("VOID RUNNER", 8000, 0xFF6C5CE7, 0xFFB399FF, 40),
    };

    public static final Trail[] TRAILS_DATA = {
            new Trail("SAND GRIT", 0, 0xFFB9AF9E),
            new Trail("SODA POP", 600, 0xFF63C56B),
            new Trail("SUNBEAM", 900, 0xFFFFC93C),
            new Trail("STARDUST", 1600, 0xFFB399FF),
    };

    public static boolean isRainbow(Trail trail) {
        return false;
    }
}
