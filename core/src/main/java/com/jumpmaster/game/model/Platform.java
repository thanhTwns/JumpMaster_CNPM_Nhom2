package com.jumpmaster.game.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.jumpmaster.game.utils.Constants;

public class Platform {
    public Body body;
    private Texture texture;
    public float width;
    public float height;
    private float widthMeters;
    private float heightMeters;
    public float getX()      { return body.getPosition().x; }

    /** Tâm Y — metres */
    public float getY()      { return body.getPosition().y; }

    /** Chiều rộng — metres */
    public float getWidth()  { return widthMeters; }

    /** Chiều cao — metres */
    public float getHeight() { return heightMeters; }
    /**
     * @param world     Box2D world
     * @param x         vị trí tâm X (pixel)
     * @param y         vị trí tâm Y (pixel)
     * @param width     chiều rộng (pixel)
     * @param height    chiều cao (pixel)
     * @param texture   ảnh platform
     */
    public Platform(World world, float x, float y, float width, float height, Texture texture) {
        this.width = width;
        this.height = height;
        this.widthMeters  = width  / Constants.PPM;
        this.heightMeters = height / Constants.PPM;
        this.texture      = texture;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x / Constants.PPM, y / Constants.PPM);
        body = world.createBody(bodyDef);
        this.body.setUserData(this);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(widthMeters / 2f, heightMeters / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape   = shape;
        fixtureDef.friction = 0.5f;
        body.createFixture(fixtureDef);
        shape.dispose();
    }

    public void draw(SpriteBatch batch) {
        if (texture == null) return;
        float drawX = body.getPosition().x - widthMeters / 2f;
        float drawY = body.getPosition().y - heightMeters / 2f;
        batch.draw(texture, drawX, drawY, widthMeters, heightMeters);
    }
}
