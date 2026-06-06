package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.controller.TimeAttackLogic;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.utils.Constants;
import com.jumpmaster.game.view.TimeAttackUI;

/**
 * TimeAttackScreen — Chế độ Time Attack.
 * Extends BaseScreen, chứa toàn bộ logic Time Attack:
 * - Background parallax nhiều lớp (3 bộ x 5 layer)
 * - Bat enemies, health potions, vortex portal
 * - Chuyển level khi vào vortex
 * - HUD riêng (TimeAttackUI)
 */
public class TimeAttackScreen extends BaseScreen {

    // ── Background ────────────────────────────────────────────────────────
    private Texture[] bgLayers;
    private float[]   bgScrollSpeeds;

    // ── TimeAttack entities ───────────────────────────────────────────────
    private TimeAttackLogic taLogic;
    private TimeAttackUI    taUI;
    private BitmapFont      hudFont;

    private Texture   vortexTexture;
    private Texture   healthPotionTexture;
    private Texture[] batFrames;

    // ── Level state ───────────────────────────────────────────────────────
    private float   topPlatformY           = 0f;
    private boolean levelTransitionPending = false;

    // ── Số platform / gap ─────────────────────────────────────────────────
    private static final int   PLATFORM_COUNT = 80;
    private static final float GAP_MIN        = 90f;
    private static final float GAP_MAX        = 130f;

