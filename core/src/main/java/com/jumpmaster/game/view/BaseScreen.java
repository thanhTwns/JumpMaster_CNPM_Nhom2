package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.jumpmaster.game.AudioManager;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.controller.InputHandler;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.utils.Constants;
import com.jumpmaster.game.utils.ScoreManager;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.jumpmaster.game.GameSettings;

/**
 * BaseScreen — abstract class chứa toàn bộ logic CHUNG giữa GameScreen và
 * SpaceScreen:
 * - Vòng đời Screen (show, render, resize, pause, dispose)
 * - Physics world + camera + viewport
 * - Player, platforms, tường
 * - UI (gameplayUI, pauseOverlay, gameOverOverlay)
 * - State machine (RUNNING / GAME_OVER)
 * - triggerGameOver, restartGame, goToMenu
 * - updateInputProcessors
 * <p>
 * Lớp con chỉ cần implement:
 * - initBackground() — load texture background riêng
 * - initPlatforms() — tạo số bậc và layout riêng
 * - drawBackground() — vẽ background riêng
 * - onLevelComplete() — xử lý khi hoàn thành màn (chuyển màn, hoặc không làm
 * gì)
 * - onExtraUpdate() — logic thêm mỗi frame (monster, v.v.)
 * - onExtraDraw() — vẽ thêm mỗi frame (monster, v.v.)
 * - onExtraDispose() — dispose thêm resource riêng
 * - getSpawnX/Y() — vị trí spawn player
 * - getLevelClearY() — Y ngưỡng chuyển màn (trả về Float.MAX_VALUE nếu không
 * có)
 */
public abstract class BaseScreen implements Screen {

    // -------------------------------------------------------
    // STATE
    // -------------------------------------------------------
    protected enum State {
        RUNNING, GAME_OVER
    }

    protected State currentState = State.RUNNING;

    // -------------------------------------------------------
    // FIELDS CHUNG
    // -------------------------------------------------------
    protected final JumpMasterGame game;

    protected Texture groundTexture;
    protected Texture stepTexture;
    protected Platform leftWall;
    protected Platform rightWall;

    protected OrthographicCamera camera;
    protected OrthographicCamera bgCam;
    private OrthographicCamera mapCam;
    protected World world;
    protected ShapeRenderer shapeRenderer;
    protected InputHandler inputHandler;
    protected ExtendViewport viewport;

    public Player player;
    protected Array<Platform> platforms;
    protected ObjectSet<Platform> visitedPlatforms = new ObjectSet<>();
    protected Platform groundPlatform; // mặt đất — không tính điểm khi chạm

    protected float highestY = 0f;
    protected float smoothCamY = 0f;


    // ── 3.2.1.5 Idle Timer ──────────────────────────────────────────────────
    // AF 3.2.2.1a: đếm ngược 30s khi ở màn GAME_OVER, không có input
    protected float idleTimer = 0f;
    private static final float IDLE_TIMEOUT = 30f;

    // ── 3.2.1.3a Game Over Delay (UC-3.2.4) ────────────────────────────────
    // NFR: overlay chỉ xuất hiện sau 1 giây fade-in kể từ khi nhận game over
    private float gameOverDelay    = 0f;
    private static final float GAME_OVER_DELAY_MAX = 1.0f;
    private boolean overlayVisible = false; // true khi delay đã đủ 1s

    protected boolean isPaused = false;
    protected GameplayUI gameplayUI;
    protected PauseOverlay pauseOverlay;
    public GameOverOverlay gameOverOverlay;
    protected InputMultiplexer multiplexer;
    public ScoreManager scoreManager;

    protected static final float DEATH_Y = -0.5f;
    protected boolean isMapView   = false;
    private   float   mapCamX     = 0f;
    private   float   mapCamY     = 0f;
    private   float   savedCamX   = 0f;
    private   float   savedCamY   = 0f;
    private   boolean mapDragging = false;
    private   float   dragStartX  = 0f;
    private   float   dragStartY  = 0f;
    private   float   camAtDragX  = 0f;
    private   float   camAtDragY  = 0f;
    // Nút Map View — toạ độ screen pixels (gốc trên-trái)
    private static final float BTN_W = 100f;
    private static final float BTN_H = 32f;
    private float getBtnX() { return Gdx.graphics.getWidth()  - BTN_W - 20f; }
    private float getBtnY() { return 20f; }

