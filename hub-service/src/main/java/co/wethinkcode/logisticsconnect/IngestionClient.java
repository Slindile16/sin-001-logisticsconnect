package co.wethinkcode.logisticsconnect;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class IngestionClient {
    private final URI ingestionBaseUri;

    public IngestionClient(URI ingestionBaseUri) {
        this.ingestionBaseUri = ingestionBaseUri;
    }

    public String fetchHubsJson() {
        HttpRequest request = HttpRequest.newBuilder(ingestionBaseUri.resolve("/hubs"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Ingestion service returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching hubs", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not fetch hubs from ingestion service", e);
        }
    }
}
