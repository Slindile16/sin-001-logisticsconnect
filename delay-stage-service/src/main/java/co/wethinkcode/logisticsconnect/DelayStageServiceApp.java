package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DelayStageServiceApp {
    private static final int PORT = 7052;
    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;

    public static void main(String[] args) {
        createApp(new ConcurrentHashMap<>()).start(PORT);
    }

    static Javalin createApp(ConcurrentMap<String, Integer> stages) {
        Javalin app = Javalin.create();
        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = normalizeHubId(ctx.pathParam("hubId"));
            ctx.json(Map.of("hubId", hubId, "stage", stages.getOrDefault(hubId, MIN_STAGE)));
        });
        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = normalizeHubId(ctx.pathParam("hubId"));
            StageUpdate update;
            try {
                update = ctx.bodyAsClass(StageUpdate.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Request body must contain an integer stage"));
                return;
            }

            if (update.getStage() == null) {
                ctx.status(400).json(Map.of("error", "Request body must contain an integer stage"));
                return;
            }
            if (update.getStage() < MIN_STAGE || update.getStage() > MAX_STAGE) {
                ctx.status(400).json(Map.of("error", "Stage must be between 0 and 8"));
                return;
            }

            stages.put(hubId, update.getStage());
            ctx.json(Map.of("hubId", hubId, "stage", update.getStage()));
        });
        return app;
    }

    private static String normalizeHubId(String hubId) {
        return hubId.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public static class StageUpdate {
        private Integer stage;

        public StageUpdate() {
        }

        public Integer getStage() {
            return stage;
        }

        public void setStage(int stage) {
            this.stage = stage;
        }
    }
}