    private BitmapFont mapFont;    // font riêng cho overlay Map View
    private InputAdapter mapViewInputAdapter;
    private boolean isPausedByMapView = false;

    // topPlatformY phải được lớp con cung cấp (pixels)
    protected abstract float getTopPlatformY();

    // -------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------
    public BaseScreen(JumpMasterGame game) {
        this.game = game;
    }

    // -------------------------------------------------------
    // ABSTRACT METHODS — lớp con bắt buộc implement
    // -------------------------------------------------------

    /**
     * Load texture background (bgLayers, bgSpace, bgPlanet, v.v.)
     */
    protected abstract void initBackground();

    /**
     * Tạo platforms (số bậc, layout, v.v.)
     */
    protected abstract void initPlatforms();

    /**
     * Vẽ background mỗi frame
     */
    protected abstract void drawBackground();

    /**
     * Gọi khi player đạt ngưỡng levelClearY.
     * GameScreen → chuyển sang SpaceScreen.
     * SpaceScreen → không làm gì (không có màn tiếp).
     */
    protected abstract void onLevelComplete();

    /**
     * Y ngưỡng để trigger chuyển màn.
     * Trả về Float.MAX_VALUE nếu màn này không có chuyển màn.
     */
    protected abstract float getLevelClearY();

    /**
     * Logic bổ sung mỗi frame (monster spawn, v.v.) — có thể để trống
     */
    protected void onExtraUpdate(float delta) {
    }

    /**
     * Vẽ bổ sung mỗi frame (monster, v.v.) — có thể để trống
     */
    protected void onExtraDraw() {
    }

    /**
     * Dispose resource bổ sung — có thể để trống
     */
    protected void onExtraDispose() {
    }
    protected void onWinContact(Platform platform) {}

    /**
     * X spawn của player (pixel)
     */
    protected float getSpawnX() {
        return 100f;
    }

    /**
     * Y spawn của player (pixel)
     */
    protected float getSpawnY() {
        return 80f;
    }

