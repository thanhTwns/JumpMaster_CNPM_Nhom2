package com.jumpmaster.game.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.screens.MainScreen;
import com.jumpmaster.game.screens.GameOverOverlay;
import com.jumpmaster.game.screens.GameplayUI;
import com.jumpmaster.game.screens.PauseOverlay;
import com.jumpmaster.game.utils.Constants;
import com.jumpmaster.game.utils.ScoreManager;

public class GameScreen implements Screen {
    private enum State {RUNNING, PAUSED, GAME_OVER}
    private float highestY = 0;
    private State currentState = State.RUNNING;
    private ScoreManager scoreManager;
    private GameOverOverlay gameOverOverlay;
    private float idleTimer = 0f;
    private JumpMasterGame game;
    private String mode;
    //PLATFORM
    private Texture groundTexture;
    private Texture stepTexture;
    private Platform leftWall;
    private Platform rightWall;

    // PARALLAX BACKGROUND
    // Thứ tự: xa nhất → gần nhất
    private Texture[] bgLayers;
    private float[] bgScrollSpeeds; // 0.0 = không scroll, 1.0 = scroll cùng tốc độ camera

    // QUAN TRONG
    private OrthographicCamera camera;
    private World world;
    //    private final Box2DDebugRenderer debugRenderer;
    private ShapeRenderer shapeRenderer;
    private InputHandler inputHandler;
    private ExtendViewport viewport;

    // Entity
    private Player player;
    private Array<Platform> platforms;

    // smooth camera for background parallax
    private float smoothCamY;

    // UI & PAUSE SYSTEM
    private boolean isPaused = false;
    private GameplayUI gameplayUI;
    private PauseOverlay pauseOverlay;
    private InputMultiplexer multiplexer;


    public GameScreen(JumpMasterGame game) {
        this(game, "classic");
    }

    public GameScreen(JumpMasterGame game, String mode) {
        this.game = game;
        this.mode = mode;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(
            Constants.VIEWPORT_WIDTH / Constants.PPM,
            Constants.VIEWPORT_HEIGHT / Constants.PPM,
            camera
        );

        world = new World(new Vector2(0, Constants.GRAVITY), true);
//        debugRenderer = new Box2DDebugRenderer();

        bgLayers = new Texture[]{
            new Texture("sky.png"),           // layer 0 - xa nhất, không scroll
            new Texture("far-mountains.png"), // layer 1
            new Texture("far-clouds.png"),    // layer 2
            new Texture("mountains.png"),     // layer 3
            new Texture("near-clouds.png"),   // layer 4
            new Texture("trees.png"),         // layer 5 - gần nhất, scroll nhanh nhất
        };

        // Tốc độ scroll theo chiều Y (so với camera)
        // 0.0 = đứng yên hoàn toàn (sky)
        // 1.0 = scroll cùng tốc độ với world (foreground)
        bgScrollSpeeds = new float[]{0.0f, 0.05f, 0.08f, 0.15f, 0.2f, 0.35f};

        // Bật texture repeat để tile ngang và dọc
        for (Texture t : bgLayers) {
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        }

        // TẠO PLATFORMS
        groundTexture = new Texture("ground.png");
        stepTexture = new Texture("platform_step.png");

        groundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        stepTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        platforms = new Array<>();

        float groundHeight = 48f;
        float groundWidth = Constants.VIEWPORT_WIDTH + 200f;
        platforms.add(new Platform(
            world,
            Constants.VIEWPORT_WIDTH / 2f,
            groundHeight / 2f,
            groundWidth,
            groundHeight,
            groundTexture
        ));

        float stepHeight = 16f;
        float currentY = 150f;
        boolean leftSide = true;

        for (int i = 0; i < 50; i++) {
            float platformWidth = MathUtils.random(100f, 180f);

            float randomX;
            if (leftSide) {
                randomX = MathUtils.random(
                    platformWidth / 2f + 10f,
                    Constants.VIEWPORT_WIDTH / 2f - 20f
                );
            } else {
                randomX = MathUtils.random(
                    Constants.VIEWPORT_WIDTH / 2f + 20f,
                    Constants.VIEWPORT_WIDTH - platformWidth / 2f - 10f
                );
            }
            leftSide = !leftSide;

            platforms.add(new Platform(
                world,
                randomX,
                currentY,
                platformWidth,
                stepHeight,
                stepTexture
            ));

            currentY += MathUtils.random(120f, 160f);
        }

        // Đặt player sát đất (y = 80px thay vì 300px)
        player = new Player(world, 100, 80);

        // Khởi tạo camera tại vị trí đáy map để nền ngang khớp với đáy màn hình
        camera.position.y = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
        camera.update();
        smoothCamY = camera.position.y;

        leftWall = new Platform(world, -10f, 50000, 20, 100000, null);
        rightWall = new Platform(world, Constants.VIEWPORT_WIDTH + 10f, 50000, 20, 100000, null);

        // Input Handling
        inputHandler = new InputHandler(player) {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    isPaused = !isPaused;
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (isPaused) return false;
                return super.touchDown(screenX, screenY, pointer, button);
            }
        };

