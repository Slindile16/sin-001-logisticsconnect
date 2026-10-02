package co.wethinkcode.logisticsconnect.mq;

public class StageChangedEvent {
    private String hubId;
    private int stage;
    private String timestamp;

    public StageChangedEvent() {
    }

    public StageChangedEvent(String hubId, int stage, String timestamp) {
        this.hubId = hubId;
        this.stage = stage;
        this.timestamp = timestamp;
    }

    public String getHubId() { return hubId; }
    public void setHubId(String hubId) { this.hubId = hubId; }
    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
