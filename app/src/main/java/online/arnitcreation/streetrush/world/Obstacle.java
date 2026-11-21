package online.arnitcreation.streetrush.world;

/** Obstacle data + vertical extents for collision. */
public final class Obstacle {
    public static final int K_LOW = 0;      // jump over (barrier / cone / barricade)
    public static final int K_HIGH = 1;     // slide under (overhead bar / tunnel gate)
    public static final int K_WALL = 2;     // change lane (crate stack)
    public static final int K_VEHICLE = 3;  // change lane (parked car; some moving)
    public static final int K_TRAIN = 4;    // change lane (long train car)
    public static final int K_GAP = 5;      // jump over (broken ground)

    public int kind;
    public int lane;
    public int variant;
    public float z;      // local distance from chunk near edge
    public float length;
    public float vz;     // own forward speed (moving vehicles)
    public boolean moving;
    public boolean counted;

    public void set(int kind, int lane, int variant, float z, float length) {
        this.kind = kind;
        this.lane = lane;
        this.variant = variant;
        this.z = z;
        this.length = length;
        this.moving = false;
        this.vz = 0;
        this.counted = false;
    }

    /** Collision vertical range lower bound. */
    public float yLow() {
        switch (kind) {
            case K_HIGH: return 1.02f;
            default: return 0f;
        }
    }

    /** Collision vertical range upper bound; also used by the renderer. */
    public float yHigh() {
        switch (kind) {
            case K_LOW: return 0.7f;
            case K_HIGH: return 2.45f;
            case K_WALL: return 2.6f;
            case K_VEHICLE: return 1.65f;
            case K_TRAIN: return 3.1f;
            default: return 0f;
        }
    }

    /** Horizontal half-width of the collision box. */
    public float halfW() {
        return (kind == K_VEHICLE || kind == K_TRAIN) ? 1.0f : 0.85f;
    }

    public boolean isHardBlock() {
        return kind == K_WALL || kind == K_VEHICLE || kind == K_TRAIN;
    }
}
