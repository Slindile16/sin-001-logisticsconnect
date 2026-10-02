package co.wethinkcode.logisticsconnect.mq;

public interface StagePublisher {
    void publish(StageChangedEvent event);
}
