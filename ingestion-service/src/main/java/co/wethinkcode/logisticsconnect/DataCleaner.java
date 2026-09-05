package co.wethinkcode.logisticsconnect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DataCleaner {

    private static final Set<String> PLACEHOLDERS = Set.of("", "n/a", "tbd", "unknown", "-", "nan");

    public List<Hub> clean(List<Hub> hubs) {
        List<Hub> cleanedHubs = new ArrayList<>();

        for (Hub hub : hubs) {
            String hubId = cleanText(hub.getHubId()).toUpperCase(Locale.ROOT);
            String sortingCenter = normalizeName(hub.getSortingCenter());

            if (hubId.isEmpty() || sortingCenter.isEmpty()) {
                continue;
            }

            cleanedHubs.add(new Hub(
                    hubId,
                    normalizeProvince(hub.getProvince()),
                    sortingCenter,
                    hub.isActive()
            ));
        }

        fillMissingProvinces(cleanedHubs);
        return removeDuplicates(cleanedHubs);
    }

    private String normalizeProvince(String value) {
        String province = normalizeName(value);
        return switch (province) {
            case "Kwa-Zulu Natal", "Kwazulu Natal", "Kwazulu-Natal" -> "KwaZulu-Natal";
            default -> province;
        };
    }

    private String normalizeName(String value) {
        String[] words = cleanText(value).toLowerCase(Locale.ROOT).split(" ");

        for (int i = 0; i < words.length; i++) {
            words[i] = titleCaseWord(words[i]);
        }

        return String.join(" ", words);
    }

    private String titleCaseWord(String word) {
        String[] parts = word.split("-");

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                parts[i] = Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1);
            }
        }

        return String.join("-", parts);
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim().replaceAll("\\s+", " ");
        return PLACEHOLDERS.contains(cleaned.toLowerCase(Locale.ROOT)) ? "" : cleaned;
    }

    private void fillMissingProvinces(List<Hub> hubs) {
        Map<String, String> provinceByCenter = new LinkedHashMap<>();
        for (Hub hub : hubs) {
            if (!hub.getProvince().isEmpty()) {
                provinceByCenter.putIfAbsent(hub.getSortingCenter(), hub.getProvince());
            }
        }

        for (Hub hub : hubs) {
            if (hub.getProvince().isEmpty()) {
                hub.setProvince(provinceByCenter.getOrDefault(hub.getSortingCenter(), "Unknown"));
            }
        }
    }

    private List<Hub> removeDuplicates(List<Hub> hubs) {
        Map<String, Hub> uniqueHubs = new LinkedHashMap<>();

        for (Hub hub : hubs) {
            String key = hub.getProvince() + "|" + hub.getSortingCenter();
            Hub existing = uniqueHubs.get(key);

            if (existing == null) {
                uniqueHubs.put(key, hub);
            } else if (hub.isActive()) {
                existing.setActive(true);
            }
        }

        return List.copyOf(uniqueHubs.values());

    }
}
