package online.arnitcreation.streetrush;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GLException;
import online.arnitcreation.streetrush.audio.Audio;
import online.arnitcreation.streetrush.engine.Camera3D;
import online.arnitcreation.streetrush.engine.Cfg;
import online.arnitcreation.streetrush.engine.Font;
import online.arnitcreation.streetrush.engine.MatrixStack;
import online.arnitcreation.streetrush.engine.Particles;
import online.arnitcreation.streetrush.engine.Primitives;
import online.arnitcreation.streetrush.engine.ShaderLib;
import online.arnitcreation.streetrush.engine.Textures;
import online.arnitcreation.streetrush.engine.UiBatch;
import online.arnitcreation.streetrush.game.Humanoid;
import online.arnitcreation.streetrush.game.Player;
import online.arnitcreation.streetrush.input.Touch;
import online.arnitcreation.streetrush.meta.Achievements;
import online.arnitcreation.streetrush.meta.Catalog;
import online.arnitcreation.streetrush.meta.Daily;
import online.arnitcreation.streetrush.meta.Missions;
import online.arnitcreation.streetrush.meta.Save;
import online.arnitcreation.streetrush.world.Chunk;
import online.arnitcreation.streetrush.world.ChunkDraw;
import online.arnitcreation.streetrush.world.Themes;
import online.arnitcreation.streetrush.world.World;

/**
 * The whole game: renderer, screen flow, HUD, meta screens.
 * Runs entirely on the GL thread; input is enqueued from the UI thread.
 */
public final class StreetRushGame implements GLSurfaceView.Renderer {

    public static final int S_SPLASH = 0, S_MENU = 1, S_PLAY = 2, S_PAUSE = 3, S_OVER = 4,
            S_CHARS = 5, S_SHOP = 6, S_MISSIONS = 7, S_SETTINGS = 8, S_DAILY = 9,
            S_PRIVACY = 10, S_CREDITS = 11;

    public static final int REVIVE_COST = 300;

    private static final int C_PLAY = 1, C_CHARS = 2, C_SHOP = 3, C_MISSIONS = 4,
            C_SETTINGS = 5, C_DAILY = 6, C_PRIVACY = 7, C_CREDITS = 8,
            C_RESUME = 9, C_RESTART = 10, C_HOME = 11,
            C_BUYP = 12, C_BUYYC = 13, C_BUYB = 14, C_BUYYT = 15,
            C_SHOPTAB = 16, C_MISTAB = 17,
            C_PREVC = 18, C_NEXTC = 19, C_CLAIM = 20,
            C_MUS = 21, C_SFX = 22, C_VIB = 23, C_QUAL = 24, C_FPS = 25, C_SENS = 26,
            C_VOLDN = 27, C_VOLUP = 28,
            C_LISTUP = 35, C_LISTDN = 36, C_BOARD = 37,
            C_YES = 32, C_NO = 33, C_RESET = 38;

    // Buy/equip argument encoding: kind offset + item index.
    private static final int A_BUYCH = 1000, A_SELC = 2000, A_BUYB = 3000, A_SELB = 4000,
            A_BUYT = 5000, A_SELT = 6000;

    public final Save save;
    public final Audio audio;
    public final Touch touch;

    private final Context ctx;
    private final GLSurfaceView view;
    private final Particles particles = new Particles();
    private final Camera3D camera = new Camera3D();
    private final MatrixStack stack = new MatrixStack();
    private UiBatch ui;
    private World world;
    private Player player;

    private final int[] levelUps = new int[6];

    private int screen = S_SPLASH;
    private int screenW = 1080, screenH = 1920;
    private float time;

    private float splashT;
    private float score;
    private float overT;
    private boolean revived;
    private boolean runFinalized;
    private float dustT;

    private int shopTab;
    private int misTab;
    private int listScroll;
    private int charsIdx;
    private int confirmKind = -1;
    private int confirmArg;
    private String toast = "";
    private float toastT;

    private boolean tapPending;
    private boolean swipeL, swipeR, swipeU, swipeD, dblPending, backPending, boardRequest;
    private int cmd, cmdArg;

    private boolean glReady;

    private final float[] skyC = new float[3];
    private static final float[] LIGHT = {0.45f, 0.8f, 0.4f};
    private static final float[] AMB = {0.45f, 0.45f, 0.52f};

    public StreetRushGame(Context ctx, GLSurfaceView view) {
        this.ctx = ctx;
        this.view = view;
        save = new Save();
        save.load(ctx);
        touch = new Touch(this);
        touch.setSensitivity(save.sens);
        audio = new Audio();
        audio.setMusicOn(save.musicOn);
        audio.setSfxOn(save.sfxOn);
        audio.setVolume(save.volume / 100f);
        ui = new UiBatch(screenW, screenH);
        world = new World(particles, audio);
        player = new Player();
        player.reset(save.selChar, save.selTrail, null, 30);
        listScroll = 0;
        charsIdx = save.selChar;
    }

    private final int[] queue = new int[64];
    private int qHead, qTail;

    public void enqueue(int action) {
        int n = (qTail + 1) % queue.length;
        if (n == qHead) {
            qHead = (qHead + 1) % queue.length;
        }
        queue[qTail] = action;
        qTail = n;
    }

    private int dequeue() {
        if (qHead == qTail) {
            return -1;
        }
        int a = queue[qHead];
        qHead = (qHead + 1) % queue.length;
        return a;
    }

    public void onHostPause() {
        audio.pauseMusic();
        save.flush(ctx);
        if (screen == S_PLAY) {
            setScreen(S_PAUSE);
        }
    }

    public void onHostResume() {
        audio.startMusic();
    }

    private void setScreen(int s) {
        screen = s;
        listScroll = 0;
        confirmKind = -1;
    }

    private void goHome() {
        finalizeRun();
        setScreen(S_MENU);
        world.clearNear(60);
    }

    private void startRun() {
        if (!runFinalized) {
            finalizeRun();
        }
        world.reset();
        for (int i = 0; i < 6; i++) {
            levelUps[i] = Math.max(0, save.powerLv[i] - 1);
        }
        player.reset(save.selChar, save.selTrail, levelUps,
                Catalog.BOARDS_DATA[save.selBoard].duration);
        score = 0;
        revived = false;
        runFinalized = false;
        setScreen(S_PLAY);
    }

