package com.oceanPark.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.oceanPark.main.model.Player;

import java.io.IOException;
import java.io.StringWriter;

public class MenuScreen implements Screen {
    final Main game;
    Stage stage;
    Skin skin;

    TextField nombre;
    Label labelNombre, labelInfo;
    TextButton button;

    List<Player> lPlayers;
    ScrollPane scrollPane;

    float escala;

    public MenuScreen(final Main game) {
        this.game = game;
        this.stage = new Stage(game.viewport);
        this.skin = game.skin;
        this.escala = game.escala;

        lPlayers = new List<>(skin);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        textFieldStyle.font = new BitmapFont();
        textFieldStyle.font.setUseIntegerPositions(false);

        nombre = new TextField("", textFieldStyle);
        nombre.setMessageText("Ingrese Nombre");

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = new BitmapFont();
        labelStyle.font.setUseIntegerPositions(false);

        labelNombre = new Label("Nombre", labelStyle);
        labelInfo = new Label("Players", labelStyle);

        button = new TextButton("login", skin);
        button.setTransform(true);
        button.setScale(2 * escala);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendJoin();
            }
        });

        scrollPane = new ScrollPane(lPlayers, skin);
        scrollPane.setFadeScrollBars(true);

        Table playersTable = new Table(skin);
        playersTable.right();
        playersTable.setFillParent(true);
        playersTable.add(labelInfo).size(160, 20).padTop(8);
        playersTable.row();
        playersTable.add(scrollPane).size(160, 80);

        Table menu = new Table();
        menu.left();
        menu.setFillParent(true);
        menu.add(labelNombre).padBottom(10).size(200, 20);
        menu.row();
        menu.add(nombre).size(200, 40).padBottom(20);
        menu.row();
        menu.add(button).size(150, 50);

        stage.addActor(menu);
        stage.addActor(playersTable);
    }

    private void sendJoin() {
        String playerName = nombre.getText() == null ? "" : nombre.getText().trim();
        if (playerName.length() == 0) playerName = "Anonymous";

        StringWriter writer = new StringWriter();
        JsonWriter json = new JsonWriter(writer);

        try {
            json.object()
                .set("type", "JOIN")
                .set("name", playerName)
                .pop();
            json.close();

            boolean sent = game.sendMessage(writer.toString());
            if (!sent) labelInfo.setText("Socket no conectado");
        } catch (IOException e) {
            Gdx.app.error("JOIN", "Error creando JOIN", e);
            labelInfo.setText("Error JOIN");
        }
    }

    public void msg(String msg) {
        JsonValue base = game.lector.parse(msg);
        String mensaje = base.getString("type", "");

        if (mensaje.equals("JOINED")) {
            game.playerId = base.getString("playerId", "");
            game.playerName = base.getString("name", "");
            game.setScreen(new GameScreen(game));
        } else if (mensaje.equals("ERROR")) {
            labelInfo.setText(base.getString("message", "Error"));
        } else if (mensaje.equals("PLAYERS_LIST") || mensaje.equals("STATE")) {
            updatePlayersFromMessage(base);
        }
    }

    private void updatePlayersFromMessage(JsonValue base) {
        JsonValue players = base.get("players");
        if (players == null) return;

        for (JsonValue jugador : players) {
            String playerId = jugador.getString("id", "");
            if (playerId.length() == 0) continue;

            Player p = game.jugadoresMap.get(playerId);
            if (p == null) {
                p = new Player(jugador.getString("name", "Player"), game.getAnimationSafe("Mushroom Idle"));
                game.jugadoresMap.put(playerId, p);
            }
            p.posX = jugador.getFloat("x", p.posX);
            p.posY = jugador.getFloat("y", p.posY);
        }

        updatePlayers();
    }

    public void updatePlayers() {
        Array<Player> arrayParaLista = new Array<>();
        for (Player p : game.jugadoresMap.values()) {
            arrayParaLista.add(p);
        }
        lPlayers.setItems(arrayParaLista);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
