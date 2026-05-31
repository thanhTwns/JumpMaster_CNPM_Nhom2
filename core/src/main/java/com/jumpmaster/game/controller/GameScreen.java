package com.jumpmaster.game.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.view.GameplayUI;
import com.jumpmaster.game.view.PauseOverlay;
import com.jumpmaster.game.ui.TimeAttackUI;
import com.jumpmaster.game.utils.Constants;

public class GameScreen implements Screen {

    // ── Core ──────────────────────────────────────────────────────────────
    private final JumpMasterGame game;
    private String mode;
    private static final String MODE_CLASSIC     = "classic";
    private static final String MODE_TIME_ATTACK = "time_attack";

    // ── Physics ───────────────────────────────────────────────────────────
    private World world;

    // ── Camera / Viewport ─────────────────────────────────────────────────
    private OrthographicCamera camera;   // world-units (metres)
    private OrthographicCamera bgCam;    // pixels — dùng cho tất cả drawing
    private ExtendViewport viewport;

    // ── Rendering ─────────────────────────────────────────────────────────
    private ShapeRenderer shapeRenderer;
    private BitmapFont    uiFont;        // font dùng cho nút Map View

    // ── Platform textures ─────────────────────────────────────────────────
    private Texture groundTexture;
    private Texture stepTexture;

    // ── Parallax background ───────────────────────────────────────────────
    private Texture[] bgLayers;
    private float[]   bgScrollSpeeds;

    // ── Input ─────────────────────────────────────────────────────────────
    private InputHandler     inputHandler;
    private InputAdapter     mapViewInputAdapter;
    private InputMultiplexer multiplexer;

    // ── Entities ──────────────────────────────────────────────────────────
    private Player          player;
    private Array<Platform> platforms;
    private Platform        leftWall;
    private Platform        rightWall;

    // ── UI & Pause ────────────────────────────────────────────────────────
    private boolean      isPaused = false;
    private PauseOverlay pauseOverlay;
    private GameplayUI   classicUI;
    private GameplayUI   timeAttackUI;

    // ── Time-Attack specific ──────────────────────────────────────────────
    private TimeAttackLogic taLogic;
    private TimeAttackUI    taUI;
    private Texture         vortexTexture;
    private Texture         healthPotionTexture;
    private Texture[]       batFrames;
    private BitmapFont      hudFont;

    // ── Level state ───────────────────────────────────────────────────────
    private boolean levelTransitionPending = false;
    private float   topPlatformY           = 0f;

    // ── Map View ──────────────────────────────────────────────────────────
    // Chế độ xem bản đồ tự do: tạm dừng game, kéo chuột để pan camera,
    // scroll wheel để cuộn lên/xuống nhanh, nhấn nút lần nữa để về game.
    private boolean isMapView     = false;
    private float   mapCamX       = 0f;   // vị trí bgCam khi đang xem map (pixels)
    private float   mapCamY       = 0f;
    private float   savedCamX     = 0f;   // lưu vị trí camera player để khôi phục
    private float   savedCamY     = 0f;
    private boolean mapDragging   = false;
    private float   dragStartX    = 0f;   // toạ độ chuột lúc bắt đầu kéo (screen px)
    private float   dragStartY    = 0f;
    private float   camAtDragX    = 0f;   // vị trí camera lúc bắt đầu kéo
    private float   camAtDragY    = 0f;

    // Kích thước & vị trí nút "Map View" / "← Back" — toạ độ screen pixels
    // tính từ góc trên-trái (Y = 0 là đỉnh màn hình)
    private static final float BTN_W  = 100f;
    private static final float BTN_H  = 32f;
    private static final float BTN_X  = 20f;   // cách mép trái
    private static final float BTN_Y  = 48f;   // cách mép trên

    // ─────────────────────────────────────────────────────────────────────
    //  Constructors
    // ─────────────────────────────────────────────────────────────────────

    public GameScreen(JumpMasterGame game) {
        this(game, MODE_CLASSIC);
    }