        // UI & Pause Logic
        gameplayUI = new GameplayUI(game.batch, new GameplayUI.GameplayListener() {
            @Override
            public void onPause() {
                isPaused = true;
            }
        });

        pauseOverlay = new PauseOverlay(game.batch, new PauseOverlay.PauseListener() {
            @Override
            public void onResume() {
                isPaused = false;
            }

            @Override
            public void onMenu() {
                goToMenu();
            }

            @Override
            public void onQuit() {
                Gdx.app.exit();
            }
        });
        // Khởi tạo logic và giao diện Game Over
        scoreManager = new ScoreManager();
        gameOverOverlay = new GameOverOverlay(game.batch, new GameOverOverlay.GameOverListener() {
            @Override public void onRestart() { restartGame(); }
            @Override public void onMenu() { goToMenu(); }
        });
        // Multiplexer để nhận cả input từ UI và Game
        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(pauseOverlay.stage);
        multiplexer.addProcessor(gameOverOverlay.stage);
        multiplexer.addProcessor(gameplayUI.stage);
        multiplexer.addProcessor(inputHandler);
        Gdx.input.setInputProcessor(multiplexer);

        shapeRenderer = new ShapeRenderer();


    }

//    @Override
//    public void show() {}

    @Override
    public void render(float delta) {
        updateGameOverLogic(delta);


        if (!isPaused && currentState != State.GAME_OVER) {
            world.step(1 / 60f, 6, 2);

            float py = player.body.getPosition().y;

            // 1. Cập nhật độ cao cao nhất mà người chơi đạt được
            if (py > highestY) {
                highestY = py;
            }

            // 2. LOGIC GAME OVER:
            // Nếu người chơi rơi thấp hơn mốc cao nhất quá 3 mét (khoảng 2-3 cái platform)
            // VÀ đang ở gần sát mặt đất (py < 1.5f chẳng hạn)
            if (highestY > 2.0f && py < 1.2f) {
                triggerGameOver();
            }

            float playerY = player.body.getPosition().y;
            // Giữ camera không xuống thấp hơn đáy màn hình (y=0)
            float targetCamY = Math.max(playerY, (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f);
            camera.position.y += (targetCamY - camera.position.y) * 0.1f;

            // Background smooth scroll follow
            smoothCamY += (camera.position.y - smoothCamY) * 0.02f;
        }

        viewport.apply();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. Vẽ background parallax
        drawParallaxBackground();

        // 2. Vẽ player
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Platform p : platforms) {
            p.draw(game.batch);
        }
        player.draw(game.batch);
        game.batch.end();

        // 3. Debug Box2D (bỏ dòng này khi làm xong)
//        debugRenderer.render(world, camera.combined);

        // 4. Vẽ đường kéo cung (chỉ khi không pause)
        if (!isPaused && inputHandler.isDragging) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            float pX = player.body.getPosition().x;
            float pY = player.body.getPosition().y;
            float endX = pX + (inputHandler.dragVector.x / Constants.PPM);
            float endY = pY + (inputHandler.dragVector.y / Constants.PPM);
            shapeRenderer.line(pX, pY, endX, endY);
            shapeRenderer.end();
        }

        // 5. Vẽ giao diện UI
        if (currentState == State.GAME_OVER) {
            gameOverOverlay.render();
        } else if (isPaused) {
            pauseOverlay.render();
        } else {
            gameplayUI.render();
        }

    }

    /**
     * Vẽ parallax background gồm 6 layer.
     * <p>
     * Mỗi layer là ảnh pixel art 320x240 (tỉ lệ 4:3).
     * Ta tile ngang để phủ đầy viewport 16:9,
     * và dịch chuyển theo Y với tốc độ khác nhau tạo hiệu ứng parallax.
     */
    private void drawParallaxBackground() {
        float vpW = Constants.VIEWPORT_WIDTH / Constants.PPM; // chiều rộng viewport (meters)
        float vpH = Constants.VIEWPORT_HEIGHT / Constants.PPM; // chiều cao viewport (meters)

        float bgH = vpW * (240f / 320f);

        // Góc trái dưới camera
        float camLeft = camera.position.x - vpW / 2f;
        float camBottom = camera.position.y - vpH / 2f;

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        for (int i = 0; i < bgLayers.length; i++) {
            Texture tex = bgLayers[i];

            // Offset Y theo parallax speed, dùng smoothCamY để không bị "nhảy" theo nhân vật
            // Tính toán offset dựa trên vị trí camera ban đầu để background bắt đầu từ đáy
            float startY = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
            float offsetY = (smoothCamY - startY) * bgScrollSpeeds[i];

            int texW = tex.getWidth();  // 320
            int texH = tex.getHeight(); // 240

            // Tính toán srcWidth và srcHeight để tile cả chiều ngang và dọc
            int srcWidth = (int) (vpW / bgH * texW);
            int srcHeight = (int) (vpH / bgH * texH);

            // Dịch chuyển texture source theo offsetY để tạo hiệu ứng cuộn lên từ từ
            int srcY = (int) (offsetY * (texH / bgH));

            // Vẽ phủ kín toàn bộ viewport, tile tự động nhờ Repeat wrap
            // Dùng -srcY để background di chuyển xuống tương đối khi camera đi lên
            game.batch.draw(tex, camLeft, camBottom, vpW, vpH, 0, -srcY, srcWidth, srcHeight, false, false);
        }

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        gameplayUI.resize(width, height);
        pauseOverlay.resize(width, height);

        if (gameOverOverlay != null) {
            gameOverOverlay.resize(width, height);
        }
    }

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (gameOverOverlay != null) gameOverOverlay.dispose();

        world.dispose();
