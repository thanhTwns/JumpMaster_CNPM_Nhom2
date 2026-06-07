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
    private Label jumpLabel;
    private Label attemptLabel;
    private float attemptTimer = 0f;
    private Label fpsLabel; // Khai báo fpsLabel ở đây
    private BitmapFont font; // Đưa font lên đây để dùng chung
    private ScoreManager scoreManager;

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

        jumpLabel = new Label("", labelStyle);
        jumpLabel.setColor(Color.WHITE);

        attemptLabel = new Label("", labelStyle);
        attemptLabel.setColor(Color.ORANGE);
        attemptLabel.setVisible(false);

        // Khởi tạo fpsLabel 1 lần duy nhất
        fpsLabel = new Label("0 FPS", labelStyle);
        fpsLabel.setVisible(GameSettings.getInstance().showFPS);

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

        // Bảng chứa điểm và FPS ở góc trái
        Table topTable = new Table();
        topTable.top().left();
        topTable.setFillParent(true);
        
        // Dòng đầu tiên: Score (trái), Attempt (giữa), Pause (phải)
        topTable.add(scoreLabel).pad(15).left();
        topTable.add(attemptLabel).expandX().center();
        topTable.add(pauseButton).pad(15).right().row();
        
        topTable.add(columnLabel).pad(15).left().row();
        topTable.add(comboLabel).pad(15).left().row();
        topTable.add(jumpLabel).padTop(-35).padLeft(15).left().row();
        topTable.add(fpsLabel).pad(15).left(); // Nhét fpsLabel vào góc trái dưới combo

        // Chỉ add lên stage 1 lần
        stage.addActor(topTable);
    }

    public void update(ScoreManager sm, BaseScreen screen) {
        this.scoreManager = sm;

        // Cập nhật attempt count
        if (attemptTimer > 0) {
            attemptTimer -= Gdx.graphics.getDeltaTime();
            attemptLabel.setText("Attempt " + screen.game.attemptCount);
            attemptLabel.setVisible(true);
        } else {
            attemptLabel.setVisible(false);
        }

        // Cập nhật text liên tục ở đây
        scoreLabel.setText("Score: " + sm.getCurrentScore());
        columnLabel.setText("Steps: " + sm.getColumnsPassed());

        if (sm.getCombo() > 1) {
            comboLabel.setText("COMBO X" + sm.getCombo() + "!");
        } else {
            comboLabel.setText("");
        }
        if ("challenge".equals(screen.getMode())) {
            int remaining = BaseScreen.MAX_CHALLENGE_JUMPS
                - screen.getJumpCount();

            jumpLabel.setText("Jumps Left: " + remaining);
            jumpLabel.setVisible(true);
        } else {
            jumpLabel.setVisible(false);
        }
        // Ẩn/hiện FPS tùy thuộc vào cài đặt
        fpsLabel.setVisible(GameSettings.getInstance().showFPS);
    }

    public void showAttempt() {
        attemptTimer = 5f;
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
