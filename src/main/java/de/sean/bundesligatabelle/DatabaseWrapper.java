package de.sean.bundesligatabelle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class DatabaseWrapper {
    private static final String baseUrl = "https://api.openligadb.de/";

    private HttpClient client;

    private Gson gson;

    public void init() {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        gson = new GsonBuilder()
                .create();
    }

    public ArrayList<Team> requestTeams(String saison) {
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
            return list;
        } catch (IOException | InterruptedException e) {
            return new ArrayList<>();
        }
    }
}
