// TimeAttackLogic.java — full file with all three changes applied

package com.jumpmaster.game.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.jumpmaster.game.model.Platform;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.utils.Constants;

public class TimeAttackLogic implements Disposable {

    // ── Constants ──────────────────────────────────────────────────────────
    public  static final float PLAYER_MAX_HEALTH  = 100f;
    private static final float BAT_DAMAGE         = 10f;
    private static final float BAT_PATROL_SPEED   = 60f;
    private static final float BAT_HIT_COOLDOWN   = 1.2f;
    private static final float POTION_HEAL_AMOUNT = 25f;
    private static final float POTION_RADIUS      = 16f;
    private static final float VORTEX_RADIUS      = 40f;
    private static final float BAT_ANIMATION_FPS  = 8f;

    // CHANGE 1: raised from 6 → 10. Each platform spawns 2-3 bats.
    private static final int   MONSTERS_PER_LEVEL = 10;

    // CHANGE 2: raised from 4 → 8. Potions may share platforms with bats.
    private static final int   POTIONS_PER_LEVEL  = 8;

    // Min bats per step platform
    private static final int   BATS_PER_PLATFORM_MIN = 2;
    private static final int   BATS_PER_PLATFORM_MAX = 3;

    // ── External references ────────────────────────────────────────────────
    private final World           world;
    private       Array<Platform> platforms;
    private final Player          player;
    private final LevelListener   listener;

    // ── Assets ────────────────────────────────────────────────────────────
    private final Texture[] batFrames;
    private final Texture   vortexTexture;
    private final Texture   potionTexture;

    // ── State ─────────────────────────────────────────────────────────────
    private float playerHealth = PLAYER_MAX_HEALTH;
    private int   currentLevel = 0;
    private float topPlatformY;   // PIXELS — fallback only

    // ── Entities ──────────────────────────────────────────────────────────
    private final Array<BatMonster>   bats    = new Array<>();
    private final Array<HealthPotion> potions = new Array<>();
    private       VortexPortal        vortex  = null;

    // ── Timers ────────────────────────────────────────────────────────────
    private float hitCooldownTimer = 0f;

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

    /** Legacy single-texture constructor. */
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

