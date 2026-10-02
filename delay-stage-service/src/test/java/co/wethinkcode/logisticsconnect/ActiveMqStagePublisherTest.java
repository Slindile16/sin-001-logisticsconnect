package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.ActiveMqStagePublisher;
import co.wethinkcode.logisticsconnect.mq.MqConfig;
import co.wethinkcode.logisticsconnect.mq.StageChangedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.Test;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActiveMqStagePublisherTest {
    @Test
    void publishesStageChangeJsonToConfiguredTopic() throws Exception {
        String brokerName = "publisher-test-" + System.nanoTime();
        BrokerService broker = startEmbeddedBroker(brokerName);
        String brokerUrl = "vm://" + brokerName + "?create=false";
        ActiveMQConnectionFactory consumerFactory = new ActiveMQConnectionFactory(brokerUrl);
        Connection consumerConnection = consumerFactory.createConnection();
        Session consumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = consumerSession.createConsumer(consumerSession.createTopic(MqConfig.TOPIC));
        consumerConnection.start();

        try (ActiveMqStagePublisher publisher = new ActiveMqStagePublisher(brokerUrl)) {
            publisher.publish(new StageChangedEvent("H-500", 4, "2026-10-02T10:15:00Z"));

            TextMessage message = (TextMessage) consumer.receive(3000);
            assertNotNull(message, "Expected a stage message on the topic");
            JsonNode payload = new ObjectMapper().readTree(message.getText());
            assertEquals("H-500", payload.get("hubId").asText());
            assertEquals(4, payload.get("stage").asInt());
            assertEquals("2026-10-02T10:15:00Z", payload.get("timestamp").asText());
        } finally {
            consumer.close();
            consumerSession.close();
            consumerConnection.close();
            broker.stop();
            broker.waitUntilStopped();
        }
    }

    private BrokerService startEmbeddedBroker(String brokerName) throws Exception {
        BrokerService broker = new BrokerService();
        broker.setBrokerName(brokerName);
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();
        broker.waitUntilStarted();
        return broker;
    }
}
