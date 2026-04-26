package com.oceanPark.main.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oceanPark.main.Main;
import com.oceanPark.main.data.PlayerData;
import com.oceanPark.main.data.RemotePlayer;

/**
 * Pantalla principal del juego.
 * Muestra fondo + joystick + jugadores sincronizados con el servidor.
 */
public class GameScreen extends ScreenAdapter {

    private final Main game;

    // Cámaras y viewports
    private OrthographicCamera gameCamera;
    private Viewport gameViewport;
    private OrthographicCamera hudCamera;
    private Viewport hudViewport;

    // Renderizado
    private ShapeRenderer shapes;
    private Texture backgroundTexture;
    private Texture playerTexture;
    private Texture remotePlayerTexture;
    private Texture joystickBaseTex;
    private Texture joystickThumbTex;
    private Texture jumpButtonTex;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Joystick virtual
    private static final float JOYSTICK_BASE_RADIUS = 80f;
    private static final float JOYSTICK_THUMB_RADIUS = 36f;
    private static final float JOYSTICK_DEAD_ZONE = 0.2f;
    private static final float JUMP_RADIUS = 50f;

    private float joystickCenterX, joystickCenterY;
    private float joystickDx, joystickDy;
    private float jumpCenterX, jumpCenterY;
    private int joystickPointer = -1;
    private int jumpPointer = -1;
    private boolean jumpPressed;

    // Input state
    private boolean moveLeft, moveRight, moveJump;
    private String lastDirection = "";

    // Envío periódico
    private float sendTimer;
    private static final float SEND_INTERVAL = 0.05f; // 20 veces/segundo

    // Jugador local
    private RemotePlayer localPlayer;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Configurar handler de WebSocket para el juego
        game.wsManager.setHandler(new GameMessageHandler());

        // Cámaras
        gameCamera = new OrthographicCamera();
        gameViewport = game.viewport; // Usar el mismo del Main
        gameCamera.setToOrtho(false, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        gameCamera.position.set(Main.WORLD_WIDTH / 2, Main.WORLD_HEIGHT / 2, 0);

        hudCamera = new OrthographicCamera();
        hudViewport = new ScreenViewport(hudCamera);
        hudViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        shapes = new ShapeRenderer();

        // Cargar texturas
        backgroundTexture = new Texture("background_oceanPark.png");
        playerTexture = new Texture("player.png"); // Tu sprite de jugador
        remotePlayerTexture = createRemoteTexture();
        joystickBaseTex = createCircleTexture((int)(JOYSTICK_BASE_RADIUS * 2),
            new Color(1, 1, 1, 0.3f));
        joystickThumbTex = createCircleTexture((int)(JOYSTICK_THUMB_RADIUS * 2),
            new Color(0, 0.75f, 1, 0.8f));
        jumpButtonTex = createCircleTexture((int)(JUMP_RADIUS * 2),
            new Color(1, 0.84f, 0, 0.7f));

        // Crear jugador local
        localPlayer = new RemotePlayer(game.myPlayerId, game.myPlayerName);
        localPlayer.x = game.spawnX;
        localPlayer.y = game.spawnY;
        localPlayer.targetX = game.spawnX;
        localPlayer.targetY = game.spawnY;
        localPlayer.active = true;

        // Joystick
        updateJoystickPositions();

        Gdx.input.setInputProcessor(null); // Captura táctil manual
    }

    /**
     * Handler específico para GameScreen
     */
    private class GameMessageHandler implements com.oceanPark.main.network.WebSocketManager.MessageHandler {
        @Override
        public void onPlayersUpdate(ObjectMap<String, PlayerData> players) {
            Gdx.app.postRunnable(() -> {
                // Marcar todos como inactivos primero
                for (RemotePlayer rp : game.remotePlayers.values()) {
                    rp.active = false;
                }

                // Actualizar/crear jugadores
                for (PlayerData data : players.values()) {
                    if (data.id.equals(game.myPlayerId)) {
                        // Actualizar posición del jugador local confirmada por servidor
                        localPlayer.targetX = data.x;
                        localPlayer.targetY = data.y;
                        localPlayer.state = data.state;
                        localPlayer.facingRight = data.facingRight;
                        localPlayer.active = true;
                    } else {
                        // Jugador remoto
                        RemotePlayer rp = game.remotePlayers.get(data.id);
                        if (rp == null) {
                            rp = new RemotePlayer(data.id, data.name);
                            game.remotePlayers.put(data.id, rp);
                        }
                        rp.targetX = data.x;
                        rp.targetY = data.y;
                        rp.state = data.state;
                        rp.facingRight = data.facingRight;
                        rp.active = true;
                    }
                }

                // Eliminar jugadores inactivos (desconectados)
                com.badlogic.gdx.utils.Array<String> toRemove = new com.badlogic.gdx.utils.Array<>();
                for (ObjectMap.Entry<String, RemotePlayer> entry : game.remotePlayers.entries()) {
                    if (!entry.value.active) {
                        toRemove.add(entry.key);
                    }
                }
                for (String id : toRemove) {
                    game.remotePlayers.remove(id);
                }
            });
        }

