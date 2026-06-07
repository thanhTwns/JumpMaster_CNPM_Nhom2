package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.jumpmaster.game.JumpMasterGame;

public class WinScreen implements Screen {

    private final JumpMasterGame game;
    private final String         fromMode; // "timeattack" | "classic" | ...

    private SpriteBatch   batch;
    private ShapeRenderer sr;
    private GlyphLayout   layout;

    private BitmapFont fontTitle;
    private BitmapFont fontBtn;
    private BitmapFont fontSub;

    private Texture chickenTex;

    private int screenW, screenH;

    // Nút
    private Rectangle btnReplay, btnMenu;

    // Animation
    private float t    = 0f;
    private float bobT = 0f;

    // Sao
    private static final int   STAR_COUNT = 50;
    private float[] starX, starY, starPhase, starR;

    // Màu
    private static final Color BG_TOP    = new Color(0.04f, 0.04f, 0.12f, 1f);
    private static final Color GOLD      = new Color(1f, 0.85f, 0.2f, 1f);
    private static final Color BTN_COLOR = new Color(0.15f, 0.15f, 0.30f, 1f);
    private static final Color BTN_HOVER = new Color(0.25f, 0.25f, 0.50f, 1f);

    public WinScreen(JumpMasterGame game, String fromMode) {
        this.game     = game;
        this.fromMode = fromMode;
    }

