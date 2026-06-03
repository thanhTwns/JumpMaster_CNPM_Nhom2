package com.jumpmaster.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ScoreManager {
    private static final String PREFS_NAME = "JumpMasterPrefs";
    private static final String KEY_HIGH_SCORE = "highScore";
    private Preferences prefs;

    private int currentScore;
    private int columnsPassed;
    private int combo;
    private float comboTimer;
    private static final float COMBO_DURATION = 5.0f; // Combo kéo dài 5 giây
    private int highScore;

    // ── 3.2.1.3b Stats phiên chơi ───────────────────────────────────────────
    // Được lấy qua getStats() → truyền vào GameOverOverlay.setStats()
    private int   maxCombo      = 0;   // combo cao nhất đạt được trong phiên
    private float survivalTime  = 0f;  // thời gian sống sót (giây)

    public ScoreManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        highScore = prefs.getInteger(KEY_HIGH_SCORE, 0);
        resetSession();
    }

    public void resetSession() {
        currentScore  = 0;
        columnsPassed = 0;
        combo         = 0;
        comboTimer    = 0;
        // ── 3.2.1.3b reset stats phiên mới ──────────────────────────────
        maxCombo     = 0;
        survivalTime = 0f;
    }

    public void update(float delta) {
        // ── 3.2.1.3b đếm thời gian sống sót ────────────────────────────
        survivalTime += delta;

        if (combo > 0) {
            comboTimer -= delta;
            if (comboTimer <= 0) {
                resetCombo();
            }
        }
    }

    public int getHighScore() {
        return highScore;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getColumnsPassed() {
        return columnsPassed;
    }

    public int getCombo() {
        return combo;
    }

    public void addPoints(int points) {
        currentScore += points;
        if (currentScore > highScore) {
            highScore = currentScore;
            // Lưu ngay lập tức để tránh mất dữ liệu nếu app crash trước flush()
            saveHighScore(highScore);
        }
    }

    public void incrementColumns() {
        columnsPassed++;
    }

    public void incrementCombo() {
        combo++;
        comboTimer = COMBO_DURATION; // Reset thời gian mỗi khi có combo mới
        // ── 3.2.1.3b cập nhật maxCombo nếu vượt qua ─────────────────────
        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    public void resetCombo() {
        combo = 0;
        comboTimer = 0;
    }

    // ── 3.2.1.3b Getters cho stats phiên chơi ───────────────────────────────
    public int getMaxCombo() {
        return maxCombo;
    }

    public float getSurvivalTime() {
        return survivalTime;
    }

    /**
     * Trả về snapshot stats phiên hiện tại.
     * Được gọi trong BaseScreen.triggerGameOver() → truyền vào setStats().
     * int[0] = columnsPassed
     * int[1] = maxCombo
     * int[2] = survivalTime (làm tròn giây)
     */
    public int[] getStats() {
        return new int[]{columnsPassed, maxCombo, (int) survivalTime};
    }

    public void saveHighScore(int score) {
        if (score > prefs.getInteger(KEY_HIGH_SCORE, 0)) {
            prefs.putInteger(KEY_HIGH_SCORE, score);
            prefs.flush();
        }
    }

    public void flush() {
        saveHighScore(currentScore);
    }
}