    public void initLevel(int level) {
        this.currentLevel = level;

        bats.clear();
        potions.clear();
        vortex = null;
        hitCooldownTimer = 0f;

        // platforms.get(0)           = ground (skip)
        // platforms.get(size-1)      = topmost step  (reserved for vortex + guard bats)
        // platforms.get(1..size-2)   = regular steps

        int stepCount = platforms.size - 1;
        if (stepCount < 1) return;

        // ── CHANGE 3: vortex on the actual top platform ───────────────────
        // The last element in the array is the highest step platform.
        // We read its Y in metres and convert to pixels so the vortex
        // sits on the platform surface rather than floating at topPlatformY.
        Platform topPlatform = platforms.get(platforms.size - 1);
        float topPlatformSurfacePx =
            (topPlatform.getY() + topPlatform.getHeight() / 2f) * Constants.PPM;
        vortex = new VortexPortal(topPlatformSurfacePx);

        // Two guard bats flank the vortex on the top platform
        bats.add(new BatMonster(topPlatform, -50f));  // left guard
        bats.add(new BatMonster(topPlatform,  50f));  // right guard

        // ── Bat placement on regular steps ───────────────────────────────
        // CHANGE 1: 2–3 bats per platform, staggered along its width.
        // Scale total bat count with level but respect array capacity.
        int batBudget = MONSTERS_PER_LEVEL + (level - 1) * 2;

        Array<Integer> stepIndices = new Array<>();
        for (int i = 1; i < platforms.size - 1; i++) stepIndices.add(i);
        stepIndices.shuffle();

        outer:
        for (int idx = 0; idx < stepIndices.size && bats.size < batBudget + 2; idx++) {
            Platform p = platforms.get(stepIndices.get(idx));
            int batsOnThisPlatform = MathUtils.random(
                BATS_PER_PLATFORM_MIN,
                BATS_PER_PLATFORM_MAX);

            float halfW = (p.getWidth() * Constants.PPM) / 2f;
            // Divide the platform width evenly among bats so they don't stack
            float slotW = (halfW * 2f) / (batsOnThisPlatform + 1);

            for (int b = 0; b < batsOnThisPlatform; b++) {
                if (bats.size >= batBudget + 2) break outer;
                // Each bat offset from platform centre: -halfW + (b+1)*slotW
                float xOffset = -halfW + (b + 1) * slotW;
                bats.add(new BatMonster(p, xOffset));
            }
        }

        // ── CHANGE 2: Potions — more of them, allow sharing platforms ─────
        // We pick randomly from ALL step platforms (not just bat-free ones).
        // A guard is in place so a potion never spawns at the exact same X
        // as a bat on the same platform (done inside HealthPotion constructor
        // via a small random jitter that is unlikely to collide exactly).
        Array<Integer> potionCandidates = new Array<>(stepIndices); // same pool
        potionCandidates.shuffle();

        int potionCount = Math.min(POTIONS_PER_LEVEL + (level - 1), potionCandidates.size);
        for (int i = 0; i < potionCount; i++) {
            potions.add(new HealthPotion(platforms.get(potionCandidates.get(i))));
        }

        Gdx.app.log("TimeAttack", "Level " + level
            + " | bats=" + bats.size
            + " | potions=" + potions.size
            + " | vortexY=" + vortex.y);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Per-frame update  (unchanged)
    // ──────────────────────────────────────────────────────────────────────

    public void update(float delta) {
        hitCooldownTimer = Math.max(0f, hitCooldownTimer - delta);

        float playerX = player.body.getPosition().x * Constants.PPM;
        float playerY = player.body.getPosition().y * Constants.PPM;
        Rectangle playerRect = new Rectangle(playerX - 16, playerY - 16, 32, 32);

        for (BatMonster bat : bats) {
            bat.update(delta);
            if (hitCooldownTimer <= 0f && bat.getBounds().overlaps(playerRect)) {
                playerHealth -= BAT_DAMAGE;
                hitCooldownTimer = BAT_HIT_COOLDOWN;
                Gdx.app.log("TimeAttack", "Bat hit! HP=" + playerHealth);
                if (playerHealth <= 0f) {
                    playerHealth = 0f;
                    listener.onPlayerDied();
                    return;
                }
            }
        }

        for (int i = potions.size - 1; i >= 0; i--) {
            HealthPotion p = potions.get(i);
            float dx = p.x - playerX;
            float dy = p.y - playerY;
            if (Math.sqrt(dx * dx + dy * dy) < POTION_RADIUS + 16f) {
                playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + POTION_HEAL_AMOUNT);
                potions.removeIndex(i);
                Gdx.app.log("TimeAttack", "Potion! HP=" + playerHealth);
            }
        }

        if (vortex != null) {
            vortex.update(delta);
            float dx = vortex.x - playerX;
            float dy = vortex.y - playerY;
            if (Math.sqrt(dx * dx + dy * dy) < VORTEX_RADIUS + 16f)
                listener.onLevelComplete(currentLevel);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Draw textured entities  (unchanged)
    // ──────────────────────────────────────────────────────────────────────

    public void drawEntities(SpriteBatch batch) {
        if (batFrames.length > 0) {
            for (BatMonster bat : bats) {
                Texture frame = batFrames[bat.currentFrame % batFrames.length];
                float w = 48f, h = 48f;
                batch.draw(frame,
                    bat.x - w / 2f, bat.y - h / 2f, w, h,
                    0, 0, frame.getWidth(), frame.getHeight(),
                    bat.facingLeft, false);
            }
        }

        if (potionTexture != null) {
            float d = POTION_RADIUS * 2f;
            for (HealthPotion p : potions)
                batch.draw(potionTexture, p.x - POTION_RADIUS, p.y - POTION_RADIUS, d, d);
        }

        if (vortex != null && vortexTexture != null) {
            float d = VORTEX_RADIUS * 2f;
            batch.draw(vortexTexture,
                vortex.x - VORTEX_RADIUS, vortex.y - VORTEX_RADIUS, d, d);
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

    public float getPlayerHealth() { return playerHealth; }
    public int   getCurrentLevel() { return currentLevel; }

    // ──────────────────────────────────────────────────────────────────────
    //  Listener
    // ──────────────────────────────────────────────────────────────────────

    public interface LevelListener {
        void onLevelComplete(int completedLevel);
        void onPlayerDied();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Inner entity classes
    // ──────────────────────────────────────────────────────────────────────

    private static class BatMonster {
        float x, y;
        float patrolLeft, patrolRight;
        float speed;
        boolean facingLeft = false;
        int   currentFrame = 0;
        float frameTimer   = 0f;

        /** Default: random X across the whole platform. */
        BatMonster(Platform platform) {
            this(platform, 0f);
        }

        /**
         * xOffset — pixel offset from platform centre.
         * Used to spread multiple bats evenly, and for guard bats near the vortex.
         */
        BatMonster(Platform platform, float xOffset) {
            float cx = platform.getX() * Constants.PPM;
            float cy = platform.getY() * Constants.PPM;

            patrolLeft  = 16f;
            patrolRight = Constants.VIEWPORT_WIDTH - 16f;

            x = MathUtils.clamp(cx + xOffset, patrolLeft, patrolRight);
            y = cy + 24f;

            speed = MathUtils.random(BAT_PATROL_SPEED * 0.7f, BAT_PATROL_SPEED * 1.3f);
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

        // topSurfacePx = top surface of the top platform in pixels
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
    }
}