    // 2.1.1. Khi người dùng bắt đầu trò chơi, hàm show() khởi tạo tài nguyên
    @Override
    public void show() {
        AudioManager.getInstance().playGameMusic();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(
            Constants.VIEWPORT_WIDTH / Constants.PPM,
            Constants.VIEWPORT_HEIGHT / Constants.PPM,
            camera);
        world = new World(new Vector2(0, Constants.GRAVITY), true);
        mapFont = new BitmapFont();
        mapCam = new OrthographicCamera(
            Constants.VIEWPORT_WIDTH  / Constants.PPM,
            Constants.VIEWPORT_HEIGHT / Constants.PPM);
        bgCam = new OrthographicCamera(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        bgCam.position.set(Constants.VIEWPORT_WIDTH / 2f, Constants.VIEWPORT_HEIGHT / 2f, 0);
        bgCam.update();
        // Background do lớp con quyết định
        initBackground();

        // Platform textures chung
        groundTexture = new Texture("ground.png");
        stepTexture = new Texture("platform_step.png");
        groundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        stepTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Platforms do lớp con quyết định
        platforms = new Array<>();
        initPlatforms();

        // Player
        player = new Player(world, getSpawnX(), getSpawnY());
        syncBgCam();
        camera.position.y = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
        camera.update();
        smoothCamY = camera.position.y;

        // Tường
        leftWall = new Platform(world, -10f, 50000, 20, 100000, null);
        rightWall = new Platform(world, Constants.VIEWPORT_WIDTH + 10f, 50000, 20, 100000, null);

        // InputHandler — block input khi pause/game over
        inputHandler = new InputHandler(player) {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    isPaused = !isPaused;
                    updateInputProcessors();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (isMapView || isPaused || currentState == State.GAME_OVER) return false;
                if (isTapOnMapButton(screenX, screenY)) { enterMapView(); return true; }
                return super.touchDown(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (isPaused || currentState == State.GAME_OVER)
                    return false;
                return super.touchUp(screenX, screenY, pointer, button);
            }
        };
        mapViewInputAdapter = new InputAdapter() {
            @Override
            public boolean touchDown(int sx, int sy, int p, int b) {
                if (isTapOnMapButton(sx, sy)) {
                    if (isMapView) exitMapView(); else enterMapView();
                    return true;
                }
                if (isMapView && b == Input.Buttons.LEFT) {
                    mapDragging = true;
                    dragStartX = sx; dragStartY = sy;
                    camAtDragX = mapCamX; camAtDragY = mapCamY;
                    return true;
                }
                return false;
            }
            @Override
            public boolean touchDragged(int sx, int sy, int p) {
                if (isMapView && mapDragging) {
                    float scaleX = (Constants.VIEWPORT_WIDTH  / Constants.PPM)
                        / (float) Gdx.graphics.getWidth();
                    float scaleY = (Constants.VIEWPORT_HEIGHT / Constants.PPM)
                        / (float) Gdx.graphics.getHeight();
                    mapCamX = camAtDragX - (sx - dragStartX) * scaleX;
                    mapCamY = camAtDragY + (sy - dragStartY) * scaleY;
                    clampMapCamera();
                    applyMapCamera();
                    return true;
                }
                return false;
            }
            @Override
            public boolean touchUp(int sx, int sy, int p, int b) {
                if (isMapView) { mapDragging = false; return true; }
                return false;
            }
            @Override
            public boolean scrolled(float ax, float ay) {
                if (isMapView) {
                    mapCamY += ay * (120f / Constants.PPM);
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
                    updateInputProcessors();
                    return true;
                }
                return false;
            }
        };

        // UI
        scoreManager = new ScoreManager();

        gameplayUI = new GameplayUI(game.batch, () -> {
            isPaused = true;
            if (inputHandler != null)
                inputHandler.reset();
            updateInputProcessors();
        });

        pauseOverlay = new PauseOverlay(game.batch, new PauseOverlay.PauseListener() {
            @Override
            public void onResume() {
                isPaused = false;
                updateInputProcessors();
            }

            @Override
            public void onQuit() {
                goToMenu();
            }
        });

        gameOverOverlay = new GameOverOverlay(game.batch, new GameOverOverlay.GameOverListener() {
            @Override
            public void onRestart() {
                restartGame();
            }

            @Override
            public void onMenu() {
                goToMenu();
            }
        });

        multiplexer = new InputMultiplexer();
        updateInputProcessors();

        shapeRenderer = new ShapeRenderer();
    }

    // -------------------------------------------------------
    // RENDER — vòng lặp chính
    // -------------------------------------------------------
    @Override
    public void render(float delta) {
        if (!isPaused && currentState != State.GAME_OVER) {
            world.step(1 / 60f, 6, 2);
            // Cập nhật ScoreManager để xử lý đếm ngược combo 5s
            scoreManager.update(delta);

            float py = player.body.getPosition().y;
            if (py > highestY)
                highestY = py;

            // Game over khi rơi xuống hố
            if (py < DEATH_Y)
                triggerGameOver();

            // Chuyển màn khi đạt ngưỡng
            if (py >= getLevelClearY())
                onLevelComplete();

            // Logic bổ sung của lớp con (monster, v.v.)
            onExtraUpdate(delta);
            updateCamera();
        }

        viewport.apply();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Background do lớp con vẽ
        drawBackground();

        // Platforms + Player
        game.batch.setProjectionMatrix(
            isMapView ? mapCam.combined : camera.combined);
        game.batch.begin();
        for (Platform p : platforms)
            p.draw(game.batch);
        player.draw(game.batch);
        onExtraDraw(); // vẽ bổ sung của lớp con (monster, v.v.)
        game.batch.end();
        if (isMapView) drawMapViewOverlay();

        // aim & dự đoán đường đi
        if (!isPaused && currentState != State.GAME_OVER && inputHandler.isDragging) {
            shapeRenderer.setProjectionMatrix(camera.combined);

            // aim bar
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            float pX = player.body.getPosition().x;
            float pY = player.body.getPosition().y;
            shapeRenderer.line(pX, pY,
                pX + inputHandler.dragVector.x / Constants.PPM,
                pY + inputHandler.dragVector.y / Constants.PPM);
            shapeRenderer.end();

            // vẽ đường dự đoán
            if (com.jumpmaster.game.GameSettings.getInstance().showTrajectory) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(Color.WHITE);
                Vector2 startPos = player.body.getPosition();
                Vector2 velocity = inputHandler.dragVector.cpy().scl(-0.005f);

                float timeStep = 0.1f;
                for (int i = 0; i < 15; i++) {
                    float t = i * timeStep;
                    float x = startPos.x + velocity.x * t;
                    float y = startPos.y + velocity.y * t + 0.5f * Constants.GRAVITY * t * t;
                    shapeRenderer.circle(x, y, 0.04f, 8);
                }
                shapeRenderer.end();
            }
        }

        // overlay
        if (currentState == State.GAME_OVER) {
            updateInputProcessors();

            // ── 3.2.1.3a Game Over Delay ────────────────────────────────
            // Đếm đủ GAME_OVER_DELAY_MAX (1s) mới hiện overlay → đúng NFR
            if (!overlayVisible) {
                gameOverDelay += delta;
                if (gameOverDelay >= GAME_OVER_DELAY_MAX) {
                    overlayVisible = true;   // unlock overlay sau 1 giây
                }
            }

            // ── 3.2.1.5 Idle Timer ──────────────────────────────────────
            // AF 3.2.2.1a: chỉ đếm khi overlay đã hiện (người chơi đang xem)
            if (overlayVisible) {
                idleTimer += delta;
                // AF 3.2.2.1b: tự động về menu sau IDLE_TIMEOUT giây không input
                if (idleTimer >= IDLE_TIMEOUT) {
                    goToMenu(); // 3.2.1.5a autoReturnMenu()
                }
                gameOverOverlay.render();
            }

        } else if (isPaused && !isPausedByMapView) pauseOverlay.render();
        else {
            gameplayUI.update(scoreManager);
            gameplayUI.render();
        }
        drawMapViewButton();
    }

