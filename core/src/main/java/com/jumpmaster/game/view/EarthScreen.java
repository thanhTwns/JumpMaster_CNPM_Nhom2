package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.utils.Constants;

/**
 * GameScreen — Màn 1.
 * Extends BaseScreen, chỉ override những gì KHÁC với base:
 * - Background parallax 6 layer
 * - 10 bậc nhảy xen kẽ trái/phải
 * - Chuyển sang SpaceScreen khi đạt bậc thứ 10
 */
public class EarthScreen extends BaseScreen {

    // Background parallax riêng của màn 1
    private Texture[] bgLayers;
    private float[] bgScrollSpeeds;

    // Ngưỡng chuyển màn
    private float levelClearY = Float.MAX_VALUE;
    private boolean levelCleared = false;

    private final String mode;

    public EarthScreen(JumpMasterGame game, String mode) {
        super(game);
        this.mode = mode;
    }

    // -------------------------------------------------------
    // INIT BACKGROUND — 6 layer parallax
    // -------------------------------------------------------
    @Override
    protected void initBackground() {
        bgLayers = new Texture[]{
            new Texture("sky.png"),
            new Texture("far-mountains.png"),
            new Texture("far-clouds.png"),
            new Texture("mountains.png"),
            new Texture("near-clouds.png"),
            new Texture("trees.png"),
        };
        bgScrollSpeeds = new float[]{0.0f, 0.05f, 0.08f, 0.15f, 0.2f, 0.35f};
        for (Texture t : bgLayers)
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    // -------------------------------------------------------
    // INIT PLATFORMS — 10 bậc xen kẽ trái/phải
    // -------------------------------------------------------
    @Override
    protected void initPlatforms() {
        // Mặt đất
        float groundHeight = 48f;
        float groundWidth = Constants.VIEWPORT_WIDTH + 200f;
        platforms.add(new Platform(world,
            Constants.VIEWPORT_WIDTH / 2f, groundHeight / 2f,
            groundWidth, groundHeight, groundTexture));

        // 10 bậc
        float stepHeight = 16f;
        float currentY = 150f;
        boolean leftSide = true;

        for (int i = 0; i < 10; i++) {
            float platformWidth = MathUtils.random(100f, 180f);
            float randomX;
            if (leftSide) {
                randomX = MathUtils.random(platformWidth / 2f + 10f, Constants.VIEWPORT_WIDTH / 2f - 20f);
            } else {
                randomX = MathUtils.random(Constants.VIEWPORT_WIDTH / 2f + 20f, Constants.VIEWPORT_WIDTH - platformWidth / 2f - 10f);
            }
            leftSide = !leftSide;

            platforms.add(new Platform(world, randomX, currentY, platformWidth, stepHeight, stepTexture));

            // Bậc thứ 10 → lưu ngưỡng chuyển màn
            if (i == 9) levelClearY = (currentY + stepHeight) / Constants.PPM;

            currentY += MathUtils.random(120f, 160f);
        }
    }

    // -------------------------------------------------------
    // DRAW BACKGROUND — parallax 6 layer
    // -------------------------------------------------------
    @Override
    protected void drawBackground() {
        float vpW = Constants.VIEWPORT_WIDTH / Constants.PPM;
        float vpH = Constants.VIEWPORT_HEIGHT / Constants.PPM;
        float bgH = vpW * (240f / 320f);

        float camLeft = camera.position.x - vpW / 2f;
        float camBottom = camera.position.y - vpH / 2f;

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        for (int i = 0; i < bgLayers.length; i++) {
            Texture tex = bgLayers[i];
            float startY = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;
            float offsetY = (smoothCamY - startY) * bgScrollSpeeds[i];
            int texW = tex.getWidth(), texH = tex.getHeight();
            int srcWidth = (int) (vpW / bgH * texW);
            int srcHeight = (int) (vpH / bgH * texH);
            int srcY = (int) (offsetY * (texH / bgH));
            game.batch.draw(tex, camLeft, camBottom, vpW, vpH, 0, -srcY, srcWidth, srcHeight, false, false);
        }

        game.batch.end();
    }

    // -------------------------------------------------------
    // LEVEL CLEAR — chuyển sang SpaceScreen
    // -------------------------------------------------------
    @Override
    protected float getLevelClearY() {
        return levelCleared ? Float.MAX_VALUE : levelClearY;
    }

    @Override
    protected void onLevelComplete() {
        if (levelCleared) return;
        levelCleared = true;
        Gdx.app.postRunnable(() -> game.setScreen(new SpaceScreen(game)));
    }

    // -------------------------------------------------------
    // RESTART — thêm reset levelCleared
    // -------------------------------------------------------
    @Override
    protected void restartGame() {
        levelCleared = false;
        super.restartGame();
    }

    // -------------------------------------------------------
    // DISPOSE — giải phóng background riêng
    // -------------------------------------------------------
    @Override
    protected void onExtraDispose() {
        if (bgLayers != null) for (Texture t : bgLayers) t.dispose();
    }
}
