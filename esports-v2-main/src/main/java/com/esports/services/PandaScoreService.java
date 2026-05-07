package com.esports.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PandaScoreService {
    private static final String API_KEY = "cRld_FqQtCRE8l-ARqUJ4X1ghHKkenQUZSuIsgoM1xddMpsIBNU";
    private static final String BASE_URL = "https://api.pandascore.co";

    public String getTeamResults(String teamName) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            // We search for matches filtered by the team name slug
            String slug = teamName.toLowerCase().replace(" ", "-");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/teams/" + slug + "/matches?filter[status]=finished&per_page=3"))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "Error fetching data: " + e.getMessage();
        }
    }
}