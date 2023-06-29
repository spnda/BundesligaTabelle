package de.sean.bundesligatabelle;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class Tabelle extends Application {
    DatabaseWrapper database;

    String selectedSeason;

    Integer liga;

    TableView<Team> tabelle;

    public static void main(String[] args) {
        launch();
    }

    public <T> TableColumn<Team, T> generateColumn(String name, String property) {
        final var col = new TableColumn<Team, T>(name);
        col.setSortable(false);
        col.setEditable(false);
        // col.setResizable(false);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    void updateTable() {
        final var teams = database.requestTeams(selectedSeason, liga);
        tabelle.getItems().clear();
        for (var team : teams) {
            tabelle.getItems().add(team);
        }
    }

    @Override
    public void start(@NotNull Stage stage) {
        database = new DatabaseWrapper();
        database.init();

        for (int i = 2010; i < 2023; ++i)
            database.addSaison(String.valueOf(i));

        selectedSeason = database.getSaisons().get(0);
        liga = database.getLigen(selectedSeason).get(0);

        final var layout = new BorderPane();
        final var scene = new Scene(layout, 800, 500);

        tabelle = new TableView<>();

        {
            final var icon = generateColumn("", "icon");
            final var vereinsName = generateColumn("Vereinsname", "name");
            final var siege = generateColumn("S", "siege");
            final var unentschieden = generateColumn("U", "unentschieden");
            final var niederlagen = generateColumn("N", "niederlagen");
            final var punkte = generateColumn("P", "punkte");
            final var goals = generateColumn("T", "stylizedGoals");

            icon.setPrefWidth(30);
            siege.setPrefWidth(25);
            unentschieden.setPrefWidth(25);
            niederlagen.setPrefWidth(25);
            punkte.setPrefWidth(25);
            goals.setPrefWidth(50);

            // Get largest
            vereinsName.setPrefWidth(300);

            // Maximise column width
            tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Make rows double-clickable
            tabelle.setRowFactory(tv -> {
                TableRow<Team> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        // Team-Übersicht öffnen
                        new Team.TeamOverview(this, row.getItem()).open();
                    }
                });
                return row;
            });

            tabelle.getColumns().addAll(icon, vereinsName, siege, unentschieden, niederlagen, punkte, goals);
            layout.setPadding(new Insets(10, 10, 10, 10));
            layout.setCenter(tabelle);
        }

        // Right panel
        {
            final var addTeamButton = new Button();
            addTeamButton.setText("Team hinzufügen");
            addTeamButton.setOnAction(event -> new Team.TeamEingabe(this).eingabe());

            final var seasonText = new Text();
            seasonText.setText("Saison: ");

            final var seasonBox = new ComboBox<String>();
            final var seasons = database.getSaisons();
            for (var season : seasons)
                seasonBox.getItems().add(season);
            seasonBox.setValue(selectedSeason);
            seasonBox.setCellFactory(season -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            });
            seasonBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                selectedSeason = newValue;
                updateTable();
            });

            final var ligaText = new Text();
            ligaText.setText("Liga: ");

            final var ligaBox = new ComboBox<Integer>();
            final var ligen = database.getLigen(selectedSeason);
            for (var liga : ligen)
                ligaBox.getItems().add(liga);
            ligaBox.setValue(ligen.get(0));
            ligaBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
                liga = newValue;
                updateTable();
            }));

            final var useActualValues = new Button();
            useActualValues.setText("Echte Werte kopieren");
            useActualValues.setOnAction(event -> {
                database.updateFromAPI(selectedSeason, liga);
                updateTable();
            });

            final var rightBox = new VBox();
            rightBox.setPadding(new Insets(10, 10, 10, 10));
            rightBox.setSpacing(10.0);
            rightBox.getChildren().addAll(addTeamButton, seasonText, seasonBox, ligaText, ligaBox, useActualValues);
            layout.setRight(rightBox);
        }

        updateTable();

        scene.getStylesheets().add("style.css");
        stage.setTitle("Bundesliga Tabelle");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
