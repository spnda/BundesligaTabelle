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

    TableView<Team> tabelle;

    public static void main(String[] args) {
        launch();
    }

    public <T> TableColumn<Team, T> generateColumn(String name, String property) {
        final var col = new TableColumn<Team, T>(name);
        col.setSortable(false);
        col.setEditable(false);
        col.setResizable(false);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    @Override
    public void start(@NotNull Stage stage) {
        database = new DatabaseWrapper();
        database.init();

        final var layout = new BorderPane();
        final var scene = new Scene(layout, 600, 500);

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

            tabelle.setRowFactory(tv -> {
                TableRow<Team> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        // Spielerverwaltung öffnen
                        new Team.SpielerListe(row.getItem()).open();
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
            addTeamButton.setOnAction(event -> {
                new Team.TeamEingabe(this).eingabe();
            });

            final var sortierenButton = new Button();
            sortierenButton.setText("Sortieren");
            sortierenButton.setOnAction(event -> {
                // Einfache InsertionSort implementation mit ArrayList (nicht mit normalen Arrays).
                var items = tabelle.getItems();
                for (int j = 1; j < tabelle.getItems().size(); ++j) {
                    var key = items.get(j);
                    int i = j - 1;
                    while (i >= 0 && items.get(i).getPunkte() < key.getPunkte()) {
                        items.set(i + 1, items.get(i));
                        i -= 1;
                    }
                    items.set(i + 1, key);
                }
                tabelle.refresh();
            });

            final var seasonBox = new ComboBox<String>();
            for (int i = 2010; i < 2023; ++i)
                seasonBox.getItems().add(String.valueOf(i));
            seasonBox.setCellFactory(season -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            });
            seasonBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                final var teams = database.requestTeams(newValue);
                tabelle.getItems().clear();
                for (var team : teams) {
                    tabelle.getItems().add(team);
                }
            });

            final var rightBox = new VBox();
            rightBox.setPadding(new Insets(10, 10, 10, 10));
            rightBox.setSpacing(10.0);
            rightBox.getChildren().addAll(addTeamButton, seasonBox, sortierenButton);
            layout.setRight(rightBox);
        }

        // scene.getStylesheets().add("style.css");
        stage.setTitle("Bundesliga Tabelle");
        stage.setScene(scene);
        stage.show();
    }
}
