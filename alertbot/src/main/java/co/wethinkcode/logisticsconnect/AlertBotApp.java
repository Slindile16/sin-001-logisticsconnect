package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.ActiveMqAlertSubscriber;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertBotApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertBotApp.class);

    public static void main(String[] args) {
        ActiveMqAlertSubscriber subscriber = new ActiveMqAlertSubscriber();
        Javalin app = Javalin.create();
        app.get("/health", ctx -> ctx.result("OK"));
        app.start(7054);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            subscriber.close();
            app.stop();
            LOGGER.info("AlertBot stopped");
        }));

        LOGGER.info("AlertBot listening on port 7054; alert threshold is stage {}",
                ActiveMqAlertSubscriber.ALERT_THRESHOLD);
    }
}
