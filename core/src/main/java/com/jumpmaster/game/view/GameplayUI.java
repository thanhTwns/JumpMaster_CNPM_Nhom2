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

public class GameplayUI {
    public Stage stage;
    private Label fpsLabel;
    private Label scoreLabel;

    public interface GameplayListener {
        void onPause();
    }

    public GameplayUI(SpriteBatch batch, final GameplayListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);

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

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        fpsLabel = new Label("0 FPS", labelStyle);
        fpsLabel.setVisible(GameSettings.getInstance().showFPS);

        scoreLabel = new Label("Score: 0", labelStyle);

        table.add(fpsLabel).left().pad(15);
        table.add(scoreLabel).expandX().center().pad(15);
        table.add(pauseButton).right().pad(15);

        stage.addActor(table);
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
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
