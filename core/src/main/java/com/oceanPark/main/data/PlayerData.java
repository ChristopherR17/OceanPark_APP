package com.oceanPark.main.data;

/**
 * Datos de un jugador recibidos del servidor en STATE
 * Formato: { type: "STATE", players: [...] }
 */
public class PlayerData {
    public String id;
    public String name;
    public float x;
    public float y;
    public States state;
    public boolean facingRight;

    // Campos adicionales que podría enviar el servidor
    public boolean onGround;
    public String currentAnimation;

    @Override
    public String toString() {
        return name + " [" + state + "] @ " + (int)x + "," + (int)y;
    }
}
