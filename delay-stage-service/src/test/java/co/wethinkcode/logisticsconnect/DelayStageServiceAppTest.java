package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayStageServiceAppTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private Javalin app;
    private URI baseUri;

    @BeforeEach
    void startApp() {
        app = DelayStageServiceApp.createApp(new ConcurrentHashMap<>()).start(0);
        baseUri = URI.create("http://localhost:" + app.port());
    }

    @AfterEach
    void stopApp() {
        if (app != null) app.stop();
    }

    @Test
    void unknownHubStartsAtStageZero() throws Exception {
        HttpResponse<String> response = get("/delay-stage/H-500");
        Map<String, Object> body = mapper.readValue(response.body(), new TypeReference<>() {});

        assertEquals(200, response.statusCode());
        assertEquals("H-500", body.get("hubId"));
        assertEquals(0, body.get("stage"));
    }

    @Test
    void postUpdatesTheStageReturnedByGet() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/delay-stage/h-500"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"stage\":3}"))
                .build();
        HttpResponse<String> updated = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> fetched = get("/delay-stage/H-500");
        Map<String, Object> body = mapper.readValue(fetched.body(), new TypeReference<>() {});

        assertEquals(200, updated.statusCode());
        assertEquals(3, body.get("stage"));
    }

    @Test
    void rejectsStageOutsideSupportedRange() throws Exception {
        HttpResponse<String> response = postStage(9);

        assertEquals(400, response.statusCode());
    }

    @Test
    void rejectsMissingStageField() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/delay-stage/H-500"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        assertEquals(400, HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(baseUri.resolve(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postStage(int stage) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/delay-stage/H-500"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"stage\":" + stage + "}"))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
