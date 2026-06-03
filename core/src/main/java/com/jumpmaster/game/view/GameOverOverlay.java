package com.jumpmaster.game.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.jumpmaster.game.utils.Constants;

public class GameOverOverlay {
    public Stage stage;
    private Label scoreLabel, highScoreLabel, recordLabel;
    // ── 3.2.1.3b Stats labels ────────────────────────────────────────────────
    private Label columnsLabel, maxComboLabel, timeLabel;
    private Texture backgroundTexture;
    private Skin skin;

    private static final Color PURPLE   = new Color(0.50f, 0.47f, 0.87f, 1f);
    private static final Color DARK_BTN = new Color(0.12f, 0.12f, 0.25f, 0.95f);

    public interface GameOverListener {
        void onRestart();
        void onMenu();
    }

    public GameOverOverlay(SpriteBatch batch, final GameOverListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);
        skin = new Skin();

        // Tạo nền mờ cho toàn màn hình
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.75f);
        pixmap.fill();
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        // Tạo Texture cho nút bấm có khung (Border)
        Pixmap btnPixmap = new Pixmap(350, 80, Pixmap.Format.RGBA8888);
        // Vẽ viền ngoài màu tím
        btnPixmap.setColor(PURPLE);
        btnPixmap.fill();
        // Vẽ nền nút bên trong màu tối (để lại viền 3px)
        btnPixmap.setColor(DARK_BTN);
        btnPixmap.fillRectangle(3, 3, 344, 74);
        skin.add("button_bg", new Texture(btnPixmap));
        btnPixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(new TextureRegionDrawable(backgroundTexture));

        // Ép toàn bộ vào giữa tâm màn hình
        table.center();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(2.2f); // giảm từ 2.5 → 2.2 để có chỗ cho stats

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.RED);
        Label.LabelStyle scoreStyle = new Label.LabelStyle(font, Color.WHITE);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = skin.newDrawable("button_bg");
        btnStyle.font = font;
        btnStyle.fontColor = Color.YELLOW;
        btnStyle.overFontColor = Color.WHITE;
        btnStyle.downFontColor = Color.GRAY;

        Label title = new Label("GAME OVER", titleStyle);
        title.setFontScale(1.5f);

        scoreLabel = new Label("Score: 0", scoreStyle);
        highScoreLabel = new Label("Best: 0", scoreStyle);
        recordLabel = new Label("NEW RECORD!", new Label.LabelStyle(font, Color.GREEN));
        recordLabel.setVisible(false);

        // ── 3.2.1.3b Stats labels — dùng font nhỏ hơn để vừa màn hình ───
        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(1.5f); // nhỏ hơn font chính
        Label.LabelStyle statsStyle = new Label.LabelStyle(smallFont, Color.CYAN);
        columnsLabel  = new Label("Bậc: 0",        statsStyle);
        maxComboLabel = new Label("Combo: 0x",     statsStyle);
        timeLabel     = new Label("Time: 0s",      statsStyle);

        TextButton btnRestart = new TextButton("RESTART", btnStyle);
        TextButton btnMenu = new TextButton("MENU", btnStyle);

        btnRestart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { listener.onRestart(); }
        });

        btnMenu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { listener.onMenu(); }
        });

        // --- LAYOUT ---
        table.add(title).padBottom(10).row();
        table.add(scoreLabel).padBottom(2).row();
        table.add(highScoreLabel).padBottom(2).row();
        table.add(recordLabel).padBottom(8).row();

        // ── 3.2.1.3b Stats section — gom 3 chỉ số trên 1 dòng ngang ─────
        // tiết kiệm chiều cao, vẫn hiển thị đủ thông tin
        Table statsRow = new Table();
        statsRow.add(columnsLabel).padRight(20);
        statsRow.add(maxComboLabel).padRight(20);
        statsRow.add(timeLabel);
        table.add(statsRow).padBottom(12).row();

        // Nút bấm — giảm height để vừa màn hình
        table.add(btnRestart).width(320).height(65).padBottom(4).row();
        table.add(btnMenu).width(320).height(65);

        stage.addActor(table);
    }

    public void setData(int score, int highScore, boolean isNewRecord) {
        scoreLabel.setText("Score: " + score);
        highScoreLabel.setText("Best: " + highScore);
        recordLabel.setVisible(isNewRecord);
    }

    // ── 3.2.1.4 setStats — nhận stats từ BaseScreen.triggerGameOver() ────────
    // int[0] = columnsPassed, int[1] = maxCombo, int[2] = survivalTime (giây)
    public void setStats(int[] stats) {
        columnsLabel.setText("Step: " + stats[0]);
        maxComboLabel.setText("Combo: " + stats[1] + "x");
        // format mm:ss nếu >= 60s, ngược lại chỉ hiện giây
        int sec = stats[2] % 60;
        int min = stats[2] / 60;
        String timeStr = min > 0
            ? String.format("%d:%02d", min, sec)
            : sec + "s";
        timeLabel.setText("Time: " + timeStr);
    }

    public void render() {
        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
        backgroundTexture.dispose();
    }
}
