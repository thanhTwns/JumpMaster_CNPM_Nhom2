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
    private ScoreManager scoreManager;
    private Label fpsLabel;

    public interface GameplayListener {
        void onPause();
    }

    public GameplayUI(SpriteBatch batch, final GameplayListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        scoreLabel = new Label("Score: 0", labelStyle);
        columnLabel = new Label("Steps: 0", labelStyle);
        comboLabel = new Label("", labelStyle);
        comboLabel.setColor(Color.YELLOW);

        Table topTable = new Table();
        topTable.top().left();
        topTable.setFillParent(true);
        topTable.add(scoreLabel).pad(15).left();
        topTable.row();
        topTable.add(columnLabel).pad(15).left();
        topTable.row();
        topTable.add(comboLabel).pad(15).left();

        Table actionTable = new Table();
        actionTable.top().right();
        actionTable.setFillParent(true);

        Table table = new Table();
        table.top();
        table.setFillParent(true);

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);

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

        actionTable.add(pauseButton).pad(15);

        stage.addActor(topTable);
        stage.addActor(actionTable);
    }

    public void update(ScoreManager sm) {
        this.scoreManager = sm;
        scoreLabel.setText("Score: " + sm.getCurrentScore());
        columnLabel.setText("Steps: " + sm.getColumnsPassed());
        if (sm.getCombo() > 1) {
            comboLabel.setText("COMBO X" + sm.getCombo() + "!");
        } else {
            comboLabel.setText("");
        }
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        fpsLabel = new Label("0 FPS", labelStyle);
        fpsLabel.setVisible(GameSettings.getInstance().showFPS);

        table.add(fpsLabel).left().expandX().pad(15);
        table.add(pauseButton).right().pad(15);

        stage.addActor(table);
    }

    public void render() {
        if (GameSettings.getInstance().showFPS) {
            fpsLabel.setVisible(true);
            fpsLabel.setText(Gdx.graphics.getFramesPerSecond() + " FPS");
        } else {
            fpsLabel.setVisible(false);
        }

        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
