package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.utils.Constants;

/**
 * GameScreen — Màn 1.
 */
public class EarthScreen extends BaseScreen {

    private Texture[] bgLayers;
    private float[] bgScrollSpeeds;
    private float levelClearY = Float.MAX_VALUE;
    private boolean levelCleared = false;
    private final String mode;

    public EarthScreen(JumpMasterGame game, String mode) {
        super(game);
        this.mode = mode;
    }

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

    @Override
    protected void initPlatforms() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Object bodyDataA = contact.getFixtureA().getBody().getUserData();
                Object bodyDataB = contact.getFixtureB().getBody().getUserData();

                boolean isA_Player = "player".equals(bodyDataA);
                boolean isB_Player = "player".equals(bodyDataB);

                Platform platform = null;
                if (isA_Player && bodyDataB instanceof Platform) platform = (Platform) bodyDataB;
                else if (isB_Player && bodyDataA instanceof Platform) platform = (Platform) bodyDataA;

                if (platform != null) {
                    if (player.body.getLinearVelocity().y <= 0.1f) {
                        handleLanding(platform);
                    }
                }
            }
            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        float groundHeight = 48f;
        float groundWidth = Constants.VIEWPORT_WIDTH + 200f;
        platforms.add(new Platform(world,
            Constants.VIEWPORT_WIDTH / 2f, groundHeight / 2f,
            groundWidth, groundHeight, groundTexture));

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

            // Chỉ tạo Platform bình thường
            platforms.add(new Platform(world, randomX, currentY, platformWidth, stepHeight, stepTexture));

            if (i == 9) levelClearY = (currentY + stepHeight) / Constants.PPM;
            currentY += MathUtils.random(120f, 160f);
        }
    }

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

    @Override
    protected void restartGame() {
        levelCleared = false;
        super.restartGame();
    }

    @Override
    protected void onExtraDispose() {
        if (bgLayers != null) for (Texture t : bgLayers) t.dispose();
    }
}
