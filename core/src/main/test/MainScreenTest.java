package test;



import com.jumpmaster.game.JumpMasterGame;

import com.jumpmaster.game.view.EarthScreen;

import com.jumpmaster.game.view.MainScreen;



import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;



import java.lang.reflect.Field;

import java.lang.reflect.Method;



import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

import static javax.management.Query.times;





class MainScreenTest {



    private JumpMasterGame game;

    private MainScreen mainScreen;



    @BeforeEach

    void setUp() {

        game = mock(JumpMasterGame.class);

        mainScreen = new MainScreen(game);

    }



    @Test

    void startSelectedMode_WhenClassicMode_ShouldSetEarthScreen() throws Exception {



        // Arrange

        Field modeField = MainScreen.class.getDeclaredField("selectedModeTag");

        modeField.setAccessible(true);

        modeField.set(mainScreen, "classic");



        Method method = MainScreen.class.getDeclaredMethod("startSelectedMode");

        method.setAccessible(true);



        // Act

        method.invoke(mainScreen);



        // Assert

        verify(game).setScreen(any(EarthScreen.class));



    }



    @Test

    void startSelectedMode_WhenTimeAttackMode_ShouldSetEarthScreen() throws Exception {



        Field modeField = MainScreen.class.getDeclaredField("selectedModeTag");

        modeField.setAccessible(true);

        modeField.set(mainScreen, "timeattack");



        Method method = MainScreen.class.getDeclaredMethod("startSelectedMode");

        method.setAccessible(true);



        method.invoke(mainScreen);



        verify(game).setScreen(any(EarthScreen.class));

    }



    @Test

    void startSelectedMode_WhenChallengeMode_ShouldSetEarthScreen() throws Exception {



        Field modeField = MainScreen.class.getDeclaredField("selectedModeTag");

        modeField.setAccessible(true);

        modeField.set(mainScreen, "challenge");



        Method method = MainScreen.class.getDeclaredMethod("startSelectedMode");

        method.setAccessible(true);



        method.invoke(mainScreen);



        verify(game).setScreen(any(EarthScreen.class));

    }



    @Test

    void startSelectedMode_ShouldHidePopup() throws Exception {



        // Arrange

        Field popupField = MainScreen.class.getDeclaredField("isShowingPopup");

        popupField.setAccessible(true);

        popupField.set(mainScreen, true);



        Field modeField = MainScreen.class.getDeclaredField("selectedModeTag");

        modeField.setAccessible(true);

        modeField.set(mainScreen, "classic");



        Method method = MainScreen.class.getDeclaredMethod("startSelectedMode");

        method.setAccessible(true);



        // Act

        method.invoke(mainScreen);



        // Assert

        assertFalse((Boolean) popupField.get(mainScreen));

    }

}






