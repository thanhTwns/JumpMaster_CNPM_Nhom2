package com.jumpmaster.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.controller.GameScreen;

public class MainScreen implements Screen {

    private final JumpMasterGame game;
    private SpriteBatch batch;
    private ShapeRenderer sr;
    private GlyphLayout layout;

    private BitmapFont fontLarge;
    private BitmapFont fontMedium;
    private BitmapFont fontSmall;

    private Texture iconSettings;
    private Texture iconTrophy;
    private Texture chickenTex;

    // ── Canonical screen size — always updated in resize() ──────────────────
    private int screenW, screenH;

    // Màu
    private static final Color BG       = new Color(0.05f, 0.05f, 0.10f, 1f);
    private static final Color RED      = new Color(0.91f, 0.27f, 0.37f, 1f);
    private static final Color PURPLE   = new Color(0.50f, 0.47f, 0.87f, 1f);
    private static final Color DARK_BTN = new Color(0.08f, 0.08f, 0.18f, 0.92f);
    private static final Color OVERLAY  = new Color(0f,    0f,    0f,    0.38f);
    private static final Color WHITE    = new Color(Color.WHITE);

    // Sao
    private static final int STAR_COUNT = 45;
    private float[] starX, starY, starPhase, starR;

    // Cột nền
    private static final float[] COL_H = {90, 150, 65, 110, 170, 80, 130};
    private static final float   COL_W = 22f;
    private float[] colBaseX;
    private float   scrollX = 0;

    // Nhân vật
    private float bobT = 0;
    private float t    = 0;
    private float inputCooldown = 0.25f; // Ngăn nhận nhầm click từ màn hình trước

    // Nút bấm
    private Rectangle btnClassic, btnTimeAttack, btnChallenge, btnSettings, btnScores;

    public MainScreen(JumpMasterGame game) {
        this.game = game;
    }

    // ── Called once when screen becomes active ───────────────────────────────
    @Override
    public void show() {
        batch  = new SpriteBatch();
        sr     = new ShapeRenderer();
        layout = new GlyphLayout();

        iconSettings = new Texture(Gdx.files.internal("ui/settings_32dp_FFFFFF_FILL0_wght400_GRAD0_opsz40.png"));
        iconTrophy   = new Texture(Gdx.files.internal("ui/trophy_32dp_FFFFFF_FILL0_wght400_GRAD0_opsz40.png"));
        chickenTex   = new Texture(Gdx.files.internal("ui/chicken.png"));

        chickenTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        iconSettings.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        iconTrophy.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        inputCooldown = 0.25f;
    }

    // ── resize() is the single source of truth for W/H ──────────────────────
    @Override
    public void resize(int width, int height) {
        screenW = width;
        screenH = height;

        // Sync projection matrices FIRST
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        sr.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);

        // Rebuild fonts at new size (dispose old ones first)
        loadFonts();

