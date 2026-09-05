package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataCleanerTest {

    @Test
    void cleansHubIdCasingAndWhitespace() {
        Hub hub = new Hub(" h-501 ", "Western Cape", "Cape Town Port", true);

        List<Hub> cleanedHubs = new DataCleaner().clean(List.of(hub));

        assertEquals("H-501", cleanedHubs.get(0).getHubId());
    }

    @Test
    void cleansProvinceCasingAndWhitespace() {
        Hub hub = new Hub("H-501", " eastern  cape ", "Port Elizabeth Hub", true);

        Hub cleanedHub = new DataCleaner().clean(List.of(hub)).get(0);

        assertEquals("Eastern Cape", cleanedHub.getProvince());
    }

    @Test
    void cleansSortingCenterCasingAndWhitespace() {
        Hub hub = new Hub("H-501", "Western Cape", " cape town  port ", true);

        Hub cleanedHub = new DataCleaner().clean(List.of(hub)).get(0);

        assertEquals("Cape Town Port", cleanedHub.getSortingCenter());
    }

    @Test
    void standardizesProvinceSpellingVariants() {
        Hub firstHub = new Hub("H-501", "Kwa-Zulu Natal", "Durban Harbour", true);
        Hub secondHub = new Hub("H-502", "KwaZulu Natal", "Pinetown Hub", true);

        List<Hub> cleanedHubs = new DataCleaner().clean(List.of(firstHub, secondHub));

        assertAll(
                () -> assertEquals("KwaZulu-Natal", cleanedHubs.get(0).getProvince()),
                () -> assertEquals("KwaZulu-Natal", cleanedHubs.get(1).getProvince())
        );
    }

    @Test
    void fillsMissingProvinceFromMatchingSortingCenter() {
        Hub knownHub = new Hub("H-501", "Gauteng", "Pretoria North", true);
        Hub missingProvinceHub = new Hub("H-502", "", "Pretoria North", false);

        List<Hub> cleanedHubs = new DataCleaner().clean(List.of(knownHub, missingProvinceHub));

        assertEquals("Gauteng", cleanedHubs.get(0).getProvince());
    }

    @Test
    void usesUnknownWhenMissingProvinceCannotBeInferred() {
        Hub hub = new Hub("H-501", "", "Unmatched Center", true);

        Hub cleanedHub = new DataCleaner().clean(List.of(hub)).get(0);

        assertEquals("Unknown", cleanedHub.getProvince());
    }

    @Test
    void removesHubsWithoutRequiredFields() {
        Hub missingId = new Hub("", "Gauteng", "Pretoria North", true);
        Hub missingCenter = new Hub("H-502", "Gauteng", "", true);

        List<Hub> cleanedHubs = new DataCleaner().clean(List.of(missingId, missingCenter));

        assertTrue(cleanedHubs.isEmpty());
    }

    @Test
    void treatsPlaceholderValuesAsMissing() {
        Hub missingId = new Hub("N/A", "Gauteng", "Pretoria North", true);
        Hub missingCenter = new Hub("H-502", "Gauteng", "unknown", true);
        Hub missingProvince = new Hub("H-503", "TBD", "Polokwane Hub", true);

        List<Hub> cleanedHubs = new DataCleaner().clean(List.of(
                missingId, missingCenter, missingProvince));

        assertAll(
                () -> assertEquals(1, cleanedHubs.size()),
                () -> assertEquals("Unknown", cleanedHubs.get(0).getProvince())
        );
    }

    @Test
    void removesDuplicateLocations() {
        Hub firstHub = new Hub("H-500", "Gauteng", "Johannesburg Central", false);
        Hub duplicateHub = new Hub("H-504", "gauteng", "johannesburg central", false);

        List<Hub> cleanedHubs = new DataCleaner().clean(List.of(firstHub, duplicateHub));

        assertAll(
                () -> assertEquals(1, cleanedHubs.size()),
                () -> assertEquals("H-500", cleanedHubs.get(0).getHubId())
        );
    }

    @Test
    void marksDuplicateLocationActiveWhenAnyRecordIsActive() {
        Hub inactiveHub = new Hub("H-500", "Gauteng", "Johannesburg Central", false);
        Hub activeHub = new Hub("H-504", "Gauteng", "Johannesburg Central", true);

        Hub cleanedHub = new DataCleaner().clean(List.of(inactiveHub, activeHub)).get(0);

        assertTrue(cleanedHub.isActive());
    }

    @Test
    void doesNotChangeOriginalHub() {
        Hub originalHub = new Hub(" h-501 ", " western cape ", " cape town port ", false);

        Hub cleanedHub = new DataCleaner().clean(List.of(originalHub)).get(0);

        assertAll(
                () -> assertNotSame(originalHub, cleanedHub),
                () -> assertEquals(" h-501 ", originalHub.getHubId()),
                () -> assertEquals(" western cape ", originalHub.getProvince()),
                () -> assertEquals(" cape town port ", originalHub.getSortingCenter()),
                () -> assertFalse(originalHub.isActive())
        );
    }
}
