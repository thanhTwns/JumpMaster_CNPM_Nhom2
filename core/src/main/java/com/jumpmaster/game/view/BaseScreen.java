package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
    protected World world;
    protected ShapeRenderer shapeRenderer;
    protected InputHandler inputHandler;
    protected ExtendViewport viewport;

    protected Player player;
    protected Array<Platform> platforms;
    protected ObjectSet<Platform> visitedPlatforms = new ObjectSet<>();

    protected float highestY = 0f;
    protected float smoothCamY = 0f;
    protected float idleTimer = 0f;

    protected boolean isPaused = false;
    protected GameplayUI gameplayUI;
    protected PauseOverlay pauseOverlay;
    protected GameOverOverlay gameOverOverlay;
    protected InputMultiplexer multiplexer;
    protected ScoreManager scoreManager;

    protected static final float DEATH_Y = -0.5f;

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

    // -------------------------------------------------------
    // SHOW — khởi tạo toàn bộ phần chung
    // -------------------------------------------------------
    @Override
    public void show() {
        AudioManager.getInstance().playGameMusic();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(
                Constants.VIEWPORT_WIDTH / Constants.PPM,
                Constants.VIEWPORT_HEIGHT / Constants.PPM,
                camera);
        world = new World(new Vector2(0, Constants.GRAVITY), true);

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
                if (isPaused || currentState == State.GAME_OVER)
                    return false;
                return super.touchDown(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (isPaused || currentState == State.GAME_OVER)
                    return false;
                return super.touchUp(screenX, screenY, pointer, button);
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

            // Camera follow player
            float targetCamY = Math.max(py, (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f);
            camera.position.y += (targetCamY - camera.position.y) * 0.1f;
            smoothCamY += (camera.position.y - smoothCamY) * 0.02f;
        }

        viewport.apply();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Background do lớp con vẽ
        drawBackground();

        // Platforms + Player
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Platform p : platforms)
            p.draw(game.batch);
        player.draw(game.batch);
        onExtraDraw(); // vẽ bổ sung của lớp con (monster, v.v.)
        game.batch.end();

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
            gameOverOverlay.render();
        } else if (isPaused) pauseOverlay.render();
        else {
            gameplayUI.update(scoreManager);
            gameplayUI.render();
        }
    }

    // -------------------------------------------------------
    // UC-3.3: Ghi nhận tiến độ - Xử lý tiếp đất (Refactored)
    // -------------------------------------------------------
    protected void handleLanding(Platform platform) {
        if (platform == null || visitedPlatforms.contains(platform)) {
            return;
        }

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

    // -------------------------------------------------------
    // TRIGGER GAME OVER
    // -------------------------------------------------------
    protected void triggerGameOver() {
        if (currentState == State.GAME_OVER)
            return;
        currentState = State.GAME_OVER;
        updateInputProcessors();
        idleTimer = 0;
        player.body.setLinearVelocity(0, 0);
        if (inputHandler != null)
            inputHandler.reset();

        scoreManager.flush(); // UC-3.3 AF3: Flush toàn bộ dữ liệu xuống local storage
        gameOverOverlay.setData(scoreManager.getCurrentScore(), scoreManager.getHighScore(),
            scoreManager.getCurrentScore() >= scoreManager.getHighScore());
    }

    // -------------------------------------------------------
    // RESTART — reset về trạng thái ban đầu của màn này
    // -------------------------------------------------------
    protected void restartGame() {
        Gdx.app.postRunnable(() -> {
            currentState = State.RUNNING;
            isPaused = false;
            idleTimer = 0;
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
        onExtraDispose();
    }
}
