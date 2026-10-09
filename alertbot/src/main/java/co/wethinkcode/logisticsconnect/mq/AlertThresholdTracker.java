package co.wethinkcode.logisticsconnect.mq;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Tracks each hub's latest stage and detects entry into the alert range. */
class AlertThresholdTracker {
    private final Map<String, Integer> currentStages = new HashMap<>();

    synchronized boolean update(String hubId, int stage) {
        if (hubId == null || hubId.isBlank() || stage < 0 || stage > 8) {
            throw new IllegalArgumentException("Stage update must contain a hub ID and a stage from 0 to 8");
        }

        String key = hubId.trim().toUpperCase(Locale.ROOT);
        int previousStage = currentStages.getOrDefault(key, 0);
        currentStages.put(key, stage);
        return previousStage < ActiveMqAlertSubscriber.ALERT_THRESHOLD
                && stage >= ActiveMqAlertSubscriber.ALERT_THRESHOLD;
    }
}
