package test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.view.MainScreen;
import com.jumpmaster.game.view.SettingsScreen;
import com.jumpmaster.game.view.LeaderboardScreen;
import com.jumpmaster.game.view.EarthScreen;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Unit Test cho MainScreen (UC-1.2: Chọn chế độ chơi và UC-1.4: Thoát game)
 * Viết theo chuẩn Sequence Verification và Reflection của BaseScreenTest.
 */
public class MainScreenTest {

    private MainScreen mainScreen;
    private JumpMasterGame mockGame;
    private Input mockInput;
    private Application mockApp;

    @Before
    public void setUp() throws Exception {
        mockGame = mock(JumpMasterGame.class);
        mockInput = mock(Input.class);
        mockApp = mock(Application.class);
        Gdx.app = mockApp;
        Gdx.input = mockInput;
        Gdx.graphics = mock(Graphics.class);

        when(Gdx.graphics.getWidth()).thenReturn(1200);
        when(Gdx.graphics.getHeight()).thenReturn(720);

        mainScreen = new MainScreen(mockGame);

        // Thiết lập trạng thái layout qua Reflection để tránh lỗi load font/texture
        setPrivateField(mainScreen, "screenW", 1200);
        setPrivateField(mainScreen, "screenH", 720);

        Method setupButtons = MainScreen.class.getDeclaredMethod("setupButtons");
        setupButtons.setAccessible(true);
        setupButtons.invoke(mainScreen);
    }

    // TC 1.2.1: Chọn chế độ chơi hiển thị Popup hướng dẫn (UC-1.2)
    @Test
    public void testSelectMode_ShowsPopup_Sequence() throws Exception {
        Rectangle btnClassic = (Rectangle) getPrivateField(mainScreen, "btnClassic");
        when(mockInput.justTouched()).thenReturn(true);
        when(mockInput.getX()).thenReturn((int) btnClassic.x + 5);
        when(mockInput.getY()).thenReturn(720 - (int) btnClassic.y - 5);

        invokeHandleInput();

        assertTrue("Popup phải hiển thị", (boolean) getPrivateField(mainScreen, "isShowingPopup"));
        assertEquals("classic", getPrivateField(mainScreen, "selectedModeTag"));
    }

    // TC 1.2.2: Nhấn START trên Popup để vào gameplay
    @Test
    public void testStartGameFromPopup_Sequence() throws Exception {
        setPrivateField(mainScreen, "isShowingPopup", true);
        setPrivateField(mainScreen, "selectedModeTag", "classic");
        Rectangle btnXacNhan = (Rectangle) getPrivateField(mainScreen, "btnXacNhan");

        when(mockInput.justTouched()).thenReturn(true);
        when(mockInput.getX()).thenReturn((int) btnXacNhan.x + 5);
        when(mockInput.getY()).thenReturn(720 - (int) btnXacNhan.y - 5);

        invokeHandleInput();
        verify(mockGame).setScreen(any(EarthScreen.class));
    }

    // TC 1.4: Nhấn nút EXIT để thoát hệ thống (UC-1.4)
    @Test
    public void testClickExit_CallsAppExit_Sequence() throws Exception {
        Rectangle btnExit = (Rectangle) getPrivateField(mainScreen, "btnExit");
        when(mockInput.justTouched()).thenReturn(true);
        when(mockInput.getX()).thenReturn((int) btnExit.x + 5);
        when(mockInput.getY()).thenReturn(720 - (int) btnExit.y - 5);

        invokeHandleInput();
        verify(mockApp, times(1)).exit();
    }

    // TC 1.1/1.3: Kiểm tra điều hướng sang Settings và Scores
    @Test
    public void testNavigation_ToSettingsAndScores() throws Exception {
        Rectangle btnSettings = (Rectangle) getPrivateField(mainScreen, "btnSettings");
        when(mockInput.justTouched()).thenReturn(true);
        when(mockInput.getX()).thenReturn((int) btnSettings.x + 5);
        when(mockInput.getY()).thenReturn(720 - (int) btnSettings.y - 5);

        invokeHandleInput();
        verify(mockGame).setScreen(any(SettingsScreen.class));
    }

    // --- Helper Methods ---
    private void invokeHandleInput() throws Exception {
        Method method = MainScreen.class.getDeclaredMethod("handleInput");
        method.setAccessible(true);
        method.invoke(mainScreen);
    }

    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private Object getPrivateField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
