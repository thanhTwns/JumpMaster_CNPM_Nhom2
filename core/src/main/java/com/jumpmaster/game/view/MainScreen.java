package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.controller.GameScreen;

public class MainScreen implements Screen {

    private final JumpMasterGame game;
    private SpriteBatch batch;
    private ShapeRenderer sr;
    private GlyphLayout layout;

    private BitmapFont fontLarge;
    private BitmapFont fontMedium;
    private BitmapFont fontSmall;

    private Texture iconSettings;
    private Texture iconTrophy;
    private Texture chickenTex;

    private int screenW, screenH;

    private static final Color BG       = new Color(0.05f, 0.05f, 0.10f, 1f);
    private static final Color RED      = new Color(0.91f, 0.27f, 0.37f, 1f);
    private static final Color PURPLE   = new Color(0.50f, 0.47f, 0.87f, 1f);
    private static final Color DARK_BTN = new Color(0.12f, 0.12f, 0.25f, 0.85f);
    private static final Color WHITE    = new Color(Color.WHITE);

    private static final int STAR_COUNT = 45;
    private float[] starX, starY, starPhase, starR;

    private static final float[] COL_H = {90, 150, 65, 110, 170, 80, 130};
    private static final float   COL_W = 22f;
    private float[] colBaseX;
    private float   scrollX = 0;

    private float bobT = 0;
    private float t    = 0;

    private Rectangle btnClassic, btnTimeAttack, btnChallenge, btnSettings, btnScores;

    public MainScreen(JumpMasterGame game) {
        this.game = game;
    }

    //

    private static final String DESC_CLASSIC =
        "CLASSIC MODE:\n" +
            "- Jump across platforms and reach the highest score.\n" +
            "- No time limit.";

    private static final String DESC_TIME_ATTACK =
        "TIME ATTACK MODE:\n" +
            "- Race against the clock.\n" +
            "- Defeat enemies and collect health potions.\n" +
            "- Find the portal and finish the stage as fast as possible.";
    private boolean isShowingPopup = false;
    private com.badlogic.gdx.math.Rectangle btnXacNhan;
    private String popupText = "";
    private String selectedModeTag = "";
    private ShapeRenderer shapeRenderer;
    private Rectangle btnBack;
    @Override
    public void show() {
        batch  = new SpriteBatch();
        sr     = new ShapeRenderer();
        layout = new GlyphLayout();

        iconSettings = new Texture(Gdx.files.internal("ui/settings_32dp_FFFFFF_FILL0_wght400_GRAD0_opsz40.png"));
        iconTrophy   = new Texture(Gdx.files.internal("ui/trophy_32dp_FFFFFF_FILL0_wght400_GRAD0_opsz40.png"));
        chickenTex   = new Texture(Gdx.files.internal("ui/chicken.png"));

        chickenTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        iconSettings.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        iconTrophy.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        shapeRenderer = new ShapeRenderer();
        //bổ sung nút xác nhận
        float btnW = 220f;
        float btnH = 60f;

        btnXacNhan = new com.badlogic.gdx.math.Rectangle(
            (screenW - btnW) / 2, (screenH / 2) - 120, btnW, btnH);

        btnBack = new Rectangle(0, 0, 120, 50);
    }

    @Override
    public void resize(int width, int height) {
        screenW = width;
        screenH = height;
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        sr.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        loadFonts();
        setupButtons();
        initStars();
        initCols();

        float popupW = screenW * 0.75f;
        float popupH = screenH * 0.55f;
        float popupX = (screenW - popupW) / 2;
        float popupY = (screenH - popupH) / 2;

        float btnW = 220;
        float btnH = 60;

        btnXacNhan = new Rectangle(
            popupX + popupW - btnW - 40,
            popupY + 20,
            btnW,
            btnH
        );
        btnBack.set(
            popupX + 40,
            popupY + 20,
            120,
            50
        );
    }

    private void loadFonts() {
        if (fontLarge  != null) fontLarge.dispose();
        if (fontMedium != null) fontMedium.dispose();
        if (fontSmall  != null) fontSmall.dispose();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("font/NunitoSans-Italic-VariableFont_YTLC,opsz,wdth,wght.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.hinting    = FreeTypeFontGenerator.Hinting.Full;
        p.kerning    = true;
        p.genMipMaps = true;
        p.minFilter  = Texture.TextureFilter.MipMapLinearNearest;
        p.magFilter  = Texture.TextureFilter.Linear;
        float base = Math.min(screenW, screenH);

        p.size = Math.round(base * 0.1f);
        p.color = RED;
        p.borderWidth = 1.5f;
        p.borderColor = RED;
        fontLarge = generator.generateFont(p);

        p.size = Math.round(base * 0.060f);
        p.color = Color.WHITE;
        p.borderWidth = 0f;
        fontMedium = generator.generateFont(p);

        p.size = Math.round(base * 0.038f);
        p.color = WHITE;
        fontSmall = generator.generateFont(p);

        generator.dispose();
    }

    private void initStars() {
        starX = new float[STAR_COUNT]; starY = new float[STAR_COUNT];
        starPhase = new float[STAR_COUNT]; starR = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = MathUtils.random(0f, screenW);
            starY[i] = MathUtils.random(screenH * 0.25f, screenH);
            starPhase[i] = MathUtils.random(0f, MathUtils.PI2);
            starR[i] = MathUtils.random(0.8f, 2f);
        }
    }

