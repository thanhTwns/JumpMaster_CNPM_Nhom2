package com.jumpmaster.game;

import com.badlogic.gdx.Game;
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


    @Override
    public void create() {
        batch = new SpriteBatch();
        // Load settings and apply display mode
        GameSettings.getInstance().applyDisplayMode();

        AudioManager.getInstance().init();
        AudioManager.getInstance().playMenuMusic();// play menu music
        setScreen(new MainScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }


}
