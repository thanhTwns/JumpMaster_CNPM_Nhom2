package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
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

    // Ship system — 6 loại phi thuyền, random mỗi lần spawn
    private Array<Body> planets;
    private Texture[] planetTexture;
    private static final int PLANET_COUNT = 6;
    private float stateTime = 0f;
    private float planetSpawnTimer = 0f;

    private static final float PLANET_SPAWN_INTERVAL = 4.0f;

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

        // Load 6 ship textures
        planetTexture = new Texture[PLANET_COUNT];
        for (int i = 0; i < PLANET_COUNT; i++) {
            planetTexture[i] = new Texture(Gdx.files.internal("planet_" + (i + 1) + ".png"));
            planetTexture[i].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
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

                // Player chạm ship → game over
                boolean isA_Monster = (bodyDataA instanceof Object[]) && "planet".equals(((Object[]) bodyDataA)[0]);
                boolean isB_Monster = (bodyDataB instanceof Object[]) && "planet".equals(((Object[]) bodyDataB)[0]);

                if ((isA_Player && isB_Monster) || (isB_Player && isA_Monster)) {
                    triggerGameOver();
                    return;
                }

                // Player tiếp đất → hết stun + UC-3.3 Ghi nhận tiến độ
                Platform platform = null;
                if (isA_Player && bodyDataB instanceof Platform) platform = (Platform) bodyDataB;
                else if (isB_Player && bodyDataA instanceof Platform) platform = (Platform) bodyDataA;

                if ((isA_Player && isB_Ground) || (isB_Player && isA_Ground)) {
                    if (player.body.getLinearVelocity().y <= 0) {
                        Body platformBody = isB_Ground ? contact.getFixtureB().getBody() : contact.getFixtureA().getBody();
                        handleLanding(platformBody);
                if (platform != null) {
                    // Chỉ tính điểm khi đang rơi xuống (velocity âm)
                    if (player.body.getLinearVelocity().y < -0.05f) {
                        player.isStunned = false;
                        handleLanding(platform);
                    }
                }
            }

            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });

        // Khởi tạo ship array
        planets = new Array<>();

        // Mặt đất
        float groundH = 48f;
        float groundW = Constants.VIEWPORT_WIDTH + 200f;
        groundPlatform = new Platform(world,
            Constants.VIEWPORT_WIDTH / 2f, groundH / 2f,
            groundW, groundH, groundTexture);
        platforms.add(groundPlatform);

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
    // EXTRA UPDATE — ship spawn timer
    // -------------------------------------------------------
    @Override
    protected void onExtraUpdate(float delta) {
        stateTime += delta;
        planetSpawnTimer += delta;
        if (planetSpawnTimer >= PLANET_SPAWN_INTERVAL) {
            spawnPlanet();
            planetSpawnTimer = 0f;
        }
    }

    // -------------------------------------------------------
    // EXTRA DRAW — vẽ ships (nằm trong batch.begin/end của base)
    // -------------------------------------------------------
    @Override
    protected void onExtraDraw() {
        Array<Body> toRemove = new Array<>();
        for (Body s : planets) {
            Object[] data = (Object[]) s.getUserData();
            boolean fromLeft = (boolean) data[1];
            Texture tex = (Texture) data[2];

            // Kích thước hiển thị cố định cho ship
            float wS = tex.getWidth() / Constants.PPM;
            float hS = tex.getHeight() / Constants.PPM;
            float drawX = s.getPosition().x - wS / 2f;
            float drawY = s.getPosition().y - hS / 2f;

            // Ship từ phải sang trái thì flip ngang
            game.batch.draw(tex,
                drawX, drawY,
                wS / 2f, hS / 2f,
                wS, hS,
                1f, 1f, 0f,
                0, 0,
                tex.getWidth(), tex.getHeight(),
                fromLeft, false); // fromLeft=true → ship đi sang phải → không flip; fromLeft=false → flip

            float camX = camera.position.x;
            float halfCamW = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
            if (s.getPosition().x > camX + halfCamW + 2f
                || s.getPosition().x < camX - halfCamW - 2f) {
                toRemove.add(s);
            }
        }
        for (Body s : toRemove) {
            world.destroyBody(s);
            planets.removeValue(s, true);
        }
    }

    // -------------------------------------------------------
    // SPAWN SHIP — random 1 trong 6 loại
    // -------------------------------------------------------
    private void spawnPlanet() {
        boolean fromLeft = MathUtils.randomBoolean();
        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
        float halfH = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;

        float spawnY = camY + MathUtils.random(-halfH * 0.8f, halfH * 0.8f);
        float spawnX = fromLeft ? (camX - halfW - 1f) : (camX + halfW + 1f);

        // Random chọn 1 trong 6 ship texture
        Texture chosenTex = planetTexture[MathUtils.random(0, PLANET_COUNT - 1)];

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.KinematicBody;
        bdef.position.set(spawnX, spawnY);
        Body planetBody = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        // Hitbox dựa theo kích thước texture ship
        shape.setRadius((chosenTex.getWidth() / 2f) / Constants.PPM * 0.7f); // 70% để dễ chơi hơn
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.isSensor = true;
        planetBody.createFixture(fdef).setUserData("planet");
        shape.dispose();

        planetBody.setLinearVelocity(fromLeft ? 2.5f : -2.5f, 0);
        // userData: [tag, fromLeft, texture]
        planetBody.setUserData(new Object[]{"planet", fromLeft, chosenTex});
        planets.add(planetBody);
    }

    // -------------------------------------------------------
    // EXTRA DISPOSE — background + ship textures
    // -------------------------------------------------------
    @Override
    protected void onExtraDispose() {
        if (bgSpace != null) bgSpace.dispose();
        if (bgPlanet != null) bgPlanet.dispose();
        if (planetTexture != null)
            for (Texture t : planetTexture)
                if (t != null) t.dispose();
    }
}
