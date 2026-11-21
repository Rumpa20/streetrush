package online.arnitcreation.streetrush.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Fully procedural audio: every sound effect and the background music loop are
 * synthesized at startup. No audio files, no network, no licensing concerns.
 */
public final class Audio {
    public static final int SFX_COIN = 0;
    public static final int SFX_JUMP = 1;
    public static final int SFX_SLIDE = 2;
    public static final int SFX_CRASH = 3;
    public static final int SFX_POWER = 4;
    public static final int SFX_CLICK = 5;
    public static final int SFX_UNLOCK = 6;
    public static final int SFX_BOARD = 7;
    public static final int SFX_REVIVE = 8;
    public static final int SFX_COUNT = 9;

    private static final int SR = 22050;

    private final short[][] sfx = new short[SFX_COUNT][];
    private final AudioTrack[] pool = new AudioTrack[6];
    private int poolIdx;
    private AudioTrack music;
    private boolean musicOn = true;
    private boolean sfxOn = true;
    private float volume = 1f;

    public void setMusicOn(boolean on) {
        musicOn = on;
        if (!on) {
            pauseMusic();
        }
    }

    public void setSfxOn(boolean on) {
        sfxOn = on;
    }

    public void setVolume(float v) {
        volume = v;
        if (music != null) {
            music.setVolume(volume * 0.5f);
        }
    }

    public void startMusic() {
        if (!musicOn) {
            return;
        }
        if (music == null) {
            music = buildMusic();
        }
        try {
            music.play();
        } catch (IllegalStateException ignored) {
        }
    }

    public void pauseMusic() {
        if (music != null) {
            try {
                music.pause();
                music.flush();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public void release() {
        pauseMusic();
        if (music != null) {
            music.release();
            music = null;
        }
        for (int i = 0; i < pool.length; i++) {
            if (pool[i] != null) {
                pool[i].release();
                pool[i] = null;
            }
        }
    }

    public void play(int id) {
        if (!sfxOn || sfx[id] == null) {
            return;
        }
        AudioTrack t = pool[poolIdx];
        poolIdx = (poolIdx + 1) % pool.length;
        try {
            if (t == null) {
                t = new AudioTrack(AudioManager.STREAM_MUSIC, SR, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, sfx[id].length * 2, AudioTrack.MODE_STATIC);
                pool[poolIdx] = t;
            }
            t.stop();
            t.reloadStaticData();
            if (sfx[id] != null) {
                short[] data = sfx[id];
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(data.length * 2)
                        .order(java.nio.ByteOrder.nativeOrder());
                bb.asShortBuffer().put(data);
                bb.position(0);
                t.write(bb, data.length * 2, AudioTrack.WRITE_BLOCKING);
            }
            t.setVolume(volume);
            t.play();
        } catch (Exception ignored) {
        }
    }

    /** Pre-synthesize all effects once (a few ms in total). */
    public void bake() {
        sfx[SFX_COIN] = coin();
        sfx[SFX_JUMP] = jump();
        sfx[SFX_SLIDE] = slide();
        sfx[SFX_CRASH] = crash();
        sfx[SFX_POWER] = power();
        sfx[SFX_CLICK] = click();
        sfx[SFX_UNLOCK] = unlock();
        sfx[SFX_BOARD] = board();
        sfx[SFX_REVIVE] = power();
    }

    private static short[] alloc(float secs) {
        return new short[(int) (SR * secs)];
    }

    private static short[] coin() {
        int n = (int) (SR * 0.14f);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) SR;
            float k = i / (float) n;
            float f = k < 0.5f ? 1568f : 2093f;
            float env = Math.min(1f, (1f - k) * 3f) * 0.5f;
            out[i] = (short) (Math.sin(2 * Math.PI * f * t) * env * 12000);
        }
        return out;
    }

    private static short[] jump() {
        int n = (int) (SR * 0.22f);
        short[] out = new short[n];
        float phase = 0;
        for (int i = 0; i < n; i++) {
            float k = i / (float) n;
            float f = 300f + k * 500f;
            phase += 2 * Math.PI * f / SR;
            float env = (1f - k) * 0.6f;
            out[i] = (short) (Math.sin(phase) * env * 11000);
        }
        return out;
    }

    private static short[] slide() {
        int n = (int) (SR * 0.3f);
        short[] out = new short[n];
        long seed = 7;
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float k = i / (float) n;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float noise = (short) (seed >> 40) / 32768f;
            float cut = 0.5f * (1f - k) + 0.08f;
            lp += cut * (noise - lp);
            float env = Math.min(1f, k * 6f) * (1f - k) * 1.4f;
            out[i] = (short) (lp * env * 9000);
        }
        return out;
    }

