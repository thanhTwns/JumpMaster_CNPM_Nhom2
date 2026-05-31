package com.jumpmaster.game.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.jumpmaster.game.utils.Constants;

public class Player {

    public Body body;
    private Texture texture;
    public boolean isStunned = false;

    // Kích thước vẽ — đơn vị PIXELS
    private static final float DRAW_WIDTH  = 64f;
    private static final float DRAW_HEIGHT = 64f;

    public Player(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        // x, y truyền vào là pixels → chia PPM để sang world-units
        bodyDef.position.set(x / Constants.PPM, y / Constants.PPM);

        body = world.createBody(bodyDef);

        // Hitbox: 16x16 pixels → 16/PPM metres mỗi chiều
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(16f / Constants.PPM, 16f / Constants.PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape       = shape;
        fixtureDef.density     = 1.0f;
        fixtureDef.restitution = 0.2f;
        fixtureDef.friction    = 0.5f;

        body.createFixture(fixtureDef);
        shape.dispose();

        texture = new Texture("chicken.png");
    }

    public void jump(Vector2 force) {
        float velocityY = body.getLinearVelocity().y;
        if (Math.abs(velocityY) < 0.1f) {
            body.applyLinearImpulse(force, body.getWorldCenter(), true);
        }
    }

    /**
     * Vẽ player.
     * Batch phải đang dùng projection pixel-space (bgCam.combined).
     * body.getPosition() trả về world-units (metres) → nhân PPM để sang pixels.
     */
    public void draw(SpriteBatch batch) {
        // Vị trí tâm body (metres) → pixels
        float centerX = body.getPosition().x * Constants.PPM;
        float centerY = body.getPosition().y * Constants.PPM;

        // Vẽ căn giữa vào tâm body
        batch.draw(texture,
            centerX - DRAW_WIDTH  / 2f,
            centerY - DRAW_HEIGHT / 2f,
            DRAW_WIDTH,
            DRAW_HEIGHT);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
