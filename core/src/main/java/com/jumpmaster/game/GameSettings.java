package com.jumpmaster.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

// toàn bộ cài đặt của game
public class GameSettings {
    private static final String PREFS_NAME = "jumpmaster_settings";

    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_SFX_VOLUME = "sfx_volume";
    private static final String KEY_SHOW_TRAJECTORY = "show_trajectory";
    private static final String KEY_SHOW_FPS = "show_fps";
    private static final String KEY_DISPLAY_MODE = "display_mode";

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_FULLSCREEN = 1;

    public float musicVolume = 0.5f;
    public float sfxVolume = 0.8f;
    public boolean showTrajectory = true;
    public boolean showFPS = false;
    public int displayMode = MODE_DEFAULT;

    private static GameSettings instance;

    private GameSettings() {
    }

    public static GameSettings getInstance() {
        if (instance == null) {
            instance = new GameSettings();
            instance.load();
        }
        return instance;
    }

    // load cài đặt
    public void load() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.5f);
        sfxVolume = prefs.getFloat(KEY_SFX_VOLUME, 0.8f);
        showTrajectory = prefs.getBoolean(KEY_SHOW_TRAJECTORY, true);
        showFPS = prefs.getBoolean(KEY_SHOW_FPS, false);
        displayMode = prefs.getInteger(KEY_DISPLAY_MODE, MODE_DEFAULT);
    }

    // save preferences
    public void save() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat(KEY_MUSIC_VOLUME, musicVolume);
        prefs.putFloat(KEY_SFX_VOLUME, sfxVolume);
        prefs.putBoolean(KEY_SHOW_TRAJECTORY, showTrajectory);
        prefs.putBoolean(KEY_SHOW_FPS, showFPS);
        prefs.putInteger(KEY_DISPLAY_MODE, displayMode);
        prefs.flush();
    }
}
