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
import com.jumpmaster.game.AudioManager;
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
    private static final Color WHITE = new Color(Color.WHITE);
    private static final Color PURPLE = new Color(0.50f, 0.47f, 0.87f, 1f);

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

    private void loadFonts() {
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

        float base = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        p.size = Math.round(base * 0.1f);
        p.color = RED;
        p.borderWidth = 1.5f;
        p.borderColor = RED;
        fontLarge = generator.generateFont(p);

        p.size = Math.round(base * 0.055f);
        p.color = Color.WHITE;
        p.borderWidth = 0.5f;
        p.borderColor = RED;
        fontMedium = generator.generateFont(p);

        p.size = Math.round(base * 0.035f);
        p.color = WHITE;
        p.borderWidth = 0f;
        fontSmall = generator.generateFont(p);

        generator.dispose();
    }

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

        pixmap.setColor(PURPLE);
        pixmap.fill();
        skin.add("purple", new Texture(pixmap));

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.newDrawable("white", Color.GRAY);
        sliderStyle.background.setMinHeight(10);
        sliderStyle.knob = skin.newDrawable("red");
        sliderStyle.knob.setMinWidth(20);
        sliderStyle.knob.setMinHeight(30);
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

        Label.LabelStyle labelLarge = new Label.LabelStyle();
        labelLarge.font = fontLarge;
        labelLarge.fontColor = Color.WHITE;
        skin.add("large", labelLarge);

        Label.LabelStyle labelMedium = new Label.LabelStyle();
        labelMedium.font = fontMedium;
        labelMedium.fontColor = Color.WHITE;
        skin.add("medium", labelMedium);

        Label.LabelStyle labelSmall = new Label.LabelStyle();
        labelSmall.font = fontSmall;
        labelSmall.fontColor = Color.WHITE;
        skin.add("small", labelSmall);
        skin.add("default", labelSmall);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = fontSmall;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        listStyle.selection = skin.newDrawable("red");
        skin.add("default", listStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", scrollPaneStyle);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = fontSmall;
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        selectBoxStyle.scrollStyle = scrollPaneStyle;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = fontMedium;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.overFontColor = RED;
        textButtonStyle.downFontColor = Color.GRAY;
        skin.add("default", textButtonStyle);

        TextButton.TextButtonStyle smallBtnStyle = new TextButton.TextButtonStyle();
        smallBtnStyle.font = fontSmall;
        smallBtnStyle.fontColor = Color.WHITE;
        smallBtnStyle.overFontColor = RED;
        smallBtnStyle.downFontColor = Color.GRAY;
        skin.add("small", smallBtnStyle);

        pixmap.dispose();
    }

    private void buildUI() {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        GameSettings settings = GameSettings.getInstance();

        // Nút Back to Menu
        Table topTable = new Table();
        topTable.setFillParent(true);
        topTable.top().right();
        TextButton backBtn = new TextButton("BACK TO MENU", skin, "small");
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.save();
                game.setScreen(new MainScreen(game));
            }
        });
        topTable.add(backBtn).pad(15);
        stage.addActor(topTable);

        Label title = new Label("SETTINGS", skin, "large");
        table.add(title).padBottom(30).center().row();

        // ----------------- PHẦN SOUND -----------------
        Label soundHeader = new Label("SOUND", skin, "medium");
        soundHeader.setColor(RED);
        table.add(soundHeader).center().padBottom(10).row();

        Table soundTable = new Table();

        // Music Volume
        soundTable.add(new Label("Music Volume", skin, "small")).padRight(20);
        Slider musicSlider = new Slider(0, 1, 0.05f, false, skin);
        musicSlider.setValue(settings.musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.musicVolume = musicSlider.getValue();
                AudioManager.getInstance().updateMusicVolume();
            }
        });
        soundTable.add(musicSlider).width(200).padBottom(10).row();

        // SFX Volume
        soundTable.add(new Label("SFX Volume", skin, "small")).padRight(20);
        Slider sfxSlider = new Slider(0, 1, 0.05f, false, skin);
        sfxSlider.setValue(settings.sfxVolume);
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.sfxVolume = sfxSlider.getValue();
            }
        });
        soundTable.add(sfxSlider).width(200).row();

        table.add(soundTable).center().padBottom(20).row();

        // ----------------- PHẦN DISPLAY -----------------
        Label displayHeader = new Label("DISPLAY", skin, "medium");
        displayHeader.setColor(RED);
        table.add(displayHeader).center().padBottom(10).row();

        Table displayTable = new Table();

        CheckBox trajectoryCb = new CheckBox(" Show Trajectory", skin);
        trajectoryCb.setChecked(settings.showTrajectory);
        trajectoryCb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.showTrajectory = trajectoryCb.isChecked();
            }
        });
        displayTable.add(trajectoryCb).center().padBottom(10).row();

        CheckBox fpsCb = new CheckBox(" Show FPS Counter", skin);
        fpsCb.setChecked(settings.showFPS);
        fpsCb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.showFPS = fpsCb.isChecked();
            }
        });
        displayTable.add(fpsCb).center().padBottom(15).row();

        // Display Mode
        Table modeRow = new Table();
        modeRow.add(new Label("Display Mode: ", skin, "small")).padRight(20);
        String[] modes = { " DEFAULT ", " MAXIMIZED ", " FULLSCREEN " };
        SelectBox<String> modeSelect = new SelectBox<>(skin);
        modeSelect.setItems(modes);
        modeSelect.setSelectedIndex(MathUtils.clamp(settings.displayMode, 0, modes.length - 1));
        modeSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.displayMode = modeSelect.getSelectedIndex();
                applyDisplayMode(settings.displayMode);
            }
        });
        modeRow.add(modeSelect).width(200);
        displayTable.add(modeRow).center().row();

        table.add(displayTable).center().padBottom(30).row();

        stage.addActor(table);
    }

    private void applyDisplayMode(int mode) {
        GameSettings.getInstance().displayMode = mode;
        GameSettings.getInstance().applyDisplayMode();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BG.r, BG.g, BG.b, 1);
        sr.setProjectionMatrix(stage.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.10f, 0.23f, 1f);
        sr.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() * 0.13f);
        sr.end();

        stage.act(delta);
        stage.draw();

        if (GameSettings.getInstance().showFPS) {
            batch.begin();
            fontSmall.setColor(Color.WHITE);
            fontSmall.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", 10, Gdx.graphics.getHeight() - 10);
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        loadFonts();
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
