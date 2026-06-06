package com.jumpmaster.game.view;

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
import com.badlogic.gdx.utils.Align;
import com.jumpmaster.game.JumpMasterGame;

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

    // UC-1.2: Chọn chế độ chơi - Khai báo vùng nhấn cho các chế độ và nút EXIT
    private Rectangle btnClassic, btnTimeAttack, btnChallenge, btnSettings, btnScores, btnExit;

    // UC-1.2: Chọn chế độ chơi - Nội dung mô tả cho từng chế độ
    private static final String DESC_CLASSIC =
        "CLASSIC MODE:\n" +
            "- Jump across platforms and reach the highest score.\n" +
            "- No time limit.";

    private static final String DESC_TIME_ATTACK =
        "TIME ATTACK MODE:\n" +
            "- Race against the clock.\n" +
            "- Defeat enemies and collect health potions.\n" +
            "- Find the portal and finish the stage as fast as possible.";
    private static final String DESC_CHALLENGE =
        "CHALLENGE MODE:\n" +
            "- Complete special objectives on each stage.\n" +
            "- Avoid enemies and environmental hazards.\n" +
            "- Limited lives and tougher platform layouts.\n" +
            "- Reach the goal before running out of chances.\n" +
            "- Earn bonus points for completing challenges.";
    private boolean isShowingPopup = false;
    private com.badlogic.gdx.math.Rectangle btnXacNhan;
    private String popupText = "";
    private String selectedModeTag = "";
    private ShapeRenderer shapeRenderer;
    private Rectangle btnBack;


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

        float btnW = 220f;
        float btnH = 60f;
        shapeRenderer = new ShapeRenderer();
        btnXacNhan = new com.badlogic.gdx.math.Rectangle(
            (screenW - btnW) / 2, (screenH / 2) - 120, btnW, btnH);

        btnBack = new Rectangle(0, 0, 120, 50);
    }

    // ── resize() is the single source of truth for W/H ──────────────────────
    @Override
    public void resize(int width, int height) {
        screenW = width;
        screenH = height;

        // Sync projection matrices FIRST
        Gdx.gl.glViewport(0, 0, width, height);
        if (batch != null) batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        if (sr != null) sr.setProjectionMatrix(sr.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        if (shapeRenderer != null) {
            shapeRenderer.setProjectionMatrix(shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        }

        // Rebuild fonts at new size (dispose old ones first)
        loadFonts();

        // Rebuild layout-dependent positions
        setupButtons();
        initStars();
        initCols();

        float popupW = screenW * 0.75f;
        float popupH = screenH * 0.55f;
        float popupX = (screenW - popupW) / 2;
        float popupY = (screenH - popupH) / 2;
        float btnW = 220;
        float btnH = 60;
        btnXacNhan = new Rectangle(
            popupX + popupW - btnW - 40,
            popupY + 20,
            btnW,
            btnH
        );
        btnBack.set(
            popupX + 40,
            popupY + 20,
            120,
            50
        );
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

    // UC-1.2: Chọn chế độ chơi - Cấu hình khung ngắn lại đồng nhất và dịch icon xuống sát cạnh dưới
    private void setupButtons() {
        float W  = screenW, H = screenH;
        float btnW    = W * 0.42f; // UC-1.2: Độ dài khung đồng nhất cho các nút mode
        float btnH    = H * 0.075f;
        float centerX = (W - btnW) / 2f;
        float startY  = H * 0.60f; // UC-1.2: Hạ thấp startY để giao diện không bị nhít lên quá
        float gap     = btnH + H * 0.025f;

        btnClassic    = new Rectangle(centerX, startY,         btnW, btnH);
        btnTimeAttack = new Rectangle(centerX, startY - gap,   btnW, btnH);
        btnChallenge  = new Rectangle(centerX, startY - gap*2, btnW, btnH);
        btnExit       = new Rectangle(centerX, startY - gap*3, btnW, btnH);

        // UC-1.2: Settings và Scores ngắn lại vừa phải nhưng đồng nhất kích thước và dịch xuống dưới sát cạnh
        float subBtnW = W * 0.30f;
        float iconY    = H * 0.05f; // Dịch xuống sát dưới cạnh theo yêu cầu
        float iconGap  = W * 0.04f;
        btnSettings = new Rectangle(W/2f - subBtnW - iconGap, iconY, subBtnW, btnH);
        btnScores   = new Rectangle(W/2f + iconGap,           iconY, subBtnW, btnH);
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

        handleInput();

        Gdx.gl.glClearColor(BG.r, BG.g, BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // All ShapeRenderer draws first
        drawStars();
        drawGround();
        drawColumns();

        // UC-1.2: Chọn chế độ chơi - Vẽ nền màu tối và khung cho tất cả các nút bấm
        drawButtonFrames();

        // All batch draws after
        batch.begin();
        drawCharacter();
        drawIcons();
        drawText();
        batch.end();

        // UC-1.2: Hiển thị popup hướng dẫn chế độ chơi
        if (isShowingPopup) drawPopup();
    }

    // UC-1.2: Chọn chế độ chơi - Hàm vẽ nền màu tối và khung viền màu sắc tương ứng cho các nút
    private void drawButtonFrames() {
        // 1. Vẽ nền DARK_BTN cho tất cả các khung để giao diện đồng bộ
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(DARK_BTN);
        sr.rect(btnClassic.x, btnClassic.y, btnClassic.width, btnClassic.height);
        sr.rect(btnTimeAttack.x, btnTimeAttack.y, btnTimeAttack.width, btnTimeAttack.height);
        sr.rect(btnChallenge.x, btnChallenge.y, btnChallenge.width, btnChallenge.height);
        sr.rect(btnExit.x, btnExit.y, btnExit.width, btnExit.height);
        sr.rect(btnSettings.x, btnSettings.y, btnSettings.width, btnSettings.height);
        sr.rect(btnScores.x, btnScores.y, btnScores.width, btnScores.height);
        sr.end();

        // 2. Vẽ khung viền (Line) với màu sắc tương ứng để làm nổi bật nút
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(WHITE);
        sr.rect(btnClassic.x, btnClassic.y, btnClassic.width, btnClassic.height);
        sr.setColor(RED);
        sr.rect(btnTimeAttack.x, btnTimeAttack.y, btnTimeAttack.width, btnTimeAttack.height);
        sr.setColor(new Color(0.68f, 0.62f, 1f, 1f));
        sr.rect(btnChallenge.x, btnChallenge.y, btnChallenge.width, btnChallenge.height);
        sr.setColor(Color.GRAY);
        sr.rect(btnExit.x, btnExit.y, btnExit.width, btnExit.height);

        // Khung cho Settings và Scores dùng chung màu trắng đồng bộ
        sr.setColor(WHITE);
        sr.rect(btnSettings.x, btnSettings.y, btnSettings.width, btnSettings.height);
        sr.rect(btnScores.x, btnScores.y, btnScores.width, btnScores.height);
        sr.end();
    }

    private void drawPopup() {
        float popupW = screenW * 0.75f;
        float popupH = screenH * 0.55f;
        float popupX = (screenW - popupW) / 2;
        float popupY = (screenH - popupH) / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.75f);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.setColor(0.12f, 0.15f, 0.22f, 1);
        shapeRenderer.rect(popupX, popupY, popupW, popupH);
        shapeRenderer.end();

        batch.begin();
        fontMedium.setColor(Color.GOLD);
        fontMedium.draw(batch, "GAME RULES", popupX + popupW/2 - 90, popupY + popupH - 35);
        fontSmall.setColor(Color.WHITE);
        fontSmall.draw(batch, popupText, popupX + 40, popupY + popupH - 100, popupW - 80, Align.left, true);
        fontMedium.setColor(Color.WHITE);
        GlyphLayout layoutStart = new GlyphLayout(fontMedium, "START");
        fontMedium.draw(batch, "START", btnXacNhan.x + (btnXacNhan.width - layoutStart.width) / 2, btnXacNhan.y + (btnXacNhan.height + layoutStart.height) / 2);
        GlyphLayout layoutBack = new GlyphLayout(fontMedium, "BACK");
        fontMedium.draw(batch, "BACK", btnBack.x + (btnBack.width - layoutBack.width) / 2, btnBack.y + (btnBack.height + layoutBack.height) / 2);
        batch.end();
    }

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
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.10f, 0.23f, 1f);
        sr.rect(0, 0, screenW, screenH * 0.13f);
        sr.end();
    }

    private void drawColumns() {
        float scale   = screenH / 600f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(RED);
        for (int i = 0; i < COL_H.length; i++) {
            float x = colBaseX[i] + scrollX;
            float colH = COL_H[i] * scale;
            while (x < -COL_W) x += screenW + COL_W;
            while (x > screenW) x -= screenW + COL_W;
            sr.rect(x, screenH * 0.13f, COL_W, colH);
        }
        sr.end();
    }

    private void drawCharacter() {
        float size = screenH * 0.12f;
        float cy = screenH * 0.13f + MathUtils.sin(bobT * 2.5f) * screenH * 0.018f;
        batch.draw(chickenTex, screenW / 2f - size / 2f, cy, size, size);
    }

    // UC-1.2: Vẽ icon được căn lề bên trái bên trong khung nút mới của Settings và Scores
    private void drawIcons() {
        float iconSize = btnSettings.height * 0.5f;
        float padY = (btnSettings.height - iconSize) / 2f;
        float padX = btnSettings.width * 0.08f;

        batch.draw(iconSettings, btnSettings.x + padX, btnSettings.y + padY, iconSize, iconSize);
        batch.draw(iconTrophy, btnScores.x + padX, btnScores.y + padY, iconSize, iconSize);
    }

    private void drawText() {
        // UC-1.2: Tiêu đề chính
        fontLarge.draw(batch, "JUMPMASTER", (screenW - new GlyphLayout(fontLarge, "JUMPMASTER").width) / 2f, screenH * 0.88f);

        fontSmall.setColor(WHITE);
        fontSmall.draw(batch, "SELECT GAME MODE", (screenW - new GlyphLayout(fontSmall, "SELECT GAME MODE").width) / 2f, btnClassic.y + btnClassic.height + screenH * 0.04f);

        drawCenteredText(fontMedium, "CLASSIC", btnClassic);
        fontMedium.setColor(RED);
        drawCenteredText(fontMedium, "TIME ATTACK", btnTimeAttack);
        fontMedium.setColor(new Color(0.68f, 0.62f, 1f, 1f));
        drawCenteredText(fontMedium, "CHALLENGE", btnChallenge);

        // UC-1.2: Vẽ nhãn EXIT với màu xám nhạt
        fontMedium.setColor(Color.GRAY);
        drawCenteredText(fontMedium, "EXIT", btnExit);
        fontMedium.setColor(Color.WHITE);

        // UC-1.2: Vẽ nhãn SETTINGS và SCORES căn giữa bên trong khung nút mới
        drawCenteredText(fontSmall, "SETTINGS", btnSettings);
        drawCenteredText(fontSmall, "SCORES", btnScores);
    }

    private void drawCenteredText(BitmapFont f, String text, Rectangle btn) {
        layout.setText(f, text);
        f.draw(batch, layout, btn.x + (btn.width - layout.width) / 2f, btn.y + (btn.height + layout.height) / 2f);
    }

    // UC-1.2: Chọn chế độ chơi - Xử lý tương tác nút chọn mode và nút EXIT
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float tx = Gdx.input.getX();
        float ty = screenH - Gdx.input.getY();

        if (isShowingPopup) {
            if (btnBack.contains(tx, ty)) { isShowingPopup = false; return; }
            if (btnXacNhan.contains(tx, ty)) { startSelectedMode(); return; }
            return;
        }

        if (btnClassic.contains(tx, ty)) {
            popupText = DESC_CLASSIC; selectedModeTag = "classic"; isShowingPopup = true;
        } else if (btnTimeAttack.contains(tx, ty)) {
            popupText = DESC_TIME_ATTACK; selectedModeTag = "timeattack"; isShowingPopup = true;
        } else if (btnChallenge.contains(tx, ty)) {
            popupText = DESC_CHALLENGE; selectedModeTag = "challenge"; isShowingPopup = true;
        } else if (btnExit.contains(tx, ty)) {
            // UC-1.2: Thoát hẳn trò chơi
            Gdx.app.exit();
        } else if (btnSettings.contains(tx, ty)) {
            game.setScreen(new SettingsScreen(game));
        } else if (btnScores.contains(tx, ty)) {
            game.setScreen(new LeaderboardScreen(game));
        }
    }

    private void startSelectedMode() {
        isShowingPopup = false;
        game.setScreen(new EarthScreen(game, selectedModeTag));
    }

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