    // -------------------------------------------------------
    // UC-3.3: Ghi nhận tiến độ - Xử lý tiếp đất (Refactored)
    // -------------------------------------------------------
    protected void handleLanding(Platform platform) {
        // Bỏ qua mặt đất (spawn platform) và platform đã đáp rồi
        if (platform == null || platform == groundPlatform || visitedPlatforms.contains(platform)) {
            return;
        }
        onWinContact(platform);
        if (visitedPlatforms.contains(platform)) return;
        visitedPlatforms.add(platform);
        scoreManager.incrementColumns();

        // Tính Base point = 10 (mặc định cho mỗi bậc bình thường)
        int basePoint = 10;

        // Combo logic (Chỉ tính trong vòng 5 giây, đã được reset tự động trong ScoreManager.update)
        scoreManager.incrementCombo();
        float comboMultiplier = 1.0f;
        if (scoreManager.getCombo() > 1) {
            comboMultiplier = 1.0f + (scoreManager.getCombo() - 1) * 0.5f; // x1.5, x2.0, x2.5...
        }

        int finalPoints = (int) (basePoint * comboMultiplier);
        scoreManager.addPoints(finalPoints);
    }
    private void updateCamera() {
        float py      = player.body.getPosition().y;
        float halfVpH = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
        camera.position.y += (Math.max(py, halfVpH) - camera.position.y) * 0.1f;
        smoothCamY        += (camera.position.y - smoothCamY) * 0.02f;
        camera.update();
        if (!isMapView) syncBgCam();
    }

    protected void syncBgCam() {
        bgCam.position.x = camera.position.x * Constants.PPM;
        bgCam.position.y = camera.position.y * Constants.PPM;
        bgCam.update();
    }

    private void enterMapView() {
        if (isMapView) return;
        isMapView = true;
        mapCam.position.set(camera.position);
        mapCam.update();
        mapCamX = savedCamX; mapCamY = savedCamY;
        mapDragging = false;
        updateInputProcessors();
    }

    private void exitMapView() {
        if (!isMapView) return;
        isMapView = false;

        // Restore camera về đúng vị trí player, không dùng savedCam
        float px = player.body.getPosition().x;
        float py = player.body.getPosition().y;
        float halfVpH = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;

        camera.position.x = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f; // X cố định giữa màn
        camera.position.y = Math.max(py, halfVpH);
        camera.update();

        updateInputProcessors();
    }

    private void applyMapCamera() {
        mapCam.position.x = mapCamX;
        mapCam.position.y = mapCamY;
        mapCam.update();
    }