    // ──────────────────────────────────────────────────────────────────────
    @Override
    public void show() {
        batch  = new SpriteBatch();
        sr     = new ShapeRenderer();
        layout = new GlyphLayout();

        chickenTex = new Texture(Gdx.files.internal("ui/chicken.png"));
        chickenTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    public void resize(int width, int height) {
        screenW = width;
        screenH = height;
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        sr.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        loadFonts();
        setupLayout();
        initStars();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Font
    // ──────────────────────────────────────────────────────────────────────
    private void loadFonts() {
        if (fontTitle != null) fontTitle.dispose();
        if (fontBtn   != null) fontBtn.dispose();
        if (fontSub   != null) fontSub.dispose();

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("font/NunitoSans-Italic-VariableFont_YTLC,opsz,wdth,wght.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        float base = Math.min(screenW, screenH);

        // "YOU WIN!"
        p.size        = Math.round(base * 0.13f);
        p.color       = GOLD;
        p.borderWidth = 2f;
        p.borderColor = new Color(1f, 0.6f, 0f, 1f);
        p.shadowOffsetX = 0; p.shadowOffsetY = -3;
        p.shadowColor = new Color(0f, 0f, 0f, 0.4f);
        fontTitle = gen.generateFont(p);

        // Button text
        p.size        = Math.round(base * 0.065f);
        p.color       = Color.WHITE;
        p.borderWidth = 0f;
        p.borderColor = null;
        p.shadowOffsetX = 0; p.shadowOffsetY = -1;
        p.shadowColor = new Color(0f, 0f, 0f, 0.3f);
        fontBtn = gen.generateFont(p);

        // Subtitle
        p.size        = Math.round(base * 0.04f);
        p.color       = new Color(0.8f, 0.8f, 1f, 1f);
        p.shadowOffsetX = 0; p.shadowOffsetY = 0;
        fontSub = gen.generateFont(p);

        gen.dispose();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Layout
    // ──────────────────────────────────────────────────────────────────────
    private void setupLayout() {
        float W = screenW, H = screenH;
        float btnW = W * 0.42f;
        float btnH = H * 0.10f;
        float gap  = W * 0.04f;
        float totalW = btnW * 2 + gap;
        float startX = (W - totalW) / 2f;
        float btnY   = H * 0.18f;

        btnReplay = new Rectangle(startX,           btnY, btnW, btnH);
        btnMenu   = new Rectangle(startX + btnW + gap, btnY, btnW, btnH);
    }

    private void initStars() {
        starX     = new float[STAR_COUNT];
        starY     = new float[STAR_COUNT];
        starPhase = new float[STAR_COUNT];
        starR     = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]     = MathUtils.random(0f, screenW);
            starY[i]     = MathUtils.random(0f, screenH);
            starPhase[i] = MathUtils.random(0f, MathUtils.PI2);
            starR[i]     = MathUtils.random(0.8f, 2.2f);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Render
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public void render(float delta) {
        if (fontTitle == null) return;
        t    += delta;
        bobT += delta;

        handleInput();

        Gdx.gl.glClearColor(BG_TOP.r, BG_TOP.g, BG_TOP.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawStars();
        drawButtons();       // shape trước
        drawParticles();     // confetti shape

        batch.begin();
        drawChicken();
        drawTexts();
        batch.end();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Draw helpers
    // ──────────────────────────────────────────────────────────────────────
    private void drawStars() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < STAR_COUNT; i++) {
            float alpha = 0.3f + 0.3f * MathUtils.sin(t * 1.6f + starPhase[i]);
            sr.setColor(0.8f, 0.8f, 1f, alpha);
            sr.circle(starX[i], starY[i], starR[i]);
        }
        sr.end();
    }

    // Confetti đơn giản bằng ShapeRenderer
    private static final int CONFETTI_COUNT = 60;
    private void drawParticles() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < CONFETTI_COUNT; i++) {
            float px = ((starX[i % STAR_COUNT] + t * (30f + i * 4f)) % screenW);
            float py = screenH - ((t * (40f + i * 3f) + starY[i % STAR_COUNT]) % screenH);
            float sz = 4f + (i % 3) * 2f;
            // Màu xen kẽ: vàng / tím / trắng
            if      (i % 3 == 0) sr.setColor(GOLD.r, GOLD.g, GOLD.b, 0.7f);
            else if (i % 3 == 1) sr.setColor(0.6f, 0.4f, 1f, 0.7f);
            else                 sr.setColor(1f, 1f, 1f, 0.5f);
            sr.rect(px, py, sz, sz);
        }
        sr.end();
    }

    private void drawButtons() {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Nền nút REPLAY
        sr.setColor(new Color(0.2f, 0.5f, 0.2f, 0.95f));
        drawRoundedRect(btnReplay, 12f);

        // Nền nút MENU
        sr.setColor(new Color(0.2f, 0.2f, 0.5f, 0.95f));
        drawRoundedRect(btnMenu, 12f);

        sr.end();

        // Viền nút
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(new Color(1f, 1f, 1f, 0.25f));
        sr.rect(btnReplay.x, btnReplay.y, btnReplay.width, btnReplay.height);
        sr.rect(btnMenu.x,   btnMenu.y,   btnMenu.width,   btnMenu.height);
        sr.end();
    }

    // ShapeRenderer không có roundedRect sẵn — dùng rect + 4 circle góc
    private void drawRoundedRect(Rectangle r, float radius) {
        sr.rect(r.x + radius, r.y,          r.width - radius*2, r.height);
        sr.rect(r.x,          r.y + radius, r.width,            r.height - radius*2);
        sr.circle(r.x + radius,           r.y + radius,           radius, 12);
        sr.circle(r.x + r.width - radius, r.y + radius,           radius, 12);
        sr.circle(r.x + radius,           r.y + r.height - radius, radius, 12);
        sr.circle(r.x + r.width - radius, r.y + r.height - radius, radius, 12);
    }

    private void drawChicken() {
        float size = screenH * 0.15f;
        float cx   = screenW / 2f;
        float cy   = screenH * 0.52f + MathUtils.sin(bobT * 2.5f) * screenH * 0.02f;
        batch.setColor(Color.WHITE);
        batch.draw(chickenTex, cx - size / 2f, cy, size, size);
    }

    private void drawTexts() {
        float W = screenW, H = screenH;

        // "YOU WIN!"  — pulse scale nhỏ bằng alpha
        float pulse = 0.92f + 0.08f * MathUtils.sin(t * 3f);
        fontTitle.setColor(GOLD.r, GOLD.g, GOLD.b, pulse);
        layout.setText(fontTitle, "YOU WIN!");
        fontTitle.draw(batch, layout,
            (W - layout.width) / 2f,
            H * 0.90f);

        // Subtitle
        fontSub.setColor(0.8f, 0.8f, 1f, 1f);
        layout.setText(fontSub, "Congratulations! Stage cleared.");
        fontSub.draw(batch, layout, (W - layout.width) / 2f, H * 0.79f);

        // Button labels
        fontBtn.setColor(Color.WHITE);
        drawCenteredText(fontBtn, "PLAY AGAIN", btnReplay);
        drawCenteredText(fontBtn, "MENU",       btnMenu);
    }

    private void drawCenteredText(BitmapFont f, String text, Rectangle btn) {
        layout.setText(f, text);
        f.draw(batch, layout,
            btn.x + (btn.width  - layout.width)  / 2f,
            btn.y + (btn.height + layout.height) / 2f);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Input
    // ──────────────────────────────────────────────────────────────────────
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float tx = Gdx.input.getX();
        float ty = screenH - Gdx.input.getY();

        if (btnReplay.contains(tx, ty)) {
            // Chơi lại đúng mode
            if ("timeattack".equals(fromMode))
                game.setScreen(new TimeAttackScreen(game));
            else
                game.setScreen(new EarthScreen(game, fromMode));
        } else if (btnMenu.contains(tx, ty)) {
            game.setScreen(new MainScreen(game));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        sr.dispose();
        if (fontTitle != null) fontTitle.dispose();
        if (fontBtn   != null) fontBtn.dispose();
        if (fontSub   != null) fontSub.dispose();
        if (chickenTex != null) chickenTex.dispose();
    }

    public static void main(String[] args) {

    }
}
