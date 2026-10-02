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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubServiceAppTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Javalin app;
    private URI baseUri;

    @BeforeEach
    void startApp() {
        HubRepository repository = new HubRepository(List.of(
                new Hub("H-500", "Gauteng", "Johannesburg Central", true),
                new Hub("H-501", "Western Cape", "Cape Town Port", false)));
        app = HubServiceApp.createApp(repository).start(0);
        baseUri = URI.create("http://localhost:" + app.port());
    }

    @AfterEach
    void stopApp() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        HttpResponse<String> response = get("/health");

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void hubsEndpointReturnsJsonList() throws Exception {
        HttpResponse<String> response = get("/hubs");
        List<Hub> hubs = objectMapper.readValue(response.body(), new TypeReference<>() {});

        assertEquals(200, response.statusCode());
        assertEquals(2, hubs.size());
        assertEquals("H-500", hubs.get(0).getHubId());
    }

    @Test
    void hubEndpointReturnsMatchingHubWithoutCaseSensitivity() throws Exception {
        HttpResponse<String> response = get("/hubs/h-500");
        Hub hub = objectMapper.readValue(response.body(), Hub.class);

        assertEquals(200, response.statusCode());
        assertEquals("H-500", hub.getHubId());
    }

    @Test
    void unknownHubReturnsJson404() throws Exception {
        HttpResponse<String> response = get("/hubs/H-999");
        Map<String, String> body = objectMapper.readValue(response.body(), new TypeReference<>() {});

        assertEquals(404, response.statusCode());
        assertEquals("Hub not found", body.get("error"));
    }

    @Test
    void placeNameEndpointsReturnDistinctValuesAsJson() throws Exception {
        HttpResponse<String> provinces = get("/provinces");
        HttpResponse<String> centers = get("/sorting-centers");

        assertEquals(200, provinces.statusCode());
        assertEquals(List.of("Gauteng", "Western Cape"),
                objectMapper.readValue(provinces.body(), new TypeReference<>() {}));
        assertEquals(200, centers.statusCode());
        assertTrue(centers.body().contains("Johannesburg Central"));
        assertTrue(centers.body().contains("Cape Town Port"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
