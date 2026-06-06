package test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.view.EarthScreen;
import com.jumpmaster.game.view.MainScreen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class MainScreenTest {

    private MainScreen screen;
    private JumpMasterGame mockGame;

    @Before
    public void setUp() {
        mockGame = Mockito.mock(JumpMasterGame.class);

        screen = new MainScreen(mockGame);

        // mock Gdx input để không crash
        Gdx.input = Mockito.mock(Input.class);
        Mockito.when(Gdx.input.justTouched()).thenReturn(false);
    }

    // =========================
    // TC1: chọn Classic mode
    // =========================
    @Test
    public void testShowClassicPopup() {

        screen.showClassicPopup();

        assertTrue(screen.isShowingPopup());
        assertEquals("classic", screen.getSelectedModeTag());
        assertTrue(screen.getPopupText().contains("CLASSIC MODE"));
    }

    // =========================
    // TC2: chọn Time Attack
    // =========================
    @Test
    public void testShowTimeAttackPopup() {

        screen.showTimeAttackPopup();

        assertTrue(screen.isShowingPopup());
        assertEquals("timeattack", screen.getSelectedModeTag());
        assertTrue(screen.getPopupText().contains("TIME ATTACK"));
    }

    // =========================
    // TC3: đóng popup
    // =========================
    @Test
    public void testClosePopup() {

        screen.showClassicPopup();
        screen.closePopup();

        assertFalse(screen.isShowingPopup());
    }

    // =========================
    // TC4: start game classic
    // =========================
    @Test
    public void testStartClassicMode() {

        screen.showClassicPopup();
        screen.startSelectedMode();

        Mockito.verify(mockGame).setScreen(
            Mockito.any(EarthScreen.class)
        );
    }

    // =========================
    // TC5: start game timeattack
    // =========================
    @Test
    public void testStartTimeAttackMode() {

        screen.showTimeAttackPopup();
        screen.startSelectedMode();

        Mockito.verify(mockGame).setScreen(
            Mockito.any(EarthScreen.class)
        );
    }
}
