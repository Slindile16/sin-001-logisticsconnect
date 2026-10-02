package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.net.URI;
import java.util.Map;

public class HubServiceApp {
    private static final String DEFAULT_INGESTION_URL = "http://localhost:7050";
    private static final int PORT = 7051;

    public static void main(String[] args) {
        String ingestionUrl = System.getenv().getOrDefault("INGESTION_SERVICE_URL", DEFAULT_INGESTION_URL);
        HubRepository repository;
        try {
            repository = new HubRepository(new IngestionClient(URI.create(ingestionUrl)).loadHubs());
        } catch (RuntimeException e) {
            throw new IllegalStateException("Hub Service could not load hubs from Ingestion Service at "
                    + ingestionUrl + ". Ensure Ingestion Service is running and try again.", e);
        }

        createApp(repository).start(PORT);
    }

    static Javalin createApp(HubRepository repository) {
        Javalin app = Javalin.create();
        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs", ctx -> ctx.json(repository.findAll()));
        app.get("/hubs/{hubId}", ctx -> {
            var hub = repository.findById(ctx.pathParam("hubId"));
            if (hub.isPresent()) {
                ctx.json(hub.get());
            } else {
                ctx.status(404).json(Map.of("error", "Hub not found"));
            }
        });
        app.get("/provinces", ctx -> ctx.json(repository.findProvinces()));
        app.get("/sorting-centers", ctx -> ctx.json(repository.findSortingCenters()));
        return app;
    }
}
