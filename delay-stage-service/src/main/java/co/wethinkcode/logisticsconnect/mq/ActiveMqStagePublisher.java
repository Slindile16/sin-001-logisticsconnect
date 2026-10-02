package co.wethinkcode.logisticsconnect.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class ActiveMqStagePublisher implements StagePublisher, AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public ActiveMqStagePublisher() {
        this(MqConfig.BROKER_URL);
    }

    public ActiveMqStagePublisher(String brokerUrl) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(session.createTopic(MqConfig.TOPIC));
            connection.start();
        } catch (JMSException e) {
            throw new IllegalStateException("Could not connect to ActiveMQ at " + brokerUrl, e);
        }
    }

    @Override
    public synchronized void publish(StageChangedEvent event) {
        try {
            TextMessage message = session.createTextMessage(JSON.writeValueAsString(event));
            producer.send(message);
        } catch (JMSException | JsonProcessingException e) {
            throw new IllegalStateException("Could not publish stage update to " + MqConfig.TOPIC, e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new IllegalStateException("Could not close ActiveMQ stage publisher", e);
        }
    }
}
