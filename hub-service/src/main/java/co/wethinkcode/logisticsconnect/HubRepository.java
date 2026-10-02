package co.wethinkcode.logisticsconnect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class HubRepository {
    private final List<Hub> hubs;

    public HubRepository(List<Hub> hubs) {
        this.hubs = List.copyOf(hubs);
    }

    public List<Hub> findAll() {
        return hubs;
    }

    public Optional<Hub> findById(String hubId) {
        String normalizedId = normalizeId(hubId);
        return hubs.stream()
                .filter(hub -> normalizeId(hub.getHubId()).equals(normalizedId))
                .findFirst();
    }

    public List<String> findProvinces() {
        Map<String, String> distinct = new LinkedHashMap<>();
        for (Hub hub : hubs) {
            distinct.putIfAbsent(normalizeName(hub.getProvince()), hub.getProvince());
        }
        return List.copyOf(distinct.values());
    }

    public List<String> findSortingCenters() {
        Map<String, String> distinct = new LinkedHashMap<>();
        for (Hub hub : hubs) {
            distinct.putIfAbsent(normalizeName(hub.getSortingCenter()), hub.getSortingCenter());
        }
        return List.copyOf(distinct.values());
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
