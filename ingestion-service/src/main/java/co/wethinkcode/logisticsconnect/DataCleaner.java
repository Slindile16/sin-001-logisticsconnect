package co.wethinkcode.logisticsconnect;

import java.util.List;
import java.util.Locale;

public class DataCleaner {

    // Cleans hub IDs so they all use uppercase letters without extra spaces.
    public List<Hub> clean(List<Hub> hubs) {
        return hubs.stream()
                .map(hub -> new Hub(
                        hub.getHubId().trim().toUpperCase(Locale.ROOT),
                        hub.getProvince(),
                        hub.getSortingCenter(),
                        hub.isActive()
                ))
                .toList();
    }
}
