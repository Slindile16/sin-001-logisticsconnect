package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubTest {

    @Test
    void constructorStoresAllHubFields() {
        Hub hub = new Hub("H-500", "Gauteng", "Johannesburg Central", true);

        assertAll(
                () -> assertEquals("H-500", hub.getHubId()),
                () -> assertEquals("Gauteng", hub.getProvince()),
                () -> assertEquals("Johannesburg Central", hub.getSortingCenter()),
                () -> assertTrue(hub.isActive())
        );
    }

    @Test
    void noArgumentConstructorCreatesEmptyHubThatCanBePopulated() {
        Hub hub = new Hub();
        hub.setHubId("H-501");
        hub.setProvince("Western Cape");
        hub.setSortingCenter("Cape Town Port");
        hub.setActive(false);

        assertAll(
                () -> assertEquals("H-501", hub.getHubId()),
                () -> assertEquals("Western Cape", hub.getProvince()),
                () -> assertEquals("Cape Town Port", hub.getSortingCenter()),
                () -> assertFalse(hub.isActive())
        );
    }
}
