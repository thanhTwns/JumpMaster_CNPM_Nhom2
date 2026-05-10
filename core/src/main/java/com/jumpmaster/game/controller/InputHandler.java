package com.jumpmaster.game.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.jumpmaster.game.AudioManager;
import com.jumpmaster.game.model.Player;

public class InputHandler extends InputAdapter {
    private Player player;
    private Vector2 startPosition;

    public boolean isDragging = false;
    public Vector2 dragVector = new Vector2(0, 0);

    // FIX: track pointer ID để không bị lẫn lộn giữa các ngón tay
    private int activePointer = -1; // -1 = không có touch nào đang active

    public InputHandler(Player player) {
        this.player = player;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (player.isStunned) return false;
        if (Math.abs(player.body.getLinearVelocity().y) > 0.1f) {
            return false;
        }

        if (activePointer != -1) return false;

        activePointer = pointer;
        startPosition = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
        isDragging = true;
        dragVector.set(0, 0);
        AudioManager.getInstance().playPullSound();// phát âm thanh kéo
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // Chỉ xử lý đúng pointer đang active
        if (pointer != activePointer) return false;

        if (isDragging) {
            Vector2 currentPos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
            dragVector = currentPos.cpy().sub(startPosition);
            dragVector.limit(150f);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        // Chỉ xử lý đúng pointer đang active
        if (pointer != activePointer) return false;

        // Reset pointer ngay lập tức để sẵn sàng cho touch tiếp theo
        activePointer = -1;

        if (!isDragging) return false;
        isDragging = false;

        Vector2 endPosition = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);

        Vector2 force = startPosition.cpy().sub(endPosition);
        force.limit(150f);
        force.scl(0.005f);

        player.jump(force);
        AudioManager.getInstance().stopPullSound(); // Stop the loop
        AudioManager.getInstance().playLaunchSound();// phát âm thanh nhảy
        dragVector.set(0, 0); // reset drag vector sau khi nhảy
        return true;
    }

    // Gọi hàm này khi cần force reset (ví dụ khi pause hoặc game over)
    public void reset() {
        isDragging    = false;
        activePointer = -1;
        dragVector.set(0, 0);
    }
}
