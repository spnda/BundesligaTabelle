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

    private String getTableName(String saison, Integer liga) {
        return "bundesliga" + saison + "_" + liga;
    }

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

        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        gson = new GsonBuilder()
                .create();
    }

    public void updateFromAPI(@NotNull String saison, Integer liga) {
        assert(liga > 0 && liga < 4);

        final var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "getbltable/bl" + liga + "/" + saison))
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
                statement.execute("DELETE FROM " + getTableName(saison, liga) + " WHERE 1");
            } catch (SQLException e) {
                System.out.println("SQLException: " + e.getMessage());
            }

            for (var team : list) {
                addTeam(saison, liga, team);
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
            if (!statement.execute("SELECT * FROM saisons ORDER BY saison DESC"))
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

    public ArrayList<Integer> getLigen(String saison) {
        return new ArrayList<>(Arrays.asList(1, 2, 3)); // TODO: Make this dynamic.
    }

    public boolean addSaison(String saison) {
        /*if (getSaisons().contains(saison)) {
            return false;
        }*/

        for (int i = 1; i < 4; ++i) {
            try (var statement = connection.createStatement()) {
                var table = getTableName(saison, i);
                var sql = "CREATE TABLE %s(teamName VARCHAR(255), siege INT, niederlagen INT, unentschieden INT, tore INT, gegentore INT, teamIcon TEXT)";
                statement.execute(String.format(sql, table));
            } catch (SQLException e) {
                System.out.println("SQLException: " + e.getMessage());
                return false;
            }
        }

        try (var statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            return statement.executeUpdate(String.format("INSERT INTO saisons VALUES(%s)", saison)) != 0;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Team> requestTeams(String saison, Integer liga) {
        try (var statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            if (!statement.execute("SELECT * FROM " + getTableName(saison, liga))) {
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
                team.setIconUrl(result.getString("teamIcon"));
                list.add(team);
            }
            return list;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public int addTeam(String saison, Integer liga, @NotNull Team team) {
        var sql = "INSERT INTO " + getTableName(saison, liga) + " VALUES(?,?,?,?,?,?,?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.getName());
            statement.setInt(2, team.getSiege());
            statement.setInt(3, team.getNiederlagen());
            statement.setInt(4, team.getUnentschieden());
            statement.setInt(5, team.getTore());
            statement.setInt(6, team.getGegentore());
            statement.setString(7, team.getIconUrl());

            return statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return 0;
        }
    }

    public int updateTeam(String saison, Integer liga, @NotNull String oldName, @NotNull Team team) {
        var sql = "UPDATE " + getTableName(saison, liga) + " SET teamName=?, siege=?, niederlagen=?, unentschieden=?, tore=?, gegentore=? WHERE teamName=?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.getName());
            statement.setInt(2, team.getSiege());
            statement.setInt(3, team.getNiederlagen());
            statement.setInt(4, team.getUnentschieden());
            statement.setInt(5, team.getTore());
            statement.setInt(6, team.getGegentore());
            statement.setString(7, oldName);
            return statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            return 0;
        }
    }
}
