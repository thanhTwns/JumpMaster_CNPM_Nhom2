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
 * SpaceScreen — Màn 2.
 * Extends BaseScreen, chỉ override những gì KHÁC với base:
 * - Background space + planet (2 layer)
 * - 20 bậc nhảy
 * - Monster system (spawn, animate, collision)
 * - ContactListener (player-monster, player-ground)
 * - Không có chuyển màn tiếp (getLevelClearY = MAX_VALUE)
 */
public class SpaceScreen extends BaseScreen {

    // Background riêng của màn 2
    private Texture bgSpace;
    private Texture bgPlanet;

    // Monster
    private Array<Body> monsters;
    private Texture monsterSheet;
    private Animation<TextureRegion> monsterAnimation;
    private float stateTime = 0f;
    private float monsterSpawnTimer = 0f;

    private static final float MONSTER_SPAWN_INTERVAL = 4.0f;
    private static final int FRAME_COLS = 6;
    private static final int FRAME_ROWS = 1;

    public SpaceScreen(JumpMasterGame game) {
        super(game);
    }

    // -------------------------------------------------------
    // INIT BACKGROUND — space + planet
    // -------------------------------------------------------
    @Override
    protected void initBackground() {
        bgSpace = new Texture("space.png");
        bgPlanet = new Texture("planet.png");
        bgSpace.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        bgPlanet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    // -------------------------------------------------------
    // INIT PLATFORMS — 20 bậc + monster sheet + contact listener
    // -------------------------------------------------------
    @Override
    protected void initPlatforms() {
        // ContactListener — player-monster và player-ground
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Object dataA = contact.getFixtureA().getUserData();
                Object dataB = contact.getFixtureB().getUserData();
                Object bodyDataA = contact.getFixtureA().getBody().getUserData();
                Object bodyDataB = contact.getFixtureB().getBody().getUserData();

                boolean isA_Player = "player".equals(dataA) || "player".equals(bodyDataA);
                boolean isB_Player = "player".equals(dataB) || "player".equals(bodyDataB);

                // Player chạm monster → game over
                boolean isA_Monster = (bodyDataA instanceof Object[]) && "monster".equals(((Object[]) bodyDataA)[0]);
                boolean isB_Monster = (bodyDataB instanceof Object[]) && "monster".equals(((Object[]) bodyDataB)[0]);

                if ((isA_Player && isB_Monster) || (isB_Player && isA_Monster)) {
                    triggerGameOver();
                    return;
                }

                // Player tiếp đất → hết stun + UC-3.3 Ghi nhận tiến độ
                Platform platform = null;
                if (isA_Player && bodyDataB instanceof Platform) platform = (Platform) bodyDataB;
                else if (isB_Player && bodyDataA instanceof Platform) platform = (Platform) bodyDataA;

                if (platform != null) {
                    if (player.body.getLinearVelocity().y <= 0.1f) {
                        player.isStunned = false;
                        handleLanding(platform);
                    }
                }
            }

            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        // Monster animation sheet
        monsters = new Array<>();
        monsterSheet = new Texture(Gdx.files.internal("flying-head.png"));

        int frameWidth = monsterSheet.getWidth() / FRAME_COLS;
        int frameHeight = monsterSheet.getHeight() / FRAME_ROWS;

        TextureRegion[][] tmp = TextureRegion.split(monsterSheet, frameWidth, frameHeight);
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < FRAME_ROWS; i++)
            for (int j = 0; j < FRAME_COLS; j++)
                frames.add(tmp[i][j]);

        monsterAnimation = new Animation<>(0.1f, frames);

        // Mặt đất
        float groundH = 48f;
        float groundW = Constants.VIEWPORT_WIDTH + 200f;
        platforms.add(new Platform(world,
            Constants.VIEWPORT_WIDTH / 2f, groundH / 2f,
            groundW, groundH, groundTexture));

        // 20 bậc xen kẽ trái/phải
        float stepHeight = 16f;
        float currentY = 150f;
        boolean leftSide = true;

