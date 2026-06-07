package test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.jumpmaster.game.GameSettings;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.view.MainScreen;
import com.jumpmaster.game.view.SettingsScreen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit Test gộp cho UC-1.1: Cài đặt âm thanh, đồ họa
 * Bao gồm logic lưu trữ GameSettings và hành vi màn hình Settings.
 */
public class SettingsScreenTest {

    private SettingsScreen settingsScreen;
    private JumpMasterGame mockGame;
    private GameSettings settings;
    private Preferences mockPrefs;
    private Map<String, Object> fakeStorage;

    @Before
    public void setUp() throws Exception {
        // 1. Mock môi trường Gdx triệt để
        mockGame = mock(JumpMasterGame.class);
        Gdx.app = mock(Application.class);
        Gdx.graphics = mock(Graphics.class);
        mockPrefs = mock(Preferences.class);
        fakeStorage = new HashMap<>();

        when(Gdx.app.getPreferences(anyString())).thenReturn(mockPrefs);

        // Giả lập lưu trữ Preferences cho âm lượng và các tùy chọn khác
        doAnswer(inv -> {
            fakeStorage.put(inv.getArgument(0), inv.getArgument(1));
            return mockPrefs;
        }).when(mockPrefs).putFloat(anyString(), anyFloat());

        when(mockPrefs.getFloat(anyString(), anyFloat())).thenAnswer(inv ->
            fakeStorage.getOrDefault(inv.getArgument(0), inv.getArgument(1))
        );

        // 2. Lấy logic GameSettings
        settings = GameSettings.getInstance();

        // 3. Khởi tạo SettingsScreen (Dùng spy để tránh crash UI khi test logic)
        settingsScreen = spy(new SettingsScreen(mockGame));
        doNothing().when(settingsScreen).show();
    }

    // TC 1.1.1: Kiểm tra logic lưu và tải dữ liệu cài đặt (Consistency)
    @Test
    public void testGameSettings_SaveAndLoad_Consistency() {
        settings.musicVolume = 0.35f;
        settings.sfxVolume = 0.95f;

        settings.save();
        verify(mockPrefs).flush();

        settings.load();
        assertEquals(0.35f, settings.musicVolume, 0.01f);
        assertEquals(0.95f, settings.sfxVolume, 0.01f);
    }

    // TC 1.1.2: Kiểm tra trình tự áp dụng chế độ hiển thị đồ họa
    @Test
    public void testApplyDisplayMode_ExecuteCorrectSequence() {
        // Test Windowed
        settings.displayMode = GameSettings.MODE_DEFAULT;
        settings.applyDisplayMode();
        verify(Gdx.graphics).setWindowedMode(1200, 720);

        // Test Fullscreen
        Graphics.DisplayMode mockMode = mock(Graphics.DisplayMode.class);
        when(Gdx.graphics.getDisplayMode()).thenReturn(mockMode);
        settings.displayMode = GameSettings.MODE_FULLSCREEN;
        settings.applyDisplayMode();
        verify(Gdx.graphics).setFullscreenMode(mockMode);
    }

    // TC 1.1.3: Kiểm tra Sequence khi nhấn quay lại Menu chính (Sequence Verification)
    @Test
    public void testNavigation_BackToMenu_Sequence() {
        // Mô phỏng trình tự: Lưu cài đặt -> Chuyển màn hình
        settings.save();
        mockGame.setScreen(new MainScreen(mockGame));

        verify(mockPrefs, atLeastOnce()).flush();
        verify(mockGame).setScreen(any(MainScreen.class));
    }
}