        // Rebuild layout-dependent positions
        setupButtons();
        initStars();
        initCols();
    }

    // ── Font generation ──────────────────────────────────────────────────────
    private void loadFonts() {
        if (fontLarge  != null) { fontLarge.dispose();  fontLarge  = null; }
        if (fontMedium != null) { fontMedium.dispose(); fontMedium = null; }
        if (fontSmall  != null) { fontSmall.dispose();  fontSmall  = null; }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("font/NunitoSans-Italic-VariableFont_YTLC,opsz,wdth,wght.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter p =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        p.hinting    = FreeTypeFontGenerator.Hinting.Full;
        p.kerning    = true;
        p.genMipMaps = true;
        p.minFilter  = Texture.TextureFilter.MipMapLinearNearest;
        p.magFilter  = Texture.TextureFilter.Linear;
        float base = Math.min(screenW, screenH);
        // Title
        p.size        = Math.round(base * 0.1f);
        p.color       = RED;
        p.borderWidth = 1.5f;
        p.borderColor = RED;
        p.shadowOffsetX = 0; p.shadowOffsetY = -2;
        p.shadowColor = new Color(0, 0, 0, 0.35f);
        fontLarge = generator.generateFont(p);

        // Button labels
        p.size        = Math.round(base * 0.060f);
        p.color       = Color.WHITE;
        p.borderWidth = 0f;
        p.borderColor = null;
        p.shadowOffsetX = 0; p.shadowOffsetY = -1;
        p.shadowColor = new Color(0, 0, 0, 0.25f);
        fontMedium = generator.generateFont(p);

        // Subtitles & icon labels
        p.size        = Math.round(base * 0.038f);
        p.color       = WHITE;
        p.shadowOffsetX = 0; p.shadowOffsetY = 0;
        fontSmall = generator.generateFont(p);

        generator.dispose();
    }

    // ── Layout helpers — all use screenW/screenH ─────────────────────────────
    private void initStars() {
        starX     = new float[STAR_COUNT];
        starY     = new float[STAR_COUNT];
        starPhase = new float[STAR_COUNT];
        starR     = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]     = MathUtils.random(0f, screenW);
            starY[i]     = MathUtils.random(screenH * 0.25f, screenH);
            starPhase[i] = MathUtils.random(0f, MathUtils.PI2);
            starR[i]     = MathUtils.random(0.8f, 2f);
        }
    }

    private void initCols() {
        colBaseX = new float[COL_H.length];
        float spacing = screenW / (COL_H.length - 1f);
        for (int i = 0; i < COL_H.length; i++) {
            colBaseX[i] = i * spacing;
        }
    }

    private void setupButtons() {
        float W  = screenW, H = screenH;
        float btnW    = W * 0.68f;
        float btnH    = H * 0.085f;
        float centerX = (W - btnW) / 2f;
        float startY  = H * 0.50f;
        float gap     = btnH + H * 0.018f;

        btnClassic    = new Rectangle(centerX, startY,         btnW, btnH);
        btnTimeAttack = new Rectangle(centerX, startY - gap,   btnW, btnH);
        btnChallenge  = new Rectangle(centerX, startY - gap*2, btnW, btnH);

        float iconSize = H * 0.09f;
        float iconY    = startY - gap*2 - iconSize - H * 0.035f;
        float iconGap  = W * 0.09f;
        btnSettings = new Rectangle(W/2f - iconSize - iconGap, iconY, iconSize, iconSize);
        btnScores   = new Rectangle(W/2f + iconGap,            iconY, iconSize, iconSize);
    }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void render(float delta) {
        // Guard: fonts not ready yet (resize not called)
        if (fontLarge == null) return;

        t       += delta;
        bobT    += delta;
        scrollX -= delta * 60f;

        if (scrollX < -(screenW / (float) COL_H.length + COL_W)) scrollX = 0;

        if (inputCooldown > 0) {
            inputCooldown -= delta;
        } else {
            handleInput();
        }

        Gdx.gl.glClearColor(BG.r, BG.g, BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // All ShapeRenderer draws first
        drawStars();
        drawGround();
        drawColumns();

        // All batch draws after
        batch.begin();
        drawCharacter();
        drawIcons();
        drawText();
        batch.end();
    }

    // ── Draw helpers ─────────────────────────────────────────────────────────
    private void drawStars() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < STAR_COUNT; i++) {
            float alpha = 0.25f + 0.25f * MathUtils.sin(t * 1.4f + starPhase[i]);
            sr.setColor(0.70f, 0.70f, 1f, alpha);
            sr.circle(starX[i], starY[i], starR[i]);
        }
        sr.end();
    }

    private void drawGround() {
        float W = screenW, H = screenH;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.10f, 0.23f, 1f);
        sr.rect(0, 0, W, H * 0.13f);
        sr.setColor(0.13f, 0.13f, 0.28f, 1f);
        sr.rect(0, H * 0.13f, W, 2f);
        sr.end();
    }

    private void drawColumns() {
        float W       = screenW, H = screenH;
        float groundH = H * 0.13f;
        float scale   = H / 600f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(RED);
        for (int i = 0; i < COL_H.length; i++) {
            float x    = colBaseX[i] + scrollX;
            float colH = COL_H[i] * scale;
            while (x < -COL_W) x += W + COL_W;
            while (x > W)      x -= W + COL_W;
            sr.rect(x, groundH, COL_W, colH);
        }
        sr.end();
    }

    private void drawCharacter() {
        float W       = screenW, H = screenH;
        float groundH = H * 0.13f;
        float size    = H * 0.12f;
        float cx      = W / 2f;
        float cy      = groundH + MathUtils.sin(bobT * 2.5f) * H * 0.018f;

        batch.setColor(Color.WHITE);
        batch.draw(chickenTex, cx - size / 2f, cy, size, size);
    }

    private void drawIcons() {
        float r   = btnSettings.width / 2f;
        float pad = r * 0.28f;

        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.draw(iconSettings,
            btnSettings.x + pad, btnSettings.y + pad,
            btnSettings.width - pad*2, btnSettings.height - pad*2);

        batch.draw(iconTrophy,
            btnScores.x + pad, btnScores.y + pad,
            btnScores.width - pad*2, btnScores.height - pad*2);

        // Restore normal blending
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawText() {
        float W = screenW, H = screenH;

        // JUMPMASTER
        float titleY = H * 0.92f;
        fontLarge.setColor(RED);
        layout.setText(fontLarge, "JUMPMASTER");
        fontLarge.draw(batch, layout, (W - layout.width) / 2f, titleY);

        // Subtitle
        float subY = titleY - layout.height - H * 0.02f;
        final String sub = "DRAG TO SHOOT";
        fontSmall.setColor(WHITE);
        layout.setText(fontSmall, sub);
        fontSmall.draw(batch, layout, (W - layout.width) / 2f, subY);

        // Mode label
        final String chooseLbl = "SELECT GAME MODE";
        fontSmall.setColor(WHITE);
        layout.setText(fontSmall, chooseLbl);
        fontSmall.draw(batch, layout,
            (W - layout.width) / 2f,
            btnClassic.y + btnClassic.height + H * 0.05f);

        // Button text
        fontMedium.setColor(Color.WHITE);
        drawCenteredText(fontMedium, "CLASSIC", btnClassic);

        fontMedium.setColor(RED);
        drawCenteredText(fontMedium, "TIME ATTACK", btnTimeAttack);

        fontMedium.setColor(new Color(0.68f, 0.62f, 1f, 1f));
        drawCenteredText(fontMedium, "CHALLENGE", btnChallenge);

        // Icon labels
        float labelY = btnSettings.y - fontSmall.getLineHeight() * 1.2f;

        fontSmall.setColor(WHITE);
        layout.setText(fontSmall, "SETTINGS");
        fontSmall.draw(batch, layout,
            btnSettings.x + (btnSettings.width - layout.width) / 2f, labelY);

        layout.setText(fontSmall, "SCORES");
        fontSmall.draw(batch, layout,
            btnScores.x + (btnScores.width - layout.width) / 2f, labelY);
    }

    private void drawCenteredText(BitmapFont f, String text, Rectangle btn) {
        layout.setText(f, text);
        f.draw(batch, layout,
            btn.x + (btn.width  - layout.width)  / 2f,
            btn.y + (btn.height + layout.height)  / 2f);
    }

    // ── Input ────────────────────────────────────────────────────────────────
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float tx = Gdx.input.getX();
        float ty = screenH - Gdx.input.getY();

        if (btnClassic.contains(tx, ty))
            game.setScreen(new GameScreen(game, "classic"));
        else if (btnTimeAttack.contains(tx, ty))
            game.setScreen(new GameScreen(game, "timeattack"));
        else if (btnChallenge.contains(tx, ty))
            game.setScreen(new GameScreen(game, "challenge"));
        else if (btnSettings.contains(tx, ty))
            game.setScreen(new SettingsScreen(game));
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        sr.dispose();
        if (fontLarge  != null) fontLarge.dispose();
        if (fontMedium != null) fontMedium.dispose();
        if (fontSmall  != null) fontSmall.dispose();
        iconSettings.dispose();
        iconTrophy.dispose();
        chickenTex.dispose();
    }
}