//        debugRenderer.dispose();
        shapeRenderer.dispose();
        gameplayUI.dispose();
        pauseOverlay.dispose();
        for (Texture t : bgLayers) t.dispose();
        groundTexture.dispose();
        stepTexture.dispose();
    }

    public void checkGameOver() {
        if (player.body.getPosition().y < 0) { // Nhân vật rơi xuống vực
            triggerGameOver();
        }
    }

    private void triggerGameOver() {
        currentState = State.GAME_OVER;
        idleTimer = 0;
        player.body.setLinearVelocity(0, 0); // Basic Flow - Bước 2

        int score = (int) (player.body.getPosition().y * 10); // Ví dụ cách tính điểm
        saveScore(score); // UC-3.3

        boolean isNew = score > scoreManager.getHighScore();
        scoreManager.saveHighScore(score); // Cập nhật record

        gameOverOverlay.setData(score, scoreManager.getHighScore(), isNew);
    }

    private void saveScore(int score) {
        // Để trống theo yêu cầu: Logic lưu tạm hoặc xử lý tín hiệu khác
    }

    private void restartGame() {
        Gdx.app.postRunnable(() -> {
            // 1. Trả trạng thái về Running để hiện gameplayUI
            currentState = State.RUNNING;
            isPaused = false;
            idleTimer = 0;

            // 2. Đưa Player về vị trí cũ (Chia cho PPM)
            player.body.setLinearVelocity(0, 0);
            player.body.setAngularVelocity(0);
            player.body.setTransform(new Vector2(100f / Constants.PPM, 80f / Constants.PPM), 0);

            // 3. Reset Camera để không bị lệch
            camera.position.y = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
            smoothCamY = camera.position.y;
            camera.update();

            // 4. CHỐT HẠ: Ép cái InputProcessor về lại ban đầu
            Gdx.input.setInputProcessor(multiplexer);

            System.out.println("Reset xong, khong bao gio vang!");
        });
        highestY = player.body.getPosition().y; // Reset mốc cao nhất
    }

    private void goToMenu() {
        // Thoát ngay lập tức về Menu và xóa Input Processor cũ
        isPaused = false;
        currentState = State.RUNNING;
        Gdx.input.setInputProcessor(null);
        game.setScreen(new MainScreen(game));
    }

    //
    private void updateGameOverLogic(float delta) {
        if (currentState == State.RUNNING) {
            if (player.body.getPosition().y < 0) triggerGameOver();
        } else if (currentState == State.GAME_OVER) {
            idleTimer += delta;
            if (idleTimer >= 30f) goToMenu();
        }
    }

    private void drawGameOverUI() {
        if (currentState == State.GAME_OVER) {
            gameOverOverlay.render();
        }
    }
}
