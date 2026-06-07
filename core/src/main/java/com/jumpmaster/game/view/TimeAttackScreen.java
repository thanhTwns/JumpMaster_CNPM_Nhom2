package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
// ── Thêm vào phần imports ──────────────────────────────────────────────
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

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
    private Animation<TextureRegion> batAnimation;
    private float         batStateTime = 0f;
    private Texture       flyingHeadTexture;
    private static final int   BAT_FRAME_COLS  = 5;
    private static final int   BAT_FRAME_ROWS  = 1;

    public TimeAttackScreen(JumpMasterGame game) {
        super(game);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  BaseScreen — abstract implementations
    // ──────────────────────────────────────────────────────────────────────
    private boolean showHealth = true;
    public void setShowHealth(boolean show) {
        this.showHealth = show;
    }
    @Override
    protected float getTopPlatformY() { return topPlatformY; }
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
    public void show() {
        super.show();  // BaseScreen.show() tạo player xong
        if (taLogic != null) taLogic.setPlayer(player);  // inject player sau khi đã tạo
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

        batFrames  = loadBatFrames();
        flyingHeadTexture = tryLoadTexture("ui-timeAttack/flying-head.png");
        if (flyingHeadTexture != null) {
            int frameW = flyingHeadTexture.getWidth() / BAT_FRAME_COLS;
            int frameH = flyingHeadTexture.getHeight() / BAT_FRAME_ROWS;
            TextureRegion[][] tmp = TextureRegion.split(flyingHeadTexture, frameW, frameH);
            Array<TextureRegion> frames = new Array<>();
            for (int i = 0; i < BAT_FRAME_COLS; i++) frames.add(tmp[0][i]);;
            batAnimation = new Animation<>(1f / 8f, frames);
            batAnimation.setPlayMode(Animation.PlayMode.LOOP);
        }
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
        // Báo gameplayUI ẩn health mặc định:
        if (gameplayUI != null) gameplayUI.setShowHealth(false);
    }

    // Thay toàn bộ method thành:
    @Override
    protected void drawBackground() {
        OrthographicCamera cam = getActiveCamera();
        float vpW   = Constants.VIEWPORT_WIDTH  / Constants.PPM;   // metres
        float vpH   = Constants.VIEWPORT_HEIGHT / Constants.PPM;
        float bgH   = vpW * (240f / 320f);
        float camLeft   = camera.position.x - vpW / 2f;
        float camBottom = camera.position.y - vpH / 2f;

        float totalMapH = (topPlatformY / Constants.PPM) + vpH;
        float progress  = MathUtils.clamp(
            (camera.position.y - vpH * 0.5f) / Math.max(totalMapH - vpH, 1f), 0f, 1f);
        float alpha1 = 1f - MathUtils.clamp((progress - 0.33f) / 0.20f, 0f, 1f);
        float alpha2 =       MathUtils.clamp((progress - 0.33f) / 0.20f, 0f, 1f)
            -      MathUtils.clamp((progress - 0.66f) / 0.20f, 0f, 1f);
        float alpha3 =       MathUtils.clamp((progress - 0.66f) / 0.20f, 0f, 1f);

        game.batch.setProjectionMatrix(camera.combined);   // ← metres
        game.batch.begin();
        for (int i = 0; i < bgLayers.length; i++) {
            float alpha = (i < 5) ? alpha1 : (i < 10) ? alpha2 : alpha3;
            if (alpha <= 0f) continue;
            Texture tex   = bgLayers[i];
            float offsetY = (camera.position.y - vpH / 2f) * bgScrollSpeeds[i];
            int texW = tex.getWidth(), texH = tex.getHeight();
            int srcWidth  = (int)(vpW / bgH * texW);
            int srcHeight = (int)(vpH / bgH * texH);
            int srcY      = (int)(offsetY / bgH * texH);
            game.batch.setColor(1f, 1f, 1f, alpha);
            game.batch.draw(tex, camLeft, camBottom, vpW, vpH,
                0, -srcY, srcWidth, srcHeight, false, false);
        }
        game.batch.setColor(Color.WHITE);
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
        batStateTime += delta;   // CHANGE 3: advance animation timer
    }

    @Override
    protected void onExtraDraw() {
        if (taLogic == null) return;

        // Vẽ platform bats (vampire1..12)
        taLogic.drawEntities(game.batch);

        if (batAnimation == null) return;

        Array<com.badlogic.gdx.physics.box2d.Body> flyingBats = taLogic.getFlyingBats();
        for (int i = 0; i < flyingBats.size; i++) {
            com.badlogic.gdx.physics.box2d.Body b = flyingBats.get(i);
            Object ud = b.getUserData();
            boolean fromLeft = false;
            if (ud instanceof Object[]) fromLeft = (Boolean)((Object[])ud)[1];

            // FIX: offset time theo index để mỗi con ở frame khác nhau
            float frameOffset = i * (1f / 8f) * 1.7f;  // lệch ~1-2 frame mỗi con
            TextureRegion frame = batAnimation.getKeyFrame(batStateTime + frameOffset, true);

            boolean needFlip = !fromLeft;
            if (frame.isFlipX() != needFlip) frame.flip(true, false);

            float bx = b.getPosition().x;
            float by = b.getPosition().y;
            float drawW = frame.getRegionWidth()  / Constants.PPM;
            float drawH = frame.getRegionHeight() / Constants.PPM;

            game.batch.draw(frame,
                bx - drawW / 2f,
                by - drawH / 2f,
                drawW, drawH);
        }
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
        if (flyingHeadTexture     != null) flyingHeadTexture.dispose();
        if (flyingHeadTexture != null) flyingHeadTexture.dispose();
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
        if (!isPaused && !isMapView && currentState != State.GAME_OVER) {
            renderTimeAttackHUD(delta);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Render TimeAttack HUD
    // ──────────────────────────────────────────────────────────────────────

    // THAY renderTimeAttackHUD hoàn chỉnh:
    private void renderTimeAttackHUD(float delta) {
        if (taLogic == null) return;

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        Matrix4 screenOrtho = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);

        float hp    = taLogic.getPlayerHealth();
        float hpPct = hp / TimeAttackLogic.PLAYER_MAX_HEALTH;
        float barX  = 20f;
        float barY  = screenH - 56f;   // ngay dưới padding top
        float barW  = 200f;
        float barH  = 14f;

        // ── Shape: nền + thanh + viền ─────────────────────────────────────
        shapeRenderer.setProjectionMatrix(screenOrtho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 0.9f);
        shapeRenderer.rect(barX, barY, barW, barH);
        Color barColor = hpPct > 0.5f ? Color.GREEN
            : hpPct > 0.25f ? new Color(1f, 0.55f, 0f, 1f) : Color.RED;
        shapeRenderer.setColor(barColor);
        shapeRenderer.rect(barX, barY, barW * hpPct, barH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.end();

        // ── Text: "HP: xx" bên phải thanh, "LEVEL x" góc phải ───────────
        game.batch.setProjectionMatrix(screenOrtho);
        game.batch.begin();
        hudFont.setColor(Color.WHITE);
        hudFont.draw(game.batch,
            "HP: " + (int) hp,
            barX + barW + 8f,
            barY + barH);
        game.batch.end();

        // Stage buttons (pause trong taUI nếu có)
        if (taUI != null) {
            taUI.updateData(hp, taLogic.getCurrentLevel(), delta);
            taUI.getStage().act(delta);
            taUI.getStage().draw();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Level advancement
    // ──────────────────────────────────────────────────────────────────────

    private void advanceToNextLevel(int completedLevel) {
        Gdx.app.postRunnable(() -> {
            scoreManager.flush();
            game.setScreen(new WinScreen(game, "timeattack"));
        });
    }
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
    @Override
    protected void updateInputProcessors() {
        super.updateInputProcessors();
        if (taUI != null && !isMapView && currentState != State.GAME_OVER && !isPaused) {
            multiplexer.addProcessor(1, taUI.getStage());   // index 1: sau mapViewAdapter
        }
        Gdx.input.setInputProcessor(multiplexer);
    }
    // TimeAttackScreen.java — thêm override restartGame()
    @Override
    protected void restartGame() {
        Gdx.app.postRunnable(() -> {
            levelTransitionPending = false;

            for (Platform p : platforms) {
                if (p != null && p.body != null) {
                    world.destroyBody(p.body);
                }
            }
            platforms.clear();
            visitedPlatforms.clear();

            buildStepPlatforms(PLATFORM_COUNT, GAP_MIN, GAP_MAX);

            if (taLogic != null) {
                taLogic.onNewLevel(platforms, topPlatformY);
                taLogic.initLevel(1);
            }

            batStateTime = 0f;
        });

        super.restartGame();
    }
}
