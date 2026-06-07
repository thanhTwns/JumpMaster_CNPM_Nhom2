package test;

import static org.junit.Assert.assertEquals;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.jumpmaster.game.utils.ScoreManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class ScoreManagerTest {

    private ScoreManager scoreManager;
    private Preferences mockPrefs;
    private Map<String, Object> fakeStorage; // Dùng Map để giả lập nơi lưu trữ của Preferences

    @Before
    public void setUp() {
        // 1. Giả lập (Mock) môi trường LibGDX
        Gdx.app = Mockito.mock(Application.class);
        mockPrefs = Mockito.mock(Preferences.class);
        fakeStorage = new HashMap<>();

        // 2. Cấu hình hành vi cho Mock Preferences để nó hoạt động như thật
        Mockito.when(Gdx.app.getPreferences(Mockito.anyString())).thenReturn(mockPrefs);

        Mockito.when(mockPrefs.getInteger(Mockito.anyString(), Mockito.anyInt())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            int defValue = invocation.getArgument(1);
            return fakeStorage.containsKey(key) ? fakeStorage.get(key) : defValue;
        });

        Mockito.when(mockPrefs.getLong(Mockito.anyString(), Mockito.anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long defValue = invocation.getArgument(1);
            return fakeStorage.containsKey(key) ? fakeStorage.get(key) : defValue;
        });

        Mockito.when(mockPrefs.putInteger(Mockito.anyString(), Mockito.anyInt())).thenAnswer(invocation -> {
            fakeStorage.put(invocation.getArgument(0), invocation.getArgument(1));
            return mockPrefs;
        });

        Mockito.when(mockPrefs.putLong(Mockito.anyString(), Mockito.anyLong())).thenAnswer(invocation -> {
            fakeStorage.put(invocation.getArgument(0), invocation.getArgument(1));
            return mockPrefs;
        });

        // 3. Khởi tạo đối tượng cần test
        scoreManager = new ScoreManager();
    }

    // ─── CÁC TEST CASE CHO UC 3.2 ────────────────────────────────────────

    //TC1 - Lưu Top 5 thành công: Điểm > 0 được thêm vào danh sách và sắp xếp giảm dần.
    @Test
    public void testFlush_NewRecordUpdatesHighScore() {
        // Giả lập đang có điểm cao là 50
        fakeStorage.put("highScore", 50);
        scoreManager = new ScoreManager(); // Khởi tạo lại để nhận 50

        // Chơi được 100 điểm
        scoreManager.addPoints(100);

        //TC5 - flush() cập nhật High Score: Nếu currentScore > highScore, sau khi gọi flush(), highScore mới phải được cập nhật.
        // Gọi hàm flush (như trong BaseScreen.triggerGameOver)
        scoreManager.flush();

        assertEquals("High score phải được cập nhật lên 100", 100, scoreManager.getHighScore());
    }

    //TC2 - Chặn điểm 0: Điểm <= 0 truyền vào saveTopScores() sẽ bị bỏ qua (không lưu).
    @Test
    public void testSaveTopScores_IgnoreZeroOrNegative() {
        scoreManager.saveTopScores(0);
        scoreManager.saveTopScores(-10);

        long[][] topScores = scoreManager.getTopScores();
        assertEquals("Không được lưu điểm <= 0", 0, topScores.length);
    }

    //TC3 - Chặn điểm trùng (Anti-duplicate): Truyền cùng 1 mức điểm 2 lần, danh sách chỉ lưu 1 lần.
    @Test
    public void testSaveTopScores_IgnoreDuplicates() {
        scoreManager.saveTopScores(100);
        scoreManager.saveTopScores(100); // Thêm lại điểm y hệt

        long[][] topScores = scoreManager.getTopScores();
        assertEquals("Chỉ được lưu 1 lần nếu trùng điểm", 1, topScores.length);
        assertEquals(100, topScores[0][0]);
    }

    //TC4 - Giới hạn 5 slot: Truyền 6 mức điểm khác nhau, hệ thống chỉ giữ lại 5 điểm cao nhất, điểm thấp nhất bị loại bỏ.
    @Test
    public void testSaveTopScores_KeepOnlyTop5Sorted() {
        // Thêm 6 mốc điểm khác nhau (không theo thứ tự)
        scoreManager.saveTopScores(10);
        scoreManager.saveTopScores(50);
        scoreManager.saveTopScores(20);
        scoreManager.saveTopScores(60);
        scoreManager.saveTopScores(30);
        scoreManager.saveTopScores(40);

        long[][] topScores = scoreManager.getTopScores();

        // Kiểm tra số lượng
        assertEquals("Chỉ được giữ tối đa 5 điểm", 5, topScores.length);


        // Kiểm tra thứ tự giảm dần và điểm 10 đã bị loại bỏ
        assertEquals(60, topScores[0][0]);
        assertEquals(50, topScores[1][0]);
        assertEquals(40, topScores[2][0]);
        assertEquals(30, topScores[3][0]);
        assertEquals(20, topScores[4][0]);
    }
}
