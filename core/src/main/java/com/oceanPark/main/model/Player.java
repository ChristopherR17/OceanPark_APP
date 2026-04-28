package com.oceanPark.main.model;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.oceanPark.main.data.States;

public class Player extends Actor {
    public float posX, posY;
    public float stateTime;
    public States state;
    public boolean facingRight;
    private Animation<TextureRegion> animation;
    private TextureRegion currentFrame;

    public Boolean ready;

    public Player(String name, Animation<TextureRegion> animation) {
        this.setName(name);
        this.posX = 50;
        this.posY = 50;
        this.stateTime = 0;
        this.setSize(32, 32);
        state = States.IDDLE;
        facingRight = true;
        ready = false;
        this.animation = animation;
    }

    public void setAnimation(Animation<TextureRegion> animation) {
        if (animation != null && this.animation != animation) {
            this.animation = animation;
        }
    }

    public void updatePoss(float x, float y) {
        posX = x;
        posY = y;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        this.setPosition(posX, posY);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (animation == null) return;
        currentFrame = animation.getKeyFrame(stateTime);
        batch.draw(currentFrame, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public String toString() {
        return getName();
    }
}
