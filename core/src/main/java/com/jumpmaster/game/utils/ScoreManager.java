package com.jumpmaster.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ScoreManager {
    private static final String PREFS_NAME = "JumpMasterPrefs";
    private static final String KEY_HIGH_SCORE = "highScore";
    private Preferences prefs;
    private int currentScore;

    public ScoreManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        currentScore = 0;
    }

    public int getHighScore() {
        return prefs.getInteger(KEY_HIGH_SCORE, 0);
    }

    public void saveHighScore(int score) {
        if (score > getHighScore()) {
            prefs.putInteger(KEY_HIGH_SCORE, score);
            prefs.flush(); // EF1: Lưu thực sự xuống file
        }
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public void addScore(int points) {
        currentScore += points;
    }

    public void resetScore() {
        currentScore = 0;
    }
}
