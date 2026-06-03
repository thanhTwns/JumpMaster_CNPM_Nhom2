package com.jumpmaster.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.jumpmaster.game.view.EarthScreen;
import com.jumpmaster.game.view.MainScreen;
import com.jumpmaster.game.view.SpaceScreen;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all
 * platforms.
 */
public class JumpMasterGame extends Game {

    // private SpriteBatch batch;
    public SpriteBatch batch;
    private BitmapFont fpsFont;

    @Override
    public void create() {
        batch = new SpriteBatch();
        fpsFont = new BitmapFont();
        fpsFont.setColor(Color.YELLOW);

        // Load settings and apply display mode
        GameSettings.getInstance().applyDisplayMode();

        AudioManager.getInstance().init();
        AudioManager.getInstance().playMenuMusic();// play menu music
        setScreen(new MainScreen(this));
    }

    @Override
    public void render() {
        super.render();

        // Hiển thị FPS nếu được bật trong cài đặt
        if (GameSettings.getInstance().showFPS) {
            batch.begin();
            fpsFont.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (fpsFont != null) fpsFont.dispose();
    }
}