    private void clampMapCamera() {
        float halfW = (Constants.VIEWPORT_WIDTH  / Constants.PPM) / 2f;
        float halfH = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
        mapCamX = MathUtils.clamp(mapCamX, halfW,
            Constants.VIEWPORT_WIDTH / Constants.PPM - halfW);
        mapCamY = MathUtils.clamp(mapCamY, halfH,
            getTopPlatformY() / Constants.PPM + halfH);
    }

    private void drawMapViewOverlay() {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        Matrix4 ortho = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        shapeRenderer.setProjectionMatrix(ortho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0.5f, 1f, 0.18f);
        shapeRenderer.rect(0, sh - 48f, sw, 48f);
        shapeRenderer.rect(0, 0, sw, 48f);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 0.6f, 1f, 0.7f);
        shapeRenderer.rect(2, 2, sw - 4, sh - 4);
        shapeRenderer.end();

        int pct = (int)(MathUtils.clamp(
            (mapCam.position.y - (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f)
                / Math.max(getTopPlatformY() / Constants.PPM, 1f),
            0f, 1f) * 100f);
        game.batch.setProjectionMatrix(ortho);
        game.batch.begin();
        mapFont.setColor(1f, 1f, 1f, 0.9f);
        mapFont.draw(game.batch,
            "",
            getBtnX() + BTN_W + 16f, sh - getBtnY() - 8f);
        game.batch.end();

        drawPlayerIndicator(ortho, sw, sh);
    }

    private void drawPlayerIndicator(Matrix4 ortho, float sw, float sh) {
        float px = player.body.getPosition().x;   // metres
        float py = player.body.getPosition().y;
        float sx = sw / 2f + (px - mapCam.position.x)
            * (sw / (Constants.VIEWPORT_WIDTH  / Constants.PPM));
        float sy = sh / 2f + (py - mapCam.position.y)
            * (sh / (Constants.VIEWPORT_HEIGHT / Constants.PPM));
        float margin = 24f;
        float cx = MathUtils.clamp(sx, margin, sw - margin);
        float cy = MathUtils.clamp(sy, margin, sh - margin);

        shapeRenderer.setProjectionMatrix(ortho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 0f, 1f);
        shapeRenderer.triangle(cx, cy + 10f, cx - 7f, cy - 5f, cx + 7f, cy - 5f);
        shapeRenderer.end();
    }

