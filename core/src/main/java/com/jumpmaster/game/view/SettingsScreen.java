package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ScreenUtils;
import com.jumpmaster.game.JumpMasterGame;

public class SettingsScreen implements Screen {

    private final JumpMasterGame game;

    public SettingsScreen(JumpMasterGame game) {
        this.game = game;
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Bấm bất kỳ đâu → về menu
        if (Gdx.input.justTouched()) {
            game.setScreen(new MainScreen(game));
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {}
}
