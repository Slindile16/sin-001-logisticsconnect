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

public class ActiveMqStageSubscriber implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMqStageSubscriber.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Connection connection;
    private final Session session;
    private final MessageConsumer consumer;

    public ActiveMqStageSubscriber(StageCache cache) {
        this(cache, MqConfig.BROKER_URL);
    }

    public ActiveMqStageSubscriber(StageCache cache, String brokerUrl) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(session.createTopic(MqConfig.TOPIC));
            consumer.setMessageListener(message -> handleMessage(message, cache));
            connection.start();
        } catch (JMSException e) {
            throw new IllegalStateException("Could not subscribe to ActiveMQ at " + brokerUrl, e);
        }
    }

    private void handleMessage(Message message, StageCache cache) {
        if (!(message instanceof TextMessage textMessage)) {
            LOGGER.warn("Ignoring non-text message on {}", MqConfig.TOPIC);
            return;
        }
        try {
            StageChangedEvent event = JSON.readValue(textMessage.getText(), StageChangedEvent.class);
            cache.update(event.getHubId(), event.getStage());
            LOGGER.info("Received delay stage update for hub {}: stage {}", event.getHubId(), event.getStage());
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
            throw new IllegalStateException("Could not close ActiveMQ stage subscriber", e);
        }
    }
}
