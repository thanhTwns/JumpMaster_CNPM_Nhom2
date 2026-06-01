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
    private final Texture backgroundTexture;
    private final Skin skin;

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
        table.center();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(2.2f);

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

        // Xếp UI
        table.add(title).padBottom(40).row();
        table.add(scoreLabel).padBottom(5).row();
        table.add(highScoreLabel).padBottom(5).row();
        table.add(recordLabel).padBottom(30).row();

        table.add(btnRestart).width(350).height(80).padBottom(20).row();
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
        skin.dispose();
        backgroundTexture.dispose();
    }
}
