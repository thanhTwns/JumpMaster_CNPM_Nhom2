package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.jumpmaster.game.GameSettings;
import com.jumpmaster.game.utils.Constants;
import com.jumpmaster.game.utils.ScoreManager;

public class GameplayUI {
    public Stage stage;
    private Label scoreLabel;
    private Label comboLabel;
    private Label columnLabel;
    private Label fpsLabel; // Khai báo fpsLabel ở đây
    private BitmapFont font; // Đưa font lên đây để dùng chung
    private ScoreManager scoreManager;
    private Label levelLabel;
    private int currentLevel = 1;
    private boolean showHealth = true;
    public void setShowHealth(boolean show) {
        this.showHealth = show;
    }
    public interface GameplayListener {
        void onPause();
    }

    public GameplayUI(SpriteBatch batch, final GameplayListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);

        // Khởi tạo font 1 lần duy nhất
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        scoreLabel = new Label("Score: 0", labelStyle);
        columnLabel = new Label("Steps: 0", labelStyle);
        comboLabel = new Label("", labelStyle);
        comboLabel.setColor(Color.YELLOW);

        // Khởi tạo fpsLabel 1 lần duy nhất
        fpsLabel = new Label("0 FPS", labelStyle);
        fpsLabel.setVisible(GameSettings.getInstance().showFPS);

        // Bảng chứa điểm và FPS ở góc trái
        Table topTable = new Table();
        topTable.top().left();
        topTable.setFillParent(true);
        topTable.add(scoreLabel).padTop(30).padLeft(15).left().row();  // padTop(30) đẩy xuống
        topTable.add(columnLabel).padTop(4).padLeft(15).left().row();  // padTop(4) gap nhỏ lại
        topTable.add(comboLabel).padTop(4).padLeft(15).left().row();   // tương tự
        topTable.add(fpsLabel).pad(15).left();


        levelLabel = new Label("LEVEL " + currentLevel, labelStyle);
        levelLabel.setColor(Color.YELLOW);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.overFontColor = Color.YELLOW;
        TextButton pauseButton = new TextButton("|| PAUSE", style);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onPause();
            }
        });
        // Bảng chứa nút Pause ở góc phải
        Table actionTable = new Table();
        actionTable.top().right();          // ← căn góc phải trên
        actionTable.setFillParent(true);
        actionTable.add(levelLabel).padTop(15).padRight(15).right().row();   // LEVEL 1
        actionTable.add(pauseButton).padTop(4).padRight(15).right().row();   // || PAUSE

        // Chỉ add lên stage 1 lần
        stage.addActor(topTable);
        stage.addActor(actionTable);
    }
    public void setLevel(int level) {
        this.currentLevel = level;
        if (levelLabel != null) levelLabel.setText("LEVEL " + level);
    }

    public void update(ScoreManager sm) {
        this.scoreManager = sm;

        // Cập nhật text liên tục ở đây
        scoreLabel.setText("Score: " + sm.getCurrentScore());
        columnLabel.setText("Steps: " + sm.getColumnsPassed());

        if (sm.getCombo() > 1) {
            comboLabel.setText("COMBO X" + sm.getCombo() + "!");
        } else {
            comboLabel.setText("");
        }

        // Ẩn/hiện FPS tùy thuộc vào cài đặt
        fpsLabel.setVisible(GameSettings.getInstance().showFPS);
    }

    public void render() {
        if (GameSettings.getInstance().showFPS) {
            fpsLabel.setText(Gdx.graphics.getFramesPerSecond() + " FPS");
        }

        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        // Cần giải phóng bộ nhớ của font để không bị memory leak của LibGDX
        if (font != null) {
            font.dispose();
        }
    }
}
