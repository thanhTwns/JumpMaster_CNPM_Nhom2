package com.jumpmaster.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.jumpmaster.game.controller.TimeAttackLogic;
import com.badlogic.gdx.Gdx;

public class TimeAttackUI {

    // ── Callback ──────────────────────────────────────────────────────────
    public interface TimeAttackUIListener {
        void onPause();
    }

    // ── Health-bar dimensions (screen pixels) ─────────────────────────────
    private static final float BAR_X          = 20f;
    private static final float BAR_Y_FROM_TOP = 20f;
    private static final float BAR_W          = 160f;
    private static final float BAR_H          = 18f;
    private static final float BAR_BORDER     = 2f;

    // ── State ─────────────────────────────────────────────────────────────
    private float   currentHealth = TimeAttackLogic.PLAYER_MAX_HEALTH;
    private int     currentLevel  = 1;
    private float   glowTimer     = 0f;
    private float   lastHealth    = TimeAttackLogic.PLAYER_MAX_HEALTH;
    private boolean isHealing     = false;

    // ── Scene2D ───────────────────────────────────────────────────────────
    private final Stage stage;
    private final Skin  skin;

    // ── Listener ──────────────────────────────────────────────────────────
    private final TimeAttackUIListener listener;

    // ─────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────

    public TimeAttackUI(SpriteBatch batch, TimeAttackUIListener listener) {
        this.listener = listener;

        // Dùng ScreenViewport giống GameplayUI dùng FitViewport —
        // cả 2 đều để Stage tự quản lý vị trí nút
        stage = new Stage(new ScreenViewport(), batch);
        skin  = buildFallbackSkin();

        // ── Nút Pause — copy y hệt cách GameplayUI làm ───────────────────
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font         = font;
        style.fontColor    = Color.WHITE;
        style.overFontColor = Color.YELLOW;

        TextButton pauseButton = new TextButton("|| PAUSE", style);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onPause();
            }
        });

        // Table căn góc trên-phải, fill toàn màn hình — giống GameplayUI
        Table table = new Table();
        table.top().right();
        table.setFillParent(true);
        table.add(pauseButton).pad(15);

        stage.addActor(table);  // FIX: trước đây thiếu dòng này nên nút không hiện
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Update
    // ─────────────────────────────────────────────────────────────────────

    public void updateData(float health, int level, float delta) {
        isHealing     = (health > lastHealth);
        lastHealth    = currentHealth;
        currentHealth = health;
        currentLevel  = level;
        glowTimer    += delta;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  renderShapes — health bar (batch phải ĐÓNG trước khi gọi)
    // ─────────────────────────────────────────────────────────────────────

    public void renderShapes(ShapeRenderer sr, float screenH) {
        float barTop = screenH - BAR_Y_FROM_TOP - BAR_H;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Viền đen
        sr.setColor(Color.BLACK);
        sr.rect(
            BAR_X - BAR_BORDER,
            barTop - BAR_BORDER,
            BAR_W + BAR_BORDER * 2f,
            BAR_H + BAR_BORDER * 2f
        );

        // Nền xám
        sr.setColor(0.3f, 0.3f, 0.3f, 1f);
        sr.rect(BAR_X, barTop, BAR_W, BAR_H);

        // Thanh máu
        float ratio = currentHealth / TimeAttackLogic.PLAYER_MAX_HEALTH;
        float fillW = BAR_W * ratio;
        sr.setColor(hpColor(ratio));
        sr.rect(BAR_X, barTop, fillW, BAR_H);

        // Hiệu ứng hồi máu
        if (isHealing) {
            float alpha = 0.4f + 0.4f * (float) Math.sin(glowTimer * 8f);
            sr.setColor(0f, 1f, 0.4f, alpha);
            sr.rect(BAR_X, barTop, fillW, BAR_H);
        }

        sr.end();
    }

    private Color hpColor(float ratio) {
        if (ratio > 0.6f) return Color.GREEN;
        if (ratio > 0.3f) return Color.YELLOW;
        return Color.RED;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  renderText — HP + Level text (batch phải ĐANG MỞ)
    // ─────────────────────────────────────────────────────────────────────

    public void renderText(SpriteBatch batch, BitmapFont font, float screenH) {
        float barTop = screenH - BAR_Y_FROM_TOP - BAR_H;

        font.setColor(Color.WHITE);
        font.draw(batch,
            (int) currentHealth + " / " + (int) TimeAttackLogic.PLAYER_MAX_HEALTH,
            BAR_X + 4f,
            barTop + BAR_H - 3f
        );

        font.setColor(Color.YELLOW);
        font.draw(batch,
            "LEVEL  " + currentLevel,
            Gdx.graphics.getWidth() - 120f,
            screenH - BAR_Y_FROM_TOP
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Skin builder
    // ─────────────────────────────────────────────────────────────────────

    private Skin buildFallbackSkin() {
        Skin s = new Skin();
        com.badlogic.gdx.graphics.Pixmap pixmap =
            new com.badlogic.gdx.graphics.Pixmap(
                1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.fill();
        s.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));
        pixmap.dispose();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font      = new BitmapFont();
        style.fontColor = Color.WHITE;
        style.up        = s.newDrawable("white", Color.DARK_GRAY);
        style.down      = s.newDrawable("white", Color.GRAY);
        s.add("default", style);

        return s;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    public Stage getStage() { return stage; }

    public void resize(int width, int height) {
        // ScreenViewport cập nhật đúng kích thước màn hình —
        // Table tự căn lại góc trên-phải, không cần setPosition thủ công
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
