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
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.utils.Constants;
import com.jumpmaster.game.controller.InputHandler;
import com.jumpmaster.game.utils.ScoreManager;

public abstract class BaseScreen implements Screen {

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
    protected Platform groundPlatform; // mặt đất — không tính điểm khi chạm

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

    protected abstract void initBackground();
    protected abstract void initPlatforms();
    protected abstract void drawBackground();
    protected abstract void onLevelComplete();
    protected abstract float getLevelClearY();

    protected void onExtraUpdate(float delta) {}
    protected void onExtraDraw() {}
    protected void onExtraDispose() {}

    protected float getSpawnX() { return 100f; }
    protected float getSpawnY() { return 80f; }

    @Override
    public void show() {
        AudioManager.getInstance().playGameMusic();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(
            Constants.VIEWPORT_WIDTH / Constants.PPM,
            Constants.VIEWPORT_HEIGHT / Constants.PPM,
            camera);
        world = new World(new Vector2(0, Constants.GRAVITY), true);

        initBackground();

        groundTexture = new Texture("ground.png");
        stepTexture = new Texture("platform_step.png");
        groundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        stepTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        platforms = new Array<>();
        initPlatforms();

        player = new Player(world, getSpawnX(), getSpawnY());
        camera.position.y = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
        camera.update();
        smoothCamY = camera.position.y;

        leftWall = new Platform(world, -10f, 50000, 20, 100000, null);
        rightWall = new Platform(world, Constants.VIEWPORT_WIDTH + 10f, 50000, 20, 100000, null);

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
                if (isPaused || currentState == State.GAME_OVER) return false;
                return super.touchDown(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (isPaused || currentState == State.GAME_OVER) return false;
                return super.touchUp(screenX, screenY, pointer, button);
            }
        };

        scoreManager = new ScoreManager();

        gameplayUI = new GameplayUI(game.batch, () -> {
            isPaused = true;
            if (inputHandler != null) inputHandler.reset();
            updateInputProcessors();
        });

        pauseOverlay = new PauseOverlay(game.batch, new PauseOverlay.PauseListener() {
            @Override
            public void onResume() {
                isPaused = false;
                updateInputProcessors();
            }
            @Override
            public void onQuit() { goToMenu(); }
        });

        gameOverOverlay = new GameOverOverlay(game.batch, new GameOverOverlay.GameOverListener() {
            @Override
            public void onRestart() { restartGame(); }
            @Override
            public void onMenu() { goToMenu(); }
        });

        multiplexer = new InputMultiplexer();
        updateInputProcessors();

        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        if (!isPaused && currentState != State.GAME_OVER) {
            world.step(1 / 60f, 6, 2);
            scoreManager.update(delta);

            float py = player.body.getPosition().y;
            if (py > highestY) highestY = py;

            // Xử lý idleTimer: Game over nếu không nhảy quá 15s
            if (Math.abs(player.body.getLinearVelocity().y) < 0.1f && Math.abs(player.body.getLinearVelocity().x) < 0.1f) {
                idleTimer += delta;
                if (idleTimer > 15f) triggerGameOver();
            } else {
                idleTimer = 0;
            }

            if (py < DEATH_Y) triggerGameOver();
            if (py >= getLevelClearY()) onLevelComplete();

            onExtraUpdate(delta);

            float targetCamY = Math.max(py, (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f);
            camera.position.y += (targetCamY - camera.position.y) * 0.1f;
            smoothCamY += (camera.position.y - smoothCamY) * 0.02f;
        }

        viewport.apply();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Platform p : platforms) p.draw(game.batch);
        player.draw(game.batch);
        onExtraDraw();
        game.batch.end();

        if (!isPaused && currentState != State.GAME_OVER && inputHandler.isDragging) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            float pX = player.body.getPosition().x;
            float pY = player.body.getPosition().y;
            shapeRenderer.line(pX, pY,
                pX + inputHandler.dragVector.x / Constants.PPM,
                pY + inputHandler.dragVector.y / Constants.PPM);
            shapeRenderer.end();

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

        if (currentState == State.GAME_OVER) {
            gameOverOverlay.render();
        } else if (isPaused) {
            pauseOverlay.render();
        } else {
            gameplayUI.update(scoreManager);
            gameplayUI.render();
        }
    }

    protected void handleLanding(Platform platform) {
        if (platform == null || platform == groundPlatform || visitedPlatforms.contains(platform)) return;

        visitedPlatforms.add(platform);
        scoreManager.incrementColumns();
        int basePoint = 10;
        scoreManager.incrementCombo();
        float comboMultiplier = 1.0f;
        if (scoreManager.getCombo() > 1) {
            comboMultiplier = 1.0f + (scoreManager.getCombo() - 1) * 0.5f;
        }
        scoreManager.addPoints((int) (basePoint * comboMultiplier));
    }

    protected void triggerGameOver() {
        if (currentState == State.GAME_OVER) return;
        currentState = State.GAME_OVER;
        updateInputProcessors();
        idleTimer = 0;
        player.body.setLinearVelocity(0, 0);
        if (inputHandler != null) inputHandler.reset();

        boolean isNewRecord = scoreManager.getCurrentScore() > scoreManager.getHighScore();
        scoreManager.flush();
        gameOverOverlay.setData(scoreManager.getCurrentScore(), scoreManager.getHighScore(), isNewRecord);
    }

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
            player.body.setTransform(new Vector2(getSpawnX() / Constants.PPM, getSpawnY() / Constants.PPM), 0);
            camera.position.y = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
            smoothCamY = camera.position.y;
            camera.update();
            updateInputProcessors();
        });
    }

    protected void goToMenu() {
        Gdx.app.postRunnable(() -> {
            AudioManager.getInstance().playMenuMusic();
            game.setScreen(new MainScreen(game));
        });
    }

    protected void updateInputProcessors() {
        multiplexer.clear();
        if (currentState == State.GAME_OVER) multiplexer.addProcessor(gameOverOverlay.stage);
        else if (isPaused) multiplexer.addProcessor(pauseOverlay.stage);
        else {
            multiplexer.addProcessor(gameplayUI.stage);
            multiplexer.addProcessor(inputHandler);
        }
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (gameplayUI != null) gameplayUI.resize(width, height);
        if (pauseOverlay != null) pauseOverlay.resize(width, height);
        if (gameOverOverlay != null) gameOverOverlay.resize(width, height);
    }

    @Override public void pause() {
        isPaused = true;
        if (inputHandler != null) inputHandler.reset();
        updateInputProcessors();
    }
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (world != null) world.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (groundTexture != null) groundTexture.dispose();
        if (stepTexture != null) stepTexture.dispose();
        if (gameplayUI != null) gameplayUI.dispose();
        if (pauseOverlay != null) pauseOverlay.dispose();
        if (gameOverOverlay != null) gameOverOverlay.dispose();
        onExtraDispose();
    }
}
