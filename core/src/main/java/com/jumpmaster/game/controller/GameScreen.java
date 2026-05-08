package com.jumpmaster.game.controller;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.utils.Constants;

public class GameScreen implements Screen {
    private final JumpMasterGame game;

    //PLATFORM
    private Texture groundTexture;
    private Texture stepTexture;
    private Platform leftWall;
    private Platform rightWall;

    // PARALLAX BACKGROUND
    // Thứ tự: xa nhất → gần nhất
    private final Texture[] bgLayers;
    private final float[] bgScrollSpeeds; // 0.0 = không scroll, 1.0 = scroll cùng tốc độ camera

    // QUAN TRONG
    private final OrthographicCamera camera;
    private final World world;
//    private final Box2DDebugRenderer debugRenderer;
    private final ShapeRenderer shapeRenderer;
    private final InputHandler inputHandler;
    private final ExtendViewport viewport;

    // Entity
    private final Player player;
    private final Array<Platform> platforms;

    public GameScreen(JumpMasterGame game) {
        this.game = game;
<<<<<<< feature/main-ui
    }

    public GameScreen(Game game, String classic) {

    }

    @Override
    public void show() {
=======
>>>>>>> master

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
        bgScrollSpeeds = new float[]{ 0.0f, 0.05f, 0.08f, 0.15f, 0.2f, 0.35f };

        // Bật texture repeat để tile ngang
        for (Texture t : bgLayers) {
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        }

        // TẠO PLATFORMS
        groundTexture = new Texture("ground.png");
        stepTexture   = new Texture("platform_step.png");

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
        float currentY   = 150f;
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

        player = new Player(world, 100, 300);

        leftWall  = new Platform(world, -10f,50000, 20, 100000, null);
        rightWall = new Platform(world, Constants.VIEWPORT_WIDTH + 10f, 50000, 20, 100000, null);

        inputHandler = new InputHandler(player);
        Gdx.input.setInputProcessor(inputHandler);

        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        world.step(1 / 60f, 6, 2);

        float playerY = player.body.getPosition().y;
        camera.position.y += (playerY - camera.position.y) * 0.1f;
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

        // 4. Vẽ đường kéo cung
        if (inputHandler.isDragging) {
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
    }

    /**
     * Vẽ parallax background gồm 6 layer.
     *
     * Mỗi layer là ảnh pixel art 320x240 (tỉ lệ 4:3).
     * Ta tile ngang để phủ đầy viewport 16:9,
     * và dịch chuyển theo Y với tốc độ khác nhau tạo hiệu ứng parallax.
     */
    private void drawParallaxBackground() {
        float vpW = Constants.VIEWPORT_WIDTH  / Constants.PPM; // chiều rộng viewport (meters)
        float vpH = Constants.VIEWPORT_HEIGHT / Constants.PPM; // chiều cao viewport (meters)

        float bgH = vpW * (240f / 320f);

        // Góc trái dưới camera
        float camLeft   = camera.position.x - vpW / 2f;
        float camBottom = camera.position.y - vpH / 2f;

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        for (int i = 0; i < bgLayers.length; i++) {
            Texture tex = bgLayers[i];

            // Offset Y theo parallax speed
            float offsetY = camera.position.y * bgScrollSpeeds[i];

            float layerY = camBottom + offsetY;

            int texW = tex.getWidth();  // 320
            int texH = tex.getHeight(); // 240

            float scaleX = texW / (bgH * (320f / 240f));
            int srcWidth = (int)(vpW * scaleX * (1.0f));
            // Cách đơn giản hơn:
            // bgW_meters = bgH * (320/240) — nhưng ta muốn fill vpW nên tile
            // Dùng draw với repeat wrap: chỉ cần set srcWidth > texW

            // Vẽ 1 dải ngang phủ đầy viewport, tile tự động nhờ Repeat wrap
            game.batch.draw(tex, camLeft, layerY, vpW, bgH, 0, 0, (int)(vpW / bgH * texW), texH, false, false);
        }

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        world.dispose();
//        debugRenderer.dispose();
        shapeRenderer.dispose();
        for (Texture t : bgLayers) t.dispose();
    }
}
