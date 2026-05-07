package com.esports.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordWebhookService {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1499216188939440168/AvJN-WTILDEfys4SQ51xoPWvVmyXQ8OsyK02CazeHXOy-BoQosCj85n0k8GZxY8hnCUw";

    // Creating one shared client is more robust and efficient
    private static final HttpClient client = HttpClient.newHttpClient();

    public void announceTournament(String name, String game, String date, String prize) {
        String jsonPayload = """
        {
          "embeds": [{
            "title": "🎮 NEW TOURNAMENT ANNOUNCEMENT",
            "color": 5814783,
            "fields": [
              {"name": "Tournament Name", "value": "%s", "inline": false},
              {"name": "Game Title", "value": "%s", "inline": true},
              {"name": "Prize Pool", "value": "%s", "inline": true},
              {"name": "Start Date", "value": "%s", "inline": false}
            ],
            "footer": {"text": "AI Management System • Automated Broadcast"}
          }]
        }
        """.formatted(name, game, prize, date);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> System.out.println("Discord Status: " + response.statusCode()))
                .exceptionally(e -> {
                    System.err.println("API Error: " + e.getMessage());
                    return null;
                });
    }
}