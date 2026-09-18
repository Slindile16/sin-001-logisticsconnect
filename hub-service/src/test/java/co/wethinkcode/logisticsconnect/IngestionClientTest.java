package co.wethinkcode.logisticsconnect;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loadsHubFromValidResponse() throws IOException {
        startServer(200, "[{\"hubId\":\"H-500\",\"province\":\"Gauteng\","
                + "\"sortingCenter\":\"Johannesburg Central\",\"active\":true}]");

        Hub hub = new IngestionClient(baseUri()).loadHubs().get(0);

        assertEquals("H-500", hub.getHubId());
        assertEquals("Gauteng", hub.getProvince());
        assertEquals("Johannesburg Central", hub.getSortingCenter());
        assertTrue(hub.isActive());
    }

    @Test
    void reportsHttpError() throws IOException {
        startServer(503, "Unavailable");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new IngestionClient(baseUri()).loadHubs());

        assertTrue(error.getMessage().contains("HTTP 503"));
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hubs", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
    }

    private URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }
}
