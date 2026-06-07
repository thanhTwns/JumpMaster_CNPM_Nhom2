package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
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

    // Bat (dơi) system
    private Array<Body> bats;
    private Texture batSheet;
    private Animation<TextureRegion> batAnimation;
    private float batStateTime = 0f;
    private float batSpawnTimer = 0f;
    private static final float BAT_SPAWN_INTERVAL = 4.0f;
    private static final int BAT_FRAME_COLS = 6;
    private static final int BAT_FRAME_ROWS = 1;

    public EarthScreen(JumpMasterGame game, String mode) {
        super(game);
        this.mode = mode;
        if ("challenge".equals(mode)) {
            jumpCount = 0;
        }
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
                Object dataA = contact.getFixtureA().getUserData();
                Object dataB = contact.getFixtureB().getUserData();
                Object bodyDataA = contact.getFixtureA().getBody().getUserData();
                Object bodyDataB = contact.getFixtureB().getBody().getUserData();

                boolean isA_Player = "player".equals(dataA) || "player".equals(bodyDataA);
                boolean isB_Player = "player".equals(dataB) || "player".equals(bodyDataB);

                // Player chạm dơi → game over
                boolean isA_Bat = (bodyDataA instanceof Object[]) && "bat".equals(((Object[]) bodyDataA)[0]);
                boolean isB_Bat = (bodyDataB instanceof Object[]) && "bat".equals(((Object[]) bodyDataB)[0]);
                if ((isA_Player && isB_Bat) || (isB_Player && isA_Bat)) {
                    triggerGameOver();
                    return;
                }

                // Player tiếp đất
                Platform platform = null;
                if (isA_Player && bodyDataB instanceof Platform) platform = (Platform) bodyDataB;
                else if (isB_Player && bodyDataA instanceof Platform) platform = (Platform) bodyDataA;

                if (platform != null) {
                    if (player.body.getLinearVelocity().y < -0.05f) {
                        handleLanding(platform);
                    }
                }
            }
            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        // Xóa bats cũ nếu có
        if (bats != null) {
            for (Body b : bats) {
                world.destroyBody(b);
            }
            bats.clear();
        } else {
            bats = new Array<>();
        }

        // Khởi tạo bat animation chỉ 1 lần duy nhất
        if (batSheet == null) {
            batSheet = new Texture(Gdx.files.internal("flying-head.png"));
            int frameW = batSheet.getWidth() / BAT_FRAME_COLS;
            int frameH = batSheet.getHeight() / BAT_FRAME_ROWS;
            TextureRegion[][] tmp = TextureRegion.split(batSheet, frameW, frameH);
            Array<TextureRegion> frames = new Array<>();
            for (int i = 0; i < BAT_FRAME_ROWS; i++)
                for (int j = 0; j < BAT_FRAME_COLS; j++)
                    frames.add(tmp[i][j]);
            batAnimation = new Animation<>(0.1f, frames);
        }

        float groundHeight = 48f;
        float groundWidth = Constants.VIEWPORT_WIDTH + 200f;
        groundPlatform = new Platform(world,
            Constants.VIEWPORT_WIDTH / 2f, groundHeight / 2f,
            groundWidth, groundHeight, groundTexture);
        platforms.add(groundPlatform);

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

    //2.3.1.1 Khi người dùng qua màn
    @Override
    protected void onLevelComplete() {
        if (levelCleared) return;
        levelCleared = true;
        scoreManager.flush(); // Lưu điểm trước khi chuyển màn
        final int carryScore = scoreManager.getCurrentScore();
        final int carryColumns = scoreManager.getColumnsPassed();
        Gdx.app.postRunnable(() -> game.setScreen(new SpaceScreen(game, mode, carryScore, carryColumns)));
    }

    @Override
    protected void restartGame() {
        levelCleared = false;
        super.restartGame();
    }

    @Override
    protected void clearPlatforms() {
        super.clearPlatforms();
        if (bats != null) {
            for (Body b : bats) {
                world.destroyBody(b);
            }
            bats.clear();
        }
    }

    // -------------------------------------------------------
    // BAT — update spawn timer
    // -------------------------------------------------------
    @Override
    protected void onExtraUpdate(float delta) {
        batStateTime += delta;
        batSpawnTimer += delta;
        if (batSpawnTimer >= BAT_SPAWN_INTERVAL) {
            spawnBat();
            batSpawnTimer = 0f;
        }
    }

    // -------------------------------------------------------
    // BAT — vẽ dơi
    // -------------------------------------------------------
    @Override
    protected void onExtraDraw() {
        Array<Body> toRemove = new Array<>();
        for (Body b : bats) {
            Object[] data = (Object[]) b.getUserData();
            boolean facingRight = (boolean) data[1];

            TextureRegion frame = batAnimation.getKeyFrame(batStateTime, true);
            float wB = frame.getRegionWidth() / Constants.PPM;
            float hB = frame.getRegionHeight() / Constants.PPM;
            float drawX = b.getPosition().x - wB / 2f;
            float drawY = b.getPosition().y - hB / 2f;

            game.batch.draw(frame.getTexture(),
                drawX, drawY,
                wB / 2f, hB / 2f,
                wB, hB,
                1f, 1f, 0f,
                frame.getRegionX(), frame.getRegionY(),
                frame.getRegionWidth(), frame.getRegionHeight(),
                !facingRight, false);

            float camX = camera.position.x;
            float halfCamW = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
            if (b.getPosition().x > camX + halfCamW + 2f
                || b.getPosition().x < camX - halfCamW - 2f) {
                toRemove.add(b);
            }
        }
        for (Body b : toRemove) {
            world.destroyBody(b);
            bats.removeValue(b, true);
        }
    }

    // -------------------------------------------------------
    // BAT — spawn dơi từ trái hoặc phải
    // -------------------------------------------------------
    private void spawnBat() {
        boolean fromLeft = MathUtils.randomBoolean();
        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
        float halfH = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;

        float spawnY = camY + MathUtils.random(-halfH * 0.8f, halfH * 0.8f);
        float spawnX = fromLeft ? (camX - halfW - 1f) : (camX + halfW + 1f);

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.KinematicBody;
        bdef.position.set(spawnX, spawnY);
        Body batBody = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(15f / Constants.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.isSensor = true;
        batBody.createFixture(fdef).setUserData("bat");
        shape.dispose();

        batBody.setLinearVelocity(fromLeft ? 2f : -2f, 0);
        batBody.setUserData(new Object[]{"bat", fromLeft});
        bats.add(batBody);
    }

    @Override
    protected void onExtraDispose() {
        if (bgLayers != null) for (Texture t : bgLayers) t.dispose();
        if (batSheet != null) batSheet.dispose();
    }
    public String getMode() {
        return mode;
    }
}
