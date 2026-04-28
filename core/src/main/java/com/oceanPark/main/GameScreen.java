package com.oceanPark.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oceanPark.main.model.Coin;
import com.oceanPark.main.model.Door;
import com.oceanPark.main.model.Key;
import com.oceanPark.main.model.Player;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;

public class GameScreen implements Screen {
    JsonReader lector;
    float escala;
    Label labelTest;
    final Main game;

    Stage stage, worldStage;
    Skin skin;
    private Viewport uiViewport, worldViewport;

    public GameScreen(final Main game) {
        uiViewport = new ScreenViewport();
        worldViewport = new FitViewport(320, 180);

        this.game = game;
        this.stage = new Stage(uiViewport, game.batch);
        this.worldStage = new Stage(worldViewport, game.batch);
        this.skin = game.skin;

        if (game.map != null) {
            worldStage.addActor(game.map);
        }

        lector = new JsonReader();
        escala = game.escala;

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = new BitmapFont();
        labelStyle.font.setUseIntegerPositions(false);
        labelTest = new Label("", labelStyle);
        labelTest.setScale(1.2f * escala);
        labelTest.setPosition(10, 10);

        createControls();

        stage.addActor(labelTest);
        Gdx.input.setInputProcessor(stage);
    }

    private void createControls() {
        TextureRegion flecha = new TextureRegion(game.flechaTexture);
        TextureRegion flechaIze = new TextureRegion(game.flechaTexture);
        flechaIze.flip(true, false);
        TextureRegion flechaUp = new TextureRegion(game.flechaUp);

        ImageButton btnDer = new ImageButton(new TextureRegionDrawable(flecha));
        ImageButton btnIzq = new ImageButton(new TextureRegionDrawable(flechaIze));
        ImageButton btnUp = new ImageButton(new TextureRegionDrawable(flechaUp));

        btnIzq.getColor().a = 0.3f;
        btnDer.getColor().a = 0.3f;
        btnUp.getColor().a = 0.3f;

        btnDer.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                move("RIGHT", true);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                move("RIGHT", false);
            }
        });

        btnIzq.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                move("LEFT", true);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                move("LEFT", false);
            }
        });

        btnUp.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                move("JUMP", true);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                move("JUMP", false);
            }
        });

        Table controles = new Table();
        controles.setFillParent(true);
        controles.bottom().left();
        controles.add(btnIzq).size(400, 400).bottom().pad(8);
        controles.add(btnDer).size(400, 400).bottom().pad(8);
        controles.add().expandX();
        controles.add(btnUp).size(550, 550).bottom().right().pad(8);
        controles.setPosition(4, 4);

        stage.addActor(controles);
    }

    public void move(String direccion, boolean pressed) {
        StringWriter writer = new StringWriter();
        JsonWriter json = new JsonWriter(writer);

        try {
            json.object()
                .set("type", "MOVE")
                .set(direccion, pressed)
                .pop();
            json.close();

            boolean sent = game.sendMessage(writer.toString());
            if (!sent) labelTest.setText("WS cerrado");
        } catch (IOException e) {
            Gdx.app.error("MOVE", "Error creando MOVE", e);
        }
    }

    public void msg(String msg) {
        JsonValue base = lector.parse(msg);
        String mensaje = base.getString("type", "");

        if (mensaje.equals("STATE") || mensaje.equals("PLAYERS_LIST")) {
            updatePlayers(base.get("players"));
            updateWorld(base.get("world"));
        } else if (mensaje.equals("JOINED")) {
            if (base.has("worldState")) updateWorld(base.get("worldState"));
        } else if (mensaje.equals("ERROR")) {
            labelTest.setText(base.getString("message", "Error"));
        }
    }

    private void updatePlayers(JsonValue players) {
        if (players == null) return;

        HashSet<String> seen = new HashSet<>();

        for (JsonValue jugador : players) {
            String playerId = jugador.getString("id", "");
            if (playerId.length() == 0) continue;
            seen.add(playerId);

            Player p = game.jugadoresMap.get(playerId);
            if (p == null) {
                p = new Player(jugador.getString("name", "Player"), game.getAnimationSafe("Mushroom Idle"));
                game.jugadoresMap.put(playerId, p);
                worldStage.addActor(p);
            }

            p.posX = jugador.getFloat("x", p.posX);
            p.posY = jugador.getFloat("y", p.posY);
            p.facingRight = jugador.getBoolean("facingRight", p.facingRight);

            String state = jugador.getString("state", "IDLE");
            if (state.equals("RUN")) {
                p.setAnimation(game.getAnimationSafe(p.facingRight ? "Mushroom Right" : "Mushroom Left"));
            } else if (state.equals("JUMP")) {
                p.setAnimation(game.getAnimationSafe("Mushroom Idle"));
            } else {
                p.setAnimation(game.getAnimationSafe("Mushroom Idle"));
            }
        }

        // Eliminar jugadores que ya no llegan en el STATE.
        HashSet<String> existing = new HashSet<>(game.jugadoresMap.keySet());
        for (String id : existing) {
            if (!seen.contains(id)) {
                Player removed = game.jugadoresMap.remove(id);
                if (removed != null) removed.remove();
            }
        }
    }

    private void updateWorld(JsonValue world) {
        if (world == null) return;

        updateKey(world.get("key"));
        updateDoor(world.get("door"));
        updateCoins(world.get("coins"));

        int players = game.jugadoresMap.size();
        labelTest.setText("Jugadores: " + players);
    }

    private void updateKey(JsonValue keyJson) {
        if (keyJson == null) return;

        String id = "1";
        Key k = game.keyMap.get(id);
        if (k == null) {
            k = new Key("key", game.getAnimationSafe("Leaf Idle"));
            game.keyMap.put(id, k);
            worldStage.addActor(k);
        }

        k.taken = keyJson.getBoolean("taken", false);
        k.holder = keyJson.getString("holderId", "");

        if (k.taken && k.holder != null && k.holder.length() > 0) {
            Player holder = game.jugadoresMap.get(k.holder);
            if (holder != null) {
                k.setVisible(true);
                k.updatePoss(holder.posX, holder.posY + 32);
            } else {
                k.setVisible(false);
            }
        } else {
            k.setVisible(true);
            k.updatePoss(keyJson.getFloat("x", 0), keyJson.getFloat("y", 0));
        }
    }

    private void updateDoor(JsonValue doorJson) {
        if (doorJson == null) return;

        String id = "1";
        Door d = game.doorMap.get(id);
        if (d == null) {
            d = new Door("door", game.getAnimationSafe("Leaf Idle"));
            game.doorMap.put(id, d);
            worldStage.addActor(d);
        }

        d.open = doorJson.getBoolean("open", false);
        d.updatePoss(doorJson.getFloat("x", d.posX), doorJson.getFloat("y", d.posY));
    }

    private void updateCoins(JsonValue coinsJson) {
        if (coinsJson == null) return;

        HashSet<String> visibleCoins = new HashSet<>();

        for (JsonValue coin : coinsJson) {
            String id = coin.getString("id", "");
            if (id.length() == 0) continue;
            visibleCoins.add(id);

            Coin c = game.coinMap.get(id);
            if (c == null) {
                c = new Coin(id, game.getAnimationSafe("Leaf Idle"), coin.getFloat("x", 0), coin.getFloat("y", 0));
                game.coinMap.put(id, c);
                worldStage.addActor(c);
            } else {
                c.updatePoss(coin.getFloat("x", c.posX), coin.getFloat("y", c.posY));
            }
        }

        // El server solo envía monedas NO recogidas; las que faltan se eliminan.
        HashSet<String> existing = new HashSet<>(game.coinMap.keySet());
        for (String id : existing) {
            if (!visibleCoins.contains(id)) {
                Coin removed = game.coinMap.remove(id);
                if (removed != null) removed.remove();
            }
        }
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0.33f, 0.54f, 0.69f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewport.apply(true);

        Player local = game.jugadoresMap.get(game.playerId);
        if (local != null) {
            worldViewport.getCamera().position.set(Math.round(local.posX), Math.round(local.posY), 0);
        } else {
            worldViewport.getCamera().position.set(160, 180, 0);
        }
        worldViewport.getCamera().update();

        worldStage.act(delta);
        worldStage.draw();

        uiViewport.apply(true);
        stage.act(delta);
        stage.draw();
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) { draw(delta); }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        worldStage.dispose();
    }
}
