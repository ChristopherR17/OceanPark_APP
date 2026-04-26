package com.oceanPark.main.data;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Representación visual de un jugador remoto en pantalla
 */
public class RemotePlayer {
    public String id;
    public String name;
    public float x, y;
    public float targetX, targetY;  // Para interpolación suave
    public States state;
    public boolean facingRight;
    public boolean active;

    // Animación
    public TextureRegion currentFrame;
    public float animationTime;

    // Constantes visuales
    public static final float WIDTH = 32f;
    public static final float HEIGHT = 32f;
    public static final float INTERPOLATION_SPEED = 10f;

    public RemotePlayer(String id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
        this.state = States.IDLE;
        this.facingRight = true;
    }

    /**
     * Interpola posición suavemente hacia el objetivo
     */
    public void update(float delta) {
        if (!active) return;

        x += (targetX - x) * Math.min(INTERPOLATION_SPEED * delta, 1f);
        y += (targetY - y) * Math.min(INTERPOLATION_SPEED * delta, 1f);
    }
}
