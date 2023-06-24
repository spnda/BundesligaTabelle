package de.sean.bundesligatabelle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class DatabaseWrapper {
    private static final String baseUrl = "https://api.openligadb.de/";

    private HttpClient client;

    private Gson gson;

    private Connection connection;

    public void init() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/bundesliga", "root", "password");
            System.out.println("Connected to SQL database");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (!doesTableExist("saisons")) {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE saisons(saison VARCHAR(4))");
            } catch (SQLException e) {
                System.out.println("SQLException: " + e.getMessage());
            }
        }

        if (getSaisons().isEmpty()) {
            if (!addSaison("2022"))
                throw new RuntimeException("Failed to create saison 2022");
        }

        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        gson = new GsonBuilder()
                .create();
    }

    public void updateFromAPI(@NotNull String saison) {
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "getbltable/bl1/" + saison))
                .GET()
                .build();

        try {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 202) {
                throw new IOException("Status code not 200");
            }

            final var matches = gson.fromJson(response.body(), Team[].class);
            final var list = new ArrayList<>(Arrays.asList(matches));
            list.sort(Comparator.comparing(Team::getPunkte).reversed());

            // Clear table
            try (var statement = connection.createStatement()) {
                statement.execute("DELETE FROM bundesliga" + saison + " WHERE 1");
            } catch (SQLException e) {
                System.out.println("SQLException: " + e.getMessage());
            }

            for (var team : list) {
                addTeam(saison, team);
            }
        } catch (IOException | InterruptedException ignored) {}
    }

    private boolean doesTableExist(@NotNull String name) {
        try (var statement = connection.createStatement()) {
            // Returns false if the result is empty.
            if (!statement.execute(String.format("SELECT count(*) FROM information_schema.TABLES WHERE (TABLE_SCHEMA = 'bundesliga') AND (TABLE_NAME = '%s');", name)))
                return false;
            var result = statement.getResultSet();
            result.next();
            return result.getInt(1) == 1;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<String> getSaisons() {
        try (var statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            if (!statement.execute("SELECT * FROM saisons"))
                return new ArrayList<>();

            final var result = statement.getResultSet();
            final var list = new ArrayList<String>();
            while (result.next()) {
                list.add(result.getString(1));
            }
            return list;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean addSaison(String saison) {
        if (getSaisons().contains(saison)) {
            return false;
        }

        try (var statement = connection.createStatement()) {
            var table = "bundesliga" + saison;
            var sql = "CREATE TABLE %s(teamName VARCHAR(255), siege INT, niederlagen INT, unentschieden INT, tore INT, gegentore INT)";
            statement.execute(String.format(sql, table));
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return false;
        }

        try (var statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            return statement.executeUpdate(String.format("INSERT INTO saisons VALUES(%s)", saison)) != 0;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Team> requestTeams(String saison) {
        try (var statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            var tabelle = "bundesliga" + saison;
            if (!statement.execute("SELECT * FROM " + tabelle)) {
                return new ArrayList<>();
            }

            final var result = statement.getResultSet();
            final var list = new ArrayList<Team>();
            while (result.next()) {
                var team = new Team();
                team.setName(result.getString("teamName"));
                team.setSiege(result.getInt("siege"));
                team.setNiederlagen(result.getInt("niederlagen"));
                team.setUnentschieden(result.getInt("unentschieden"));
                team.setTore(result.getInt("tore"));
                team.setGegentore(result.getInt("gegentore"));
                list.add(team);
            }
            return list;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean addTeam(String saison, @NotNull Team team) {
        var sql = "INSERT INTO bundesliga" + saison + " VALUES(?,?,?,?,?,?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.getName());
            statement.setInt(2, team.getSiege());
            statement.setInt(3, team.getNiederlagen());
            statement.setInt(4, team.getUnentschieden());
            statement.setInt(5, team.getTore());
            statement.setInt(6, team.getGegentore());

            return statement.execute();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return false;
        }
    }
}
