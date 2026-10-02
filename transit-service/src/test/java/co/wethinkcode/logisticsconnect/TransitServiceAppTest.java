package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import co.wethinkcode.logisticsconnect.mq.StageCache;
import co.wethinkcode.logisticsconnect.mq.ActiveMqStageSubscriber;
import co.wethinkcode.logisticsconnect.mq.MqConfig;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
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
import javax.jms.Connection;
import javax.jms.Session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitServiceAppTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer hubServer;
    private Javalin app;
    private URI transitUri;
    private int hubStatus = 200;
    private StageCache stageCache;
    private BrokerService broker;
    private ActiveMqStageSubscriber subscriber;

    @BeforeEach
    void startServices() throws IOException {
        hubServer = startStubServer("/hubs/H-500", () -> hubStatus,
                "{\"hubId\":\"H-500\",\"province\":\"Gauteng\","
                        + "\"sortingCenter\":\"Johannesburg Central\",\"active\":true}");
        stageCache = new StageCache();
        stageCache.update("H-500", 3);
        app = TransitServiceApp.createApp(baseUri(hubServer), stageCache).start(0);
        transitUri = URI.create("http://localhost:" + app.port());
    }

    @AfterEach
    void stopServices() throws Exception {
        if (app != null) app.stop();
        if (subscriber != null) subscriber.close();
        if (hubServer != null) hubServer.stop(0);
        if (broker != null) {
            broker.stop();
            broker.waitUntilStopped();
        }
    }

    @Test
    void etaUsesHubDetailsAndSubscribedDelayStage() throws Exception {
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

    @Test
    void etaUsesZeroWhenNoStageUpdateHasArrived() throws Exception {
        stageCache = new StageCache();
        app.stop();
        app = TransitServiceApp.createApp(baseUri(hubServer), stageCache).start(0);
        transitUri = URI.create("http://localhost:" + app.port());

        JsonNode body = mapper.readTree(get("/eta/H-500").body());

        assertEquals(0, body.get("delayStage").asInt());
        assertEquals(60, body.get("estimatedArrivalMinutes").asInt());
    }

    @Test
    void etaUsesStageReceivedFromActiveMqTopic() throws Exception {
        String brokerName = "transit-test-" + System.nanoTime();
        broker = new BrokerService();
        broker.setBrokerName(brokerName);
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();
        broker.waitUntilStarted();

        stageCache = new StageCache();
        subscriber = new ActiveMqStageSubscriber(stageCache, "vm://" + brokerName + "?create=false");
        app.stop();
        app = TransitServiceApp.createApp(baseUri(hubServer), stageCache).start(0);
        transitUri = URI.create("http://localhost:" + app.port());

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://" + brokerName + "?create=false");
        try (Connection connection = factory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            var producer = session.createProducer(session.createTopic(MqConfig.TOPIC));
            producer.send(session.createTextMessage(
                    "{\"hubId\":\"H-500\",\"stage\":5,\"timestamp\":\"2026-10-02T10:15:00Z\"}"));
            producer.close();
        }

        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(3).toNanos();
        while (stageCache.getStage("H-500") != 5 && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        JsonNode body = mapper.readTree(get("/eta/H-500").body());

        assertEquals(5, body.get("delayStage").asInt());
        assertEquals(210, body.get("estimatedArrivalMinutes").asInt());
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
