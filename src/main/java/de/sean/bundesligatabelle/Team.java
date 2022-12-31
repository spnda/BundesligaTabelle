package de.sean.bundesligatabelle;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class Team {
    private String name;
    private Integer punkte;

    public Team() {

    }

    public String getName() {
        return name;
    }

    public Integer getPunkte() {
        return punkte;
    }

    public static class TeamEingabe extends Stage {
        private final Tabelle tabelle;

        public TeamEingabe(@NotNull Tabelle tabelle) {
            this.tabelle = tabelle;
        }

        public void eingabe() {
            final var layout = new BorderPane();
            final var scene = new Scene(layout, 200, 200);

            final var vbox = new VBox();
            vbox.setPadding(new Insets(10, 10, 10, 10));
            vbox.setSpacing(10.0f);
            layout.setCenter(vbox);

            final var vereinInput = new TextField();
            vereinInput.setPromptText("Vereinsname");
            final var punkteInput = new TextField();
            punkteInput.setPromptText("Punkte");

            vereinInput.requestFocus();
            vbox.getChildren().addAll(vereinInput, punkteInput);

            final var speichernButton = new Button();
            speichernButton.setText("Speichern");
            speichernButton.setOnAction(event -> {
                var team = new Team();
                team.name = vereinInput.getText();
                team.punkte = Integer.parseInt(punkteInput.getText());
                tabelle.tabelle.getItems().add(team);
                close();
            });
            vbox.getChildren().add(speichernButton);

            setScene(scene);
            setTitle("Team eingeben");
            show();
        }
    }
}
