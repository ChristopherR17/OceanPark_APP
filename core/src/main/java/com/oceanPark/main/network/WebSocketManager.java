package com.oceanPark.main.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketAdapter;
import com.github.czyzby.websocket.WebSockets;
import com.oceanPark.main.data.PlayerData;
import com.oceanPark.main.data.States;

/**
 * Gestor centralizado de WebSocket.
 * Maneja conexión, reconexión y parseo de mensajes del servidor.
 */
public class WebSocketManager {

    private static final String SERVER_URL = "wss://pico3.ieti.site";
    private static final float RECONNECT_DELAY = 3f;

    // Interfaces para notificar eventos
    public interface MessageHandler {
        void onPlayersUpdate(ObjectMap<String, PlayerData> players);
        void onJoined(String playerId, String name, float spawnX, float spawnY);
        void onConnectionChange(boolean connected);
        void onError(String message);
    }

    private WebSocket socket;
    private MessageHandler handler;
    private boolean connected;
    private boolean shouldReconnect = true;
    private float reconnectTimer;
    private String playerId;
    private String playerName;
    private final JsonReader jsonReader = new JsonReader();

    public WebSocketManager() {
        this.connected = false;
    }

    public void setHandler(MessageHandler handler) {
        this.handler = handler;
    }

    /**
     * Inicia la conexión al servidor
     */
    public void connect() {
        Gdx.app.log("WS", "Conectando a " + SERVER_URL);

        socket = WebSockets.newSocket(SERVER_URL);
        socket.setSendGracefully(true);

        socket.addListener(new WebSocketAdapter() {
            @Override
            public boolean onOpen(WebSocket ws) {
                connected = true;
                reconnectTimer = 0;
                Gdx.app.log("WS", "✅ Conectado");
                notifyConnection(true);
                return FULLY_HANDLED;
            }

            @Override
            public boolean onClose(WebSocket ws, int code, String reason) {
                connected = false;
                Gdx.app.log("WS", "🔌 Desconectado: " + reason);
                notifyConnection(false);

                if (shouldReconnect) {
                    reconnectTimer = 0;
                }
                return FULLY_HANDLED;
            }

            @Override
            public boolean onMessage(WebSocket ws, String packet) {
                parseMessage(packet);
                return FULLY_HANDLED;
            }

            @Override
            public boolean onError(WebSocket ws, Throwable error) {
                Gdx.app.error("WS", "Error: " + error.getMessage());
                return FULLY_HANDLED;
            }
        });

        socket.connect();
    }

    /**
     * Actualiza el timer de reconexión
     */
    public void update(float delta) {
        if (!connected && shouldReconnect) {
            reconnectTimer += delta;
            if (reconnectTimer >= RECONNECT_DELAY) {
                reconnectTimer = 0;
                connect();
            }
        }
    }

    /**
     * Parsea mensajes del servidor
     */
    private void parseMessage(String raw) {
        try {
            JsonValue json = jsonReader.parse(raw);
            String type = json.getString("type", "");

            Gdx.app.log("WS", "📩 Recibido: " + type);

            switch (type) {
                case "JOINED":
                    handleJoined(json);
                    break;

                case "STATE":
                    handleState(json);
                    break;

                case "ERROR":
                    handleError(json);
                    break;

                default:
                    Gdx.app.log("WS", "Tipo desconocido: " + type);
            }
        } catch (Exception e) {
            Gdx.app.error("WS", "Error parseando: " + e.getMessage());
        }
    }

    private void handleJoined(JsonValue json) {
        playerId = json.getString("playerId", "");
        playerName = json.getString("name", "");
        float spawnX = json.get("spawnPosition") != null ?
            json.get("spawnPosition").getFloat("x", 100) : 100;
        float spawnY = json.get("spawnPosition") != null ?
            json.get("spawnPosition").getFloat("y", 100) : 100;

        Gdx.app.log("WS", "✅ JOINED: " + playerName + " ID=" + playerId);

        if (handler != null) {
            Gdx.app.postRunnable(() ->
                handler.onJoined(playerId, playerName, spawnX, spawnY));
        }
    }

    private void handleState(JsonValue json) {
        JsonValue playersArray = json.get("players");
        if (playersArray == null) return;

        ObjectMap<String, PlayerData> players = new ObjectMap<>();

        for (JsonValue p : playersArray) {
            PlayerData data = new PlayerData();
            data.id = p.getString("id", "");
            data.name = p.getString("name", "???");
            data.x = p.getFloat("x", 0);
            data.y = p.getFloat("y", 0);

            // Parsear estado como string del enum
            String stateStr = p.getString("state", "IDLE");
            try {
                data.state = States.valueOf(stateStr);
            } catch (IllegalArgumentException e) {
                data.state = States.IDLE;
            }

            data.facingRight = p.getBoolean("facingRight", true);
            players.put(data.id, data);
        }

        if (handler != null && players.size > 0) {
            Gdx.app.postRunnable(() -> handler.onPlayersUpdate(players));
        }
    }

    private void handleError(JsonValue json) {
        String msg = json.getString("message", "Error desconocido");
        Gdx.app.error("WS", "❌ Error servidor: " + msg);
        if (handler != null) {
            handler.onError(msg);
        }
    }

    private void notifyConnection(boolean isConnected) {
        if (handler != null) {
            Gdx.app.postRunnable(() -> handler.onConnectionChange(isConnected));
        }
    }

    // ============ ENVÍO DE MENSAJES ============

    /**
     * Envía JOIN con el nombre del jugador
     */
    public void sendJoin(String nickname) {
        if (!connected || socket == null) return;

        String msg = "{" +
            "\"type\":\"JOIN\"," +
            "\"name\":\"" + escapeJson(nickname) + "\"" +
            "}";

        socket.send(msg);
        Gdx.app.log("WS", "📤 JOIN: " + nickname);
    }

    /**
     * Envía MOVE con las teclas presionadas
     */
    public void sendMove(boolean left, boolean right, boolean jump) {
        if (!connected || socket == null) return;

        String msg = "{" +
            "\"type\":\"MOVE\"," +
            "\"left\":" + left + "," +
            "\"right\":" + right + "," +
            "\"jump\":" + jump +
            "}";

        socket.send(msg);
    }

    /**
     * Envía LEAVE al desconectarse
     */
    public void sendLeave() {
        if (!connected || socket == null) return;

        String msg = "{\"type\":\"LEAVE\"}";
        try {
            socket.send(msg);
        } catch (Exception e) {
            Gdx.app.error("WS", "Error enviando LEAVE");
        }
    }

    // ============ GETTERS ============

    public boolean isConnected() { return connected; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public float getReconnectProgress() {
        return Math.min(reconnectTimer / RECONNECT_DELAY, 1f);
    }

    // ============ UTILIDADES ============

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
    }

    /**
     * Cierra la conexión permanentemente
     */
    public void dispose() {
        shouldReconnect = false;
        if (socket != null) {
            sendLeave();
            WebSockets.closeGracefully(socket);
            socket = null;
        }
    }
}
