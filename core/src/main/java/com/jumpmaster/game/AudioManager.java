package com.jumpmaster.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class AudioManager {
    private static AudioManager instance;

    private Music backgroundMusic;
    private Music menuMusic;
    private Sound pullSound;
    private Sound launchSound;
    private long pullSoundId = -1;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    // check file tồn tại để tránh crash app
    public void init() {
        try {
            if (Gdx.files.internal("sfx/bg.ogg").exists()) {
                backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sfx/bg.ogg"));
                backgroundMusic.setLooping(true);
            }

            if (Gdx.files.internal("sfx/menu.ogg").exists()) {
                menuMusic = Gdx.audio.newMusic(Gdx.files.internal("sfx/menu.ogg"));
                menuMusic.setLooping(true);
            }

            updateMusicVolume();

            if (Gdx.files.internal("sfx/sfx-001.ogg").exists()) {
                pullSound = Gdx.audio.newSound(Gdx.files.internal("sfx/sfx-001.ogg"));
            }

            if (Gdx.files.internal("sfx/sfx-002.ogg").exists()) {
                launchSound = Gdx.audio.newSound(Gdx.files.internal("sfx/sfx-002.ogg"));
            }
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Could not load audio files: " + e.getMessage());
        }
    }

    public void playGameMusic() {
        if (menuMusic != null)
            menuMusic.stop();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    public void playMenuMusic() {
        if (backgroundMusic != null)
            backgroundMusic.stop();
        if (menuMusic != null && !menuMusic.isPlaying()) {
            menuMusic.play();
        }
    }

    public void updateMusicVolume() {
        float vol = GameSettings.getInstance().musicVolume;
        if (backgroundMusic != null)
            backgroundMusic.setVolume(vol);
        if (menuMusic != null)
            menuMusic.setVolume(vol);
    }

    public void playPullSound() {
        if (pullSound != null) {
            if (pullSoundId != -1)
                pullSound.stop(pullSoundId);
            pullSoundId = pullSound.loop(GameSettings.getInstance().sfxVolume);
        }
    }

    public void stopPullSound() {
        if (pullSound != null && pullSoundId != -1) {
            pullSound.stop(pullSoundId);
            pullSoundId = -1;
        }
    }

    public void playLaunchSound() {
        if (launchSound != null) {
            launchSound.play(GameSettings.getInstance().sfxVolume);
        }
    }

    public void dispose() {
        if (backgroundMusic != null)
            backgroundMusic.dispose();
        if (menuMusic != null)
            menuMusic.dispose();
        if (pullSound != null)
            pullSound.dispose();
        if (launchSound != null)
            launchSound.dispose();
    }
}
