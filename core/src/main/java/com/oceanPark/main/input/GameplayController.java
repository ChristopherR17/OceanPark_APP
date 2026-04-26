package com.oceanPark.main.input;

public interface GameplayController {

    void handleInput(GameInputState input);

    void update(float dt);

    String animationForState();

    float getCameraX();
    float getCameraY();
}
