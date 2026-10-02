package co.wethinkcode.logisticsconnect.mq;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StageCache {
    private final ConcurrentMap<String, Integer> stages = new ConcurrentHashMap<>();

    public void update(String hubId, int stage) {
        if (hubId == null || hubId.isBlank() || stage < 0 || stage > 8) {
            throw new IllegalArgumentException("Stage event must contain a hub ID and a stage from 0 to 8");
        }
        stages.put(normalizeHubId(hubId), stage);
    }

    public int getStage(String hubId) {
        return stages.getOrDefault(normalizeHubId(hubId), 0);
    }

    private String normalizeHubId(String hubId) {
        return hubId == null ? "" : hubId.trim().toUpperCase(Locale.ROOT);
    }
}
