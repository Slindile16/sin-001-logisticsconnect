package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) {
        List<Hub> hubs = CsvReader.readHubs();

        Javalin app = Javalin.create();

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs", ctx -> ctx.json(hubs));

        app.start(7050);
    }
}
