package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class WardServiceApp {

    private static final String INGESTION_URL = "http://localhost:7030/wards";
    private static final List<Ward> wardCache = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7031);

        app.get("/health", ctx -> ctx.result("OK"));


        app.post("/refresh", ctx -> {
            try {
                fetchWardsFromIngestion();
                ctx.json("Cache refreshed: " + wardCache.size() + " wards");
            } catch (Exception e) {
                ctx.status(500).json("Failed to refresh: " + e.getMessage());
            }
        });

        app.get("/wards", ctx -> ctx.json(wardCache));

        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Ward found = wardCache.stream()
                    .filter(w -> w.getWardId().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);
            if (found == null) {
                ctx.status(404).json("Ward not found: " + id);
            } else {
                ctx.json(found);
            }
        });

        try {
            fetchWardsFromIngestion();
            System.out.println("Startup: loaded " + wardCache.size() + " wards from ingestion-service");
        } catch (Exception e) {
            System.out.println("WARNING: Could not reach ingestion-service on startup: " + e.getMessage());
        }
    }

    private static void fetchWardsFromIngestion() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ingestion returned status " + response.statusCode());
        }

        List<Ward> wards = mapper.readValue(response.body(), new TypeReference<List<Ward>>() {});
        wardCache.clear();
        wardCache.addAll(wards);
    }
}

        // TODO (Provides lists of wards and departments.)
        // Add domain endpoints for ward-service here.
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
