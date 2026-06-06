package com.jumpmaster.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ScoreManager {
    private static final String PREFS_NAME    = "JumpMasterPrefs";
    private static final String KEY_HIGH_SCORE = "highScore";

    // ── 3.2.1.3c Top 5 Leaderboard keys ─────────────────────────────────────
    // Mỗi entry lưu 2 key: score_N và date_N (N = 0..4)
    // Không lưu trùng điểm — nếu điểm đã tồn tại thì bỏ qua.
    private static final int    TOP5_SIZE       = 5;
    private static final String KEY_TOP5_SCORE  = "top5_score_"; // + index
    private static final String KEY_TOP5_DATE   = "top5_date_";  // + index

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

    // ── 3.2.1.3c Top 5 Leaderboard ───────────────────────────────────────────

    /**
     * Lưu điểm vào Top 5 nếu đủ điều kiện.
     * Được gọi trong BaseScreen.triggerGameOver() sau flush().
     * Logic: load danh sách hiện tại → thêm entry mới →
     *        loại trùng điểm → sort giảm dần → giữ top 5 → lưu lại.
     */
    public void saveTopScores(int score) {
        if (score <= 0) return;

        // Load top 5 hiện tại
        java.util.List<long[]> entries = loadTopEntries();

        // Kiểm tra trùng điểm — nếu đã có thì không lưu thêm
        for (long[] e : entries) {
            if ((int) e[0] == score) return;
        }

        // Thêm entry mới: [score, timestamp]
        long now = System.currentTimeMillis();
        entries.add(new long[]{score, now});

        // Sort giảm dần theo điểm
        entries.sort((a, b) -> Integer.compare((int) b[0], (int) a[0]));

        // Giữ tối đa TOP5_SIZE
        if (entries.size() > TOP5_SIZE) {
            entries = entries.subList(0, TOP5_SIZE);
        }

        // Ghi xuống Preferences
        for (int i = 0; i < entries.size(); i++) {
            prefs.putInteger(KEY_TOP5_SCORE + i, (int) entries.get(i)[0]);
            prefs.putLong   (KEY_TOP5_DATE  + i,       entries.get(i)[1]);
        }
        // Xóa các slot thừa (khi danh sách ngắn hơn TOP5_SIZE)
        for (int i = entries.size(); i < TOP5_SIZE; i++) {
            prefs.remove(KEY_TOP5_SCORE + i);
            prefs.remove(KEY_TOP5_DATE  + i);
        }
        prefs.flush();
    }

    /**
     * Trả về danh sách Top 5 dưới dạng int[2][]:
     *   result[i][0] = score
     *   result[i][1] = timestamp (milliseconds)
     * Được gọi trong BaseScreen.triggerGameOver() → setLeaderboard().
     */
    public long[][] getTopScores() {
        java.util.List<long[]> entries = loadTopEntries();
        long[][] result = new long[entries.size()][2];
        for (int i = 0; i < entries.size(); i++) {
            result[i][0] = entries.get(i)[0];
            result[i][1] = entries.get(i)[1];
        }
        return result;
    }

    // Helper: load danh sách entry từ Preferences
    private java.util.List<long[]> loadTopEntries() {
        java.util.List<long[]> list = new java.util.ArrayList<>();
        for (int i = 0; i < TOP5_SIZE; i++) {
            if (prefs.contains(KEY_TOP5_SCORE + i)) {
                int  s = prefs.getInteger(KEY_TOP5_SCORE + i, 0);
                long d = prefs.getLong   (KEY_TOP5_DATE  + i, 0L);
                if (s > 0) list.add(new long[]{s, d});
            }
        }
        return list;
    }
}
