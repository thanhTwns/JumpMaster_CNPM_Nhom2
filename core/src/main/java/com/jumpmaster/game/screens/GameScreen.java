package com.jumpmaster.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen implements Screen {

    private final Game game;
    private final String mode;

    public GameScreen(Game game, String mode) {
        this.game = game;
        this.mode = mode;
    }

    @Override public void show() {}

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1); // màn hình đen tạm thời
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
