package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitServiceAppTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer hubServer;
    private HttpServer delayServer;
    private Javalin app;
    private URI transitUri;
    private int hubStatus = 200;

    @BeforeEach
    void startServices() throws IOException {
        hubServer = startStubServer("/hubs/H-500", () -> hubStatus,
                "{\"hubId\":\"H-500\",\"province\":\"Gauteng\","
                        + "\"sortingCenter\":\"Johannesburg Central\",\"active\":true}");
        delayServer = startStubServer("/delay-stage/H-500", () -> 200, "{\"hubId\":\"H-500\",\"stage\":3}");
        app = TransitServiceApp.createApp(baseUri(hubServer), baseUri(delayServer)).start(0);
        transitUri = URI.create("http://localhost:" + app.port());
    }

    @AfterEach
    void stopServices() {
        if (app != null) app.stop();
        if (hubServer != null) hubServer.stop(0);
        if (delayServer != null) delayServer.stop(0);
    }

    @Test
    void etaCallsBothServicesAndIncludesDelayAdjustedArrival() throws Exception {
        HttpResponse<String> response = get("/eta/H-500");
        JsonNode body = mapper.readTree(response.body());

        assertEquals(200, response.statusCode(), response.body());
        assertEquals("H-500", body.get("hubId").asText());
        assertEquals("Gauteng", body.get("province").asText());
        assertEquals("Johannesburg Central", body.get("sortingCenter").asText());
        assertEquals(3, body.get("delayStage").asInt());
        assertEquals(150, body.get("estimatedArrivalMinutes").asInt());
        assertTrue(InstantParser.isIsoInstant(body.get("estimatedArrival").asText()));
    }

    @Test
    void returnsNotFoundWhenHubServiceDoesNotKnowHub() throws Exception {
        hubStatus = 404;

        HttpResponse<String> response = get("/eta/H-500");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Hub not found"));
    }

    private HttpServer startStubServer(String path, StatusProvider statusProvider, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            int status = statusProvider.status();
            exchange.sendResponseHeaders(status, status == 404 ? -1 : bytes.length);
            if (status != 404) {
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            } else {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private URI baseUri(HttpServer server) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(transitUri.resolve(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @FunctionalInterface
    private interface StatusProvider { int status(); }

    private static class InstantParser {
        static boolean isIsoInstant(String value) {
            try {
                java.time.Instant.parse(value);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
    }
}