    private void drawMapViewButton() {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        float BTN_X = getBtnX();
        float btnY = getBtnY();
        Matrix4 ortho = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        shapeRenderer.setProjectionMatrix(ortho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(isMapView ? new Color(0.1f,0.5f,1f,0.92f)
            : new Color(0f,0f,0f,0.65f));
        shapeRenderer.rect(BTN_X, btnY, BTN_W, BTN_H);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(isMapView ? Color.CYAN : Color.WHITE);
        shapeRenderer.rect(BTN_X, btnY, BTN_W, BTN_H);
        shapeRenderer.end();

        String label = isMapView ? "< Back" : "Map View";
        GlyphLayout gl = new GlyphLayout(mapFont, label);
        game.batch.setProjectionMatrix(ortho);
        game.batch.begin();
        mapFont.setColor(Color.WHITE);
        mapFont.draw(game.batch, label,
            BTN_X + (BTN_W - gl.width) / 2f,
            btnY  + (BTN_H + gl.height) / 2f);
        game.batch.end();
    }

    private boolean isTapOnMapButton(int sx, int sy) {
        float btnX = getBtnX();
        float btnY = Gdx.graphics.getHeight() - getBtnY() - BTN_H; // flip Y vì touch dùng top-left origin
        return sx >= btnX && sx <= btnX + BTN_W
            && sy >= btnY && sy <= btnY + BTN_H;
    }
    // -------------------------------------------------------
    // TRIGGER GAME OVER
    // -------------------------------------------------------
    public void triggerGameOver() {
        if (currentState == State.GAME_OVER)
            return;
        currentState = State.GAME_OVER;
        if (isMapView) exitMapView();
        updateInputProcessors();

        // ── 3.2.1.2 stopPhysics ─────────────────────────────────────────
        player.body.setLinearVelocity(0, 0);
        if (inputHandler != null)
            inputHandler.reset();

        // ── 3.2.1.3a reset delay counter — bắt đầu đếm 1s fade-in ──────
        gameOverDelay  = 0f;
        overlayVisible = false;

        // ── 3.2.1.5 reset idle timer — bắt đầu đếm 30s sau khi overlay hiện
        idleTimer = 0f;

        // ── 3.2.1.3 saveHighScore ────────────────────────────────────────
        // UC-3.3 AF3: kiểm tra new record TRƯỚC khi flush
        // vì flush() cập nhật highScore = currentScore
        boolean isNewRecord = scoreManager.getCurrentScore() > scoreManager.getHighScore();
        scoreManager.flush();
        // 3.2.1.3c: cập nhật điểm số vào leaderboard
        scoreManager.saveTopScores(scoreManager.getCurrentScore());
        // ── 3.2.1.3b lấy stats phiên trước khi reset ────────────────────
        int[] stats = scoreManager.getStats();

        // ── 3.2.1.4 setData + setStats → GameOverOverlay ─────────────────
        gameOverOverlay.setData(scoreManager.getCurrentScore(), scoreManager.getHighScore(), isNewRecord);
        gameOverOverlay.setStats(stats); // 3.2.1.4 hiển thị thống kê phiên
    }

    // -------------------------------------------------------
    // RESTART — reset về trạng thái ban đầu của màn này
    // -------------------------------------------------------
    protected void restartGame() {
        Gdx.app.postRunnable(() -> {
            isMapView = false;
            // ── 3.2.1.6a restartGame ────────────────────────────────────
            currentState   = State.RUNNING;
            isPaused       = false;

            // reset tất cả timer liên quan game over
            idleTimer      = 0f;  // 3.2.1.5 reset idle
            gameOverDelay  = 0f;  // 3.2.1.3a reset delay
            overlayVisible = false;

            highestY = 0;
            visitedPlatforms.clear();
            scoreManager.resetSession();

            player.body.setLinearVelocity(0, 0);
            player.body.setAngularVelocity(0);
            player.body.setTransform(
                new Vector2(getSpawnX() / Constants.PPM, getSpawnY() / Constants.PPM), 0);

            camera.position.y = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
            smoothCamY = camera.position.y;
            camera.update();

            updateInputProcessors();
        });
    }

    // -------------------------------------------------------
    // MENU
    // -------------------------------------------------------
    protected void goToMenu() {
        Gdx.app.postRunnable(() -> {
            AudioManager.getInstance().playMenuMusic();
            game.setScreen(new MainScreen(game));
        });
    }

    // -------------------------------------------------------
    // INPUT PROCESSOR ROUTING
    // -------------------------------------------------------
    protected void updateInputProcessors() {
        multiplexer.clear();
        if (mapViewInputAdapter != null)
            multiplexer.addProcessor(mapViewInputAdapter);
        if (currentState == State.GAME_OVER)
            multiplexer.addProcessor(gameOverOverlay.stage);
        else if (isPaused)
            multiplexer.addProcessor(pauseOverlay.stage);
        else {
            multiplexer.addProcessor(gameplayUI.stage);
            multiplexer.addProcessor(inputHandler);
        }
        Gdx.input.setInputProcessor(multiplexer);
    }

    // -------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (gameplayUI != null)
            gameplayUI.resize(width, height);
        if (pauseOverlay != null)
            pauseOverlay.resize(width, height);
        if (gameOverOverlay != null)
            gameOverOverlay.resize(width, height);
        if (bgCam != null)
            bgCam.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
    }

    @Override
    public void pause() {
        isPaused = true;
        if (inputHandler != null)
            inputHandler.reset();
        updateInputProcessors();
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (world != null)
            world.dispose();
        if (shapeRenderer != null)
            shapeRenderer.dispose();
        if (groundTexture != null)
            groundTexture.dispose();
        if (stepTexture != null)
            stepTexture.dispose();
        if (gameplayUI != null)
            gameplayUI.dispose();
        if (pauseOverlay != null)
            pauseOverlay.dispose();
        if (gameOverOverlay != null)
            gameOverOverlay.dispose();
        if (mapFont != null) mapFont.dispose();
        onExtraDispose();
    }
    protected OrthographicCamera getActiveCamera() {
        return isMapView ? mapCam : camera;
    }
}