        for (int i = 0; i < 20; i++) {
            float platW = MathUtils.random(100f, 180f);
            float randomX;
            if (leftSide) {
                randomX = MathUtils.random(platW / 2f + 10f, Constants.VIEWPORT_WIDTH / 2f - 20f);
            } else {
                randomX = MathUtils.random(Constants.VIEWPORT_WIDTH / 2f + 20f, Constants.VIEWPORT_WIDTH - platW / 2f - 10f);
            }
            leftSide = !leftSide;

            // Đã loại bỏ Platform.Type, chỉ tạo platform bình thường
            platforms.add(new Platform(world, randomX, currentY, platW, stepHeight, stepTexture));
            currentY += MathUtils.random(120f, 160f);
        }
    }

    // -------------------------------------------------------
    // DRAW BACKGROUND — space tile dọc + planet cố định
    // -------------------------------------------------------
    @Override
    protected void drawBackground() {
        float vpW = camera.viewportWidth;
        float vpH = camera.viewportHeight;
        float camLeft = camera.position.x - vpW / 2f;
        float camBot = camera.position.y - vpH / 2f;

        float bgW = vpW;
        float bgH = bgW * (1024f / 576f);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Layer 1: space — tile dọc, scroll chậm
        float offsetY_space = smoothCamY * 0.05f;
        float tileStartY = (float) Math.floor((camBot - offsetY_space) / bgH) * bgH + offsetY_space;
        int tilesY = (int) Math.ceil(vpH / bgH) + 2;
        for (int row = 0; row < tilesY; row++)
            game.batch.draw(bgSpace, camLeft, tileStartY + row * bgH, bgW, bgH);

        // Layer 2: planet — scroll nhanh hơn, không tile
        float offsetY_planet = smoothCamY * 0.15f;
        game.batch.draw(bgPlanet, camLeft, camBot + offsetY_planet, bgW, bgH);

        game.batch.end();
    }

    // -------------------------------------------------------
    // LEVEL CLEAR — màn 2 không có màn tiếp
    // -------------------------------------------------------
    @Override
    protected float getLevelClearY() {
        return Float.MAX_VALUE; // không có chuyển màn
    }

    @Override
    protected void onLevelComplete() {
        // Không làm gì — màn 2 là màn cuối
    }

    // -------------------------------------------------------
    // EXTRA UPDATE — monster spawn
    // -------------------------------------------------------
    @Override
    protected void onExtraUpdate(float delta) {
        stateTime += delta;
        monsterSpawnTimer += delta;
        if (monsterSpawnTimer >= MONSTER_SPAWN_INTERVAL) {
            spawnMonster();
            monsterSpawnTimer = 0f;
        }
    }

    // -------------------------------------------------------
    // EXTRA DRAW — vẽ monsters (phải nằm trong batch.begin/end của base)
    // -------------------------------------------------------
    @Override
    protected void onExtraDraw() {
        for (int i = 0; i < monsters.size; i++) {
            Body m = monsters.get(i);
            Object[] data = (Object[]) m.getUserData();
            boolean facingRight = (boolean) data[1];

            TextureRegion frame = monsterAnimation.getKeyFrame(stateTime, true);
            float wM = frame.getRegionWidth() / Constants.PPM;
            float hM = frame.getRegionHeight() / Constants.PPM;
            float drawX = m.getPosition().x - wM / 2f;
            float drawY = m.getPosition().y - hM / 2f;

            game.batch.draw(frame.getTexture(),
                drawX, drawY,
                wM / 2f, hM / 2f,
                wM, hM,
                1f, 1f, 0f,
                frame.getRegionX(), frame.getRegionY(),
                frame.getRegionWidth(), frame.getRegionHeight(),
                !facingRight, false);

            // Xóa monster nếu ra ngoài camera
            float camX = camera.position.x;
            float halfCamW = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
            if (m.getPosition().x > camX + halfCamW + 2f
                || m.getPosition().x < camX - halfCamW - 2f) {
                world.destroyBody(m);
                monsters.removeIndex(i);
                i--;
            }
        }
    }

    // -------------------------------------------------------
    // SPAWN MONSTER
    // -------------------------------------------------------
    private void spawnMonster() {
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
        Body monsterBody = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(15f / Constants.PPM);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.isSensor = true;
        monsterBody.createFixture(fdef).setUserData("monster");
        shape.dispose();

        monsterBody.setLinearVelocity(fromLeft ? 2f : -2f, 0);
        monsterBody.setUserData(new Object[]{"monster", fromLeft});
        monsters.add(monsterBody);
    }

    // -------------------------------------------------------
    // EXTRA DISPOSE — background + monster sheet
    // -------------------------------------------------------
    @Override
    protected void onExtraDispose() {
        if (bgSpace != null) bgSpace.dispose();
        if (bgPlanet != null) bgPlanet.dispose();
        if (monsterSheet != null) monsterSheet.dispose();
    }
}
