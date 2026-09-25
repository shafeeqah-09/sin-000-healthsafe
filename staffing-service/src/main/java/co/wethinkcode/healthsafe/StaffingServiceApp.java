package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaffingServiceApp {

    private static final String WARD_URL = "http://localhost:7031/wards/";
    private static final String ALERT_URL = "http://localhost:7032/alert-level";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/schedule", ctx -> {
            String wardId = ctx.queryParam("wardId");
            if (wardId == null || wardId.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Missing wardId parameter"));
                return;
            }

            try {
                //  Validate ward against WardService
                HttpResponse<String> wardResponse = client.send(
                        HttpRequest.newBuilder().uri(URI.create(WARD_URL + wardId)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

                if (wardResponse.statusCode() == 404) {
                    ctx.status(404).json(Map.of("error", "Ward not found: " + wardId));
                    return;
                }
                if (wardResponse.statusCode() != 200) {
                    ctx.status(502).json(Map.of("error", "WardService unavailable"));
                    return;
                }

                JsonNode ward = mapper.readTree(wardResponse.body());

                // Get current emergency level
                HttpResponse<String> alertResponse = client.send(
                        HttpRequest.newBuilder().uri(URI.create(ALERT_URL)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

                if (alertResponse.statusCode() != 200) {
                    ctx.status(502).json(Map.of("error", "AlertLevelService unavailable"));
                    return;
                }

                JsonNode alert = mapper.readTree(alertResponse.body());
                int level = alert.get("level").asInt();

                // Compute for schedule size
                int doctorsOnCall;
                if (level <= 2) doctorsOnCall = 1;
                else if (level <= 5) doctorsOnCall = 2;
                else doctorsOnCall = 3;

                // Build a response
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("wardId", ward.get("wardId").asText());
                response.put("wing", ward.get("wing").asText());
                response.put("department", ward.get("department").asText());
                response.put("alertLevel", level);
                response.put("doctorsOnCall", doctorsOnCall);
                response.put("doctors", List.of("Dr. Smith", "Dr. Naidoo", "Dr. Patel")
                        .subList(0, doctorsOnCall));

                ctx.json(response);

            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "Internal error: " + e.getMessage()));
            }
        });
        // TODO (Provides on-call schedules for doctors based on ward and status.)
        // Add domain endpoints for staffing-service here.
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
