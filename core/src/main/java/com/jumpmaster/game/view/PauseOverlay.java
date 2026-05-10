package com.jumpmaster.game.view;

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
        void onQuit();
    }

    public PauseOverlay(SpriteBatch batch, final PauseListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);

        // background mờ đen
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.6f);
        pixmap.fill();
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(new TextureRegionDrawable(backgroundTexture));

        // Font và Style
        BitmapFont font = new BitmapFont();
        font.getData().setScale(2f);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.YELLOW;

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label titleLabel = new Label("PAUSED", labelStyle);
        titleLabel.setFontScale(3f);

        TextButton resumeButton = new TextButton("RESUME", buttonStyle);
        TextButton quitButton = new TextButton("QUIT", buttonStyle);

        // Sự kiện nhấn nút
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onResume();
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.onQuit();
            }
        });

        // xếp UI
        table.add(titleLabel).padBottom(50).row();
        table.add(resumeButton).padBottom(20).row();
        table.add(quitButton);

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
