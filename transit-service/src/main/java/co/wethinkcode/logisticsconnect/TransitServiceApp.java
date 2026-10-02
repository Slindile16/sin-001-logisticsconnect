package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import co.wethinkcode.logisticsconnect.mq.ActiveMqStageSubscriber;
import co.wethinkcode.logisticsconnect.mq.StageCache;
import io.javalin.Javalin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class TransitServiceApp {
    private static final int PORT = 7053;
    private static final String DEFAULT_HUB_URL = "http://localhost:7051";
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        String hubUrl = System.getenv().getOrDefault("HUB_SERVICE_URL", DEFAULT_HUB_URL);
        StageCache cache = new StageCache();
        ActiveMqStageSubscriber subscriber = new ActiveMqStageSubscriber(cache);
        Javalin app = createApp(URI.create(hubUrl), cache).start(PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            subscriber.close();
        }));
    }

    static Javalin createApp(URI hubService, StageCache stageCache) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        Javalin app = Javalin.create();
        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/eta/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            try {
                Hub hub = getHub(client, hubService, hubId);
                int stage = stageCache.getStage(hubId);
                int estimatedMinutes = 60 + stage * 30;
                ctx.json(new EtaResponse(hub.getHubId(), hub.getProvince(), hub.getSortingCenter(), stage,
                        estimatedMinutes, Instant.now().plusSeconds(estimatedMinutes * 60L).toString()));
            } catch (HubNotFoundException e) {
                ctx.status(404).json(Map.of("error", "Hub not found"));
            } catch (UpstreamServiceException e) {
                ctx.status(502).json(Map.of("error", e.getMessage()));
            }
        });
        return app;
    }

    private static Hub getHub(HttpClient client, URI baseUri, String hubId) {
        HttpResponse<String> response = get(client, baseUri.resolve("/hubs/" + encodePathSegment(hubId)), "Hub Service");
        if (response.statusCode() == 404) {
            throw new HubNotFoundException();
        }
        if (response.statusCode() != 200) {
            throw new UpstreamServiceException("Hub Service returned HTTP " + response.statusCode());
        }
        try {
            return JSON.readValue(response.body(), Hub.class);
        } catch (IOException e) {
            throw new UpstreamServiceException("Hub Service returned invalid hub data: " + e.getMessage());
        }
    }

    private static HttpResponse<String> get(HttpClient client, URI uri, String serviceName) {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException(serviceName + " request was interrupted");
        } catch (IOException e) {
            throw new UpstreamServiceException(serviceName + " is unavailable");
        }
    }

    private static String encodePathSegment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static class EtaResponse {
        private String hubId;
        private String province;
        private String sortingCenter;
        private int delayStage;
        private int estimatedArrivalMinutes;
        private String estimatedArrival;

        public EtaResponse() {
        }

        EtaResponse(String hubId, String province, String sortingCenter, int delayStage,
                    int estimatedArrivalMinutes, String estimatedArrival) {
            this.hubId = hubId;
            this.province = province;
            this.sortingCenter = sortingCenter;
            this.delayStage = delayStage;
            this.estimatedArrivalMinutes = estimatedArrivalMinutes;
            this.estimatedArrival = estimatedArrival;
        }

        public String getHubId() { return hubId; }
        public String getProvince() { return province; }
        public String getSortingCenter() { return sortingCenter; }
        public int getDelayStage() { return delayStage; }
        public int getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }
        public String getEstimatedArrival() { return estimatedArrival; }
    }

    public static class Hub {
        private String hubId;
        private String province;
        private String sortingCenter;
        private boolean active;
        public Hub() { }
        public String getHubId() { return hubId; }
        public String getProvince() { return province; }
        public String getSortingCenter() { return sortingCenter; }
        public boolean isActive() { return active; }
        public void setHubId(String hubId) { this.hubId = hubId; }
        public void setProvince(String province) { this.province = province; }
        public void setSortingCenter(String sortingCenter) { this.sortingCenter = sortingCenter; }
        public void setActive(boolean active) { this.active = active; }
    }

    private static class HubNotFoundException extends RuntimeException { }
    private static class UpstreamServiceException extends RuntimeException {
        UpstreamServiceException(String message) { super(message); }
    }
}
