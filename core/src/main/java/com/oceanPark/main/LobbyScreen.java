package com.oceanPark.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.oceanPark.main.core.Main;
import com.oceanPark.main.model.Player;

public class LobbyScreen implements Screen {
    final Main game;
    Stage stage;
    Skin skin;

    List<Player> lPlayers;

    ScrollPane scrollPane;
    Label labelInfo;
    Button button;


    public LobbyScreen(final Main game){


        this.game=game;
        this.stage = new Stage(game.viewport);
        this.skin= game.skin;
        lPlayers=new List<>(skin);

        float escala = game.viewport.getWorldHeight() / Gdx.graphics.getHeight();

        Table table = new Table();
        table.setFillParent(true);
        table.top();

        //iniciamos lista de players
        scrollPane = new ScrollPane(lPlayers,skin);
        scrollPane.setFadeScrollBars(true);

        //creamos estilo para los labels
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = new BitmapFont();
        labelStyle.font.setUseIntegerPositions(false);

        labelInfo = new Label("Players",labelStyle);
        button= new TextButton("Ready",skin);
        button.setTransform(true);
        button.setScale(2*escala);
        button.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){

                updatePlayers();


            }

        });

        table.add(labelInfo).size(400,50).padTop(20);
        table.row();
        table.add(lPlayers).size(600,200);
        table.row();
        table.add(button).size(300,50);

        //borrar luego
        updatePlayers();

        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);

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

    }

    @Override
    public void render(float delta) {
        // Netejar la pantalla
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Actualitzar i dibuixar l'Stage
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        // Alliberar recursos
        stage.dispose();
        skin.dispose();

    }
}
