package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import co.wethinkcode.logisticsconnect.mq.ActiveMqStagePublisher;
import co.wethinkcode.logisticsconnect.mq.StageChangedEvent;
import co.wethinkcode.logisticsconnect.mq.StagePublisher;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DelayStageServiceApp {
    private static final int PORT = 7052;
    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;

    public static void main(String[] args) {
        ActiveMqStagePublisher publisher = new ActiveMqStagePublisher();
        Javalin app = createApp(new ConcurrentHashMap<>(), publisher).start(PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            publisher.close();
        }));
    }

    static Javalin createApp(ConcurrentMap<String, Integer> stages) {
        return createApp(stages, event -> { });
    }

    static Javalin createApp(ConcurrentMap<String, Integer> stages, StagePublisher publisher) {
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

            int newStage = update.getStage();
            synchronized (stages) {
                int currentStage = stages.getOrDefault(hubId, MIN_STAGE);
                if (currentStage != newStage) {
                    try {
                        publisher.publish(new StageChangedEvent(hubId, newStage, Instant.now().toString()));
                    } catch (RuntimeException e) {
                        ctx.status(503).json(Map.of("error", "Could not publish stage update"));
                        return;
                    }
                    stages.put(hubId, newStage);
                }
            }
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