        @Override
        public void onJoined(String playerId, String name, float spawnX, float spawnY) {
            // Ya estamos en juego, otro jugador se unió
            Gdx.app.log("Game", "Nuevo jugador: " + name);
        }

        @Override
        public void onConnectionChange(boolean connected) {
            if (!connected) {
                Gdx.app.log("Game", "Desconectado - volviendo al menú");
                game.setScreen(new MenuScreen(game));
            }
        }

        @Override
        public void onError(String message) {
            Gdx.app.error("Game", "Error: " + message);
        }
    }

    @Override
    public void render(float delta) {
        // --- INPUT ---
        readTouchInput();

        // Enviar movimiento periódicamente
        sendTimer += delta;
        if (sendTimer >= SEND_INTERVAL) {
            sendTimer = 0;
            sendMoveState();
        }

        // --- ACTUALIZAR ---
        localPlayer.update(delta);
        for (RemotePlayer rp : game.remotePlayers.values()) {
            rp.update(delta);
        }

        // --- DIBUJAR MUNDO ---
        gameViewport.apply();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(gameCamera.combined);
        batch.begin();

        // Fondo
        batch.draw(backgroundTexture, 0, 0, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);

        // Jugador local
        drawPlayer(batch, localPlayer, playerTexture, true);

        // Jugadores remotos
        for (RemotePlayer rp : game.remotePlayers.values()) {
            drawPlayer(batch, rp, remotePlayerTexture, false);
        }

        batch.end();

        // --- DIBUJAR HUD (joystick) ---
        hudViewport.apply();
        drawJoystick();

        // Nombre del jugador
        drawHudText();
    }

    private void drawPlayer(SpriteBatch batch, RemotePlayer player, Texture texture, boolean isLocal) {
        if (!player.active) return;

        float x = player.x - RemotePlayer.WIDTH / 2;
        float y = Main.toScreenY(player.y) - RemotePlayer.HEIGHT / 2;

        // Color diferente para jugador local
        Color originalColor = batch.getColor();
        if (isLocal) {
            batch.setColor(Color.CYAN);
        }

        // Flip horizontal según dirección
        boolean flip = !player.facingRight;
        batch.draw(texture,
            x, y,
            RemotePlayer.WIDTH / 2, RemotePlayer.HEIGHT / 2,
            RemotePlayer.WIDTH, RemotePlayer.HEIGHT,
            flip ? -1 : 1, 1,
            0);

        // Nombre encima del jugador
        BitmapFont font = getFont();
        glyphLayout.setText(font, player.name);
        font.setColor(isLocal ? Color.CYAN : Color.WHITE);
        font.draw(batch, glyphLayout,
            x + RemotePlayer.WIDTH / 2 - glyphLayout.width / 2,
            y + RemotePlayer.HEIGHT + 15);

        batch.setColor(originalColor);
    }

