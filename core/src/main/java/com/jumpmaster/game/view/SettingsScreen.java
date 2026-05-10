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

    // sử dụng lại assets từ main
    private static final Color BG = new Color(0.05f, 0.05f, 0.10f, 1f);
    private static final Color RED = new Color(0.91f, 0.27f, 0.37f, 1f);
    private static final Color WHITE = new Color(Color.WHITE);
    private static final Color PURPLE = new Color(0.50f, 0.47f, 0.87f, 1f);

    private BitmapFont fontLarge;
    private BitmapFont fontMedium;
    private BitmapFont fontSmall;

    private float t = 0;
    private int screenW, screenH;

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

    // load font theo size
    private void loadFonts() {
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

        p.size = Math.round(base * 0.05f);
        p.color = Color.WHITE;
        p.borderWidth = 0f;
        fontMedium = generator.generateFont(p);

        p.size = Math.round(base * 0.035f);
        p.color = WHITE;
        fontSmall = generator.generateFont(p);

        generator.dispose();
    }

    // tạo element
    private void createSkin() {
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

        // volume slider
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.newDrawable("white", Color.GRAY);
        sliderStyle.background.setMinHeight(10);
        sliderStyle.knob = skin.newDrawable("red");
        sliderStyle.knob.setMinWidth(20);
        sliderStyle.knob.setMinHeight(30);
        skin.add("default-horizontal", sliderStyle);

        // toggle trigger
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

        // label
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

        // chọn small làm font default
        skin.add("default", labelSmall);

        // button
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

        Label title = new Label("SETTINGS", skin, "large");
        table.add(title).padBottom(50).colspan(2).row();

        final GameSettings settings = GameSettings.getInstance();

        // music slider
        Label musicLabel = new Label("Music Volume", skin, "small");
        table.add(musicLabel).left().padRight(20);

        final Slider musicSlider = new Slider(0, 1, 0.05f, false, skin);
        musicSlider.setValue(settings.musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.musicVolume = musicSlider.getValue();
                com.jumpmaster.game.AudioManager.getInstance().updateMusicVolume();
            }
        });
        table.add(musicSlider).width(200).padBottom(15).row();

        // sfx slider
        Label sfxLabel = new Label("SFX Volume", skin, "small");
        table.add(sfxLabel).left().padRight(20);

        final Slider sfxSlider = new Slider(0, 1, 0.05f, false, skin);
        sfxSlider.setValue(settings.sfxVolume);
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.sfxVolume = sfxSlider.getValue();
            }
        });
        table.add(sfxSlider).width(200).padBottom(30).row();

        // toggle buttons
        final CheckBox trajectoryCb = new CheckBox(" Show Trajectory", skin);
        trajectoryCb.setChecked(settings.showTrajectory);
        trajectoryCb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.showTrajectory = trajectoryCb.isChecked();
            }
        });
        table.add(trajectoryCb).colspan(2).left().padBottom(15).row();

        final CheckBox fpsCb = new CheckBox(" Show FPS Counter", skin);
        fpsCb.setChecked(settings.showFPS);
        fpsCb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.showFPS = fpsCb.isChecked();
            }
        });
        table.add(fpsCb).colspan(2).left().padBottom(25).row();

        // chọn mode màn hình
        table.add(new Label("Display Mode", skin, "small")).left().padRight(20);

        Table modeTable = new Table();
        final String[] modes = { "DEFAULT", "FULLSCREEN" };
        final Label modeLabel = new Label(modes[settings.displayMode], skin, "small");
        modeLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        TextButton leftArrow = new TextButton("<", skin, "small");
        TextButton rightArrow = new TextButton(">", skin, "small");

        leftArrow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.displayMode--;
                if (settings.displayMode < 0)
                    settings.displayMode = modes.length - 1;
                modeLabel.setText(modes[settings.displayMode]);
                applyDisplayMode(settings.displayMode);
            }
        });

        rightArrow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.displayMode++;
                if (settings.displayMode >= modes.length)
                    settings.displayMode = 0;
                modeLabel.setText(modes[settings.displayMode]);
                applyDisplayMode(settings.displayMode);
            }
        });

        modeTable.add(leftArrow).padRight(10);
        modeTable.add(modeLabel).width(120);
        modeTable.add(rightArrow).padLeft(10);
        table.add(modeTable).left().padBottom(40).row();

        // save option và quay lại main
        TextButton backBtn = new TextButton("BACK TO MENU", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.save();
                game.setScreen(new MainScreen(game));
            }
        });
        table.add(backBtn).colspan(2).padTop(20).row();

        stage.addActor(table);
    }

    private void applyDisplayMode(int mode) {
        if (mode == GameSettings.MODE_DEFAULT) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else if (mode == GameSettings.MODE_FULLSCREEN) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    @Override
    public void render(float delta) {
        t += delta;
        ScreenUtils.clear(BG.r, BG.g, BG.b, 1);

        sr.setProjectionMatrix(stage.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.10f, 0.23f, 1f);
        sr.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() * 0.13f);
        sr.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

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
