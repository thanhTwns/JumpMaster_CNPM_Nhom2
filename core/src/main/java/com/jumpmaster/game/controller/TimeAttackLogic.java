package com.jumpmaster.game.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.utils.Constants;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class TimeAttackLogic implements Disposable {

    // ── Constants ──────────────────────────────────────────────────────────
    public  static final float PLAYER_MAX_HEALTH  = 100f;
    private static final float BAT_DAMAGE         = 10f;
    private static final float BAT_HIT_COOLDOWN   = 1.2f;   // FIX: khôi phục constant bị mất
    private static final float POTION_HEAL_AMOUNT = 25f;
    private static final float POTION_RADIUS      = 16f;
    private static final float VORTEX_RADIUS      = 40f;
    private static final float BAT_ANIMATION_FPS  = 8f;
    private static final int   POTIONS_PER_LEVEL  = 8;
    private static final int   BAT_SPAWN_EVERY_N_PLATFORMS = 2;

    private static final float BAT_FLY_SPEED_MIN  = 1.5f;
    private static final float BAT_FLY_SPEED_MAX  = 2.5f;
    private static final float BAT_SPAWN_INTERVAL = 3.5f;
    private static final int   BAT_WAVE_MIN       = 3;
    private static final int   BAT_WAVE_MAX       = 6;

    private static final float BAT_SPEED_BASE = 120f;

    // ── External references ────────────────────────────────────────────────
    private final World           world;
    private       Array<Platform> platforms;
    private Player          player;
    private final LevelListener   listener;

    // ── Assets ────────────────────────────────────────────────────────────
    private final Texture[] batFrames;
    private final Texture   vortexTexture;
    private final Texture   potionTexture;

    // ── State ─────────────────────────────────────────────────────────────
    private float playerHealth = PLAYER_MAX_HEALTH;
    private int   currentLevel = 0;
    private float topPlatformY;

    // ── Entities ──────────────────────────────────────────────────────────
    private final Array<BatMonster>   bats    = new Array<>();
    private final Array<HealthPotion> potions = new Array<>();
    private       VortexPortal        vortex  = null;

    // ── Timers ────────────────────────────────────────────────────────────
    // FIX: 2 cooldown riêng — không còn hitCooldownTimer chung nữa
    private float platformBatCooldown = 0f;
    private float flyingBatCooldown   = 0f;

    private final Array<Body> flyingBats   = new Array<>();
    private float             batSpawnTimer = 0f;
    private static final float BAT_SPEED       = 0.7f;
    private static final float PLATFORM_STEP_H = 140f;
    private int batSpawnIndex = 0;

    // ──────────────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────────────

    public TimeAttackLogic(
        World           world,
        Array<Platform> platforms,
        Player          player,
        Texture[]       batFrames,
        Texture         vortexTexture,
        Texture         potionTexture,
        float           topPlatformY,
        LevelListener   listener
    ) {
        this.world         = world;
        this.platforms     = platforms;
        this.player        = player;
        this.batFrames     = (batFrames != null) ? batFrames : new Texture[0];
        this.vortexTexture = vortexTexture;
        this.potionTexture = potionTexture;
        this.topPlatformY  = topPlatformY;
        this.listener      = listener;
    }

    public TimeAttackLogic(
        World           world,
        Array<Platform> platforms,
        Player          player,
        Texture         animalTexture,
        Texture         vortexTexture,
        LevelListener   listener
    ) {
        this(world, platforms, player,
            animalTexture != null ? new Texture[]{ animalTexture } : new Texture[0],
            vortexTexture, null, 0f, listener);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────

    public void onNewLevel(Array<Platform> newPlatforms, float newTopPlatformY) {
        this.platforms    = newPlatforms;
        this.topPlatformY = newTopPlatformY;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
    public void initLevel(int level) {
        Gdx.app.log("TALogic", "initLevel called, level=" + level);
        this.currentLevel = level;
        playerHealth = PLAYER_MAX_HEALTH;
        bats.clear();
        potions.clear();
        // FIX: destroy Box2D bodies trước khi clear để tránh memory leak
        for (Body b : flyingBats) world.destroyBody(b);
        flyingBats.clear();
        vortex = null;
        platformBatCooldown = 0f;
        flyingBatCooldown   = 0f;
        batSpawnIndex = 0;
        batSpawnTimer       = 0f;

        int stepCount = platforms.size - 1;
        if (stepCount < 1) return;

        Platform topPlatform = platforms.get(platforms.size - 1);
        float topPlatformSurfacePx =
            (topPlatform.getY() + topPlatform.getHeight() / 2f) * Constants.PPM;
        vortex = new VortexPortal(topPlatformSurfacePx);

        // Spawn 1 quái / 2 bậc, tốc độ tăng dần theo độ cao
        int totalSteps = platforms.size - 2;
        for (int i = 1; i < platforms.size - 1; i++) {
            if ((i % BAT_SPAWN_EVERY_N_PLATFORMS) != 0) continue;
            Platform p = platforms.get(i);
            float stepRatio = (float)(i - 1) / Math.max(totalSteps - 1, 1);
            float speed = BAT_SPEED_BASE + stepRatio * 200f + (level - 1) * 40f;
            bats.add(new BatMonster(p, speed));
        }

        // Potions — shuffle toàn bộ step platforms
        Array<Integer> potionCandidates = new Array<>();
        for (int i = 1; i < platforms.size - 1; i++) potionCandidates.add(i);
        potionCandidates.shuffle();
        int potionCount = Math.min(POTIONS_PER_LEVEL + (level - 1), potionCandidates.size);
        for (int i = 0; i < potionCount; i++)
            potions.add(new HealthPotion(platforms.get(potionCandidates.get(i))));

        Gdx.app.log("TimeAttack", "Level " + level
            + " | bats=" + bats.size
            + " | potions=" + potions.size
            + " | vortexY=" + vortex.y);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Per-frame update
    // ──────────────────────────────────────────────────────────────────────

    public void update(float delta) {
        Gdx.app.log("TALogic", "update called");
        platformBatCooldown = Math.max(0f, platformBatCooldown - delta);
        flyingBatCooldown   = Math.max(0f, flyingBatCooldown   - delta);

        if (player == null || player.body == null) return;

        batSpawnTimer -= delta;
        if (batSpawnTimer <= 0f) {
            spawnBatWave();
            batSpawnTimer = BAT_SPAWN_INTERVAL;
        }
        updateFlyingBats(delta);

        float playerX = player.body.getPosition().x * Constants.PPM;
        float playerY = player.body.getPosition().y * Constants.PPM;
        Gdx.app.log("PlayerPos", "player px=(" + (int)playerX + "," + (int)playerY + ")");
        float playerHitW = 40f;
        float playerHitH = 48f;
        Rectangle playerRect = new Rectangle(
            playerX - playerHitW / 2f,
            playerY - playerHitH / 2f,
            playerHitW,
            playerHitH);

        // ── Platform bats ─────────────────────────────────────────────────
        for (BatMonster bat : bats) {
            bat.update(delta);

            // DEBUG tạm — xóa sau khi xác nhận
            float debugDx = bat.x - playerX;
            float debugDy = bat.y - playerY;
            if (Math.abs(debugDx) < 200f && Math.abs(debugDy) < 200f) {
                Gdx.app.log("CollisionDebug",
                    "bat=(" + (int)bat.x + "," + (int)bat.y + ")"
                        + " player=(" + (int)playerX + "," + (int)playerY + ")"
                        + " cooldown=" + platformBatCooldown);
            }

            if (platformBatCooldown <= 0f && bat.getBounds().overlaps(playerRect)) {
                playerHealth -= BAT_DAMAGE;
                platformBatCooldown = BAT_HIT_COOLDOWN;
                Gdx.app.log("TimeAttack", "PlatformBat HIT! HP=" + playerHealth);
                if (playerHealth <= 0f) {
                    playerHealth = 0f;
                    listener.onPlayerDied();
                    return;
                }
            }
        }

        // ── Potions ───────────────────────────────────────────────────────
        for (int i = potions.size - 1; i >= 0; i--) {
            HealthPotion p = potions.get(i);
            float dx = p.x - playerX;
            float dy = p.y - playerY;
            if (Math.sqrt(dx * dx + dy * dy) < POTION_RADIUS + 16f) {
                playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + POTION_HEAL_AMOUNT);
                potions.removeIndex(i);
                Gdx.app.log("TimeAttack", "Potion collected! HP=" + playerHealth);
            }
        }

        // ── Vortex ────────────────────────────────────────────────────────
        if (vortex != null) {
            vortex.update(delta);
            float dx = vortex.x - playerX;
            float dy = vortex.y - playerY;
            if (Math.sqrt(dx * dx + dy * dy) < VORTEX_RADIUS + 16f)
                listener.onLevelComplete(currentLevel);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Flying bats
    // ──────────────────────────────────────────────────────────────────────

    private void spawnBatWave() {  // giữ tên để không đổi chỗ gọi
        float camX  = player.body.getPosition().x;
        float camY  = player.body.getPosition().y;
        float halfW = (Constants.VIEWPORT_WIDTH  / Constants.PPM) / 2f;  // 3.6f
        float halfH = (Constants.VIEWPORT_HEIGHT / Constants.PPM) / 2f;  // 2.025f
        float twoSteps = 2.8f;
        boolean fromLeft = (batSpawnIndex % 2 == 0);  // xen kẽ trái/phải
        float totalRange = halfH * 2f + twoSteps;           // 6.075f metres
        float slot       = totalRange / 6f;      // = 1.0125f mỗi slot
        float baseY      = camY + slot * (batSpawnIndex % 6);
        float jitter     = MathUtils.random(-slot * 0.2f, slot * 0.2f);
        float spawnY     = baseY + jitter;

        float spawnX = fromLeft ? (camX - halfW - 1f) : (camX + halfW + 1f);

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.KinematicBody;
        bdef.position.set(spawnX, spawnY);
        Body body = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(15f / Constants.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape    = shape;
        fdef.isSensor = true;
        body.createFixture(fdef).setUserData("flyingBat");
        shape.dispose();

        float speed = MathUtils.random(BAT_FLY_SPEED_MIN, BAT_FLY_SPEED_MAX);
        body.setLinearVelocity(fromLeft ? speed : -speed, 0f);
        body.setUserData(new Object[]{ "flyingBat", fromLeft });
        flyingBats.add(body);

        batSpawnIndex++;  // tăng index để lần sau Y và hướng khác
        Gdx.app.log("TimeAttack", "Spawned bat #" + batSpawnIndex
            + " spawnY=" + spawnY + " fromLeft=" + fromLeft);
    }

    private void updateFlyingBats(float delta) {
        float camX    = player.body.getPosition().x;
        float halfW   = (Constants.VIEWPORT_WIDTH / Constants.PPM) / 2f;
        float playerX = player.body.getPosition().x * Constants.PPM;
        float playerY = player.body.getPosition().y * Constants.PPM;
        Rectangle playerRect = new Rectangle(playerX - 16, playerY - 16, 32, 32);
        Array<Body> toRemove = new Array<>();

        for (Body b : flyingBats) {
            float bx = b.getPosition().x;
            float by = b.getPosition().y;

            if (bx > camX + halfW + 2f || bx < camX - halfW - 2f) {
                toRemove.add(b);
                continue;
            }

            Rectangle batRect = new Rectangle(
                bx * Constants.PPM - 15f,
                by * Constants.PPM - 15f,
                30f, 30f);

            // FIX: dùng flyingBatCooldown thay vì hitCooldownTimer (đã xóa)
            if (flyingBatCooldown <= 0f && batRect.overlaps(playerRect)) {
                playerHealth -= BAT_DAMAGE;
                flyingBatCooldown = BAT_HIT_COOLDOWN;
                Gdx.app.log("TimeAttack", "FlyingBat hit! HP=" + playerHealth);
                if (playerHealth <= 0f) {
                    playerHealth = 0f;
                    listener.onPlayerDied();
                    return;
                }
            }
        }

        for (Body b : toRemove) {
            world.destroyBody(b);
            flyingBats.removeValue(b, true);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Draw
    // ──────────────────────────────────────────────────────────────────────

    public void drawEntities(SpriteBatch batch) {
        float ppm = Constants.PPM;

        if (batFrames.length > 0) {
            for (BatMonster bat : bats) {
                Texture frame = batFrames[bat.currentFrame % batFrames.length];
                float w = 48f / ppm, h = 48f / ppm;
                batch.draw(frame,
                    (bat.x - 24f) / ppm, (bat.y - 24f) / ppm,
                    w, h,
                    0, 0, frame.getWidth(), frame.getHeight(),
                    bat.facingLeft, false);
            }
        }

        if (potionTexture != null) {
            float r = POTION_RADIUS / ppm;
            for (HealthPotion p : potions)
                batch.draw(potionTexture,
                    (p.x - POTION_RADIUS) / ppm,
                    (p.y - POTION_RADIUS) / ppm,
                    r * 2f, r * 2f);
        }

        if (vortex != null && vortexTexture != null) {
            float r = VORTEX_RADIUS / ppm;
            batch.draw(vortexTexture,
                (vortex.x - VORTEX_RADIUS) / ppm,
                (vortex.y - VORTEX_RADIUS) / ppm,
                r * 2f, r * 2f);
        }
    }

    public void drawFallbacks(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        if (batFrames.length == 0) {
            sr.setColor(Color.MAGENTA);
            for (BatMonster bat : bats)
                sr.rect(bat.x - 16f, bat.y - 12f, 32f, 24f);
        }

        if (potionTexture == null) {
            sr.setColor(Color.GREEN);
            for (HealthPotion p : potions)
                sr.circle(p.x, p.y, POTION_RADIUS, 12);
        }

        if (vortex != null && vortexTexture == null) {
            float pulse = VORTEX_RADIUS + 6f * MathUtils.sin(vortex.animTimer * 4f);
            sr.setColor(0f, 0.9f, 1f, 1f);
            sr.circle(vortex.x, vortex.y, pulse, 24);
        }

        sr.end();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Accessors
    // ──────────────────────────────────────────────────────────────────────

    public float      getPlayerHealth() { return playerHealth; }
    public int        getCurrentLevel() { return currentLevel; }
    public Array<Body> getFlyingBats()  { return flyingBats;   }

    // ──────────────────────────────────────────────────────────────────────
    //  Listener
    // ──────────────────────────────────────────────────────────────────────

    public interface LevelListener {
        void onLevelComplete(int completedLevel);
        void onPlayerDied();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Inner classes
    // ──────────────────────────────────────────────────────────────────────

    private static class BatMonster {
        float x, y;
        float patrolLeft, patrolRight;
        float speed;
        boolean facingLeft = false;
        int   currentFrame = 0;
        float frameTimer   = 0f;

        BatMonster(Platform platform, float speed) {
            float cx = platform.getX() * Constants.PPM;
            float cy = platform.getY() * Constants.PPM;
            float hw = (platform.getWidth() * Constants.PPM) / 2f;

            float screenHalfW = Constants.VIEWPORT_WIDTH / 2f;
            float margin = Math.min(8f, hw * 0.1f);
            patrolLeft  = cx - screenHalfW;
            patrolRight = cx + screenHalfW;

            x = cx;
            y = cy + 24f;
            this.speed = speed;
            // Thêm tạm vào BatMonster constructor để debug:
            Gdx.app.log("BatDebug", "cx=" + cx + " cy=" + cy + " hw=" + hw
                + " patrol=[" + patrolLeft + "," + patrolRight + "]"
                + " screenW=" + Constants.VIEWPORT_WIDTH);
        }

        void update(float delta) {
            x += facingLeft ? -speed * delta : speed * delta;
            if (x >= patrolRight) { x = patrolRight; facingLeft = true;  }
            if (x <= patrolLeft)  { x = patrolLeft;  facingLeft = false; }

            frameTimer += delta;
            if (frameTimer >= 1f / BAT_ANIMATION_FPS) {
                frameTimer -= 1f / BAT_ANIMATION_FPS;
                currentFrame++;
            }
        }

        Rectangle getBounds() {
            return new Rectangle(x - 16f, y - 12f, 32f, 24f);
        }
    }

    private static class HealthPotion {
        float x, y;

        HealthPotion(Platform platform) {
            float cx = platform.getX() * Constants.PPM;
            float cy = platform.getY() * Constants.PPM;
            float hw = (platform.getWidth() * Constants.PPM) / 2f;
            x = MathUtils.random(cx - hw * 0.6f, cx + hw * 0.6f);
            y = cy + POTION_RADIUS + 8f;
        }
    }

    private static class VortexPortal {
        float x, y;
        float animTimer = 0f;

        VortexPortal(float topSurfacePx) {
            x = Constants.VIEWPORT_WIDTH / 2f;
            y = topSurfacePx + VORTEX_RADIUS + 8f;
        }

        void update(float delta) { animTimer += delta; }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Disposable
    // ──────────────────────────────────────────────────────────────────────

    @Override
    public void dispose() {
        bats.clear();
        potions.clear();
        vortex = null;
        for (Body b : flyingBats) world.destroyBody(b);
        flyingBats.clear();
    }
}
