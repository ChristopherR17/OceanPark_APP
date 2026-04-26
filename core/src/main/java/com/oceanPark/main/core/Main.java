package com.oceanPark.main;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.oceanPark.main.data.PlayerData;
import com.oceanPark.main.data.RemotePlayer;
import com.oceanPark.main.network.WebSocketManager;
import com.oceanPark.main.screens.GameScreen;
import com.oceanPark.main.screens.MenuScreen;

public class Main extends Game {

    // Constantes de pantalla
    public static final float WORLD_WIDTH = 800f;
    public static final float WORLD_HEIGHT = 480f;

    // Recursos compartidos
    public SpriteBatch batch;
    public Skin skin;
    public FitViewport viewport;

    // Red
    public WebSocketManager wsManager;

    // Estado del juego
    public String myPlayerId;
    public String myPlayerName;
    public float spawnX = 100, spawnY = 100;

    // Jugadores remotos (renderizados por el GameScreen)
    public final ObjectMap<String, RemotePlayer> remotePlayers = new ObjectMap<>();

    @Override
    public void create() {
        // Inicializar recursos
        batch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Inicializar WebSocket
        wsManager = new WebSocketManager();
        wsManager.connect();

        // Pantalla inicial
        setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Actualizar WebSocket
        wsManager.update(Gdx.graphics.getDeltaTime());

        // Llamar al render del Game
        super.render();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        skin.dispose();
        wsManager.dispose();
        super.dispose();
    }

    /**
     * Convierte Y del servidor (coordenadas del mundo de juego)
     * a Y de pantalla (invertida porque libGDX tiene Y=0 abajo)
     */
    public static float toScreenY(float serverY) {
        return WORLD_HEIGHT - serverY;
    }
}
