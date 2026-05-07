package com.jumpmaster.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.ScreenUtils;

public class SettingsScreen implements Screen {

    private final Game game;

    public SettingsScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Bấm bất kỳ đâu → về menu
        if (Gdx.input.justTouched()) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {}
}
