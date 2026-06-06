package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.jumpmaster.game.GameSettings;
import com.jumpmaster.game.JumpMasterGame;

public class SettingsScreen implements Screen {

    private final JumpMasterGame game;
    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;
    private ShapeRenderer sr;

    private static final Color BG = new Color(0.05f, 0.05f, 0.10f, 1f);
    private static final Color RED = new Color(0.91f, 0.27f, 0.37f, 1f);
    private static final Color YELLOW = new Color(Color.YELLOW);

    private BitmapFont fontLarge;
    private BitmapFont fontMedium;
    private BitmapFont fontSmall;

    public SettingsScreen(JumpMasterGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        sr = new ShapeRenderer();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        loadFonts();
        createSkin();
        buildUI();
    }

    // UC 1.1: Cập nhật font chữ phù hợp với thiết kế Settings mới
    private void loadFonts() {
        loadFonts(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void loadFonts(int width, int height) {
        if (fontLarge != null) fontLarge.dispose();
        if (fontMedium != null) fontMedium.dispose();
        if (fontSmall != null) fontSmall.dispose();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("font/NunitoSans-Italic-VariableFont_YTLC,opsz,wdth,wght.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.hinting = FreeTypeFontGenerator.Hinting.Full;
        p.kerning = true;
        p.genMipMaps = true;
        p.minFilter = Texture.TextureFilter.MipMapLinearNearest;
        p.magFilter = Texture.TextureFilter.Linear;

        float base = Math.min(width, height);

        p.size = Math.round(base * 0.12f);
        p.color = RED;
        fontLarge = generator.generateFont(p);

        p.size = Math.round(base * 0.06f);
        p.color = RED;
        fontMedium = generator.generateFont(p);

        p.size = Math.round(base * 0.04f);
        p.color = Color.WHITE;
        fontSmall = generator.generateFont(p);

        generator.dispose();
    }

    // UC 1.1: Tạo các Style mới cho SelectBox, Slider và CheckBox theo mẫu thiết kế
    private void createSkin() {
        if (skin != null) skin.dispose();
        skin = new Skin();
        skin.add("default-font", fontSmall);

        Pixmap pixmap = new Pixmap(10, 10, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        pixmap.setColor(RED);
        pixmap.fill();
        skin.add("red", new Texture(pixmap));

        pixmap.setColor(0.2f, 0.2f, 0.2f, 1f);
        pixmap.fill();
        skin.add("dark_grey", new Texture(pixmap));

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.newDrawable("white", Color.GRAY);
        sliderStyle.background.setMinHeight(6);
        sliderStyle.knob = skin.newDrawable("red");
        sliderStyle.knob.setMinWidth(22);
        sliderStyle.knob.setMinHeight(22);
        skin.add("default-horizontal", sliderStyle);

        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = fontSmall;
        checkBoxStyle.fontColor = Color.WHITE;
        checkBoxStyle.checkboxOff = skin.newDrawable("white", Color.DARK_GRAY);
        checkBoxStyle.checkboxOff.setMinWidth(25);
        checkBoxStyle.checkboxOff.setMinHeight(25);
        checkBoxStyle.checkboxOn = skin.newDrawable("red");
        checkBoxStyle.checkboxOn.setMinWidth(25);
        checkBoxStyle.checkboxOn.setMinHeight(25);
        skin.add("default", checkBoxStyle);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = fontSmall;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        listStyle.selection = skin.newDrawable("red");
        listStyle.background = skin.newDrawable("dark_grey");
        skin.add("default", listStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = skin.newDrawable("dark_grey");
        skin.add("default", scrollPaneStyle);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = fontSmall;
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = skin.newDrawable("dark_grey");
        selectBoxStyle.scrollStyle = scrollPaneStyle;
        selectBoxStyle.listStyle = listStyle;
        selectBoxStyle.overFontColor = RED;
        skin.add("default", selectBoxStyle);

        skin.add("large", new Label.LabelStyle(fontLarge, Color.WHITE));
        skin.add("medium", new Label.LabelStyle(fontMedium, Color.WHITE));
        skin.add("small", new Label.LabelStyle(fontSmall, Color.WHITE));

        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        backStyle.font = fontSmall;
        backStyle.fontColor = Color.WHITE;
        backStyle.overFontColor = RED;
        skin.add("back", backStyle);

        pixmap.dispose();
    }

    // UC 1.1: Xây dựng UI Settings với sự căn chỉnh Slider và SelectBox thẳng hàng
    private void buildUI() {
        final GameSettings settings = GameSettings.getInstance();

        // Nút Back ở góc trên bên phải
        Table topTable = new Table();
        topTable.setFillParent(true);
        topTable.top().right();
        TextButton backBtn = new TextButton("BACK TO MENU", skin, "back");
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.save();
                game.setScreen(new MainScreen(game));
            }
        });
        topTable.add(backBtn).pad(20);
        stage.addActor(topTable);

        // Table chính (chứa toàn bộ nội dung)
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();

        // Tiêu đề SETTINGS
        Label title = new Label("SETTINGS", skin, "large");
        title.setColor(RED);
        mainTable.add(title).padBottom(40).row();

        Label soundHeader = new Label("SOUND", skin, "medium");
        soundHeader.setColor(RED);
        mainTable.add(soundHeader).padBottom(20).row();

        Table soundTable = new Table();
        soundTable.columnDefaults(0).left().padRight(40);
        soundTable.columnDefaults(1).left();

        soundTable.add(new Label("Music Volume", skin, "small")).padBottom(15);
        final Slider musicSlider = new Slider(0, 1, 0.05f, false, skin);
        musicSlider.setValue(settings.musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.musicVolume = musicSlider.getValue();
                com.jumpmaster.game.AudioManager.getInstance().updateMusicVolume();
            }
        });
        soundTable.add(musicSlider).width(250).padBottom(15).row();

        // SFX Volume
        soundTable.add(new Label("SFX Volume", skin, "small")).padBottom(15);
        final Slider sfxSlider = new Slider(0, 1, 0.05f, false, skin);
        sfxSlider.setValue(settings.sfxVolume);
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.sfxVolume = sfxSlider.getValue();
            }
        });
        soundTable.add(sfxSlider).width(250).padBottom(15).row();
        mainTable.add(soundTable).padBottom(40).row();

        // UC-1.1: Cập nhật phần DISPLAY (Trajectory và FPS Counter)
        Label displayHeader = new Label("DISPLAY", skin, "medium");
        displayHeader.setColor(RED);
        mainTable.add(displayHeader).padBottom(20).row();

        Table optionsTable = new Table();
        optionsTable.columnDefaults(0).left();

        final CheckBox trajectoryCb = new CheckBox(" Show Trajectory", skin);
        trajectoryCb.setChecked(settings.showTrajectory);
        trajectoryCb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.showTrajectory = trajectoryCb.isChecked();
            }
        });
        optionsTable.add(trajectoryCb).padBottom(15).row();

        final CheckBox fpsCb = new CheckBox(" Show FPS Counter", skin);
        fpsCb.setChecked(settings.showFPS);
        fpsCb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.showFPS = fpsCb.isChecked();
            }
        });
        optionsTable.add(fpsCb).padBottom(20).row();
        mainTable.add(optionsTable).row();

        // UC-1.1: Sử dụng SelectBox cho Display Mode theo yêu cầu
        Table modeRow = new Table();
        modeRow.add(new Label("Display Mode: ", skin, "small")).padRight(15);

        final String[] modes = { "DEFAULT", "MAXIMIZED", "FULLSCREEN" };
        final SelectBox<String> selectBox = new SelectBox<>(skin);
        selectBox.setItems(modes);
        selectBox.setSelectedIndex(MathUtils.clamp(settings.displayMode, 0, modes.length - 1));
        selectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.displayMode = selectBox.getSelectedIndex();
                settings.applyDisplayMode();
            }
        });
        modeRow.add(selectBox).width(160);
        mainTable.add(modeRow).center().row();

        stage.addActor(mainTable);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BG.r, BG.g, BG.b, 1);

        batch.setProjectionMatrix(stage.getCamera().combined);
        sr.setProjectionMatrix(stage.getCamera().combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.10f, 0.23f, 1f);
        sr.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() * 0.12f);
        sr.end();

        stage.act(delta);
        stage.draw();

        // UC 1.1: Cập nhật hiển thị FPS màu vàng ở góc trái phía trên
        if (GameSettings.getInstance().showFPS) {
            batch.begin();
            fontSmall.setColor(YELLOW);
            fontSmall.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 15, Gdx.graphics.getHeight() - 15);
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        loadFonts(width, height);
        createSkin();
        stage.clear();
        buildUI();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        sr.dispose();
        fontLarge.dispose();
        fontMedium.dispose();
        fontSmall.dispose();
    }
}
