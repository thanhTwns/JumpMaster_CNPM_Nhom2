package com.jumpmaster.game.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.jumpmaster.game.controller.InputHandler;
import com.jumpmaster.game.utils.Constants;

public class Player {
    public Body body;
    private Texture texture;
    private float playerWidth;
    private float playerHeight;

    public Player(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x / Constants.PPM, y / Constants.PPM);

        body = world.createBody(bodyDef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(16 / Constants.PPM, 16 / Constants.PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f; //Khối lượng
        fixtureDef.restitution = 0.2f; // Độ nảy (0 là không nảy, 1 là nảy như bóng cao su)
        fixtureDef.friction = 0.5f; //Lực ma sát

        //player
        texture = new Texture("chicken.png");
        playerWidth = 64f;
        playerHeight = 64f;

        body.createFixture(fixtureDef);
        shape.dispose();

    }

    public void jump(Vector2 force) {
        float velocityY = body.getLinearVelocity().y;
        if(Math.abs(velocityY) < 0.1f) {
            body.applyLinearImpulse(force, body.getWorldCenter(), true);
        }
    }

    public void draw(SpriteBatch batch) {
        float drawWidth = playerWidth / Constants.PPM;
        float drawHeight = playerHeight / Constants.PPM;

        float drawX = body.getPosition().x - (drawWidth / 2f);
        float drawY = body.getPosition().y - (drawHeight / 2f);

        batch.draw(texture, drawX, drawY, drawWidth, drawHeight);
    }
}
