package com.jumpmaster.game.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.jumpmaster.game.utils.Constants;

/**
 * Platform — static Box2D body với texture tuỳ chọn.
 *
 * Tất cả constructor nhận tọa độ PIXELS.
 * Nội bộ lưu kích thước bằng METRES để dùng với Box2D.
 * draw() vẽ bằng PIXELS (dùng với bgCam — pixel-space projection).
 */
public class Platform {

    public  final Body    body;
    private final Texture texture;

    // Kích thước lưu bằng metres (để getWidth/getHeight trả về metres
    // cho TimeAttackLogic tính toán vị trí bat/potion/vortex)
    private final float widthMetres;
    private final float heightMetres;

    /**
     * @param world   Box2D world
     * @param x       tâm X — PIXELS
     * @param y       tâm Y — PIXELS
     * @param w       chiều rộng — PIXELS
     * @param h       chiều cao  — PIXELS
     * @param texture sprite (null = vô hình, dùng cho tường)
     */
    public Platform(World world, float x, float y,
                    float w, float h, Texture texture) {
        this.texture      = texture;
        this.widthMetres  = w / Constants.PPM;
        this.heightMetres = h / Constants.PPM;

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        // Chuyển pixels → metres cho Box2D
        def.position.set(x / Constants.PPM, y / Constants.PPM);

        body = world.createBody(def);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(widthMetres / 2f, heightMetres / 2f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape    = shape;
        fdef.friction = 0.4f;
        body.createFixture(fdef);
        shape.dispose();
    }

    // ── Accessors (world-units / metres) ─────────────────────────────────────
    // TimeAttackLogic dùng các hàm này rồi tự nhân PPM để ra pixels

    /** Tâm X — metres */
    public float getX()      { return body.getPosition().x; }

    /** Tâm Y — metres */
    public float getY()      { return body.getPosition().y; }

    /** Chiều rộng — metres */
    public float getWidth()  { return widthMetres; }

    /** Chiều cao — metres */
    public float getHeight() { return heightMetres; }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Vẽ platform.
     * Batch phải đang dùng projection pixel-space (bgCam.combined).
     * getX()/getY() trả về metres → nhân PPM để sang pixels.
     */
    public void draw(SpriteBatch batch) {
        if (texture == null) return;

        // Tâm body (metres) → pixels
        float cx = getX() * Constants.PPM;
        float cy = getY() * Constants.PPM;
        float pw = widthMetres  * Constants.PPM;
        float ph = heightMetres * Constants.PPM;

        batch.draw(texture, cx - pw / 2f, cy - ph / 2f, pw, ph);
    }
}
