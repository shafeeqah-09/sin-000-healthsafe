package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.util.Map;

public class AlertLevelServiceApp {

    public static currentLevel = 0;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));


        //Read the current level
        app.get("/alert-level", ctx -> ctx.json(Map.of("level", currentLevel)));

        //Update the level
        app.post("/alert-level", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int level = ((Number) body.get("level")).intValue();

                if (level < 0 || level > 8) {
                    ctx.status(400).json(Map.of("error", "Level must be between 0 and 8"));
                    return;
                }

                currentLevel = level;
                ctx.json(Map.of("level", currentLevel, "status", "updated"));
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Invalid body: " + e.getMessage()));
            }
        });
    }
}



        // TODO (Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).)
        // Add domain endpoints for alert-level-service here.
    }
}