    private void finalizeRun() {
        if (runFinalized) {
            return;
        }
        runFinalized = true;
        save.totalCoins += world.coinsRun;
        save.lifetimeCoins += world.coinsRun;
        int dist = (int) world.dist;
        save.totalDist += dist;
        save.totalRuns++;
        save.jumps += world.jumpsRun;
        save.slides += world.slidesRun;
        save.passed += world.passedRun;
        save.powerTaken += world.powerRun;
        int sc = (int) score;
        if (sc > save.bestScore) {
            save.bestScore = sc;
        }
        if (dist > save.bestRunDist) {
            save.bestRunDist = dist;
        }
        ArrayList<String> doneMissions = Missions.afterRun(save, world, sc);
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < doneMissions.size(); i++) {
            if (msg.length() > 0) {
                msg.append(" + ");
            }
            msg.append("MISSION! ").append(doneMissions.get(i));
        }
        ArrayList<Integer> gotAch = Achievements.check(save);
        if (!gotAch.isEmpty()) {
            audio.play(Audio.SFX_UNLOCK);
            if (msg.length() > 0) {
                msg.append(" + ");
            }
            msg.append("BADGE! ").append(Achievements.TITLE[gotAch.get(0)]);
        }
        if (!doneMissions.isEmpty()) {
            audio.play(Audio.SFX_UNLOCK);
        }
        save.tutorialShown = true;
        save.flush(ctx);
        if (msg.length() > 0) {
            showToast(msg.toString());
        }
    }

    private void showToast(String s) {
        toast = s;
        toastT = 4f;
    }

    private void vibrate(int ms) {
        if (!save.vibOn) {
            return;
        }
        try {
            Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(ms,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vib.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        try {
            ShaderLib.init();
        } catch (GLException e) {
            throw new RuntimeException("Shader init failed", e);
        }
        Primitives.init();
        Textures.init();
        Font.init();
        audio.bake();
        glReady = true;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int w, int h) {
        screenW = w;
        screenH = h;
        ui = new UiBatch(w, h);
        camera.perspective(62f, (float) w / (float) h, 0.1f, Cfg.DRAW_DIST + 30f);
    }

    private long lastFrame = System.nanoTime();

    @Override
    public void onDrawFrame(GL10 unused) {
        long now = System.nanoTime();
        if (save.fpsMode == 1) {
            long wait = lastFrame + 33333333L - now;
            if (wait > 0) {
                try {
                    Thread.sleep(wait / 1000000L);
                } catch (InterruptedException ignored) {
                }
            }
            now = System.nanoTime();
        }
        float dt = (now - lastFrame) / 1e9f;
        if (dt <= 0f) {
            dt = 0.0001f;
        }
        if (dt > 0.05f) {
            dt = 0.05f;
        }
        lastFrame = now;
        if (!glReady) {
            return;
        }
        time += dt;

        drainInput();
        update(dt);
        render3D(dt);

        ui.begin();
        drawUI();
        ui.flush();

        if (cmd != 0) {
            int c = cmd, a = cmdArg;
            cmd = 0;
            cmdArg = 0;
            applyCmd(c, a);
        }
    }

    private void drainInput() {
        tapPending = false;
        int a;
        while ((a = dequeue()) != -1) {
            switch (a) {
                case Cfg.SWIPE_L: swipeL = true; break;
                case Cfg.SWIPE_R: swipeR = true; break;
                case Cfg.SWIPE_U: swipeU = true; break;
                case Cfg.SWIPE_D: swipeD = true; break;
                case Cfg.TAP:
                    if (screen == S_SPLASH && splashT > 0.6f) {
                        splashT = 99f;
                    } else {
                        tapPending = true;
                    }
                    break;
                case Cfg.DBLTAP: dblPending = true; break;
                case Cfg.BACK: backPending = true; break;
                default: break;
            }
        }
        if (backPending) {
            backPending = false;
            switch (screen) {
                case S_PLAY: setScreen(S_PAUSE); break;
                case S_PAUSE: setScreen(S_PLAY); break;
                case S_OVER: goHome(); break;
                case S_MENU: askConfirm(9, 0); break;
                case S_SPLASH: break;
                default: goHome(); break;
            }
        }
    }

    private void update(float dt) {
        if (toastT > 0) {
            toastT -= dt;
        }
        particles.update(dt);

        switch (screen) {
            case S_SPLASH:
                splashT += dt;
                demoScroll(dt);
                if (splashT > 2.6f) {
                    setScreen(S_MENU);
                }
                break;
            case S_MENU: case S_CHARS: case S_SHOP: case S_MISSIONS:
            case S_SETTINGS: case S_DAILY: case S_PRIVACY: case S_CREDITS:
                demoScroll(dt);
                break;
            case S_PLAY:
                updatePlay(dt);
                break;
            case S_OVER:
                if (!revived) {
                    overT -= dt;
                }
                if (overT <= 0 && !revived) {
                    finalizeRun();
                }
                break;
            default:
                break;
        }
    }

    private void updatePlay(float dt) {
        if (swipeL) { player.doLane(-1); }
        if (swipeR) { player.doLane(1); }
        if (swipeU) {
            if (player.doJump()) {
                world.jumpsRun++;
                audio.play(Audio.SFX_JUMP);
            }
        }
        if (swipeD) {
            if (player.doSlide()) {
                world.slidesRun++;
                audio.play(Audio.SFX_SLIDE);
            }
        }
        swipeL = swipeR = swipeU = swipeD = false;

        if (dblPending || boardRequest) {
            boardRequest = false;
            if (player.activateBoard()) {
                audio.play(Audio.SFX_BOARD);
            }
        }
        dblPending = false;

        world.update(dt, player);
        world.collide(player);
        if (world.crashEvent >= 0) {
            handleCrash();
        }
        if (screen != S_PLAY) {
            return;
        }
        score += world.speed * dt * 10f * (player.multT > 0 ? 2f : 1f);
        if (world.shakeT > 0) {
            world.shakeT -= dt;
        }
        dustT -= dt;
        if (dustT <= 0 && player.grounded() && !player.sliding && player.jetT <= 0) {
            dustT = 0.08f;
            particles.spawn(player.x + (float) (Math.random() - 0.5) * 0.4f, 0.15f, 0.7f,
                    0.5f, 1.4f, Catalog.TRAILS_DATA[save.selTrail].dustColor, 0.5f, 7f);
        }
    }

    private void handleCrash() {
        world.crashEvent = -1;
        particles.burst(player.x, player.centerY(), 0.2f, 28, 0xFFFF5A3C, 7f);
        audio.play(Audio.SFX_CRASH);
        vibrate(200);
        if (player.shieldOn) {
            player.consumeShield();
            audio.play(Audio.SFX_POWER);
            showToast("SHIELD ABSORBED THE HIT");
        } else if (player.boardActive && player.breakBoard()) {
            particles.burst(player.x, 0.6f, 0.6f, 14, 0xFFB399FF, 5f);
            showToast("BOARD DESTROYED - SURVIVED");
        } else if (player.invulnT > 0) {
            audio.play(Audio.SFX_CLICK);
        } else {
            player.dead = true;
            overT = 5f;
            setScreen(S_OVER);
        }
    }

    private void demoScroll(float dt) {
        float sp = 7f;
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            Chunk c = world.chunks[i];
            c.offset += sp * dt;
            if (c.offset >= 44f) {
                float min = Float.MAX_VALUE;
                for (int j = 0; j < Cfg.CHUNKS; j++) {
                    if (world.chunks[j].offset < min) {
                        min = world.chunks[j].offset;
                    }
                }
                c.offset = min - Cfg.CHUNK_LEN;
                c.generate(world.themeIdx, 0, true, false);
            }
        }
        player.runPhase += sp * dt * 1.7f;
        player.lane = 1;
        player.x += (Cfg.laneX(1) - player.x) * Math.min(1f, dt * 8f);
        player.tilt = 0;
    }

    private void render3D(float dt) {
        Cfg.col3(Themes.cur[Themes.I_SKY], skyC);
        GLES20.glClearColor(skyC[0], skyC[1], skyC[2], 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, screenW, screenH);

        float shakeX = 0, shakeY = 0;
        float py = player.y;
        if (world.shakeT > 0) {
            float amp = world.shakeAmp * (world.shakeT / 0.5f);
            shakeX = (float) (Math.random() - 0.5) * amp * 2f;
            shakeY = (float) (Math.random() - 0.5) * amp * 2f;
        }
        if (screen == S_PLAY || screen == S_PAUSE || screen == S_OVER) {
            camera.lookAt(player.x * 0.55f + shakeX, 3.5f + py * 0.25f + shakeY, 6.2f,
                    player.x * 0.8f, 1.3f + py * 0.25f, -9f, 0, 1, 0);
        } else {
            float ox = (float) Math.sin(time * 0.35f) * 2.1f;
            camera.lookAt(ox + shakeX, 2.7f + shakeY, 5.6f, 0f, 1.25f, -4f, 0, 1, 0);
        }
        camera.refresh();

        GLES20.glUseProgram(ShaderLib.prog3D);
        GLES20.glUniformMatrix4fv(ShaderLib.uVP, 1, false, camera.vp, 0);
        GLES20.glUniform3fv(ShaderLib.uLight, 1, LIGHT, 0);
        GLES20.glUniform3fv(ShaderLib.uAmb, 1, AMB, 0);
        Cfg.col3(Themes.cur[Themes.I_FOG], skyC);
        GLES20.glUniform3f(ShaderLib.uFogCol, skyC[0], skyC[1], skyC[2]);
        GLES20.glUniform2f(ShaderLib.uFog, Cfg.FOG_START, Cfg.FOG_END);
        GLES20.glUniform3f(ShaderLib.uCam, camera.eyeX, camera.eyeY, camera.eyeZ);

        boolean dense = save.quality != 2;
        stack.reset();
        for (int i = 0; i < Cfg.CHUNKS; i++) {
            ChunkDraw.draw(world.chunks[i], stack, time, dense);
        }

        boolean show = player.invulnT <= 0 || ((int) (time * 12f) & 1) == 0;
        Catalog.Char ch = screen == S_CHARS
                ? Catalog.CHARS_DATA[charsIdx] : Catalog.CHARS_DATA[save.selChar];
        Humanoid.draw(stack, ch, player, time, show);

        particles.draw(camera.vp, camera.eyeZ, dense);
    }

    // ===================== UI =====================

    private float su() {
        return screenH / 1920f;
    }

    private boolean tapIn(float x, float y, float w, float h) {
        return tapPending
                && touch.lastTapUpX >= x && touch.lastTapUpX <= x + w
                && touch.lastTapUpY >= y && touch.lastTapUpY <= y + h;
    }

    private void hit(int c, int a) {
        if (cmd == 0) {
            cmd = c;
            cmdArg = a;
        }
    }

    private void btn(String label, float x, float y, float w, float h, int argb, boolean enabled) {
        ui.roundRect(x, y, w, h, enabled ? argb : 0x88555555);
        Font.drawCentered(ui, label, x + w / 2f, y + h / 2f - Font.height(h * 0.5f) * 0.5f,
                h * 0.5f, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void bcmd(String label, float x, float y, float w, float h, int argb,
                      boolean enabled, int c, int a) {
        btn(label, x, y, w, h, argb, enabled);
        if (confirmKind < 0 && enabled && tapIn(x, y, w, h)) {
            audio.play(Audio.SFX_CLICK);
            hit(c, a);
        }
    }

    private void bar(float x, float y, float w, float h, float frac, int bg, int fg) {
        ui.solid(x, y, w, h, bg);
        float fw = w * Math.max(0f, Math.min(1f, frac));
        ui.solid(x, y, fw, h, fg);
    }

    private float coinBar(float topY) {
        float u = su();
        String s = String.valueOf(save.totalCoins);
        float tw = Font.width(s, 46 * u);
        float x = screenW - 70 * u - tw - 56 * u;
        ui.coinIcon(screenW - 56 * u - 52 * u, topY, 52 * u);
        Font.draw(ui, s, screenW - 60 * u - tw, topY + 2 * u, 46 * u, 0xFFFFF1B8);
        return topY + 62 * u;
    }

    private float drawBackBtn() {
        float u = su();
        btn("<", 36 * u, 36 * u, 96 * u, 96 * u, 0xFF2E3A66, true);
        if (confirmKind < 0 && tapIn(36 * u, 36 * u, 96 * u, 96 * u)) {
            audio.play(Audio.SFX_CLICK);
            hit(C_HOME, 0);
        }
        return 36 * u + 96 * u;
    }

    private void screenHeader(String title) {
        float u = su();
        drawBackBtn();
        Font.drawCentered(ui, title, screenW / 2f, 52 * u, 64 * u, 0xFFFFFFFF);
        coinBar(40 * u);
    }

    private void drawLogo() {
        float u = su();
        float lw = screenW * 0.8f, lh = lw * 0.25f;
        ui.tex(Textures.logo, (screenW - lw) / 2f, screenH * 0.05f, lw, lh, 0xFFFFFFFF);
    }

    private void drawUI() {
        float u = su();
        switch (screen) {
            case S_SPLASH: drawSplash(); break;
            case S_MENU: drawMenu(); break;
            case S_PLAY: drawHUD(); break;
            case S_PAUSE: drawHUD(); drawPause(); break;
            case S_OVER: drawHUD(); drawOver(); break;
            case S_CHARS: drawChars(); break;
            case S_SHOP: drawShop(); break;
            case S_MISSIONS: drawMissions(); break;
            case S_SETTINGS: drawSettings(); break;
            case S_DAILY: drawDaily(); break;
            case S_PRIVACY: drawPrivacy(); break;
            case S_CREDITS: drawCredits(); break;
            default: break;
        }
        if (confirmKind >= 0) {
            drawConfirm();
        }
        if (toastT > 0) {
            int a = toastT > 0.8f ? 0xFF2E3A66 : (int) (0x77 + 0x88 * toastT) << 24 | 0x2E3A66;
            ui.roundRect(screenW * 0.05f, screenH * 0.86f, screenW * 0.9f, 110 * u, a);
            Font.drawCentered(ui, toast, screenW / 2f, screenH * 0.86f + 26 * u, 46 * u, 0xFFFFFFFF);
        }
    }

    private void drawSplash() {
        float u = su();
        ui.solid(0, 0, screenW, screenH, 0xE60E1023);
        float lw = screenW * 0.86f;
        ui.tex(Textures.logo, (screenW - lw) / 2f, screenH * 0.36f, lw, lw * 0.25f, 0xFFFFFFFF);
        Font.drawCentered(ui, "ONLINE.ARNITCREATION", screenW / 2f, screenH * 0.63f, 34 * u, 0xFF8899CC);
        if (splashT > 1.2f) {
            Font.drawCentered(ui, "TAP TO START", screenW / 2f, screenH * 0.7f, 44 * u,
                    ((int) (time * 2f) & 1) == 0 ? 0xFF22E4C8 : 0xFF118877);
        }
        // consume stray taps not used for skipping
        tapPending = false;
    }

    private void drawMenu() {
        float u = su();
        drawLogo();
        Font.drawCentered(ui, "BEST SCORE  " + save.bestScore,
                screenW / 2f, screenH * 0.335f, 46 * u, 0xFFFFF1B8);
        coinBar(40 * u);

        float bw = screenW * 0.62f, bx = (screenW - bw) / 2f;
        float by = screenH * 0.44f;
        bcmd("PLAY", bx, by, bw, 150 * u, 0xFF16C79A, true, C_PLAY, 0);
        if (Daily.claimable(save)) {
            ui.disc(bx + bw - 20 * u, by - 16 * u, 40 * u, 0xFFFF3D8A);
        }

        float rw = (screenW * 0.86f - 24 * u) / 3f;
        float rx = screenW * 0.07f;
        float ry = by + 190 * u;
        bcmd("CHARS", rx, ry, rw, 120 * u, 0xFF2E3A66, true, C_CHARS, 0);
        bcmd("SHOP", rx + rw + 12 * u, ry, rw, 120 * u, 0xFF2E3A66, true, C_SHOP, 0);
        bcmd("MISSIONS", rx + (rw + 12 * u) * 2, ry, rw, 120 * u, 0xFF2E3A66, true, C_MISSIONS, 0);
        ry += 140 * u;
        bcmd("DAILY", rx, ry, rw, 120 * u, 0xFF2E3A66, true, C_DAILY, 0);
        bcmd("SETTINGS", rx + rw + 12 * u, ry, rw, 120 * u, 0xFF2E3A66, true, C_SETTINGS, 0);
        bcmd("CREDITS", rx + (rw + 12 * u) * 2, ry, rw, 120 * u, 0xFF2E3A66, true, C_CREDITS, 0);

        Catalog.Char ch = Catalog.CHARS_DATA[save.selChar];
        Font.drawCentered(ui, "RUNNER: " + ch.name, screenW / 2f, screenH * 0.93f, 38 * u, 0xFFB399FF);
    }

    private void drawHUD() {
        float u = su();
        Font.draw(ui, "SCORE", 40 * u, 36 * u, 36 * u, 0xFFB399FF);
        Font.draw(ui, String.valueOf((int) score), 40 * u, 80 * u, 62 * u, 0xFFFFFFFF);
        Font.draw(ui, String.valueOf((int) world.dist) + "M", 40 * u, 156 * u, 40 * u, 0xFFD7F9FF);

        String cs = String.valueOf(world.coinsRun);
        float cw = Font.width(cs, 58 * u);
        ui.coinIcon(screenW - 40 * u - cw - 64 * u, 36 * u, 60 * u);
        Font.draw(ui, cs, screenW - 36 * u - cw, 40 * u, 58 * u, 0xFFFFF1B8);

        // pause button
        float px = screenW / 2f - 52 * u;
        ui.roundRect(px, 30 * u, 104 * u, 104 * u, 0x882E3A66);
        Font.drawCentered(ui, "II", screenW / 2f, 52 * u, 62 * u, 0xFFFFFFFF);
        if (tapPending && tapIn(px, 30 * u, 104 * u, 104 * u)) {
            audio.play(Audio.SFX_CLICK);
            setScreen(S_PAUSE);
            return;
        }

        // active power timers
        float py = 210 * u;
        for (int i = 0; i < 6; i++) {
            float t = powerTimeLeft(i);
            if (t <= 0) {
                continue;
            }
            float max = powerTimeMax(i);
            ui.powerIcon(i, 40 * u, py, 48 * u, 0xFFFFFFFF);
            bar(100 * u, py + 14 * u, 190 * u, 16 * u, t / max, 0x88334455, 0xFF22E4C8);
            py += 62 * u;
        }

        // shield icon separately
        if (player.shieldOn) {
            ui.powerIcon(Textures.P_SHIELD, screenW - 130 * u, 210 * u, 90 * u, 0xCCFFFFFF);
        }

        // board button
        float bw = 200 * u, bx = screenW - bw - 36 * u, by = screenH - 150 * u;
        boolean on = player.boardActive;
        boolean cool = player.boardCooldown > 0;
        ui.roundRect(bx, by, bw, 110 * u, on ? 0xFFB399FF : (cool ? 0x88555555 : 0xFF16C79A));
        Font.drawCentered(ui, on ? "BOARD " + (int) player.boardT
                        : (cool ? (int) player.boardCooldown + "S" : "BOARD"),
                bx + bw / 2f, by + 28 * u, 44 * u, 0xFFFFFFFF);
        if (tapPending && !on && !cool && tapIn(bx, by, bw, 110 * u)) {
            audio.play(Audio.SFX_BOARD);
            player.activateBoard();
        }

        // boost speed lines
        if (player.boostT > 0) {
            for (int i = 0; i < 10; i++) {
                float fx = (i % 2 == 0) ? screenW * 0.05f : screenW * 0.93f;
                float fy = ((time * 2600f) + i * 233f * u) % (screenH + 300f) - 300f;
                ui.solid(fx, fy, 10 * u, 150 * u * ((i % 3) + 1), 0x40FFFFFF);
            }
        }

        // tutorial
        if (!save.tutorialShown && screen == S_PLAY && world.runTime < 8f) {
            ui.solid(0, screenH * 0.25f, screenW, screenH * 0.5f, 0x66000000);
            float oy = screenH * 0.29f;
            Font.drawCentered(ui, "SWIPE LEFT/RIGHT: CHANGE LANE", screenW / 2f, oy, 40 * u, 0xFFFFFFFF);
            Font.drawCentered(ui, "SWIPE UP: JUMP", screenW / 2f, oy + 150 * u, 40 * u, 0xFF22E4C8);
            Font.drawCentered(ui, "SWIPE DOWN: SLIDE", screenW / 2f, oy + 300 * u, 40 * u, 0xFFFFC93C);
            Font.drawCentered(ui, "DOUBLE TAP: HOVERBOARD", screenW / 2f, oy + 450 * u, 40 * u, 0xFFB399FF);
            if (world.runTime > 7f) {
                save.tutorialShown = true;
                save.flush(ctx);
            }
        }
    }

    private float powerTimeLeft(int i) {
        switch (i) {
            case 0: return player.magnetT;
            case 1: return player.jetT;
            case 2: return player.sjumpT;
            case 3: return player.multT;
            case 4: return player.shieldT;
            case 5: return player.boostT;
            default: return 0;
        }
    }

    private float powerTimeMax(int i) {
        int lv = player.powerLv[i];
        switch (i) {
            case 1: return 5f + (lv - 1) * 2f;
            case 5: return 6f + (lv - 1) * 2f;
            default: return 8f + (lv - 1) * 3f;
        }
    }

    private void drawPause() {
        float u = su();
        ui.solid(0, 0, screenW, screenH, 0x99000000);
        Font.drawCentered(ui, "PAUSED", screenW / 2f, screenH * 0.25f, 84 * u, 0xFFFFFFFF);
        float bw = screenW * 0.6f, bx = (screenW - bw) / 2f;
        bcmd("RESUME", bx, screenH * 0.42f, bw, 130 * u, 0xFF16C79A, true, C_RESUME, 0);
        bcmd("RESTART", bx, screenH * 0.42f + 160 * u, bw, 130 * u, 0xFF2E3A66, true, C_RESTART, 0);
        bcmd("HOME", bx, screenH * 0.42f + 320 * u, bw, 130 * u, 0xFF2E3A66, true, C_HOME, 0);
    }

    private void drawOver() {
        float u = su();
        ui.solid(0, 0, screenW, screenH, 0x99101230);
        Font.drawCentered(ui, "GAME OVER", screenW / 2f, screenH * 0.13f, 92 * u, 0xFFFF5A3C);

        float bx = screenW * 0.12f, bw = screenW * 0.76f;
        float ty = screenH * 0.25f, rh = 96 * u;
        String[] labels = {"SCORE", "DISTANCE", "COINS", "BEST SCORE"};
        String[] vals = {
                String.valueOf((int) score),
                (int) world.dist + "M",
                String.valueOf(world.coinsRun),
                String.valueOf(save.bestScore),
        };
        for (int i = 0; i < 4; i++) {
            ui.roundRect(bx, ty, bw, rh - 16 * u, 0x55304A6B);
            Font.draw(ui, labels[i], bx + 30 * u, ty + 14 * u, 42 * u, 0xFFB399FF);
            Font.draw(ui, vals[i], bx + bw - 30 * u - Font.width(vals[i], 42 * u), ty + 14 * u, 42 * u, 0xFFFFFFFF);
            ty += rh;
        }

        float bw2 = screenW * 0.6f, bx2 = (screenW - bw2) / 2f;
        if (!revived && overT > 0 && save.totalCoins >= REVIVE_COST) {
            String rev = "REVIVE  " + REVIVE_COST + " COINS (" + Math.max(0, (int) Math.ceil(overT)) + "S)";
            bcmd(rev, bx2, ty + 60 * u, bw2, 130 * u, 0xFFFFB703, true, C_YES, 0);
        } else if (!revived && overT > 0 && save.totalCoins < REVIVE_COST) {
            btn("REVIVE - NEED " + REVIVE_COST + " COINS", bx2, ty + 60 * u, bw2, 130 * u, 0x88555555, false);
        }
        float by = screenH * 0.78f;
        bcmd("PLAY AGAIN", bx2, by, bw2, 130 * u, 0xFF16C79A, true, C_RESTART, 0);
        bcmd("HOME", bx2, by + 150 * u, bw2, 110 * u, 0xFF2E3A66, true, C_HOME, 0);
    }

    private void drawChars() {
        float u = su();
        screenHeader("CHARACTERS");
        Catalog.Char ch = Catalog.CHARS_DATA[charsIdx];
        float cy = screenH * 0.30f;
        float bw = screenW * 0.84f, bx = (screenW - bw) / 2f;
        ui.roundRect(bx, cy - screenH * 0.25f, bw, screenH * 0.25f, 0x55304A6B);
        float arrH = 130 * u, arrW = 110 * u;
        float ary = cy + 40 * u;
        bcmd("<", screenW * 0.05f, ary, arrW, arrH, 0xFF2E3A66, true, C_PREVC, 0);
        bcmd(">", screenW * 0.95f - arrW, ary, arrW, arrH, 0xFF2E3A66, true, C_NEXTC, 0);

        float ty = screenH * 0.25f;
        Font.drawCentered(ui, ch.name, screenW / 2f, ty, 76 * u, 0xFF22E4C8);
        Font.drawCentered(ui, ch.desc, screenW / 2f, ty + 84 * u, 36 * u, 0xFFB399FF);

        boolean owned = save.owned(save.ownedChars, charsIdx);
        float byy = ary + arrH + 60 * u;
        if (owned && save.selChar == charsIdx) {
            btn("SELECTED", bx, byy, bw, 120 * u, 0xFF16C79A, false);
        } else if (owned) {
            bcmd("SELECT", bx, byy, bw, 120 * u, 0xFF16C79A, true, C_BUYYC, A_SELC + charsIdx);
        } else if (ch.requirementMet(save)) {
            bcmd("BUY  " + ch.cost + " COINS", bx, byy, bw, 120 * u,
                    save.totalCoins >= ch.cost ? 0xFFFFB703 : 0xFF8A5A20, true, C_BUYYC, A_BUYCH + charsIdx);
        } else {
            btn(ch.reqText(), bx, byy, bw, 120 * u, 0x88555555, false);
        }
    }

    private void drawShop() {
        float u = su();
        screenHeader("SHOP");
        float tw = (screenW - 80 * u) / 3f;
        for (int i = 0; i < 3; i++) {
            String[] tabs = {"POWER-UPS", "BOARDS", "TRAILS"};
            bcmd(tabs[i], 40 * u + i * (tw + 8 * u), 200 * u, tw, 96 * u,
                    shopTab == i ? 0xFF16C79A : 0xFF2E3A66, true, C_SHOPTAB, i);
        }
        if (shopTab == 0) {
            drawShopPowers();
        } else if (shopTab == 1) {
            drawShopItems("BOARDS", Catalog.BOARDS.length, true);
        } else {
            drawShopItems("TRAILS", Catalog.TRAILS.length, false);
        }
    }

    private void drawShopPowers() {
        float u = su();
        float ty = 340 * u;
        for (int i = 0; i < Player.POWER_NAMES.length; i++) {
            int lv = Math.max(1, save.powerLv[i]);
            int cost = lv >= 3 ? 0 : (lv == 1 ? 500 : 1500);
            ui.roundRect(40 * u, ty, screenW - 80 * u, 150 * u, 0x55304A6B);
            ui.powerIcon(i, 70 * u, ty + 40 * u, 70 * u, 0xFFFFFFFF);
            Font.draw(ui, Player.POWER_NAMES[i], 170 * u, ty + 22 * u, 40 * u, 0xFFFFFFFF);
            Font.draw(ui, lv >= 3 ? "MAX LEVEL" : "LEVEL " + lv, 170 * u, ty + 76 * u, 32 * u, 0xFFB399FF);
            if (lv < 3) {
                bcmd("UPGRADE " + cost, screenW - 320 * u, ty + 40 * u, 250 * u, 80 * u,
                        save.totalCoins >= cost ? 0xFFFFB703 : 0xFF8A5A20, true, C_BUYP, i * 10 + cost);
            } else {
                btn("MAX", screenW - 320 * u, ty + 40 * u, 250 * u, 80 * u, 0x88555555, false);
            }
            ty += 165 * u;
        }
    }

    private void drawShopItems(String kind, int count, boolean isBoard) {
        float u = su();
        float ty = 340 * u;
        for (int i = 0; i < count; i++) {
            boolean owned = save.owned(isBoard ? save.ownedBoards : save.ownedTrails, i);
            String name = isBoard ? Catalog.BOARDS_DATA[i].name : Catalog.TRAILS_DATA[i].name;
            int cost = isBoard ? Catalog.BOARDS_DATA[i].cost : Catalog.TRAILS_DATA[i].cost;
            int deck = isBoard ? Catalog.BOARDS_DATA[i].deckColor : Catalog.TRAILS_DATA[i].dustColor;
            boolean selected = isBoard ? save.selBoard == i : save.selTrail == i;
            ui.roundRect(40 * u, ty, screenW - 80 * u, 190 * u, 0x55304A6B);
            ui.roundRect(70 * u, ty + 60 * u, 90 * u, 70 * u, deck);
            Font.draw(ui, name, 190 * u, ty + 26 * u, 42 * u, 0xFFFFFFFF);
            Font.draw(ui, isBoard ? (Catalog.BOARDS_DATA[i].duration + "S RIDE") : "COSMETIC DUST",
                    190 * u, ty + 84 * u, 32 * u, 0xFFB399FF);
            Font.draw(ui, cost + " COINS", 190 * u, ty + 128 * u, 32 * u, 0xFFFFF1B8);
            float bbw = 300 * u;
            float bbx = screenW - bbw - 70 * u;
            if (owned && selected) {
                btn("EQUIPPED", bbx, ty + 55 * u, bbw, 80 * u, 0xFF16C79A, false);
            } else if (owned) {
                bcmd("EQUIP", bbx, ty + 55 * u, bbw, 80 * u, 0xFF16C79A, true,
                        C_BUYYC, (isBoard ? A_SELB : A_SELT) + i);
            } else {
                bcmd("BUY", bbx, ty + 55 * u, bbw, 80 * u,
                        save.totalCoins >= cost ? 0xFFFFB703 : 0xFF8A5A20, true,
                        C_BUYYC, (isBoard ? A_BUYB : A_BUYT) + i);
            }
            ty += 205 * u;
        }
    }

    private void drawMissions() {
        float u = su();
        screenHeader("MISSIONS");
        float tw = (screenW - 80 * u) / 2f;
        bcmd("MISSIONS", 40 * u, 200 * u, tw, 96 * u, misTab == 0 ? 0xFF16C79A : 0xFF2E3A66, true, C_MISTAB, 0);
        bcmd("BADGES", 40 * u + tw + 8 * u, 200 * u, tw, 96 * u, misTab == 1 ? 0xFF16C79A : 0xFF2E3A66, true, C_MISTAB, 1);

        int count = misTab == 0 ? Missions.COUNT : Achievements.COUNT;
        int visible = 6;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, count - visible)));

        float ty = 340 * u;
        float rh = (screenH - ty - 200 * u) / visible;
        for (int j = 0; j < visible; j++) {
            int i = listScroll + j;
            if (i >= count) {
                break;
            }
            ui.roundRect(40 * u, ty, screenW - 80 * u, rh - 12 * u, 0x55304A6B);
            if (misTab == 0) {
                boolean done = save.missionDone[i];
                int prog = Missions.currentProgress(save, i);
                Font.draw(ui, Missions.TITLE[i], 70 * u, ty + 14 * u, 36 * u, done ? 0xFF22E4C8 : 0xFFFFFFFF);
                Font.draw(ui, Missions.DESC[i], 70 * u, ty + 60 * u, 28 * u, 0xFFB399FF);
                Font.draw(ui, done ? "DONE  +" + Missions.REWARD[i] : prog + "/" + Missions.TARGET[i]
                                + "  +" + Missions.REWARD[i],
                        70 * u, ty + rh - 74 * u, 28 * u, done ? 0xFF22E4C8 : 0xFFFFF1B8);
                bar(70 * u, ty + rh - 96 * u, screenW - 200 * u, 10 * u, (float) prog / Missions.TARGET[i],
                        0x88334455, 0xFF22E4C8);
            } else {
                boolean got = save.achievements[i];
                Font.draw(ui, Achievements.TITLE[i], 70 * u, ty + 14 * u, 36 * u, got ? 0xFFFFC93C : 0xFFFFFFFF);
                Font.draw(ui, Achievements.DESC[i], 70 * u, ty + 60 * u, 28 * u, 0xFFB399FF);
                Font.draw(ui, got ? "UNLOCKED" : "LOCKED", screenW - 240 * u, ty + 14 * u, 28 * u,
                        got ? 0xFFFFC93C : 0x88AAAAAA);
            }
            ty += rh;
        }
        bcmd("^", screenW * 0.25f, screenH - 170 * u, screenW * 0.2f, 100 * u, 0xFF2E3A66, true, C_LISTUP, 0);
        bcmd("v", screenW * 0.55f, screenH - 170 * u, screenW * 0.2f, 100 * u, 0xFF2E3A66, true, C_LISTDN, 0);
    }

    private void settingRow(String label, String value, float ty, int cmdId) {
        float u = su();
        ui.roundRect(40 * u, ty, screenW - 80 * u, 120 * u, 0x55304A6B);
        Font.draw(ui, label, 70 * u, ty + 36 * u, 38 * u, 0xFFFFFFFF);
        bcmd(value, screenW - 420 * u, ty + 20 * u, 350 * u, 80 * u, 0xFF16C79A, true, cmdId, 0);
    }

    private void drawSettings() {
        float u = su();
        screenHeader("SETTINGS");
        float ty = 210 * u;
        float rh = 132 * u;
        settingRow("MUSIC", save.musicOn ? "ON" : "OFF", ty, C_MUS); ty += rh;
        settingRow("SOUND FX", save.sfxOn ? "ON" : "OFF", ty, C_SFX); ty += rh;
        settingRow("VIBRATION", save.vibOn ? "ON" : "OFF", ty, C_VIB); ty += rh;
        String q = save.quality == 0 ? "AUTO" : (save.quality == 1 ? "HIGH" : "LOW");
        String f = save.fpsMode == 0 ? "60" : "30";
        String sn = save.sens == 0 ? "LOW" : (save.sens == 1 ? "MED" : "HIGH");
        settingRow("GRAPHICS", q, ty, C_QUAL); ty += rh;
        settingRow("FPS", f, ty, C_FPS); ty += rh;
        settingRow("SENSITIVITY", sn, ty, C_SENS); ty += rh;

        ui.roundRect(40 * u, ty, screenW - 80 * u, 120 * u, 0x55304A6B);
        Font.draw(ui, "VOLUME", 70 * u, ty + 36 * u, 38 * u, 0xFFFFFFFF);
        bcmd("-", screenW - 560 * u, ty + 20 * u, 110 * u, 80 * u, 0xFF2E3A66, true, C_VOLDN, 0);
        bcmd("+", screenW - 180 * u, ty + 20 * u, 110 * u, 80 * u, 0xFF2E3A66, true, C_VOLUP, 0);
        Font.draw(ui, save.volume + "%", screenW - 420 * u, ty + 40 * u, 40 * u, 0xFFFFF1B8);
        ty += rh;

        bcmd("PRIVACY POLICY", 40 * u, ty, (screenW - 100 * u) / 2f, 100 * u, 0xFF2E3A66, true, C_PRIVACY, 0);
        bcmd("CREDITS", 60 * u + (screenW - 100 * u) / 2f, ty, (screenW - 100 * u) / 2f, 100 * u, 0xFF2E3A66, true, C_CREDITS, 0);
        ty += 120 * u;
        bcmd("RESET GAME DATA", 40 * u, ty, screenW - 80 * u, 100 * u, 0xFFB3261E, true, C_RESET, 0);
    }

    private void drawDaily() {
        float u = su();
        screenHeader("DAILY REWARDS");
        float ty = 220 * u;
        boolean claimable = Daily.claimable(save);
        int nextDay = save.dailyStreak >= 7 || !claimable ? save.dailyStreak : save.dailyStreak + 1;
        if (nextDay < 1) {
            nextDay = 1;
        }
        for (int i = 0; i < 7; i++) {
            int day = i + 1;
            boolean isNext = claimable && day == nextDay;
            String label = "DAY " + day + "   " + Daily.rewardText(day);
            ui.roundRect(60 * u, ty, screenW - 120 * u, 130 * u,
                    isNext ? 0xFF3DDCFF : (save.dailyStreak >= day ? 0xFF2E5A4A : 0x55304A6B));
            Font.drawCentered(ui, label, screenW / 2f, ty + 36 * u, 40 * u,
                    isNext ? 0xFF102030 : 0xFFFFFFFF);
            if (save.dailyStreak >= day && !isNext) {
                Font.draw(ui, "OK", screenW - 140 * u, ty + 44 * u, 36 * u, 0xFF22E4C8);
            }
            ty += 144 * u;
        }
        String bt = claimable ? "CLAIM DAY " + nextDay : "COME BACK TOMORROW";
        bcmd(bt, screenW * 0.2f, screenH - 170 * u, screenW * 0.6f, 120 * u, 0xFFFFB703, claimable, C_CLAIM, 0);
    }

    private void wrapText(String s, float size, float maxW, java.util.ArrayList<String> out) {
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String cand = line.length() == 0 ? w : line + " " + w;
            if (Font.width(cand, size) > maxW && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(cand);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
    }

    private void textScreen(String title, String body) {
        float u = su();
        screenHeader(title);
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        wrapText(body, 34 * u, screenW - 120 * u, lines);
        float ty = 240 * u;
        for (int i = 0; i < lines.size(); i++) {
            Font.draw(ui, lines.get(i), 60 * u, ty, 34 * u, 0xFFD7E4FF);
            ty += 52 * u;
        }
    }

    private void drawPrivacy() {
        textScreen("PRIVACY POLICY",
                "AR NIT STREET RUSH DOES NOT COLLECT OR SEND ANY PERSONAL DATA. "
                        + "THE GAME WORKS FULLY OFFLINE. YOUR SCORES COINS AND UNLOCKS ARE "
                        + "STORED ONLY ON THIS DEVICE AS A LOCAL SAVE FILE. NO ACCOUNT IS "
                        + "REQUIRED AND NO INTERNET CONNECTION IS USED. CLEARING APP DATA OR "
                        + "USING RESET GAME DATA IN SETTINGS REMOVES EVERYTHING PERMANENTLY.");
    }

    private void drawCredits() {
        float u = su();
        textScreen("CREDITS",
                "AR NIT STREET RUSH VERSION 1.0. DESIGN CODE ART AND SOUND BY AR NIT "
                        + "CREATION. ALL CHARACTERS TRACKS ICONS AND MUSIC ARE ORIGINAL AND "
                        + "GENERATED BY THE GAME ITSELF - NO THIRDPARTY ASSETS ARE USED. "
                        + "BUILT WITH A CUSTOM OPENGL ES 2.0 ENGINE. THANK YOU FOR PLAYING!");
    }

    private void askConfirm(int kind, int arg) {
        confirmKind = kind;
        confirmArg = arg;
    }

    private void drawConfirm() {
        float u = su();
        ui.solid(0, 0, screenW, screenH, 0x88000000);
        float bw = screenW * 0.78f, bx = (screenW - bw) / 2f;
        float by = screenH * 0.36f, bh = 520 * u;
        ui.roundRect(bx, by, bw, bh, 0xFF22304F);
        String msg;
        switch (confirmKind) {
            case 9: msg = "QUIT THE GAME?"; break;
            case 8: msg = "RESET ALL GAME DATA? THIS CANNOT BE UNDONE."; break;
            default: msg = buyConfirmText(); break;
        }
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        wrapText(msg, 40 * u, bw - 60 * u, lines);
        float ty = by + 50 * u;
        for (String l : lines) {
            Font.drawCentered(ui, l, screenW / 2f, ty, 40 * u, 0xFFFFFFFF);
            ty += 56 * u;
        }
        bcmd("YES", bx + 30 * u, by + bh - 170 * u, bw / 2f - 45 * u, 120 * u, 0xFF16C79A, true, C_YES, 1);
        bcmd("NO", bx + bw / 2f + 15 * u, by + bh - 170 * u, bw / 2f - 45 * u, 120 * u, 0xFFB3261E, true, C_NO, 0);
    }

    private String buyConfirmText() {
        int kind = confirmKind;
        int arg = confirmArg;
        if (kind == 13) {
            if (arg >= A_BUYT) {
                int i = arg - A_BUYT;
                return "BUY " + Catalog.TRAILS_DATA[i].name + " FOR " + Catalog.TRAILS_DATA[i].cost + " COINS?";
            }
            if (arg >= A_BUYB) {
                int i = arg - A_BUYB;
                return "BUY " + Catalog.BOARDS_DATA[i].name + " FOR " + Catalog.BOARDS_DATA[i].cost + " COINS?";
            }
            if (arg >= A_BUYCH) {
                int i = arg - A_BUYCH;
                Catalog.Char ch = Catalog.CHARS_DATA[i];
                if (ch.reqType != 0 && !ch.requirementMet(save)) {
                    return "REQUIREMENT NOT MET";
                }
                return "BUY " + ch.name + " FOR " + ch.cost + " COINS?";
            }
            return "PURCHASE?";
        }
        if (kind == 14) {
            int i = arg / 10;
            int cost = (arg % 10) * 100;
            return "UPGRADE " + Player.POWER_NAMES[i] + " FOR " + cost + " COINS?";
        }
        return "PURCHASE?";
    }

    private void applyCmd(int c, int a) {
        switch (c) {
            case C_PLAY: startRun(); break;
            case C_CHARS: setScreen(S_CHARS); break;
            case C_SHOP: setScreen(S_SHOP); break;
            case C_MISSIONS: setScreen(S_MISSIONS); break;
            case C_SETTINGS: setScreen(S_SETTINGS); break;
            case C_DAILY: setScreen(S_DAILY); break;
            case C_PRIVACY: setScreen(S_PRIVACY); break;
            case C_CREDITS: setScreen(S_CREDITS); break;
            case C_RESUME: setScreen(S_PLAY); break;
            case C_RESTART: startRun(); break;
            case C_HOME: goHome(); break;
            case C_SHOPTAB: shopTab = a; listScroll = 0; break;
            case C_MISTAB: misTab = a; listScroll = 0; break;
            case C_PREVC: charsIdx = (charsIdx + Catalog.CHARS - 1) % Catalog.CHARS; break;
            case C_NEXTC: charsIdx = (charsIdx + 1) % Catalog.CHARS; break;
            case C_LISTUP: listScroll -= 3; break;
            case C_LISTDN: listScroll += 3; break;
            case C_CLAIM: {
                String res = Daily.claim(save, new java.util.Random());
                if (res != null) {
                    audio.play(Audio.SFX_UNLOCK);
                    showToast("DAILY REWARD! " + res);
                    save.flush(ctx);
                } else {
                    showToast("ALREADY CLAIMED TODAY");
                }
                break;
            }
            case C_MUS:
                save.musicOn = !save.musicOn;
                audio.setMusicOn(save.musicOn);
                if (save.musicOn) {
                    audio.startMusic();
                }
                save.flush(ctx);
                break;
            case C_SFX:
                save.sfxOn = !save.sfxOn;
                audio.setSfxOn(save.sfxOn);
                save.flush(ctx);
                break;
            case C_VIB:
                save.vibOn = !save.vibOn;
                vibrate(60);
                save.flush(ctx);
                break;
            case C_QUAL:
                save.quality = (save.quality + 1) % 3;
                save.flush(ctx);
                break;
            case C_FPS:
                save.fpsMode = (save.fpsMode + 1) % 2;
                lastFrame = System.nanoTime();
                save.flush(ctx);
                break;
            case C_SENS:
                save.sens = (save.sens + 1) % 3;
                touch.setSensitivity(save.sens);
                save.flush(ctx);
                break;
            case C_VOLDN:
                save.volume = Math.max(0, save.volume - 25);
                audio.setVolume(save.volume / 100f);
                save.flush(ctx);
                break;
            case C_VOLUP:
                save.volume = Math.min(100, save.volume + 25);
                audio.setVolume(save.volume / 100f);
                save.flush(ctx);
                break;
            case C_YES:
                if (a == 1) {
                    doConfirm();
                } else {
                    doRevive();
                }
                break;
            case C_NO: confirmKind = -1; break;
            case C_BUYYC:
                if (a >= A_SELT) {
                    save.selTrail = a - A_SELT;
                    save.flush(ctx);
                } else if (a >= A_SELB) {
                    save.selBoard = a - A_SELB;
                    save.flush(ctx);
                } else if (a >= A_SELC) {
                    save.selChar = a - A_SELC;
                    save.flush(ctx);
                } else if (a >= A_BUYCH) {
                    askConfirm(13, a);
                }
                break;
            case C_RESET:
                askConfirm(8, 0);
                break;
            case C_BUYP:
                askConfirm(14, a);
                break;
            default: break;
        }
    }

    private void doConfirm() {
        int kind = confirmKind;
        int arg = confirmArg;
        confirmKind = -1;
        if (kind == 9) {
            try {
                save.flush(ctx);
            } catch (Exception ignored) {
            }
            if (ctx instanceof Activity) {
                ((Activity) ctx).finish();
            }
            return;
        }
        if (kind == 8) {
            save.wipe(ctx);
            audio.setMusicOn(save.musicOn);
            audio.setSfxOn(save.sfxOn);
            audio.setVolume(save.volume / 100f);
            touch.setSensitivity(save.sens);
            world.reset();
            player.reset(0, 0, null, 30);
            finalizeResetStats();
            showToast("GAME DATA RESET");
            return;
        }
        if (kind == 13) {
            if (arg >= A_BUYT) {
                int i = arg - A_BUYT;
                Catalog.Trail tr = Catalog.TRAILS_DATA[i];
                if (!save.owned(save.ownedTrails, i) && save.spend(tr.cost)) {
                    save.own(save.ownedTrails, i);
                    save.selTrail = i;
                    audio.play(Audio.SFX_UNLOCK);
                    showToast(tr.name + " UNLOCKED!");
                } else {
                    showToast("NOT ENOUGH COINS");
                }
            } else if (arg >= A_BUYB) {
                int i = arg - A_BUYB;
                Catalog.Board b = Catalog.BOARDS_DATA[i];
                if (!save.owned(save.ownedBoards, i) && save.spend(b.cost)) {
                    save.own(save.ownedBoards, i);
                    save.selBoard = i;
                    audio.play(Audio.SFX_UNLOCK);
                    showToast(b.name + " UNLOCKED!");
                } else {
                    showToast("NOT ENOUGH COINS");
                }
            } else if (arg >= A_BUYCH) {
                int i = arg - A_BUYCH;
                Catalog.Char ch = Catalog.CHARS_DATA[i];
                if (ch.reqType != 0 && !ch.requirementMet(save)) {
                    showToast(ch.reqText());
                } else if (!save.owned(save.ownedChars, i) && save.spend(ch.cost)) {
                    save.own(save.ownedChars, i);
                    save.selChar = i;
                    audio.play(Audio.SFX_UNLOCK);
                    showToast(ch.name + " UNLOCKED!");
                } else {
                    showToast("NOT ENOUGH COINS");
                }
            }
            save.flush(ctx);
            return;
        }
        if (kind == 14) {
            int i = arg / 10;
            int cost = (arg % 10) * 100;
            int lv = Math.max(1, save.powerLv[i]);
            if (lv >= 3) {
                return;
            }
            if (save.spend(cost)) {
                save.powerLv[i] = lv + 1;
                audio.play(Audio.SFX_UNLOCK);
                showToast(Player.POWER_NAMES[i] + " LEVEL " + (lv + 1));
            } else {
                showToast("NOT ENOUGH COINS");
            }
            save.flush(ctx);
        }
    }

    private void finalizeResetStats() {
        charsIdx = 0;
        shopTab = 0;
        misTab = 0;
    }

    private void doRevive() {
        if (revived || screen != S_OVER) {
            return;
        }
        if (!save.spend(REVIVE_COST)) {
            showToast("NOT ENOUGH COINS");
            return;
        }
        save.flush(ctx);
        revived = true;
        player.revive();
        world.clearNear(30);
        world.crashEvent = -1;
        audio.play(Audio.SFX_REVIVE);
        particles.burst(player.x, 1f, 0f, 24, 0xFF22E4C8, 6f);
        setScreen(S_PLAY);
    }
}
