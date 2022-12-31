package de.sean.bundesligatabelle;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Tabelle extends Application {
    TableView<Team> tabelle;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(@NotNull Stage stage) {
        final var layout = new BorderPane();
        final var scene = new Scene(layout, 320, 240);

        tabelle = new TableView<>();
        layout.setCenter(tabelle);

        {
            final var vereinsName = new TableColumn<Team, String>("Vereinsname");
            vereinsName.setSortable(false);
            vereinsName.setCellValueFactory(new PropertyValueFactory<>("Name"));

            final var punkte = new TableColumn<Team, Integer>("Punkte");
            punkte.setSortable(false);
            punkte.setCellValueFactory(new PropertyValueFactory<>("Punkte"));

            tabelle.getColumns().addAll(vereinsName, punkte);
        }

        final var teamButton = new Button();
        teamButton.setText("Team hinzufügen");
        teamButton.setOnAction(event -> {
            new Team.TeamEingabe(this).eingabe();
        });
        layout.setRight(teamButton);

        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
