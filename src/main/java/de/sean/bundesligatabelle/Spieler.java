package de.sean.bundesligatabelle;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Spieler {
    private String vorname;
    private String nachname;

    private int trikotNummer;

    public Spieler() {
        vorname = nachname = "";
        trikotNummer = 0;
    }

    public String getName() {
        return vorname + " " + nachname;
    }

    public Integer getTrikotNummer() {return trikotNummer; }

    public static class SpielerEingabe extends Stage {
        private final Team.TeamOverview liste;

        public SpielerEingabe(Team.TeamOverview liste) {
            this.liste = liste;
        }

        public void eingabe() {
            final var layout = new BorderPane();
            final var scene = new Scene(layout, 200, 200);

            final var vornameInput = new TextField();
            vornameInput.setPromptText("Vorname");
            final var nameInput = new TextField();
            nameInput.setPromptText("Name");
            final var trikotNummerInput = new TextField();
            trikotNummerInput.setPromptText("Trikotnummer");

            final var speichernButton = new Button();
            speichernButton.setText("Speichern");
            speichernButton.setOnAction(event -> {
                var spieler = new Spieler();
                spieler.vorname = vornameInput.getText();
                spieler.nachname = nameInput.getText();
                try {
                    spieler.trikotNummer = Integer.parseUnsignedInt(trikotNummerInput.getText());
                } catch (NumberFormatException ignored) {}

                liste.team.getSpieler().add(spieler);
                liste.spielerTabelle.getItems().add(spieler);
            });

            final var vbox = new VBox();
            vbox.setPadding(new Insets(10, 10, 10, 10));
            vbox.setSpacing(10.0);
            vbox.getChildren().addAll(vornameInput, nameInput, speichernButton);
            layout.setCenter(vbox);

            setScene(scene);
            setTitle("Spieler eingeben");
            show();
        }
    }
}
