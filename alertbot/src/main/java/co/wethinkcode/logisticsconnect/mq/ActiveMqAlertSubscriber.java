package co.wethinkcode.logisticsconnect.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Listens for delay changes and simulates a public alert when a hub enters a high-delay stage. */
public class ActiveMqAlertSubscriber implements AutoCloseable {
    public static final int ALERT_THRESHOLD = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMqAlertSubscriber.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Connection connection;
    private final Session session;
    private final MessageConsumer consumer;
    private final ConcurrentMap<String, Integer> currentStages = new ConcurrentHashMap<>();

    public ActiveMqAlertSubscriber() {
        this(MqConfig.BROKER_URL);
    }

    public ActiveMqAlertSubscriber(String brokerUrl) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(session.createTopic(MqConfig.TOPIC));
            consumer.setMessageListener(this::handleMessage);
            connection.start();
        } catch (JMSException e) {
            throw new IllegalStateException("Could not subscribe to ActiveMQ at " + brokerUrl, e);
        }
    }

    private void handleMessage(Message message) {
        if (!(message instanceof TextMessage textMessage)) {
            LOGGER.warn("Ignoring non-text message on {}", MqConfig.TOPIC);
            return;
        }

        try {
            StageChangedEvent event = JSON.readValue(textMessage.getText(), StageChangedEvent.class);
            if (event.getHubId() == null || event.getHubId().isBlank()
                    || event.getStage() < 0 || event.getStage() > 8) {
                LOGGER.warn("Ignoring invalid stage update on {}", MqConfig.TOPIC);
                return;
            }

            currentStages.compute(event.getHubId(), (hubId, previousStage) -> {
                int previous = previousStage == null ? 0 : previousStage;
                int current = event.getStage();
                if (previous < ALERT_THRESHOLD && current >= ALERT_THRESHOLD) {
                    LOGGER.warn("SIMULATED ALERT: Hub {} entered delay stage {} (threshold {}). "
                                    + "A public transit notification would be posted.",
                            hubId, current, ALERT_THRESHOLD);
                } else {
                    LOGGER.info("Received delay stage update for hub {}: stage {}", hubId, current);
                }
                return current;
            });
        } catch (Exception e) {
            LOGGER.warn("Ignoring invalid stage update on {}", MqConfig.TOPIC, e);
        }
    }

    @Override
    public void close() {
        try {
            consumer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new IllegalStateException("Could not close ActiveMQ alert subscriber", e);
        }
    }
}
