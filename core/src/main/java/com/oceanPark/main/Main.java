package com.oceanPark.main;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketAdapter;
import com.github.czyzby.websocket.WebSockets;
import com.oceanPark.main.model.Coin;
import com.oceanPark.main.model.Door;
import com.oceanPark.main.model.Key;
import com.oceanPark.main.model.Map;
import com.oceanPark.main.model.Player;

import java.util.HashMap;

/** Main libGDX Game. */
public class Main extends Game {
    public String playerId;
    public String playerName;

    public final JsonReader lector = new JsonReader();

    public HashMap<String, Player> jugadoresMap = new HashMap<>();
    public HashMap<String, Key> keyMap = new HashMap<>();
    public HashMap<String, Door> doorMap = new HashMap<>();
    public HashMap<String, Coin> coinMap = new HashMap<>();

    private final Array<String> queue = new Array<>();

    public WebSocket socket;
    public FitViewport viewport;
    public Skin skin;
    public Float escala;
    public SpriteBatch batch;

    public Texture flechaTexture, flechaUp, mushPlayer, key;

    public HashMap<String, TextureRegion[][]> mapaSprites;
    public HashMap<String, Animation<TextureRegion>> mapaAnimation;

    private JsonValue levelData;
    private Texture tilesetTexture;
    private JsonValue tileMapData;

    public Map map;

    // Cambiad solo esta URL si el profe cambia el dominio.
    private static final String WS_URL = "wss://pico3.ieti.site:443";

    @Override
    public void create() {
        jugadoresMap = new HashMap<>();
        keyMap = new HashMap<>();
        doorMap = new HashMap<>();
        coinMap = new HashMap<>();
        mapaAnimation = new HashMap<>();
        mapaSprites = new HashMap<>();

        flechaTexture = new Texture("flecha.png");
        flechaUp = new Texture("flecha_up.png");
        mushPlayer = new Texture("mushroom_iddle.png");
        key = new Texture("key-rbg.png");

        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        cargarGameData();

        escala = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        batch = new SpriteBatch();

        connectSocket();
        setScreen(new MenuScreen(this));
    }

    private void connectSocket() {
        socket = WebSockets.newSocket(WS_URL);

        socket.addListener(new WebSocketAdapter() {
            @Override
            public boolean onOpen(WebSocket webSocket) {
                Gdx.app.log("WS", "Conectado a " + WS_URL);
                return FULLY_HANDLED;
            }

            @Override
            public boolean onMessage(WebSocket webSocket, String packet) {
                Gdx.app.log("MSG_RECIBIDO", packet);
                synchronized (queue) {
                    queue.add(packet);
                }
                return FULLY_HANDLED;
            }

            @Override
            public boolean onClose(WebSocket webSocket, int closeCode, String reason) {
                Gdx.app.log("WS", "Cerrado: " + closeCode + " / " + reason);
                return FULLY_HANDLED;
            }

            @Override
            public boolean onError(WebSocket webSocket, Throwable error) {
                Gdx.app.error("WS", "Error WebSocket", error);
                return FULLY_HANDLED;
            }
        });

        Thread networkThread = new Thread(() -> {
            try {
                socket.connect();
            } catch (Exception e) {
                Gdx.app.error("WS", "No se pudo conectar", e);
            }
        });
        networkThread.setPriority(Thread.MIN_PRIORITY);
        networkThread.start();
    }

    public boolean sendMessage(String json) {
        Gdx.app.log("MSG_TEST_ENVIAR", json);

        if (socket == null) {
            Gdx.app.log("WS", "No se envía: socket null");
            return false;
        }

        if (!socket.isOpen()) {
            Gdx.app.log("WS", "No se envía: socket cerrado/no abierto");
            return false;
        }

        socket.send(json);
        return true;
    }

    @Override
    public void render() {
        synchronized (queue) {
            if (queue.size > 0) {
                Screen pantallaActual = getScreen();
                for (String msg : queue) {
                    try {
                        if (pantallaActual instanceof GameScreen) {
                            ((GameScreen) pantallaActual).msg(msg);
                        } else if (pantallaActual instanceof MenuScreen) {
                            ((MenuScreen) pantallaActual).msg(msg);
                        }
                    } catch (Exception e) {
                        Gdx.app.error("MSG_PARSE", "Error procesando mensaje: " + msg, e);
                    }
                }
                queue.clear();
            }
        }

        super.render();
    }

