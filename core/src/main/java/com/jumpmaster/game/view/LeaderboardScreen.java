package com.jumpmaster.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.jumpmaster.game.JumpMasterGame;
import com.jumpmaster.game.utils.Constants;
import com.jumpmaster.game.utils.ScoreManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
    ĐÂY LÀ CLASS THỂ HIỆN TỔNG QUÁT TỪ UC 3.2.2 ĐƯỢC TÍNH TOÁN TỪ 3.2.1.3c
 */

public class LeaderboardScreen implements Screen {

    private final JumpMasterGame game;
    private Stage stage;
    private ScoreManager scoreManager;

    public LeaderboardScreen(final JumpMasterGame game) {
        this.game = game;
        scoreManager = new ScoreManager();

        // Setup Stage với kích thước cố định
        stage = new Stage(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Cài đặt Fonts
        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(2.0f);
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.YELLOW);

        BitmapFont textFont = new BitmapFont();
        textFont.getData().setScale(1.5f);
        Label.LabelStyle textStyle = new Label.LabelStyle(textFont, Color.WHITE);
        Label.LabelStyle emptyStyle = new Label.LabelStyle(textFont, Color.GRAY);

        // Tiêu đề
        Label titleLabel = new Label("TOP 5 JUMPERS", titleStyle);
        table.add(titleLabel).padBottom(40).row();

        // Lấy dữ liệu Top 5 từ ScoreManager
        long[][] topScores = scoreManager.getTopScores();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        if (topScores.length == 0) {
            // Nếu chưa có ai chơi
            table.add(new Label("No record yet", emptyStyle)).padBottom(30).row();
        } else {
            // Hiển thị danh sách
            Table listTable = new Table();
            for (int i = 0; i < topScores.length; i++) {
                int score = (int) topScores[i][0];
                long timestamp = topScores[i][1];
                String dateStr = sdf.format(new Date(timestamp));

                Label rankLabel = new Label("#" + (i + 1), textStyle);
                Label scoreLabel = new Label(String.valueOf(score), new Label.LabelStyle(textFont, Color.GREEN));
                Label dateLabel = new Label(dateStr, new Label.LabelStyle(textFont, Color.LIGHT_GRAY));
                dateLabel.setFontScale(0.8f); // Chữ ngày tháng nhỏ lại một chút

                listTable.add(rankLabel).padRight(20).left();
                listTable.add(scoreLabel).padRight(30).right();
                listTable.add(dateLabel).right().row();
            }
            table.add(listTable).padBottom(40).row();
        }

        // Nút BACK để quay lại MainScreen
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = textFont;
        btnStyle.fontColor = Color.RED;

        TextButton btnBack = new TextButton("BACK TO MENU", btnStyle);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainScreen(game));
            }
        });

        // Nút để xóa record
        TextButton.TextButtonStyle btnDeleteStyle = new TextButton.TextButtonStyle();
        btnDeleteStyle.font = textFont;
        btnDeleteStyle.fontColor = Color.GRAY;

        TextButton btnDelete = new TextButton("DELETE ALL RECORDS", btnDeleteStyle);
        btnDelete.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                scoreManager.clearAllRecords();
                game.setScreen(new LeaderboardScreen(game));
            }
        });

        table.add(btnBack).width(300).height(60).padBottom(10).row();
        table.add(btnDelete).width(300).height(40);

        stage.addActor(table);
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        // Clear màn hình với màu xanh đen/tối
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
