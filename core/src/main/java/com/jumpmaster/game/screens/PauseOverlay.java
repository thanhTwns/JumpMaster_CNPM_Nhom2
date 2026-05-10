package com.jumpmaster.game.screens;

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

public class PauseOverlay {
    public Stage stage;
    private final Texture backgroundTexture;

    public interface PauseListener {
        void onResume();
        void onMenu();
        void onQuit();
    }

    public PauseOverlay(SpriteBatch batch, final PauseListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);

        // background mờ đen
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.7f); // Tăng độ mờ một chút
        pixmap.fill();
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(new TextureRegionDrawable(backgroundTexture));

        // Font và Style
        BitmapFont font = new BitmapFont();
        font.getData().setScale(2.5f); // Tăng kích thước font nút
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.YELLOW;
        buttonStyle.downFontColor = Color.CYAN;

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label titleLabel = new Label("PAUSED", labelStyle);
        titleLabel.setFontScale(2.0f);

        TextButton resumeButton = new TextButton("RESUME", buttonStyle);
        TextButton menuButton = new TextButton("MENU", buttonStyle);
        TextButton quitButton = new TextButton("QUIT", buttonStyle);

        // Sự kiện nhấn nút
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onResume();
            }
        });

        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onMenu();
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onQuit();
            }
        });

        table.add(titleLabel).padBottom(60).row();
        table.add(resumeButton).padBottom(30).width(300).height(80).row();
        table.add(menuButton).padBottom(30).width(300).height(80).row();
        table.add(quitButton).width(300).height(80);

        stage.addActor(table);
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
