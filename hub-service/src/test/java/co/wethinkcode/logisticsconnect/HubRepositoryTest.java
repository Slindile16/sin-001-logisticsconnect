package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubRepositoryTest {
    private final Hub first = new Hub("H-500", "Gauteng", "Johannesburg Central", true);
    private final Hub second = new Hub("H-501", "Western Cape", "Cape Town Port", false);

    @Test
    void returnsAllHubsInTheirOriginalOrder() {
        HubRepository repository = new HubRepository(List.of(first, second));

        assertEquals(List.of(first, second), repository.findAll());
    }

    @Test
    void findsHubByIdIgnoringCaseAndSurroundingWhitespace() {
        HubRepository repository = new HubRepository(List.of(first, second));

        assertSame(first, repository.findById("  h-500 ").orElseThrow());
    }

    @Test
    void returnsEmptyWhenHubDoesNotExist() {
        HubRepository repository = new HubRepository(List.of(first));

        assertTrue(repository.findById("H-999").isEmpty());
    }

    @Test
    void returnsDistinctProvinceAndSortingCenterNamesInEncounterOrder() {
        Hub repeatedLocation = new Hub("H-502", "gauteng", "johannesburg central", false);
        HubRepository repository = new HubRepository(List.of(first, second, repeatedLocation));

        assertEquals(List.of("Gauteng", "Western Cape"), repository.findProvinces());
        assertEquals(List.of("Johannesburg Central", "Cape Town Port"), repository.findSortingCenters());
    }
}