    @Override
    public void dispose() {
        if (socket != null) socket.close();
        if (batch != null) batch.dispose();
        if (skin != null) skin.dispose();
        if (flechaTexture != null) flechaTexture.dispose();
        if (flechaUp != null) flechaUp.dispose();
        if (mushPlayer != null) mushPlayer.dispose();
        if (key != null) key.dispose();
        if (tilesetTexture != null) tilesetTexture.dispose();
    }

    public void cargarGameData() {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal("game_data.json"));
        levelData = root.get("levels").get(0);

        viewport = new FitViewport(levelData.getInt("viewportWidth"), levelData.getInt("viewportHeight"));

        JsonValue layer = levelData.get("layers").get(0);
        tilesetTexture = new Texture(Gdx.files.internal(layer.getString("tilesSheetFile")));
        tilesetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        tileMapData = reader.parse(Gdx.files.internal(layer.getString("tileMapFile")));

        cargarSprites(root);

        JsonValue animRoot = reader.parse(Gdx.files.internal("animations/animations.json"));
        cargarAnimaciones(animRoot);

        map = new Map(levelData, tileMapData, tilesetTexture);
    }

    public void cargarSprites(JsonValue root) {
        JsonValue mediaAssets = root.get("mediaAssets");
        for (JsonValue asset : mediaAssets) {
            String archivo = asset.getString("fileName");
            int tileW = asset.getInt("tileWidth");
            int tileH = asset.getInt("tileHeight");

            if (!Gdx.files.internal(archivo).exists()) {
                Gdx.app.log("SPRITE_MISSING", "No existe: " + archivo);
                continue;
            }

            try {
                Texture tex = new Texture(Gdx.files.internal(archivo));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                TextureRegion[][] regions = TextureRegion.split(tex, tileW, tileH);
                mapaSprites.put(archivo, regions);
                Gdx.app.log("SPRITE_OK", archivo);
            } catch (Exception e) {
                Gdx.app.error("SPRITE_ERROR", "Error cargando " + archivo, e);
            }
        }
    }

    public void cargarAnimaciones(JsonValue animRoot) {
        JsonValue animations = animRoot.get("animations");
        if (animations == null) return;

        for (JsonValue animData : animations) {
            String animName = animData.getString("name");
            String mediaFile = animData.getString("mediaFile");
            int start = animData.getInt("startFrame");
            int end = animData.getInt("endFrame");
            float fps = animData.getFloat("fps", 8f);
            boolean loop = animData.getBoolean("loop", true);

            TextureRegion[][] regiones = mapaSprites.get(mediaFile);
            if (regiones == null || regiones.length == 0 || regiones[0].length == 0) {
                Gdx.app.log("ANIM_MISSING", animName + " -> " + mediaFile);
                continue;
            }

            Array<TextureRegion> frames = new Array<>();
            int max = regiones[0].length - 1;
            int safeStart = Math.max(0, Math.min(start, max));
            int safeEnd = Math.max(0, Math.min(end, max));

            for (int i = safeStart; i <= safeEnd; i++) {
                frames.add(regiones[0][i]);
            }

            if (frames.size == 0) frames.add(regiones[0][0]);

            Animation<TextureRegion> anim = new Animation<>(1f / fps, frames);
            anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            mapaAnimation.put(animName, anim);
            Gdx.app.log("ANIM_OK", animName);
        }
    }

    public Animation<TextureRegion> getAnimationSafe(String preferredName) {
        Animation<TextureRegion> anim = mapaAnimation.get(preferredName);
        if (anim != null) return anim;

        // Fallbacks por nombres que tenéis ahora en game_data/animations.
        if (preferredName.equals("Mushroom Left")) {
            anim = mapaAnimation.get("Mushroom  Left");
            if (anim != null) return anim;
        }
        if (preferredName.equals("Mushroom  Left")) {
            anim = mapaAnimation.get("Mushroom Left");
            if (anim != null) return anim;
        }
        if (preferredName.equals("Mushroom Idle")) {
            anim = mapaAnimation.get("Mushroom Green Idle");
            if (anim != null) return anim;
        }

        // Último recurso: cualquier animación disponible para no crashear.
        if (!mapaAnimation.isEmpty()) {
            return mapaAnimation.values().iterator().next();
        }

        return null;
    }
}