    private void initCols() {
        colBaseX = new float[COL_H.length];
        float spacing = screenW / (COL_H.length - 1f);
        for (int i = 0; i < COL_H.length; i++) colBaseX[i] = i * spacing;
    }

    private void setupButtons() {
        float W = screenW, H = screenH;
        float btnH = H * 0.085f;
        float startY = H * 0.45f, gap = btnH + H * 0.02f;

        // Tìm chiều rộng lớn nhất để các nút bằng nhau
        float maxW = 0;
        layout.setText(fontMedium, "CLASSIC");
        maxW = Math.max(maxW, layout.width);
        layout.setText(fontMedium, "TIME ATTACK");
        maxW = Math.max(maxW, layout.width);
        layout.setText(fontMedium, "CHALLENGE");
        maxW = Math.max(maxW, layout.width);

        float btnW = maxW + 200; // Làm nút dài thêm (trước đó là 150)

        btnClassic = new Rectangle((W - btnW) / 2f, startY, btnW, btnH);
        btnTimeAttack = new Rectangle((W - btnW) / 2f, startY - gap, btnW, btnH);
        btnChallenge = new Rectangle((W - btnW) / 2f, startY - gap*2, btnW, btnH);

        float iconSize = H * 0.09f, iconY = startY - gap*2 - iconSize - H * 0.04f, iconGap = W * 0.09f;
        btnSettings = new Rectangle(W/2f - iconSize - iconGap, iconY, iconSize, iconSize);
        btnScores   = new Rectangle(W/2f + iconGap, iconY, iconSize, iconSize);
    }

