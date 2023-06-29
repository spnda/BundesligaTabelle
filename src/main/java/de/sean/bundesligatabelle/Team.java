package de.sean.bundesligatabelle;

import com.google.gson.annotations.SerializedName;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Team {
    @SerializedName("teamName")
    private String name;

    @SerializedName("won")
    private Integer siege;
    @SerializedName("lost")
    private Integer niederlagen;
    @SerializedName("draw")
    private Integer unentschieden;

    @SerializedName("goals")
    private Integer tore;
    @SerializedName("opponentGoals")
    private Integer gegentore;

    @SerializedName("teamIconUrl")
    private String iconUrl;

    private final ArrayList<Spieler> spieler;

    private transient Image iconImageCache;

    public Team() {
        spieler = new ArrayList<>();
        siege = niederlagen = unentschieden = tore = gegentore = 0;
    }

    public String getName() {
        return name;
    }
    public void setName(@NotNull String name) { this.name = name; }

    public Integer getSiege() {
        return siege;
    }
    public void setSiege(@NotNull Integer siege) { this.siege = siege; }

    public Integer getNiederlagen() {
        return niederlagen;
    }
    public void setNiederlagen(@NotNull Integer niederlagen) { this.niederlagen = niederlagen; }

    public Integer getUnentschieden() {
        return unentschieden;
    }
    public void setUnentschieden(@NotNull Integer unentschieden) { this.unentschieden = unentschieden; }

    public Integer getSpiele() {
        return siege + niederlagen + unentschieden;
    }

    public Integer getTore() {
        return tore;
    }
    public void setTore(@NotNull Integer tore) { this.tore = tore; }

    public Integer getGegentore() {
        return gegentore;
    }
    public void setGegentore(@NotNull Integer gegentore) { this.gegentore = gegentore; }

    public Integer getTorDifferenz() { return tore - gegentore; }

    public String getStylizedGoals() {
        return "" + tore + ":" + gegentore;
    }

    public Integer getPunkte() {
        return getSiege() * 3 + getUnentschieden();
    }

    public ArrayList<Spieler> getSpieler() {
        return spieler;
    }

    public Node getIcon() {
        if (iconUrl == null) {
            return null;
        }

        if (iconImageCache == null) {
            iconImageCache = new Image(iconUrl);
        }
        ImageView view = new ImageView(iconImageCache);
        view.setFitWidth(25);
        view.setFitHeight(25);
        view.setPreserveRatio(true);
        view.setCache(true);

        final var box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.getChildren().add(view);
        return box;
    }

    public static class TeamEingabe extends Stage {
        private final Tabelle tabelle;

        public TeamEingabe(@NotNull Tabelle tabelle) {
            this.tabelle = tabelle;
        }

        public void eingabe() {
            final var layout = new BorderPane();
            final var scene = new Scene(layout, 200, 300);

            final var vbox = new VBox();
            vbox.setPadding(new Insets(10, 10, 10, 10));
            vbox.setSpacing(10.0);
            layout.setCenter(vbox);

            final var vereinInput = new TextField();
            vereinInput.setPromptText("Vereinsname");
            final var siegeInput = new TextField();
            siegeInput.setPromptText("Siege");
            final var unentschiedenInput = new TextField();
            unentschiedenInput.setPromptText("Unentschieden");
            final var niederlagenInput = new TextField();
            niederlagenInput.setPromptText("Niederlagen");
            final var toreInput = new TextField();
            toreInput.setPromptText("Tore");
            final var gegentoreInput = new TextField();
            gegentoreInput.setPromptText("Gegentore");

            vereinInput.requestFocus();
            vbox.getChildren().addAll(vereinInput, siegeInput, unentschiedenInput, niederlagenInput, toreInput, gegentoreInput);

            final var speichernButton = new Button();
            speichernButton.setText("Speichern");
            speichernButton.setOnAction(event -> {
                if (vereinInput.getText().isEmpty()) return; // Nicht das Fenster schließen ohne Namen.

                var team = new Team();
                team.name = vereinInput.getText();

                try {
                    team.siege = Integer.parseUnsignedInt(siegeInput.getText());
                } catch (NumberFormatException ignored) {}
                try {
                    team.unentschieden = Integer.parseUnsignedInt(unentschiedenInput.getText());
                } catch (NumberFormatException ignored) {}
                try {
                    team.niederlagen = Integer.parseUnsignedInt(niederlagenInput.getText());
                } catch (NumberFormatException ignored) {}
                try {
                    team.tore = Integer.parseUnsignedInt(toreInput.getText());
                } catch (NumberFormatException ignored) {}
                try {
                    team.gegentore = Integer.parseUnsignedInt(gegentoreInput.getText());
                } catch (NumberFormatException ignored) {}

                tabelle.tabelle.getItems().add(team);
                tabelle.database.addTeam(tabelle.selectedSeason, team);
                close();
            });
            vbox.getChildren().add(speichernButton);

            setScene(scene);
            setTitle("Team eingeben");
            show();
        }
    }

    public static class TeamOverview extends Stage {
        private final Tabelle tabelle;

        Team team;
        TableView<Spieler> spielerTabelle;

        public TeamOverview(@NotNull Tabelle tabelle, @NotNull Team team) {
            this.tabelle = tabelle;
            this.team = team;
        }

        private @NotNull TextFieldWithTitle createTextField(String initialText, String prompt) {
            final var vbox = new VBox();

            final var text = new Text();
            text.setText(prompt);

            final var input = new TextField();
            input.setText(initialText);
            input.setPromptText(prompt);
            vbox.getChildren().addAll(text, input);

            final var field = new TextFieldWithTitle();
            field.vbox = vbox;
            field.field = input;
            return field;
        }

        public static class TextFieldWithTitle {
            public VBox vbox;
            public TextField field;
        }

        private void updateData(Consumer<Team> consumer) {
            var oldName = team.getName();
            consumer.accept(team);
            tabelle.tabelle.refresh();
            tabelle.database.updateTeam(tabelle.selectedSeason, oldName, team);
        }

        public void open() {
            final var layout = new BorderPane();
            layout.setPadding(new Insets(10, 10, 10, 10));

            final var scene = new Scene(layout, 500, 400);

            // Edit text fields
            {
                final var inputList = new HBox();

                final var nameInput = createTextField(team.getName(), "Vereinsname");
                nameInput.field.setOnKeyPressed(event -> {
                    if (event.getCode() != KeyCode.ENTER) return;
                    updateData((team) -> team.setName(nameInput.field.getText()));
                });

                final var siegeInput = createTextField(String.valueOf(team.getSiege()), "Siege");
                siegeInput.field.setOnKeyPressed(event -> {
                    if (event.getCode() != KeyCode.ENTER) return;
                    updateData((team) -> team.setSiege(Integer.parseInt(siegeInput.field.getText())));
                });

                final var niederlagenInput = createTextField(String.valueOf(team.getNiederlagen()), "Niederlagen");
                niederlagenInput.field.setOnKeyPressed(event -> {
                    if (event.getCode() != KeyCode.ENTER) return;
                    updateData((team) -> team.setNiederlagen(Integer.parseInt(niederlagenInput.field.getText())));
                });

                final var unentschiedenInput = createTextField(String.valueOf(team.getUnentschieden()), "Unentschieden");
                unentschiedenInput.field.setOnKeyPressed(event -> {
                    if (event.getCode() != KeyCode.ENTER) return;
                    updateData((team) -> team.setUnentschieden(Integer.parseInt(unentschiedenInput.field.getText())));
                });

                final var toreInput = createTextField(String.valueOf(team.getTore()), "Tore");
                toreInput.field.setOnKeyPressed(event -> {
                    if (event.getCode() != KeyCode.ENTER) return;
                    updateData((team) -> team.setTore(Integer.parseInt(toreInput.field.getText())));
                });

                final var gegentoreInput = createTextField(String.valueOf(team.getGegentore()), "Gegentore");
                gegentoreInput.field.setOnKeyPressed(event -> {
                    if (event.getCode() != KeyCode.ENTER) return;
                    updateData((team) -> team.setGegentore(Integer.parseInt(gegentoreInput.field.getText())));
                });

                inputList.setSpacing(5.0);
                inputList.getChildren().addAll(nameInput.vbox, siegeInput.vbox, niederlagenInput.vbox, unentschiedenInput.vbox, toreInput.vbox, gegentoreInput.vbox);
                inputList.setPadding(new Insets(0, 0, 10, 0));

                layout.setTop(inputList);

                setOnCloseRequest(event -> updateData((team) -> {
                    team.setName(nameInput.field.getText());
                    team.setSiege(Integer.parseInt(siegeInput.field.getText()));
                    team.setNiederlagen(Integer.parseInt(niederlagenInput.field.getText()));
                    team.setUnentschieden(Integer.parseInt(unentschiedenInput.field.getText()));
                    team.setTore(Integer.parseInt(toreInput.field.getText()));
                    team.setGegentore(Integer.parseInt(gegentoreInput.field.getText()));
                }));
            }

            // Player list
            {
                spielerTabelle = new TableView<>();
                final var name = new TableColumn<Spieler, String>("Name");
                name.setCellValueFactory(new PropertyValueFactory<>("Name"));
                final var trikotNummer = new TableColumn<Spieler, Integer>("Trikotnummer");
                trikotNummer.setCellValueFactory(new PropertyValueFactory<>("TrikotNummer"));

                spielerTabelle.getColumns().addAll(name, trikotNummer);
                for (var spieler : team.spieler) {
                    spielerTabelle.getItems().add(spieler);
                }
                layout.setCenter(spielerTabelle);
            }

            // Player buttons
            {
                final var addPlayerButton = new Button();
                addPlayerButton.setText("Spieler hinzufügen");
                addPlayerButton.setOnAction(event -> {
                    new Spieler.SpielerEingabe(this).eingabe();
                });

                final var rightBox = new VBox();
                rightBox.setPadding(new Insets(10, 10, 10, 10));
                rightBox.setSpacing(10.0);
                rightBox.getChildren().add(addPlayerButton);
                layout.setRight(rightBox);
            }

            setScene(scene);
            setTitle("Team Overview");
            show();
        }
    }
}
