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

public class PauseOverlay {
    public Stage stage;
    private final Texture backgroundTexture;
    private final Skin skin;

    private static final Color PURPLE   = new Color(0.50f, 0.47f, 0.87f, 1f);
    private static final Color DARK_BTN = new Color(0.12f, 0.12f, 0.25f, 0.95f);

    public interface PauseListener {
        void onResume();
        void onQuit();
    }

    public PauseOverlay(SpriteBatch batch, final PauseListener listener) {
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT), batch);
        skin = new Skin();

        // Background mờ đen cho toàn màn hình
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0, 0, 0, 0.7f);
        bgPixmap.fill();
        backgroundTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        // Tạo Texture cho nút bấm có khung (Border)
        Pixmap btnPixmap = new Pixmap(200, 60, Pixmap.Format.RGBA8888);
        // Vẽ viền ngoài
        btnPixmap.setColor(PURPLE);
        btnPixmap.fill();
        // Vẽ nền nút bên trong (để lại viền 3px)
        btnPixmap.setColor(DARK_BTN);
        btnPixmap.fillRectangle(3, 3, 194, 54);
        skin.add("button_bg", new Texture(btnPixmap));
        btnPixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        table.setBackground(new TextureRegionDrawable(backgroundTexture));

        // Font và Style
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("button_bg");
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.YELLOW;
        buttonStyle.downFontColor = Color.GRAY;

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label titleLabel = new Label("PAUSED", labelStyle);
        titleLabel.setFontScale(3.5f);

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

        // Xếp UI
        table.add(titleLabel).padBottom(60).row();
        table.add(resumeButton).width(200).height(60).padBottom(20).row();
        table.add(quitButton).width(200).height(60);

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
        skin.dispose();
        backgroundTexture.dispose();
    }
}