    @Override
    public void render(float delta) {
        if (fontLarge == null) return;
        t += delta; bobT += delta; scrollX -= delta * 60f;
        if (scrollX < -(screenW / (float) COL_H.length + COL_W)) scrollX = 0;
        handleInput();

        Gdx.gl.glClearColor(BG.r, BG.g, BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawStars();
        drawGround();
        drawColumns();
        drawButtonBackgrounds();

        batch.begin();
        drawCharacter();
        drawIcons();
        drawText();
        batch.end();

        if (isShowingPopup) {

            float popupW = screenW * 0.75f;
            float popupH = screenH * 0.55f;
            float popupX = (screenW - popupW) / 2;
            float popupY = (screenH - popupH) / 2;

            // =====================
            // VẼ NỀN POPUP
            // =====================


            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // nền tối toàn màn hình
            shapeRenderer.setColor(0, 0, 0, 0.75f);
            shapeRenderer.rect(0, 0, screenW, screenH);

            // bóng popup
            shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
            shapeRenderer.rect(
                popupX + 8,
                popupY - 8,
                popupW,
                popupH
            );

            // nền popup
            shapeRenderer.setColor(0.12f, 0.15f, 0.22f, 1);
            shapeRenderer.rect(
                popupX,
                popupY,
                popupW,
                popupH
            );



            shapeRenderer.end();



            batch.begin();


            fontMedium.setColor(Color.GOLD);

            fontMedium.draw(
                batch,
                "GAME RULES",
                popupX + popupW/2 - 90,
                popupY + popupH - 35
            );

            // đường kẻ dưới tiêu đề
            fontSmall.setColor(Color.LIGHT_GRAY);

            fontSmall.draw(
                batch,
                "----------------------------",
                popupX + 40,
                popupY + popupH - 60
            );


            fontSmall.setColor(Color.WHITE);

            fontSmall.draw(
                batch,
                popupText,
                popupX + 40,
                popupY + popupH - 100,
                popupW - 80,
                Align.left,
                true
            );


            fontMedium.setColor(Color.WHITE);

            GlyphLayout layout = new GlyphLayout(fontMedium, "START");

            fontMedium.draw(
                batch,
                "START",
                btnXacNhan.x + (btnXacNhan.width - layout.width) / 2,
                btnXacNhan.y + (btnXacNhan.height + layout.height) / 2
            );
            GlyphLayout backLayout = new GlyphLayout(fontMedium, "BACK");

            fontMedium.draw(
                batch,
                "BACK",
                btnBack.x + (btnBack.width - backLayout.width) / 2,
                btnBack.y + (btnBack.height + backLayout.height) / 2
            );

            batch.end();
        }




    }

    private void drawStars() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < STAR_COUNT; i++) {
            float alpha = 0.25f + 0.25f * MathUtils.sin(t * 1.4f + starPhase[i]);
            sr.setColor(0.70f, 0.70f, 1f, alpha);
            sr.circle(starX[i], starY[i], starR[i]);
        }
        sr.end();
    }

    private void drawGround() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.10f, 0.23f, 1f);
        sr.rect(0, 0, screenW, screenH * 0.13f);
        sr.setColor(0.13f, 0.13f, 0.28f, 1f);
        sr.rect(0, screenH * 0.13f, screenW, 2f);
        sr.end();
    }

    private void drawColumns() {
        float groundH = screenH * 0.13f, scale = screenH / 600f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(RED);
        for (int i = 0; i < COL_H.length; i++) {
            float x = colBaseX[i] + scrollX;
            float h = COL_H[i] * scale;
            while (x < -COL_W) x += screenW + COL_W;
            while (x > screenW) x -= screenW + COL_W;
            sr.rect(x, groundH, COL_W, h);
        }
        sr.end();
    }

    private void drawButtonBackgrounds() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        Rectangle[] buttons = {btnClassic, btnTimeAttack, btnChallenge, btnSettings, btnScores};
        for (Rectangle r : buttons) {
            sr.setColor(0, 0, 0, 0.4f);
            sr.rect(r.x + 4, r.y - 4, r.width, r.height);
            sr.setColor(PURPLE);
            sr.rect(r.x - 4, r.y - 4, r.width + 8, r.height + 8);
            sr.setColor(DARK_BTN);
            sr.rect(r.x, r.y, r.width, r.height);
            sr.setColor(1, 1, 1, 0.08f);
            sr.rect(r.x, r.y + r.height * 0.5f, r.width, r.height * 0.5f);
        }
        sr.end();
    }

    private void drawCharacter() {
        float groundH = screenH * 0.13f, size = screenH * 0.12f;
        float cy = groundH + MathUtils.sin(bobT * 2.5f) * screenH * 0.018f;
        batch.setColor(Color.WHITE);
        batch.draw(chickenTex, screenW / 2f - size / 2f, cy, size, size);
    }

    private void drawIcons() {
        float pad = btnSettings.width * 0.14f;
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        batch.draw(iconSettings, btnSettings.x + pad, btnSettings.y + pad, btnSettings.width - pad*2, btnSettings.height - pad*2);
        batch.draw(iconTrophy, btnScores.x + pad, btnScores.y + pad, btnScores.width - pad*2, btnScores.height - pad*2);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawText() {
        float titleY = screenH * 0.92f;
        fontLarge.setColor(RED);
        layout.setText(fontLarge, "JUMPMASTER");
        float titleW = layout.width;
        float titleH = layout.height;
        fontLarge.draw(batch, layout, (screenW - titleW) / 2f, titleY);

        fontSmall.setColor(WHITE);
        layout.setText(fontSmall, "DRAG TO SHOOT");
        // Đặt sát ngay dưới JUMPMASTER
        fontSmall.draw(batch, layout, (screenW - layout.width) / 2f, titleY - titleH - 15);

        layout.setText(fontSmall, "SELECT GAME MODE");
        fontSmall.draw(batch, layout, (screenW - layout.width) / 2f, btnClassic.y + btnClassic.height + screenH * 0.05f);

        fontMedium.setColor(Color.WHITE); drawCenteredText(fontMedium, "CLASSIC", btnClassic);
        fontMedium.setColor(RED); drawCenteredText(fontMedium, "TIME ATTACK", btnTimeAttack);
        fontMedium.setColor(new Color(0.68f, 0.62f, 1f, 1f)); drawCenteredText(fontMedium, "CHALLENGE", btnChallenge);

        float labelY = btnSettings.y - fontSmall.getLineHeight() * 1.2f;
        layout.setText(fontSmall, "SETTINGS");
        fontSmall.draw(batch, layout, btnSettings.x + (btnSettings.width - layout.width) / 2f, labelY);
        layout.setText(fontSmall, "SCORES");
        fontSmall.draw(batch, layout, btnScores.x + (btnScores.width - layout.width) / 2f, labelY);
    }

    private void drawCenteredText(BitmapFont f, String text, Rectangle btn) {
        layout.setText(f, text);
        f.draw(batch, layout, btn.x + (btn.width - layout.width) / 2f, btn.y + (btn.height + layout.height) / 2f);
    }

    private void handleInput() {
        if (!Gdx.input.justTouched()) return;
        float tx = Gdx.input.getX(), ty = screenH - Gdx.input.getY();


        if (isShowingPopup) {

            // BACK
            if (btnBack.contains(tx, ty)) {
                isShowingPopup = false;
                return;
            }

            // START
            if (btnXacNhan.contains(tx, ty)) {
                Gdx.app.log("SystemLog",
                    "XAC NHAN HOP LE: Bat dau van choi " + selectedModeTag);

                isShowingPopup = false;
                game.setScreen(new GameScreen(game, selectedModeTag));
                return;
            }

            return;
        }


        if (btnClassic.contains(tx, ty)) {

            com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("GameConfig");
            prefs.putString("current_mode", "MODE_CLASSIC");
            prefs.putFloat("initial_score", 0f);
            prefs.putFloat("time_limit", -1f);
            prefs.flush();


            popupText = DESC_CLASSIC;
            selectedModeTag = "classic";
            isShowingPopup = true;
        }
        else if (btnTimeAttack.contains(tx, ty)) {

            com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("GameConfig");
            prefs.putString("current_mode", "MODE_TIME_ATTACK");
            prefs.putFloat("initial_score", 0f);
            prefs.putFloat("time_limit", 60f);
            prefs.flush();


            popupText = DESC_TIME_ATTACK;
            selectedModeTag = "time_attack";
            isShowingPopup = true;
        }
        else if (btnChallenge.contains(tx, ty)) {
            Gdx.app.log("SystemLog", "CHE DO DANG PHAT TRIEN: Vui long chon che do khac!");
            Gdx.app.log("SystemLog", "LOI: Che do choi khong hop le hoac chua hoan thien!");
        }
        else if (btnSettings.contains(tx, ty)) {
            game.setScreen(new SettingsScreen(game));
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose(); sr.dispose();
        if (fontLarge != null) fontLarge.dispose();
        if (fontMedium != null) fontMedium.dispose();
        if (fontSmall  != null) fontSmall.dispose();
        iconSettings.dispose(); iconTrophy.dispose(); chickenTex.dispose();
    }
    /**
     * Thực hiện chuỗi nghiệp vụ từ Bước 3 đến Bước 7 theo đúng đặc tả thiết kế
     * @param selectedMode Chế độ người chơi nhấp chọn ("classic", "time_attack", hoặc "challenge")
     * @return true nếu chế độ hợp lệ và nạp thông số thành công, false nếu bị từ chối
     */
    private boolean validateAndSetupGameMode(String selectedMode) {

        // --- BƯỚC 3: Hệ thống hiển thị thông tin mô tả và luật chơi của chế độ đã chọn ---
        String infoMessage = "";
        if ("classic".equalsIgnoreCase(selectedMode)) {
            infoMessage = DESC_CLASSIC;
        } else if ("time_attack".equalsIgnoreCase(selectedMode)) {
            infoMessage = DESC_TIME_ATTACK;
        } else {
            infoMessage = "CHE DO DANG PHAT TRIEN: Vui long chon che do choi khac!";
        }

        // In thông tin đặc tả luật chơi ra Logcat hệ thống để kiểm tra nghiệp vụ
        Gdx.app.log("SystemLog", infoMessage);

        // --- BƯỚC 4: Người chơi xác nhận lựa chọn ---
        // (Bước này được ghi nhận khi hàm xử lý phản hồi thành công)

        // --- BƯỚC 5: Hệ thống kiểm tra tính hợp lệ của chế độ chơi được chọn ---
        // "Challenge" được coi là [Chế độ đang phát triển] nên hệ thống sẽ chặn lại, không cho vào chơi
        if (!"classic".equalsIgnoreCase(selectedMode) && !"time_attack".equalsIgnoreCase(selectedMode)) {
            Gdx.app.log("SystemLog", "LOI: Che do choi khong hop le hoac chua hoan thien!");
            return false; // Trả về thất bại, chặn đứng không chuyển màn hình
        }

        // --- BƯỚC 6 & 7: Hệ thống tải cấu hình và khởi tạo các tham số ván chơi ban đầu ---
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("GameConfig");

        if ("classic".equalsIgnoreCase(selectedMode)) {
            prefs.putString("current_mode", "MODE_CLASSIC");
            prefs.putFloat("initial_score", 0f);
            prefs.putFloat("time_limit", -1f); // Classic không giới hạn thời gian
        } else if ("time_attack".equalsIgnoreCase(selectedMode)) {
            prefs.putString("current_mode", "MODE_TIME_ATTACK");
            prefs.putFloat("initial_score", 0f);
            prefs.putFloat("time_limit", 60f); // Thiết lập luật chơi giới hạn thời gian 60 giây
        }
        prefs.flush(); // Đồng bộ ghi dữ liệu cấu hình xuống thiết bị bộ nhớ tạm

        Gdx.app.log("SystemLog", "XAC NHAN HOP LE: Da thiet lap xong tham so ván choi cho: " + selectedMode);
        return true; // Hợp lệ hoàn toàn, cho đi tiếp
    }
}