    public GameScreen(JumpMasterGame game, String mode) {
        this.game = game;
        this.mode = (mode != null) ? mode : MODE_CLASSIC;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Screen lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void show() {
        setupCommon();
        switch (mode) {
            case MODE_CLASSIC:     setupClassic();    break;
            case MODE_TIME_ATTACK: setupTimeAttack(); break;
            default:
                Gdx.app.log("GameScreen", "Unknown mode: " + mode);
                setupClassic();
        }
        setupInput();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Setup helpers
    // ─────────────────────────────────────────────────────────────────────

    private void setupCommon() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(
            Constants.VIEWPORT_WIDTH  / Constants.PPM,
            Constants.VIEWPORT_HEIGHT / Constants.PPM,
            camera
        );
        camera.position.set(
            (Constants.VIEWPORT_WIDTH  / Constants.PPM) / 2f,
            (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f,
            0
        );
        camera.update();

        bgCam = new OrthographicCamera(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        bgCam.position.set(
            Constants.VIEWPORT_WIDTH  / 2f,
            Constants.VIEWPORT_HEIGHT / 2f,
            0
        );
        bgCam.update();

        world         = new World(new Vector2(0, Constants.GRAVITY), true);
        shapeRenderer = new ShapeRenderer();
        uiFont        = new BitmapFont();
        platforms     = new Array<>();
        player        = new Player(world, 100, 80);

        leftWall  = new Platform(world, -10f,                           50000f, 20f, 100000f, null);
        rightWall = new Platform(world, Constants.VIEWPORT_WIDTH + 10f, 50000f, 20f, 100000f, null);

        pauseOverlay = new PauseOverlay(game.batch, new PauseOverlay.PauseListener() {
            @Override public void onResume() { isPaused = false; }
            @Override public void onQuit()   { Gdx.app.exit(); }
        });
    }

    private void setupClassic() {
        Texture gTex = new Texture("ground.png");
        Texture sTex = new Texture("platform_step.png");
        gTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        buildPlatforms(gTex, sTex);

        bgLayers = new Texture[]{
            new Texture("sky.png"),
            new Texture("far-mountains.png"),
            new Texture("far-clouds.png"),
            new Texture("mountains.png"),
            new Texture("near-clouds.png"),
            new Texture("trees.png"),
        };
        bgScrollSpeeds = new float[]{ 0.0f, 0.05f, 0.08f, 0.15f, 0.2f, 0.35f };
        applyBgWrap();

        classicUI = new GameplayUI(game.batch, new GameplayUI.GameplayListener() {
            @Override public void onPause() { isPaused = true; }
        });
    }

    private void setupTimeAttack() {
        Texture gTex = new Texture("ground.png");
        Texture sTex = new Texture("ui-timeAttack/groundAndPlatformStep/PNG/Pads/Pad_04_1.png");
        gTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // buildPlatforms PHẢI trước taLogic
        buildPlatforms(gTex, sTex, 80, 90f, 130f);

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
            0.00f, 0.04f, 0.08f, 0.14f, 0.22f,
            0.00f, 0.04f, 0.08f, 0.14f, 0.22f,
            0.00f, 0.04f, 0.08f, 0.14f, 0.22f,
        };
        applyBgWrap();

        batFrames = loadBatFrames();
        Gdx.app.log("GameScreen", "batFrames loaded: " + batFrames.length);

        vortexTexture = tryLoadTexture("ui-timeAttack/portal.png");
        if (vortexTexture == null)
            vortexTexture = tryLoadTexture("ui-timeAttack/vortex.png");

        healthPotionTexture = tryLoadTexture("ui-timeAttack/Health-Potion.png");

        taLogic = new TimeAttackLogic(
            world, platforms, player,
            batFrames, vortexTexture, healthPotionTexture, topPlatformY,
            new TimeAttackLogic.LevelListener() {
                @Override public void onLevelComplete(int completedLevel) {
                    if (!levelTransitionPending) {
                        levelTransitionPending = true;
                        advanceToNextLevel(completedLevel);
                    }
                }
                @Override public void onPlayerDied() {
                    Gdx.app.log("GameScreen", "Player died — game over.");
                    Gdx.app.exit();
                }
            }
        );
        taLogic.initLevel(1);

        hudFont = new BitmapFont();
        taUI = new TimeAttackUI(game.batch, new TimeAttackUI.TimeAttackUIListener() {
            @Override public void onPause() { isPaused = true; }
        });
    }

    private void setupInput() {
        // ── Adapter xử lý Map View drag & scroll ─────────────────────────
        mapViewInputAdapter = new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Kiểm tra có tap vào nút Map View / Back không
                if (isTapOnMapButton(screenX, screenY)) {
                    if (isMapView) exitMapView(); else enterMapView();
                    return true;
                }
                // Bắt đầu kéo trong Map View
                if (isMapView && button == Input.Buttons.LEFT) {
                    mapDragging = true;
                    dragStartX  = screenX;
                    dragStartY  = screenY;
                    camAtDragX  = mapCamX;
                    camAtDragY  = mapCamY;
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isMapView && mapDragging) {
                    // Tỉ lệ chuyển đổi screen pixel → world pixel
                    float scaleX = Constants.VIEWPORT_WIDTH  / (float) Gdx.graphics.getWidth();
                    float scaleY = Constants.VIEWPORT_HEIGHT / (float) Gdx.graphics.getHeight();
                    // Kéo ngược chiều: kéo chuột sang phải → bản đồ dịch trái
                    float dx = (screenX - dragStartX) * scaleX;
                    float dy = (screenY - dragStartY) * scaleY;
                    mapCamX = camAtDragX - dx;
                    mapCamY = camAtDragY + dy;   // Y màn hình ngược Y world
                    clampMapCamera();
                    applyMapCamera();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (isMapView) {
                    mapDragging = false;
                    return true;
                }
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (isMapView) {
                    // Scroll wheel: cuộn lên/xuống 120px mỗi bước
                    mapCamY += amountY * 120f;
                    clampMapCamera();
                    applyMapCamera();
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.M || keycode == Input.Keys.TAB) {
                    if (isMapView) exitMapView(); else enterMapView();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    if (isMapView) { exitMapView(); return true; }
                    isPaused = !isPaused;
                    return true;
                }
                return false;
            }
        };

        // ── InputHandler gốc cho player ───────────────────────────────────
        inputHandler = new InputHandler(player) {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Chặn input player khi đang Map View hoặc Pause
                if (isMapView || isPaused) return false;
                // Nếu tap vào nút Map View → chuyển sang Map View
                if (isTapOnMapButton(screenX, screenY)) {
                    enterMapView();
                    return true;
                }
                return super.touchDown(screenX, screenY, pointer, button);
            }
        };

        multiplexer = new InputMultiplexer();
        // mapViewInputAdapter luôn đứng đầu để bắt nút Map View trước
        multiplexer.addProcessor(mapViewInputAdapter);
        switch (mode) {
            case MODE_TIME_ATTACK:
                if (taUI != null) multiplexer.addProcessor(taUI.getStage());
                break;
            default:
                if (classicUI != null) multiplexer.addProcessor(classicUI.stage);
                break;
        }
        multiplexer.addProcessor(pauseOverlay.stage);
        multiplexer.addProcessor(inputHandler);
        Gdx.input.setInputProcessor(multiplexer);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Map View helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Vào chế độ xem bản đồ: lưu camera hiện tại, tạm dừng game. */
    private void enterMapView() {
        if (isMapView) return;
        isMapView   = true;
        isPaused    = true;       // tạm dừng physics & logic
        savedCamX   = bgCam.position.x;
        savedCamY   = bgCam.position.y;
        mapCamX     = savedCamX;
        mapCamY     = savedCamY;
        mapDragging = false;
        Gdx.app.log("MapView", "Entered — cam=" + mapCamX + "," + mapCamY);
    }

    /** Thoát Map View: khôi phục camera về vị trí player, tiếp tục game. */
    private void exitMapView() {
        if (!isMapView) return;
        isMapView = false;
        isPaused  = false;
        bgCam.position.x = savedCamX;
        bgCam.position.y = savedCamY;
        bgCam.update();
        // Đồng bộ lại physics camera
        camera.position.x = savedCamX / Constants.PPM;
        camera.position.y = savedCamY / Constants.PPM;
        camera.update();
        Gdx.app.log("MapView", "Exited");
    }

    /** Áp vị trí mapCam lên bgCam & camera. */
    private void applyMapCamera() {
        bgCam.position.x = mapCamX;
        bgCam.position.y = mapCamY;
        bgCam.update();
        camera.position.x = mapCamX / Constants.PPM;
        camera.position.y = mapCamY / Constants.PPM;
        camera.update();
    }

    /**
     * Giới hạn camera map view không ra ngoài bản đồ.
     * X: giữ trong [vpW/2, vpW/2] vì bản đồ không rộng hơn viewport.
     * Y: từ vpH/2 (nhìn thấy đất) đến topPlatformY + vpH (trên cùng).
     */
    private void clampMapCamera() {
        float halfW = Constants.VIEWPORT_WIDTH  / 2f;
        float halfH = Constants.VIEWPORT_HEIGHT / 2f;
        // X: bản đồ rộng đúng bằng VIEWPORT_WIDTH nên camera X cố định ở giữa
        mapCamX = MathUtils.clamp(mapCamX, halfW, halfW);
        // Y: giới hạn từ đất đến trên đỉnh bản đồ
        float minY = halfH;
        float maxY = topPlatformY + halfH * 2f;
        mapCamY = MathUtils.clamp(mapCamY, minY, maxY);
    }

    /**
     * Kiểm tra touch có trúng nút Map View không.
     * screenX/Y là toạ độ màn hình thực (gốc trên-trái).
     */
    private boolean isTapOnMapButton(int screenX, int screenY) {
        float screenH = Gdx.graphics.getHeight();
        // Chuyển Y: LibGDX screenY gốc trên-trái, nút vẽ từ góc trên-trái
        return screenX >= BTN_X
            && screenX <= BTN_X + BTN_W
            && screenY >= BTN_Y
            && screenY <= BTN_Y + BTN_H;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Platform / texture helpers
    // ─────────────────────────────────────────────────────────────────────

    private void buildPlatforms(Texture groundTex, Texture stepTex,
                                int count, float minGap, float maxGap) {
        this.groundTexture = groundTex;
        this.stepTexture   = stepTex;

        float groundHeight = 48f;
        float groundWidth  = Constants.VIEWPORT_WIDTH + 200f;
        platforms.add(new Platform(
            world,
            Constants.VIEWPORT_WIDTH / 2f, groundHeight / 2f,
            groundWidth, groundHeight,
            groundTexture
        ));

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

            platforms.add(new Platform(world, randomX, currentY,
                platformWidth, stepHeight, stepTexture));

            currentY += MathUtils.random(120f, 160f);
        }

        topPlatformY = currentY - MathUtils.random(minGap, maxGap);
    }

    private void buildPlatforms(Texture groundTex, Texture stepTex) {
        buildPlatforms(groundTex, stepTex, 50, 120f, 160f);
    }

    private void applyBgWrap() {
        for (Texture t : bgLayers)
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    private Texture[] loadBatFrames() {
        Array<Texture> frames = new Array<>(12);
        for (int i = 1; i <= 12; i++) {
            String path = "ui-timeAttack/vampire" + i + ".png";
            Gdx.app.log("BatDebug", "trying: " + path
                + " exists=" + Gdx.files.internal(path).exists());
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
            Gdx.app.log("GameScreen", "File not found: " + path);
        } catch (Exception e) {
            Gdx.app.log("GameScreen", "Failed to load: " + path);
            e.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Level advancement
    // ─────────────────────────────────────────────────────────────────────

    private void advanceToNextLevel(int completedLevel) {
        int next = completedLevel + 1;
        Gdx.app.log("GameScreen", "Level " + completedLevel + " → " + next);

        platforms.clear();
        buildPlatforms(groundTexture, stepTexture, 80, 90f, 130f);

        taLogic.onNewLevel(platforms, topPlatformY);
        taLogic.initLevel(next);

        player.body.setTransform(
            Constants.VIEWPORT_WIDTH / 2f / Constants.PPM,
            120f / Constants.PPM, 0);
        player.body.setLinearVelocity(0, 0);

        levelTransitionPending = false;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Per-frame update
    // ─────────────────────────────────────────────────────────────────────

    private void updateCamera() {
        float playerYm = player.body.getPosition().y;
        float halfVpHm = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
        float targetYm = Math.max(playerYm, halfVpHm);
        camera.position.y += (targetYm - camera.position.y) * 0.25f;

        float halfVpWm = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
        float targetXm = MathUtils.clamp(
            player.body.getPosition().x,
            halfVpWm,
            Constants.VIEWPORT_WIDTH / Constants.PPM - halfVpWm);
        camera.position.x += (targetXm - camera.position.x) * 0.25f;

        camera.update();

        bgCam.position.x = camera.position.x * Constants.PPM;
        bgCam.position.y = camera.position.y * Constants.PPM;
        bgCam.update();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Render
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {

        // 1. Physics + logic — bỏ qua khi pause HOẶC map view
        if (!isPaused && !isMapView) {
            world.step(1 / 60f, 6, 2);
            updateCamera();
            if (mode.equals(MODE_CLASSIC))     { /* future */ }
            if (mode.equals(MODE_TIME_ATTACK)) taLogic.update(delta);
        }

        // 2. Clear
        viewport.apply();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 3. Parallax background
        drawParallaxBackground();

        // 4. Platforms + Player + TA entities
        game.batch.setProjectionMatrix(bgCam.combined);
        game.batch.begin();
        for (Platform p : platforms) p.draw(game.batch);
        player.draw(game.batch);
        if (mode.equals(MODE_TIME_ATTACK) && taLogic != null)
            taLogic.drawEntities(game.batch);
        game.batch.end();

        // 5. Drag-aim line (chỉ khi đang chơi bình thường)
        if (!isPaused && !isMapView && inputHandler != null && inputHandler.isDragging) {
            shapeRenderer.setProjectionMatrix(bgCam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            float pX   = player.body.getPosition().x * Constants.PPM;
            float pY   = player.body.getPosition().y * Constants.PPM;
            float endX = pX + inputHandler.dragVector.x;
            float endY = pY + inputHandler.dragVector.y;
            shapeRenderer.line(pX, pY, endX, endY);
            shapeRenderer.end();
        }

        // 6. TA shape fallbacks
        if (mode.equals(MODE_TIME_ATTACK) && taLogic != null) {
            shapeRenderer.setProjectionMatrix(bgCam.combined);
            taLogic.drawFallbacks(shapeRenderer);
        }

        // 7. Map View overlay — chỉ hiện khi đang xem map
        if (isMapView) {
            drawMapViewOverlay();
        }

        // 8. HUD / Pause (chỉ hiện khi không ở Map View)
        if (!isMapView) {
            if (isPaused) {
                pauseOverlay.render();
            } else {
                if (mode.equals(MODE_CLASSIC) && classicUI != null)
                    classicUI.render();
                if (mode.equals(MODE_TIME_ATTACK))
                    renderTimeAttackHUD(delta);
            }
        }

        // 9. Nút Map View luôn hiện — kể cả khi pause, kể cả khi đang map view
        drawMapViewButton();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Map View overlay
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Vẽ lớp overlay mờ + hướng dẫn khi đang Map View.
     * Dùng screen-space ortho để vẽ lên trên cùng.
     */
    private void drawMapViewOverlay() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        Matrix4 screenOrtho = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        // Viền mờ xanh lam quanh màn hình để báo hiệu đang ở Map View
        shapeRenderer.setProjectionMatrix(screenOrtho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Top bar mờ
        shapeRenderer.setColor(0f, 0.5f, 1f, 0.18f);
        shapeRenderer.rect(0, sh - 48f, sw, 48f);
        // Bottom bar mờ
        shapeRenderer.rect(0, 0, sw, 48f);
        shapeRenderer.end();

        // Viền xanh lam
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 0.6f, 1f, 0.7f);
        shapeRenderer.rect(2, 2, sw - 4, sh - 4);
        shapeRenderer.end();

        // Text hướng dẫn
        game.batch.setProjectionMatrix(screenOrtho);
        game.batch.begin();
        uiFont.setColor(1f, 1f, 1f, 0.9f);
        // Hiển thị độ cao camera hiện tại để player biết đang xem tầng nào
        int heightPct = (int)(((mapCamY - Constants.VIEWPORT_HEIGHT / 2f)
            / Math.max(topPlatformY, 1f)) * 100f);
        heightPct = MathUtils.clamp(heightPct, 0, 100);
        uiFont.draw(game.batch,
            "MAP VIEW  |  kéo để cuộn  |  scroll wheel lên/xuống  |  cao: " + heightPct + "%",
            BTN_X + BTN_W + 16f,
            sh - BTN_Y - 8f);

        // Mũi tên chỉ vị trí player (luôn hiện dù camera đang ở đâu)
        drawPlayerIndicator(screenOrtho, sw, sh);

        game.batch.end();
    }

    /**
     * Vẽ tam giác nhỏ chỉ vị trí player trên màn hình.
     * Nếu player đang nằm trong vùng nhìn thấy thì vẽ ngay tại vị trí đó.
     * Nếu ngoài vùng nhìn thấy thì vẽ mũi tên ở mép màn hình.
     */
    private void drawPlayerIndicator(Matrix4 screenOrtho, float sw, float sh) {
        float playerWorldX = player.body.getPosition().x * Constants.PPM;
        float playerWorldY = player.body.getPosition().y * Constants.PPM;

        // Chuyển world pixel → screen pixel
        float halfVpW = Constants.VIEWPORT_WIDTH  / 2f;
        float halfVpH = Constants.VIEWPORT_HEIGHT / 2f;
        float relX = playerWorldX - mapCamX;   // tương đối với camera
        float relY = playerWorldY - mapCamY;

        // Scale từ viewport pixel sang screen pixel
        float scaleX = sw / Constants.VIEWPORT_WIDTH;
        float scaleY = sh / Constants.VIEWPORT_HEIGHT;
        float screenX = sw / 2f + relX * scaleX;
        float screenY = sh / 2f + relY * scaleY;   // Y không đảo vì bgCam đã dùng Y-up

        float margin = 24f;
        boolean inView = screenX >= margin && screenX <= sw - margin
            && screenY >= margin && screenY <= sh - margin;

        shapeRenderer.setProjectionMatrix(screenOrtho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 0f, 1f);   // vàng

        if (inView) {
            // Tam giác nhỏ ngay tại vị trí player
            shapeRenderer.triangle(
                screenX,       screenY + 10f,
                screenX - 7f,  screenY - 5f,
                screenX + 7f,  screenY - 5f
            );
        } else {
            // Clamp ra mép, vẽ mũi tên chỉ hướng
            float clampedX = MathUtils.clamp(screenX, margin, sw - margin);
            float clampedY = MathUtils.clamp(screenY, margin, sh - margin);
            shapeRenderer.triangle(
                clampedX,       clampedY + 10f,
                clampedX - 7f,  clampedY - 5f,
                clampedX + 7f,  clampedY - 5f
            );
        }
        shapeRenderer.end();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Nút Map View
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Vẽ nút "Map View" / "← Back" ở góc trên-trái màn hình.
     * Luôn dùng screen-space ortho (không phụ thuộc camera game).
     */
    private void drawMapViewButton() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        // LibGDX screen Y=0 ở dưới, nhưng input screenY=0 ở trên.
        // Để vẽ ở góc trên-trái: y = sh - BTN_Y - BTN_H
        float btnDrawY = sh - BTN_Y - BTN_H;

        Matrix4 screenOrtho = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        // Nền nút
        shapeRenderer.setProjectionMatrix(screenOrtho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (isMapView) {
            shapeRenderer.setColor(0.1f, 0.5f, 1f, 0.92f);   // xanh lam khi đang Map View
        } else {
            shapeRenderer.setColor(0f, 0f, 0f, 0.65f);        // đen mờ khi bình thường
        }
        shapeRenderer.rect(BTN_X, btnDrawY, BTN_W, BTN_H);
        shapeRenderer.end();

        // Viền nút
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(isMapView ? Color.CYAN : Color.WHITE);
        shapeRenderer.rect(BTN_X, btnDrawY, BTN_W, BTN_H);
        shapeRenderer.end();

        // Text nút
        game.batch.setProjectionMatrix(screenOrtho);
        game.batch.begin();
        uiFont.setColor(Color.WHITE);
        String label = isMapView ? "< Back" : "Map View";
        GlyphLayout layout = new GlyphLayout(uiFont, label);
        uiFont.draw(game.batch, label,
            BTN_X + (BTN_W - layout.width)  / 2f,
            btnDrawY + (BTN_H + layout.height) / 2f);
        game.batch.end();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Background rendering
    // ─────────────────────────────────────────────────────────────────────

    private void drawParallaxBackground() {
        float vpW = Constants.VIEWPORT_WIDTH;
        float vpH = Constants.VIEWPORT_HEIGHT;
        float bgH = vpW * (240f / 320f);

        float camCenterYpx = bgCam.position.y;
        float camLeft      = bgCam.position.x - vpW / 2f;
        float camBottom    = camCenterYpx      - vpH / 2f;

        float totalMapH = topPlatformY + vpH;
        float progress  = MathUtils.clamp(
            (camCenterYpx - vpH * 0.5f) / Math.max(totalMapH - vpH, 1f),
            0f, 1f);

        float alpha1 = 1f - MathUtils.clamp((progress - 0.33f) / 0.20f, 0f, 1f);
        float alpha2 =       MathUtils.clamp((progress - 0.33f) / 0.20f, 0f, 1f)
            -       MathUtils.clamp((progress - 0.66f) / 0.20f, 0f, 1f);
        float alpha3 =       MathUtils.clamp((progress - 0.66f) / 0.20f, 0f, 1f);

        game.batch.setProjectionMatrix(bgCam.combined);
        game.batch.begin();

        for (int i = 0; i < bgLayers.length; i++) {
            Texture tex = bgLayers[i];

            float scrollPx = camCenterYpx - vpH / 2f;
            float offsetY  = scrollPx * bgScrollSpeeds[i];

            int texW      = tex.getWidth();
            int texH      = tex.getHeight();
            int srcWidth  = (int)(vpW / bgH * texW);
            int srcHeight = (int)(vpH / bgH * texH);
            int srcY      = (int)(offsetY / bgH * texH);

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

        game.batch.setColor(Color.WHITE);
        game.batch.end();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Time-Attack HUD
    // ─────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────
    //  Screen callbacks
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        bgCam.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        if (classicUI    != null) classicUI.resize(width, height);
        if (pauseOverlay != null) pauseOverlay.resize(width, height);
        if (taUI         != null) taUI.resize(width, height);
    }

    @Override public void pause()  { isPaused = true; }
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        world.dispose();
        shapeRenderer.dispose();
        if (uiFont       != null) uiFont.dispose();
        if (pauseOverlay != null) pauseOverlay.dispose();
        if (classicUI    != null) classicUI.dispose();
        if (taUI         != null) taUI.dispose();
        if (taLogic      != null) taLogic.dispose();
        if (hudFont      != null) hudFont.dispose();

        if (batFrames != null)
            for (Texture t : batFrames) if (t != null) t.dispose();
        if (vortexTexture       != null) vortexTexture.dispose();
        if (healthPotionTexture != null) healthPotionTexture.dispose();
        if (groundTexture       != null) groundTexture.dispose();
        if (stepTexture         != null) stepTexture.dispose();
        if (bgLayers            != null)
            for (Texture t : bgLayers) if (t != null) t.dispose();
    }
}
