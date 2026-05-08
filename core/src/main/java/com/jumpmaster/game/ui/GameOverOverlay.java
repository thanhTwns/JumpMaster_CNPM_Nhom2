package com.jumpmaster.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.jumpmaster.game.utils.Constants;

public class GameOverOverlay {
    public Stage stage;
    private Label scoreLabel, highScoreLabel, recordLabel;
    private final Texture backgroundTexture;

    public interface GameOverListener {
        void onRestart();
        void onMenu();
    }

    public GameOverOverlay(SpriteBatch batch, final GameOverListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);

        // Tạo nền mờ
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.75f);
        pixmap.fill();
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(new TextureRegionDrawable(backgroundTexture));

        // Ép toàn bộ vào giữa tâm màn hình
        table.center();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(2.5f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.RED);
        Label.LabelStyle scoreStyle = new Label.LabelStyle(font, Color.WHITE);
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = font;
        btnStyle.fontColor = Color.YELLOW;

        Label title = new Label("GAME OVER", titleStyle);
        scoreLabel = new Label("Score: 0", scoreStyle);
        highScoreLabel = new Label("Best: 0", scoreStyle);
        recordLabel = new Label("NEW RECORD!", new Label.LabelStyle(font, Color.GREEN));
        recordLabel.setVisible(false);

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

        // --- LAYOUT ÉP SÁT ---
        // Giảm padBottom xuống mức cực thấp để các dòng dính vào nhau
        table.add(title).padBottom(15).row();       // Tiêu đề cách Score một chút
        table.add(scoreLabel).padBottom(2).row();    // Score sát Best
        table.add(highScoreLabel).padBottom(2).row(); // Best sát Record
        table.add(recordLabel).padBottom(15).row();  // Record sát nút bấm

        // Hai nút bấm dính gần như sát rạt nhau
        table.add(btnRestart).width(350).height(80).padBottom(5).row();
        table.add(btnMenu).width(350).height(80);

        stage.addActor(table);
    }

    public void setData(int score, int highScore, boolean isNewRecord) {
        scoreLabel.setText("Score: " + score);
        highScoreLabel.setText("Best: " + highScore);
        recordLabel.setVisible(isNewRecord);
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
        backgroundTexture.dispose();
    }
}
