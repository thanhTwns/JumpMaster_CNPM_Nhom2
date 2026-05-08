package com.jumpmaster.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.jumpmaster.game.screens.MainScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class JumpMasterGame extends Game {

//    private SpriteBatch batch;
    public SpriteBatch batch;


    @Override
    public void create() {
        batch = new SpriteBatch();
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
