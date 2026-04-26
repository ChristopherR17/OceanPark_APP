package com.oceanPark.main.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.oceanPark.main.Main;

/**
 * Pantalla de menú principal con nickname y botón PLAY
 */
public class MenuScreen extends ScreenAdapter {

    private final Main game;
    private Stage stage;

    private TextField nameField;
    private TextButton playButton;
    private Label statusLabel;
    private Label playersLabel;

    private boolean waitingForJoin = false;
    private float waitTimer = 0;
    private static final float JOIN_TIMEOUT = 8f;

    // Colores
    private static final Color COLOR_PRIMARY = Color.valueOf("00BFFF");   // Azul océano
    private static final Color COLOR_SUCCESS = Color.valueOf("00FF88");
    private static final Color COLOR_ERROR   = Color.valueOf("FF4444");
    private static final Color COLOR_WARNING = Color.valueOf("FFD700");

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(game.viewport);
        Gdx.input.setInputProcessor(stage);

        Skin skin = game.skin;

        // Configurar handler de WebSocket
        game.wsManager.setHandler(new MenuMessageHandler());

        // --- UI ---
        Table table = new Table();
        table.setFillParent(true);

        // Título
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = new BitmapFont();
        titleStyle.font.setUseIntegerPositions(false);
        titleStyle.fontColor = COLOR_PRIMARY;

        Label title = new Label("OCEAN PARK", titleStyle);
        title.setFontScale(3f);
        title.setAlignment(Align.center);

        // Campo de nombre
        nameField = new TextField("", skin);
        nameField.setMessageText("Tu nickname...");
        nameField.setMaxLength(16);

        // Botón PLAY
        playButton = new TextButton("PLAY", skin);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onPlayClicked();
            }
        });

        // Estado de conexión
        Label.LabelStyle statusStyle = new Label.LabelStyle();
        statusStyle.font = new BitmapFont();
        statusStyle.font.setUseIntegerPositions(false);
        statusStyle.fontColor = COLOR_WARNING;

        statusLabel = new Label("Conectando...", statusStyle);

        // Lista de jugadores
        playersLabel = new Label("", statusStyle);

        // Layout
        table.add(title).padBottom(40).row();
        table.add(statusLabel).padBottom(10).row();
        table.add(nameField).width(280).height(50).padBottom(20).row();
        table.add(playButton).width(280).height(60).padBottom(20).row();
        table.add(playersLabel).padTop(20);

        stage.addActor(table);

        // Foco en el campo de texto
        stage.setKeyboardFocus(nameField);
    }

    /**
     * Handler de mensajes del WebSocket específico para el menú
     */
    private class MenuMessageHandler implements com.oceanPark.main.network.WebSocketManager.MessageHandler {
        @Override
        public void onPlayersUpdate(com.badlogic.gdx.utils.ObjectMap<com.oceanPark.main.data.PlayerData> players) {
            // Actualizar UI en hilo principal
            Gdx.app.postRunnable(() -> {
                StringBuilder sb = new StringBuilder("Jugadores: ");
                int count = 0;
                for (com.oceanPark.main.data.PlayerData p : players.values()) {
                    if (count > 0) sb.append(", ");
                    sb.append(p.name);
                    count++;
                }
                playersLabel.setText(sb.toString());
            });
        }

        @Override
        public void onJoined(String playerId, String name, float spawnX, float spawnY) {
            Gdx.app.postRunnable(() -> {
                waitingForJoin = false;
                game.myPlayerId = playerId;
                game.myPlayerName = name;
                game.spawnX = spawnX;
                game.spawnY = spawnY;

                game.setScreen(new GameScreen(game));
            });
        }

        @Override
        public void onConnectionChange(boolean connected) {
            Gdx.app.postRunnable(() -> {
                if (connected) {
                    statusLabel.setText("✅ Conectado al servidor");
                    statusLabel.getStyle().fontColor = COLOR_SUCCESS;
                    playButton.setDisabled(false);
                } else {
                    statusLabel.setText("⏳ Reconectando...");
                    statusLabel.getStyle().fontColor = COLOR_WARNING;
                }
            });
        }

        @Override
        public void onError(String message) {
            Gdx.app.postRunnable(() -> {
                waitingForJoin = false;
                statusLabel.setText("❌ " + message);
                statusLabel.getStyle().fontColor = COLOR_ERROR;
            });
        }
    }

    private void onPlayClicked() {
        String nickname = nameField.getText().trim();

        if (nickname.isEmpty()) {
            statusLabel.setText("⚠️ Escribe un nickname");
            return;
        }

        if (!game.wsManager.isConnected()) {
            statusLabel.setText("❌ Sin conexión al servidor");
            return;
        }

        // Enviar JOIN
        waitingForJoin = true;
        waitTimer = 0;
        game.wsManager.sendJoin(nickname);
        statusLabel.setText("🔗 Entrando al juego...");
    }

    @Override
    public void render(float delta) {
        // Timeout de JOIN
        if (waitingForJoin) {
            waitTimer += delta;
            if (waitTimer > JOIN_TIMEOUT) {
                waitingForJoin = false;
                statusLabel.setText("⚠️ Timeout - Intenta de nuevo");
            }
        }

        // Limpiar pantalla con color océano
        Gdx.gl.glClearColor(0.02f, 0.1f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