    private static short[] crash() {
        int n = (int) (SR * 0.5f);
        short[] out = new short[n];
        long seed = 13;
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float k = i / (float) n;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float noise = (short) (seed >> 39) / 32768f;
            lp += 0.25f * (noise - lp);
            float thud = Math.sin(2 * Math.PI * 90f * (i / (float) SR)) * (1f - k) * 0.25f;
            float env = (float) Math.exp(-k * 6f);
            out[i] = (short) ((lp * 0.8f + thud) * env * 12000);
        }
        return out;
    }

    private static short[] power() {
        int n = (int) (SR * 0.4f);
        short[] out = new short[n];
        float[] notes = {523.25f, 659.25f, 783.99f, 1046.5f};
        int perNote = n / notes.length;
        for (int i = 0; i < n; i++) {
            int note = Math.min(notes.length - 1, i / perNote);
            float t = i / (float) SR;
            float localK = (i % perNote) / (float) perNote;
            float env = (1f - localK) * 0.55f;
            float v = (float) (Math.sin(2 * Math.PI * notes[note] * t)
                    + 0.4 * Math.sin(2 * Math.PI * notes[note] * 2 * t));
            out[i] = (short) (v * env * 9000);
        }
        return out;
    }

    private static short[] click() {
        int n = (int) (SR * 0.05f);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float k = i / (float) n;
            out[i] = (short) (Math.sin(2 * Math.PI * 900f * (i / (float) SR)) * (1f - k) * 5000);
        }
        return out;
    }

    private static short[] unlock() {
        int n = (int) (SR * 0.55f);
        short[] out = new short[n];
        float[] notes = {440f, 554.37f, 659.25f, 880f, 1108.73f};
        int perNote = n / notes.length;
        for (int i = 0; i < n; i++) {
            int note = Math.min(notes.length - 1, i / perNote);
            float t = i / (float) SR;
            float k = (i % perNote) / (float) perNote;
            float env = Math.min(1f, (1f - k) * 2f) * 0.5f;
            out[i] = (short) (Math.sin(2 * Math.PI * notes[note] * t) * env * 9500);
        }
        return out;
    }

    private static short[] board() {
        int n = (int) (SR * 0.35f);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float k = i / (float) n;
            float t = i / (float) SR;
            float hum = Math.sin(2 * Math.PI * 160f * t) * 0.6f
                    + Math.sin(2 * Math.PI * 240f * t) * 0.4f;
            float sweep = Math.sin(2 * Math.PI * (600f + 400f * k) * t) * 0.3f;
            float env = Math.min(1f, k * 8f) * (1f - k) * 0.9f;
            out[i] = (short) ((hum + sweep) * env * 9000);
        }
        return out;
    }

    /** Chiptune loop: A-minor progression (Am F C G), square bass, triangle lead, noise hats. */
    private static AudioTrack buildMusic() {
        int beat = (int) (SR * 60f / 126f);
        int total = beat * 16;
        short[] out = new short[total];
        float[] roots = {55f, 43.65f, 65.41f, 49f};
        float[] melody = {440f, 523.25f, 587.33f, 0, 659.25f, 587.33f, 523.25f, 0,
                523.25f, 659.25f, 783.99f, 659.25f, 587.33f, 523.25f, 440f, 0};
        for (int b = 0; b < 16; b++) {
            int chordIdx = b / 4;
            float root = roots[chordIdx];
            float rootFreq = root * 2f;
            // bass: 8ths per beat
            for (int h = 0; h < 8; h++) {
                int start = b * beat + h * (beat / 8);
                int len = Math.min(beat / 8, total - start);
                float f = (h % 2 == 0) ? rootFreq : rootFreq * 1.5f;
                for (int i = 0; i < len; i++) {
                    float t = i / (float) SR;
                    float k = i / (float) len;
                    float sq = Math.signum((float) Math.sin(2 * Math.PI * f * t)) * 0.5f
                            + Math.sin(2 * Math.PI * f * t) * 0.5f;
                    float env = (1f - k) * 0.25f;
                    addAt(out, start + i, (short) (sq * env * 12000));
                }
            }
            // melody: full beat with decay
            float m = melody[b];
            if (m > 0) {
                for (int i = 0; i < beat && b * beat + i < total; i++) {
                    float t = i / (float) SR;
                    float k = i / (float) beat;
                    float tri = (2f / (float) Math.PI) * (float) Math.asin(Math.sin(2 * Math.PI * m * t));
                    float env = (1f - k) * 0.5f;
                    addAt(out, b * beat + i, (short) (tri * env * 12000));
                }
            }
            // hats on half beats
            for (int h = 0; h < 2; h++) {
                int start = b * beat + h * (beat / 2);
                for (int i = 0; i < beat / 8 && start + i < total; i++) {
                    float k = i / (float) (beat / 8);
                    long seed = (start + i) * 2654435761L;
                    float noise = (short) (seed >> 40) / 32768f * (1f - k) * 0.25f;
                    addAt(out, start + i, (short) (noise * 8000));
                }
            }
        }
        AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, SR,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                out.length * 2, AudioTrack.MODE_STATIC);
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(out.length * 2)
                .order(java.nio.ByteOrder.nativeOrder());
        bb.asShortBuffer().put(out);
        t.write(bb, out.length * 2, AudioTrack.WRITE_BLOCKING);
        t.setLoopPoints(0, out.length, -1);
        t.setVolume(0.5f);
        return t;
    }

    private static void addAt(short[] buf, int idx, short v) {
        if (idx >= 0 && idx < buf.length) {
            int s = buf[idx] + v;
            if (s > 32767) {
                s = 32767;
            }
            if (s < -32768) {
                s = -32768;
            }
            buf[idx] = (short) s;
        }
    }
}
