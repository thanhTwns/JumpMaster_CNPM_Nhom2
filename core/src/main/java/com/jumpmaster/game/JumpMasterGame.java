package com.jumpmaster.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.jumpmaster.game.controller.GameScreen;
import com.jumpmaster.game.screens.MenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class JumpMasterGame extends Game {
//    private SpriteBatch batch;

    @Override
    public void create() {
//        batch = new SpriteBatch();
        setScreen(new MenuScreen(this));
    }

//    @Override
//    public void render() {
//        super.render();
//    }
//
//    @Override
//    public void dispose() {
//        batch.dispose();
//    }
}
