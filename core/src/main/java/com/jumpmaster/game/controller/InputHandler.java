package com.jumpmaster.game.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.jumpmaster.game.model.Player;

public class InputHandler extends InputAdapter {
    private Player player;
    private Vector2 startPosition;

    public boolean isDragging = false;
    public Vector2 dragVector = new Vector2(0, 0);

    public InputHandler(Player player) {
        this.player = player;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (isDragging) {
            Vector2 currentPos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
            dragVector = currentPos.cpy().sub(startPosition);
            dragVector.limit(150f);
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        //dao nguoc Y, ghi lai diem tiep xuc dau tien khi cham vao nhan vat
        startPosition = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
        isDragging = true; // Bắt đầu kéo
        dragVector.set(0, 0);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!isDragging) return false;
        isDragging = false;
        Vector2 endPosition = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);

        //tinh toan luc
        Vector2 force = startPosition.cpy().sub(endPosition);
        float maxDragDistance = 150f;
        force.limit(maxDragDistance);
        force.scl(0.005f);

        player.jump(force);
        return true;
    }

}
