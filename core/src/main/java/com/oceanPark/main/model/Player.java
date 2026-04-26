package com.oceanPark.main.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.oceanPark.main.data.States;

public class Player extends Actor {

    public float posX,posY;
    public float stateTime;
    public States state;
    public boolean facingRight;
    public Texture currentFrame;
    Boolean ready;

    public Player(String name) {
        this.setName(name);
        this.posX=50;
        this.posY=50;
        this.stateTime = 0;
        state=States.IDLE;
        facingRight=false;
        ready=false;
        currentFrame= new Texture("flecha.png");

    }
    public Player(String name,float posX,float posY,States state,boolean facingRight,Texture texture){
        this.setName(name);
        this.posX=posX;
        this.posY=posY;
        this.state=state;
        this.facingRight=facingRight;
        this.currentFrame=texture;
    }

    public void updatePoss(float x,float y){
        posX=x;
        posY=y;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        this.setPosition(posX, posY);
        this.setSize(150,150);
    }
    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Aquí dibujas tu textura o animación
        batch.draw(currentFrame, getX(), getY(),getWidth(),getHeight());
    }

    @Override
    public String toString() {
        return getName();
    }

}



