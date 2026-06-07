package test;

import com.badlogic.gdx.physics.box2d.Body;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.model.Player;
import com.jumpmaster.game.utils.ScoreManager;
import com.jumpmaster.game.view.BaseScreen;
import com.jumpmaster.game.view.GameOverOverlay;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BaseScreenTest {

    private BaseScreen baseScreen;
    private ScoreManager mockScoreManager;
    private GameOverOverlay mockGameOverOverlay;
    private Player mockPlayer;
    private Body mockBody;

    @Before
    public void setUp() {
        // 1. Mock các dependency
        mockScoreManager = Mockito.mock(ScoreManager.class);
        mockGameOverOverlay = Mockito.mock(GameOverOverlay.class);
        mockPlayer = Mockito.mock(Player.class);
        mockBody = Mockito.mock(Body.class);

        // Gắn mock body vào mock player
        mockPlayer.body = mockBody;

        // 2. Tạo một Anonymous Class từ BaseScreen (vì nó là Abstract)
        JumpMasterGame mockGame = Mockito.mock(JumpMasterGame.class);
        baseScreen = new BaseScreen(mockGame) {
            @Override
            protected void drawBackground() {

            }

            @Override
            protected void initBackground() {

            }

            @Override
            protected void initPlatforms() {

            }

            @Override
            protected void onLevelComplete() {

            }

            @Override
            protected float getLevelClearY() {
                return 0;
            }

            @Override
            protected void onExtraDispose() {}
        };

        baseScreen.scoreManager = mockScoreManager;
        baseScreen.gameOverOverlay = mockGameOverOverlay;
        baseScreen.player = mockPlayer;
    }

    //TC3: Đảm bảo gameOverOverlay.setData() và gameOverOverlay.setStats() được gọi với dữ liệu chính xác lấy từ scoreManager.
    @Test
    public void testTriggerGameOver_ExecuteCorrectSequence() {
        // Dữ liệu giả lập
        int currentScore = 150;
        int highScore = 100;
        int[] fakeStats = {15, 3, 45}; // columns, combo, time

        // Thiết lập hành vi cho mock
        Mockito.when(mockScoreManager.getCurrentScore()).thenReturn(currentScore);
        Mockito.when(mockScoreManager.getHighScore()).thenReturn(highScore);
        Mockito.when(mockScoreManager.getStats()).thenReturn(fakeStats);

        // Kích hoạt Use Case 3.2
        baseScreen.triggerGameOver();

        // KIỂM CHỨNG THEO SEQUENCE DIAGRAM UC 3.2

        // 1. Player phải dừng lại (stopPhysics)
        Mockito.verify(mockBody).setLinearVelocity(0, 0);

        // 2. Lưu điểm chung (UC 3.2.1.3)
        Mockito.verify(mockScoreManager).flush();

        // 3. Lưu điểm vào Leaderboard (UC 3.2.1.3c)
        Mockito.verify(mockScoreManager).saveTopScores(currentScore);

        // 4. Lấy stats và truyền cho GameOverOverlay (UC 3.2.1.4)
        // isNewRecord sẽ là true vì currentScore (150) > highScore (100)
        Mockito.verify(mockGameOverOverlay).setData(150, 100, true);
        Mockito.verify(mockGameOverOverlay).setStats(fakeStats);
    }

    //TC1: Khi gọi triggerGameOver(), trạng thái currentState phải chuyển thành State.GAME_OVER.
    @Test
    public void testTriggerGameOver_CalledTwice_IgnoresSecondCall() {
        // Đảm bảo nếu triggerGameOver bị gọi 2 lần (do va chạm liên tục), logic chỉ chạy 1 lần
        baseScreen.triggerGameOver();
        baseScreen.triggerGameOver();

        //TC2: Đảm bảo hàm scoreManager.flush() và scoreManager.saveTopScores() được gọi đúng 1 lần (verify bằng Mockito).
        // Các hàm quan trọng chỉ được thực thi đúng 1 lần
        Mockito.verify(mockScoreManager, Mockito.times(1)).flush();
        Mockito.verify(mockGameOverOverlay, Mockito.times(1)).setData(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());
    }
}