    private void readTouchInput() {
        moveLeft = false;
        moveRight = false;
        moveJump = false;

        // Teclado físico (para desarrollo)
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT) ||
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            moveLeft = true;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT) ||
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            moveRight = true;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SPACE) ||
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            moveJump = true;
        }

        // Joystick táctil
        float halfScreen = hudViewport.getScreenWidth() / 2f;

        for (int i = 0; i < 10; i++) {
            if (!Gdx.input.isTouched(i)) continue;

            float tx = Gdx.input.getX(i);
            float ty = hudViewport.getScreenHeight() - Gdx.input.getY(i);

            // Lado izquierdo = joystick de movimiento
            if (tx < halfScreen) {
                if (joystickPointer < 0) joystickPointer = i;

                if (i == joystickPointer) {
                    float dx = tx - joystickCenterX;
                    float dy = ty - joystickCenterY;
                    float dist = (float)Math.sqrt(dx * dx + dy * dy);
                    float maxDist = JOYSTICK_BASE_RADIUS - JOYSTICK_THUMB_RADIUS;

                    if (dist > maxDist) {
                        dx = dx / dist * maxDist;
                        dy = dy / dist * maxDist;
                    }

                    joystickDx = (maxDist > 0) ? dx / maxDist : 0;
                    joystickDy = (maxDist > 0) ? dy / maxDist : 0;
                }
            }

            // Lado derecho = botón de salto
            if (tx >= halfScreen) {
                if (jumpPointer < 0) jumpPointer = i;

                if (i == jumpPointer) {
                    float dx = tx - jumpCenterX;
                    float dy = ty - jumpCenterY;
                    float dist = (float)Math.sqrt(dx * dx + dy * dy);

                    if (dist <= JUMP_RADIUS) {
                        moveJump = true;
                        if (!jumpPressed) {
                            jumpPressed = true;
                        }
                    }
                }
            }
        }

        // Resetear pointers si no se tocan
        if (joystickPointer >= 0 && !Gdx.input.isTouched(joystickPointer)) {
            joystickPointer = -1;
            joystickDx = 0;
            joystickDy = 0;
        }
        if (jumpPointer >= 0 && !Gdx.input.isTouched(jumpPointer)) {
            jumpPointer = -1;
            jumpPressed = false;
        }

        // Aplicar deadzone al joystick
        if (Math.abs(joystickDx) > JOYSTICK_DEAD_ZONE) {
            if (joystickDx < 0) moveLeft = true;
            else moveRight = true;
        }
    }

    private void sendMoveState() {
        if (!game.wsManager.isConnected()) return;

        // Solo enviar si cambió algo o es envío periódico
        boolean changed = (moveLeft != false || moveRight != false || moveJump != false);

        game.wsManager.sendMove(moveRight, moveLeft, moveJump);
    }

    private void drawJoystick() {
        shapes.setProjectionMatrix(hudCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // Base del joystick
        batch.draw(joystickBaseTex,
            joystickCenterX - JOYSTICK_BASE_RADIUS,
            joystickCenterY - JOYSTICK_BASE_RADIUS,
            JOYSTICK_BASE_RADIUS * 2,
            JOYSTICK_BASE_RADIUS * 2);

        // Thumb del joystick
        float thumbX = joystickCenterX + joystickDx * (JOYSTICK_BASE_RADIUS - JOYSTICK_THUMB_RADIUS);
        float thumbY = joystickCenterY + joystickDy * (JOYSTICK_BASE_RADIUS - JOYSTICK_THUMB_RADIUS);
        batch.draw(joystickThumbTex,
            thumbX - JOYSTICK_THUMB_RADIUS,
            thumbY - JOYSTICK_THUMB_RADIUS,
            JOYSTICK_THUMB_RADIUS * 2,
            JOYSTICK_THUMB_RADIUS * 2);

        // Botón de salto
        batch.draw(jumpButtonTex,
            jumpCenterX - JUMP_RADIUS,
            jumpCenterY - JUMP_RADIUS,
            JUMP_RADIUS * 2,
            JUMP_RADIUS * 2);

        // Texto "JUMP"
        BitmapFont font = getFont();
        font.setColor(Color.BLACK);
        glyphLayout.setText(font, "JUMP");
        font.draw(batch, glyphLayout,
            jumpCenterX - glyphLayout.width / 2,
            jumpCenterY + glyphLayout.height / 2);

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHudText() {
        SpriteBatch batch = game.batch;
        BitmapFont font = getFont();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // Nombre del jugador
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
        glyphLayout.setText(font, game.myPlayerName);
        font.draw(batch, glyphLayout,
            hudViewport.getWorldWidth() - 20 - glyphLayout.width,
            hudViewport.getWorldHeight() - 20);

        // Número de jugadores
        font.getData().setScale(1f);
        font.setColor(Color.GRAY);
        String count = "Jugadores: " + (1 + game.remotePlayers.size);
        glyphLayout.setText(font, count);
        font.draw(batch, glyphLayout, 20, hudViewport.getWorldHeight() - 20);

        batch.end();
        font.getData().setScale(1f);
    }

    private void updateJoystickPositions() {
        joystickCenterX = JOYSTICK_BASE_RADIUS + 20;
        joystickCenterY = JOYSTICK_BASE_RADIUS + 20;
        jumpCenterX = hudViewport.getWorldWidth() - JUMP_RADIUS - 20;
        jumpCenterY = JUMP_RADIUS + 20;
    }

    // --- UTILIDADES GRÁFICAS ---

    private Texture createRemoteTexture() {
        Pixmap pixmap = new Pixmap((int)RemotePlayer.WIDTH, (int)RemotePlayer.HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.SALMON);
        pixmap.fillCircle((int)(RemotePlayer.WIDTH/2), (int)(RemotePlayer.HEIGHT/2),
            (int)(RemotePlayer.WIDTH/2));
        pixmap.setColor(Color.WHITE);
        pixmap.fillCircle((int)(RemotePlayer.WIDTH/2), (int)(RemotePlayer.HEIGHT/2),
            (int)(RemotePlayer.WIDTH/4));
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    private Texture createCircleTexture(int size, Color color) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillCircle(size/2, size/2, size/2);
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    private BitmapFont getFont() {
        return new BitmapFont();
    }

    // --- CICLO DE VIDA ---

    @Override
    public void resize(int width, int height) {
        if (gameViewport != null) gameViewport.update(width, height, true);
        if (hudViewport != null) {
            hudViewport.update(width, height, true);
            updateJoystickPositions();
        }
    }

    @Override
    public void dispose() {
        if (shapes != null) shapes.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (remotePlayerTexture != null) remotePlayerTexture.dispose();
        if (joystickBaseTex != null) joystickBaseTex.dispose();
        if (joystickThumbTex != null) joystickThumbTex.dispose();
        if (jumpButtonTex != null) jumpButtonTex.dispose();
        game.wsManager.sendLeave();
    }

    @Override
    public void hide() {
        super.hide();
    }
}
