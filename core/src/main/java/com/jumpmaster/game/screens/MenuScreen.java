package com.jumpmaster.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class MenuScreen implements Screen {

    private final Game game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout layout;

    // Màu sắc theo mockup
    private static final Color BG_COLOR      = new Color(0.08f, 0.08f, 0.15f, 1f);
    private static final Color COL_COLOR     = new Color(0.91f, 0.27f, 0.37f, 1f);  // đỏ cột
    private static final Color BTN_RED       = new Color(0.91f, 0.27f, 0.37f, 1f);  // Classic
    private static final Color BTN_OUTLINE   = new Color(0.91f, 0.27f, 0.37f, 1f);  // Time Attack
    private static final Color BTN_PURPLE    = new Color(0.5f,  0.47f, 0.87f, 1f);  // Challenge
    private static final Color BTN_DARK      = new Color(0.12f, 0.12f, 0.22f, 0.9f);
    private static final Color STAR_COLOR    = new Color(1f, 1f, 1f, 0.6f);
    private static final Color TEXT_MUTED    = new Color(0.6f, 0.6f, 0.7f, 1f);

    // Cột nền
    private float[] colHeights = {90, 140, 70, 110, 160, 85};
    private float[] colX;
    private float colScrollX = 0;

    // Nhân vật
    private float charBobY = 0;
    private float charBobTime = 0;

    // Sao
    private float[] starX, starY, starPhase;
    private static final int STAR_COUNT = 40;

    // Nút bấm
    private Rectangle btnClassic, btnTimeAttack, btnChallenge, btnSettings, btnScores;
    private boolean pressedClassic, pressedTime, pressedChallenge;

    // Animation
    private float stateTime = 0;

    public MenuScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font          = new BitmapFont();
        font.getData().setScale(1.5f);
        layout        = new GlyphLayout();

        // Khởi tạo sao ngẫu nhiên
        starX     = new float[STAR_COUNT];
        starY     = new float[STAR_COUNT];
        starPhase = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]     = (float) Math.random() * Gdx.graphics.getWidth();
            starY[i]     = (float) Math.random() * Gdx.graphics.getHeight() * 0.75f
                + Gdx.graphics.getHeight() * 0.25f;
            starPhase[i] = (float) Math.random() * 6.28f;
        }

        // Khởi tạo vị trí cột nền (6 cột, scroll từ phải sang trái)
        colX = new float[6];
        float W = Gdx.graphics.getWidth();
        for (int i = 0; i < 6; i++) {
            colX[i] = i * (W / 4f);
        }

        setupButtons();
    }

    private void setupButtons() {
        float W   = Gdx.graphics.getWidth();
        float H   = Gdx.graphics.getHeight();
        float btnW = W * 0.65f;
        float btnH = H * 0.09f;
        float centerX = (W - btnW) / 2f;
        float startY  = H * 0.52f;
        float gap     = btnH + H * 0.02f;

        btnClassic    = new Rectangle(centerX, startY,          btnW, btnH);
        btnTimeAttack = new Rectangle(centerX, startY - gap,    btnW, btnH);
        btnChallenge  = new Rectangle(centerX, startY - gap*2,  btnW, btnH);

        float iconSize = H * 0.08f;
        float iconY    = startY - gap*2 - iconSize - H*0.04f;
        btnSettings    = new Rectangle(W/2f - iconSize - 16, iconY, iconSize, iconSize);
        btnScores      = new Rectangle(W/2f + 16,            iconY, iconSize, iconSize);
    }

    @Override
    public void render(float delta) {
        stateTime    += delta;
        charBobTime  += delta;
        colScrollX   -= delta * 60f;  // tốc độ scroll cột

        // Reset scroll khi cột ra khỏi màn hình
        float W = Gdx.graphics.getWidth();
        if (colScrollX < -W / 4f) colScrollX = 0;

        // Bob nhân vật
        charBobY = (float) Math.sin(charBobTime * 2.5f) * 8f;

        // Xử lý input touch/click
        handleInput();

        // Vẽ
        Gdx.gl.glClearColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground();
        drawColumns();
        drawCharacter();
        drawUI();
    }

    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float tx = Gdx.input.getX();
        float ty = Gdx.graphics.getHeight() - Gdx.input.getY(); // flip Y

        if (btnClassic.contains(tx, ty)) {
            game.setScreen(new GameScreen(game, "classic"));
        } else if (btnTimeAttack.contains(tx, ty)) {
            game.setScreen(new GameScreen(game, "timeattack"));
        } else if (btnChallenge.contains(tx, ty)) {
            game.setScreen(new GameScreen(game, "challenge"));
        } else if (btnSettings.contains(tx, ty)) {
            game.setScreen(new SettingsScreen(game));
        }
    }

    private void drawBackground() {
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Sao nhấp nháy
        for (int i = 0; i < STAR_COUNT; i++) {
            float alpha = 0.3f + 0.3f * (float) Math.sin(stateTime * 1.5f + starPhase[i]);
            shapeRenderer.setColor(1f, 1f, 1f, alpha);
            shapeRenderer.circle(starX[i], starY[i], 2f);
        }

        // Mặt đất
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 1f);
        shapeRenderer.rect(0, 0, W, H * 0.12f);

        shapeRenderer.end();
    }

    private void drawColumns() {
        float W       = Gdx.graphics.getWidth();
        float H       = Gdx.graphics.getHeight();
        float groundH = H * 0.12f;
        float colW    = W * 0.07f;
        float spacing = W / 4f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COL_COLOR);

        for (int i = 0; i < 6; i++) {
            float x = colX[i] + colScrollX;
            // wrap around
            while (x < -colW) x += W * 1.5f;
            float colH = colHeights[i] / 200f * H * 0.35f;
            shapeRenderer.rect(x, groundH, colW, colH);
        }

        shapeRenderer.end();
    }

    private void drawCharacter() {
        float W       = Gdx.graphics.getWidth();
        float H       = Gdx.graphics.getHeight();
        float groundH = H * 0.12f;
        float cx      = W / 2f;
        float cy      = groundH + H * 0.08f + charBobY;
        float r       = H * 0.04f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Thân tròn đỏ
        shapeRenderer.setColor(COL_COLOR);
        shapeRenderer.circle(cx, cy, r);

        // Mắt trái
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(cx - r * 0.3f, cy + r * 0.2f, r * 0.22f);
        shapeRenderer.circle(cx + r * 0.3f, cy + r * 0.2f, r * 0.22f);

        // Đồng tử
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.circle(cx - r * 0.3f, cy + r * 0.2f, r * 0.1f);
        shapeRenderer.circle(cx + r * 0.3f, cy + r * 0.2f, r * 0.1f);

        shapeRenderer.end();
    }

    private void drawUI() {
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Overlay tối
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
        shapeRenderer.rect(0, 0, W, H);
        shapeRenderer.end();

        // Vẽ nút Classic (solid đỏ)
        drawButton(btnClassic, BTN_RED, true, false);
        // Vẽ nút Time Attack (outline đỏ)
        drawButton(btnTimeAttack, BTN_OUTLINE, false, false);
        // Vẽ nút Challenge (outline tím)
        drawButton(btnChallenge, BTN_PURPLE, false, false);

        // Vẽ icon Settings và Scores (hình tròn tối)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BTN_DARK);
        float r = btnSettings.height / 2f;
        shapeRenderer.circle(btnSettings.x + r, btnSettings.y + r, r);
        shapeRenderer.circle(btnScores.x  + r, btnScores.y  + r, r);
        shapeRenderer.end();

        // Text
        batch.begin();

        // Tiêu đề JUMPMASTER
        font.getData().setScale(2.2f);
        font.setColor(COL_COLOR);
        layout.setText(font, "JUMPMASTER");
        font.draw(batch, "JUMPMASTER",
            (W - layout.width) / 2f,
            H * 0.92f);

        // Subtitle
        font.getData().setScale(0.9f);
        font.setColor(TEXT_MUTED);
        layout.setText(font, "TAP TO AIM  *  RELEASE TO LAUNCH");
        font.draw(batch, "TAP TO AIM  *  RELEASE TO LAUNCH",
            (W - layout.width) / 2f,
            H * 0.86f);

        // Label chọn chế độ
        font.getData().setScale(1f);
        font.setColor(TEXT_MUTED);
        layout.setText(font, "CHON CHE DO CHOI");
        font.draw(batch, "CHON CHE DO CHOI",
            (W - layout.width) / 2f,
            btnClassic.y + btnClassic.height + H * 0.04f);

        // Text nút
        font.getData().setScale(1.3f);
        font.setColor(Color.WHITE);
        drawCenteredText("CLASSIC",     btnClassic);
        drawCenteredText("TIME ATTACK", btnTimeAttack);

        font.setColor(new Color(0.7f, 0.65f, 1f, 1f));
        drawCenteredText("CHALLENGE", btnChallenge);

        // Icon text
        font.getData().setScale(0.8f);
        font.setColor(TEXT_MUTED);
        float r2 = btnSettings.height / 2f;
        layout.setText(font, "SETTINGS");
        font.draw(batch, "SETTINGS",
            btnSettings.x + r2 - layout.width/2f,
            btnSettings.y - 4);
        layout.setText(font, "SCORES");
        font.draw(batch, "SCORES",
            btnScores.x + r2 - layout.width/2f,
            btnScores.y - 4);

        batch.end();
    }

    private void drawButton(Rectangle btn, Color color, boolean filled, boolean pressed) {
        float W = Gdx.graphics.getWidth();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (filled) {
            shapeRenderer.setColor(color);
            shapeRenderer.rect(btn.x, btn.y, btn.width, btn.height);
        } else {
            // Background tối
            shapeRenderer.setColor(BTN_DARK);
            shapeRenderer.rect(btn.x, btn.y, btn.width, btn.height);
        }
        shapeRenderer.end();

        // Viền outline
        if (!filled) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(color);
            Gdx.gl.glLineWidth(2f);
            shapeRenderer.rect(btn.x, btn.y, btn.width, btn.height);
            shapeRenderer.end();
        }
    }

    private void drawCenteredText(String text, Rectangle btn) {
        layout.setText(font, text);
        font.draw(batch, text,
            btn.x + (btn.width  - layout.width)  / 2f,
            btn.y + (btn.height + layout.height)  / 2f);
    }

    @Override
    public void resize(int width, int height) {
        setupButtons();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