    public TimeAttackScreen(JumpMasterGame game) {
        super(game);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  BaseScreen — abstract implementations
    // ──────────────────────────────────────────────────────────────────────

    @Override
    protected void initBackground() {
        bgLayers = new Texture[]{
            new Texture("ui-timeAttack/m1/1.png"),
            new Texture("ui-timeAttack/m1/2.png"),
            new Texture("ui-timeAttack/m1/3.png"),
            new Texture("ui-timeAttack/m1/4.png"),
            new Texture("ui-timeAttack/m1/5.png"),
            new Texture("ui-timeAttack/m3/1.png"),
            new Texture("ui-timeAttack/m3/2.png"),
            new Texture("ui-timeAttack/m3/3.png"),
            new Texture("ui-timeAttack/m3/4.png"),
            new Texture("ui-timeAttack/m3/5.png"),
            new Texture("ui-timeAttack/m6/1.png"),
            new Texture("ui-timeAttack/m6/2.png"),
            new Texture("ui-timeAttack/m6/3.png"),
            new Texture("ui-timeAttack/m6/4.png"),
            new Texture("ui-timeAttack/m6/5.png"),
        };
        bgScrollSpeeds = new float[]{
            0.00f, 0.04f, 0.08f, 0.14f, 0.22f,   // m1
            0.00f, 0.04f, 0.08f, 0.14f, 0.22f,   // m3
            0.00f, 0.04f, 0.08f, 0.14f, 0.22f,   // m6
        };
        for (Texture t : bgLayers)
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    @Override
    protected void initPlatforms() {
        // Contact listener — bat hit player
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Object dataA = contact.getFixtureA().getUserData();
                Object dataB = contact.getFixtureB().getUserData();
                Object bodyDataA = contact.getFixtureA().getBody().getUserData();
                Object bodyDataB = contact.getFixtureB().getBody().getUserData();

                boolean isA_Player = "player".equals(dataA) || "player".equals(bodyDataA);
                boolean isB_Player = "player".equals(dataB) || "player".equals(bodyDataB);

                if (isA_Player || isB_Player) {
                    Platform platform = null;
                    if (isA_Player && bodyDataB instanceof Platform)
                        platform = (Platform) bodyDataB;
                    else if (isB_Player && bodyDataA instanceof Platform)
                        platform = (Platform) bodyDataA;

                    if (platform != null && player.body.getLinearVelocity().y < -0.05f)
                        handleLanding(platform);
                }
            }
            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        // Load assets cho TimeAttack
        Texture stepTex = new Texture("ui-timeAttack/groundAndPlatformStep/PNG/Pads/Pad_04_1.png");
        stepTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        // Override stepTexture từ BaseScreen
        if (this.stepTexture != null) this.stepTexture.dispose();
        this.stepTexture = stepTex;

        batFrames           = loadBatFrames();
        vortexTexture       = tryLoadTexture("ui-timeAttack/portal.png");
        if (vortexTexture == null)
            vortexTexture   = tryLoadTexture("ui-timeAttack/vortex.png");
        healthPotionTexture = tryLoadTexture("ui-timeAttack/Health-Potion.png");

        // Build platforms
        buildStepPlatforms(PLATFORM_COUNT, GAP_MIN, GAP_MAX);

        // TimeAttackLogic
        taLogic = new TimeAttackLogic(
            world, platforms, player,
            batFrames, vortexTexture, healthPotionTexture, topPlatformY,
            new TimeAttackLogic.LevelListener() {
                @Override
                public void onLevelComplete(int completedLevel) {
                    if (!levelTransitionPending) {
                        levelTransitionPending = true;
                        advanceToNextLevel(completedLevel);
                    }
                }
                @Override
                public void onPlayerDied() {
                    triggerGameOver();
                }
            }
        );
        taLogic.initLevel(1);

        // HUD
        hudFont = new BitmapFont();
        taUI = new TimeAttackUI(game.batch, new TimeAttackUI.TimeAttackUIListener() {
            @Override
            public void onPause() {
                isPaused = true;
                if (inputHandler != null) inputHandler.reset();
                updateInputProcessors();
            }
        });
    }

    @Override
    protected void drawBackground() {
        float vpW  = Constants.VIEWPORT_WIDTH  / Constants.PPM;
        float vpH  = Constants.VIEWPORT_HEIGHT / Constants.PPM;
        // background height scaled to viewport
        float bgHm = vpW * (240f / 320f);

        float camCY = camera.position.y;
        float camLeft   = camera.position.x - vpW / 2f;
        float camBottom = camCY - vpH / 2f;

        float totalMapH = (topPlatformY / Constants.PPM) + vpH;
        float progress  = MathUtils.clamp(
            (camCY - vpH * 0.5f) / Math.max(totalMapH - vpH, 1f), 0f, 1f);

        float alpha1 = 1f - MathUtils.clamp((progress - 0.33f) / 0.20f, 0f, 1f);
        float alpha2 =       MathUtils.clamp((progress - 0.33f) / 0.20f, 0f, 1f)
            -      MathUtils.clamp((progress - 0.66f) / 0.20f, 0f, 1f);
        float alpha3 =       MathUtils.clamp((progress - 0.66f) / 0.20f, 0f, 1f);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        for (int i = 0; i < bgLayers.length; i++) {
            Texture tex = bgLayers[i];

            float scrollPx = camCY * Constants.PPM - Constants.VIEWPORT_HEIGHT / 2f;
            float offsetY  = scrollPx * bgScrollSpeeds[i];

            int texW      = tex.getWidth();
            int texH      = tex.getHeight();
            float bgHpx   = (Constants.VIEWPORT_WIDTH / Constants.PPM) * (240f / 320f) * Constants.PPM;
            int srcWidth  = (int)(Constants.VIEWPORT_WIDTH  / bgHpx * texW);
            int srcHeight = (int)(Constants.VIEWPORT_HEIGHT / bgHpx * texH);
            int srcY      = (int)(offsetY / bgHpx * texH);

            float alpha;
            if      (i < 5)  alpha = alpha1;
            else if (i < 10) alpha = alpha2;
            else             alpha = alpha3;
            if (alpha <= 0f) continue;

            game.batch.setColor(1f, 1f, 1f, alpha);
            game.batch.draw(tex,
                camLeft, camBottom, vpW, vpH,
                0, -srcY, srcWidth, srcHeight,
                false, false);
        }

        game.batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        game.batch.end();
    }

    /** Time Attack không có Y ngưỡng cứng — level clear do vortex kích hoạt. */
    @Override
    protected float getLevelClearY() {
        return Float.MAX_VALUE;
    }

    /** Không dùng — level complete do TimeAttackLogic.LevelListener.onLevelComplete(). */
    @Override
    protected void onLevelComplete() { /* no-op */ }

    // ──────────────────────────────────────────────────────────────────────
    //  Extra hooks từ BaseScreen
    // ──────────────────────────────────────────────────────────────────────

    @Override
    protected void onExtraUpdate(float delta) {
        if (taLogic != null) taLogic.update(delta);
    }

    @Override
    protected void onExtraDraw() {
        if (taLogic == null) return;

        // Vẽ entities có texture
        taLogic.drawEntities(game.batch);

        // Vẽ fallback shape nếu không có texture
        // (batch đang end() ở BaseScreen trước khi gọi onExtraDraw —
        //  xem lại BaseScreen.render: onExtraDraw gọi TRONG game.batch.begin/end)
        // → fallback phải vẽ sau khi batch kết thúc; dùng flag để vẽ ở onExtraShapeDraw()
    }

    /**
     * Vẽ shape fallback sau khi batch.end() — override hook riêng nếu BaseScreen có.
     * Nếu BaseScreen chưa có hook này, hãy thêm:
     *   protected void onExtraShapeDraw() {} vào BaseScreen và gọi sau game.batch.end().
     * Tạm thời để trống — fallback chỉ hiện khi không có texture.
     */
    // protected void onExtraShapeDraw() {
    //     if (taLogic != null) taLogic.drawFallbacks(shapeRenderer);
    // }

    @Override
    protected void onExtraDispose() {
        if (taLogic      != null) taLogic.dispose();
        if (taUI         != null) taUI.dispose();
        if (hudFont      != null) hudFont.dispose();
        if (vortexTexture       != null) vortexTexture.dispose();
        if (healthPotionTexture != null) healthPotionTexture.dispose();
        if (bgLayers     != null)
            for (Texture t : bgLayers) if (t != null) t.dispose();
        if (batFrames    != null)
            for (Texture t : batFrames) if (t != null) t.dispose();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Resize — thêm taUI
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (taUI != null) taUI.resize(width, height);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Render override — thêm TimeAttack HUD
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        super.render(delta);   // BaseScreen xử lý physics, platforms, player, overlays

        // TimeAttack HUD (chỉ khi đang chơi, không pause, không game over)
        if (!isPaused && currentState != State.GAME_OVER) {
            renderTimeAttackHUD(delta);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Render TimeAttack HUD
    // ──────────────────────────────────────────────────────────────────────

    private void renderTimeAttackHUD(float delta) {
        if (taUI == null || taLogic == null) return;

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        taUI.updateData(taLogic.getPlayerHealth(), taLogic.getCurrentLevel(), delta);

        Matrix4 screenOrtho = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);

        shapeRenderer.setProjectionMatrix(screenOrtho);
        taUI.renderShapes(shapeRenderer, screenH);

        game.batch.setProjectionMatrix(screenOrtho);
        game.batch.begin();
        taUI.renderText(game.batch, hudFont, screenH);
        game.batch.end();

        taUI.getStage().act(delta);
        taUI.getStage().draw();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Level advancement
    // ──────────────────────────────────────────────────────────────────────

    private void advanceToNextLevel(int completedLevel) {
        int next = completedLevel + 1;
        Gdx.app.log("TimeAttackScreen", "Level " + completedLevel + " → " + next);

        // Xây lại platforms
        // Xoá platforms cũ (giữ ground)
        platforms.clear();
        buildStepPlatforms(PLATFORM_COUNT, GAP_MIN, GAP_MAX);

        // Reset player về spawn
        player.body.setTransform(
            getSpawnX() / Constants.PPM,
            getSpawnY() / Constants.PPM, 0);
        player.body.setLinearVelocity(0, 0);

        taLogic.onNewLevel(platforms, topPlatformY);
        taLogic.initLevel(next);

        levelTransitionPending = false;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Platform builder (tách riêng để dùng lại khi chuyển level)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Xây ground + step platforms.
     * Kết quả lưu vào this.platforms và cập nhật this.topPlatformY.
     */
    private void buildStepPlatforms(int count, float minGap, float maxGap) {
        float groundHeight = 48f;
        float groundWidth  = Constants.VIEWPORT_WIDTH + 200f;

        groundPlatform = new Platform(
            world,
            Constants.VIEWPORT_WIDTH / 2f, groundHeight / 2f,
            groundWidth, groundHeight,
            groundTexture);
        platforms.add(groundPlatform);

        float stepHeight = 16f;
        float currentY   = 150f;
        boolean leftSide = true;

        for (int i = 0; i < count; i++) {
            float platformWidth = MathUtils.random(100f, 180f);
            float randomX;
            if (leftSide) {
                randomX = MathUtils.random(
                    platformWidth / 2f + 10f,
                    Constants.VIEWPORT_WIDTH / 2f - 20f);
            } else {
                randomX = MathUtils.random(
                    Constants.VIEWPORT_WIDTH / 2f + 20f,
                    Constants.VIEWPORT_WIDTH - platformWidth / 2f - 10f);
            }
            leftSide = !leftSide;

            platforms.add(new Platform(
                world, randomX, currentY,
                platformWidth, stepHeight,
                stepTexture));

            currentY += MathUtils.random(minGap, maxGap);
        }

        // topPlatformY = pixel Y của platform cuối (dùng cho background fade và taLogic)
        topPlatformY = currentY - MathUtils.random(minGap, maxGap);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Asset helpers
    // ──────────────────────────────────────────────────────────────────────

    private Texture[] loadBatFrames() {
        Array<Texture> frames = new Array<>(12);
        for (int i = 1; i <= 12; i++) {
            String path = "ui-timeAttack/vampire" + i + ".png";
            Texture t = tryLoadTexture(path);
            if (t != null) {
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                frames.add(t);
            }
        }
        Texture[] result = new Texture[frames.size];
        for (int i = 0; i < frames.size; i++) result[i] = frames.get(i);
        return result;
    }

    private Texture tryLoadTexture(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Texture t = new Texture(path);
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                return t;
            }
            Gdx.app.log("TimeAttackScreen", "File not found: " + path);
        } catch (Exception e) {
            Gdx.app.log("TimeAttackScreen", "Failed to load: " + path);
        }
        return null;
    }
}
