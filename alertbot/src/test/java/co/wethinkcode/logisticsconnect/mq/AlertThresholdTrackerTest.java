package co.wethinkcode.logisticsconnect.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertThresholdTrackerTest {
    private final AlertThresholdTracker tracker = new AlertThresholdTracker();

    @Test
    void alertsOnlyWhenHubEntersThresholdRange() {
        assertFalse(tracker.update("H-500", 4));
        assertTrue(tracker.update("H-500", 5));
        assertFalse(tracker.update("H-500", 7));
    }

    @Test
    void alertsAgainAfterStageFallsBelowThreshold() {
        assertTrue(tracker.update("H-500", 5));
        assertFalse(tracker.update("H-500", 2));
        assertTrue(tracker.update("H-500", 6));
    }

    @Test
    void treatsHubIdsWithDifferentCasingAsTheSameHub() {
        assertTrue(tracker.update("H-500", 5));
        assertFalse(tracker.update(" h-500 ", 6));
    }

    @Test
    void rejectsInvalidHubIdsAndStages() {
        assertThrows(IllegalArgumentException.class, () -> tracker.update(" ", 5));
        assertThrows(IllegalArgumentException.class, () -> tracker.update("H-500", 9));
    }
}
