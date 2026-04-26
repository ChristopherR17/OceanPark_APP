package com.oceanPark.main.input;

public class GameInputState {
    public float moveX;
    public float moveY;
    public boolean jumpPressed;
    public boolean jumpHeld;
    public boolean actionPressed;

    public void reset() {
        moveX = 0f;
        moveY = 0f;
        jumpPressed = false;
        jumpHeld = false;
        actionPressed = false;
    }
}
