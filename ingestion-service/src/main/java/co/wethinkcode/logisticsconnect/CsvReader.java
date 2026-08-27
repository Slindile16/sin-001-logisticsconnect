package co.wethinkcode.logisticsconnect;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CsvReader {

    private static final String HUBS_FILE = "hubs-global.csv";
    private static final int EXPECTED_COLUMNS = 4;
    private static final Set<String> TRUE_VALUES = Set.of("true", "yes", "y", "1");
    private static final Set<String> FALSE_VALUES = Set.of("false", "no", "n", "0", "");
    private static final Set<String> PLACEHOLDERS = Set.of("", "n/a", "tbd", "unknown", "-", "nan");

    public static List<Hub> readHubs() {
        InputStream input = CsvReader.class.getClassLoader().getResourceAsStream(HUBS_FILE);
        if (input == null) {
            throw new IllegalStateException("Could not find " + HUBS_FILE + " on the classpath");
        }

        List<Hub> hubs = new ArrayList<>();

        try (com.opencsv.CSVReader reader = new com.opencsv.CSVReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.readNext();

            String[] row;
            int lineNumber = 1;
            while ((row = reader.readNext()) != null) {
                lineNumber++;
                if (row.length != EXPECTED_COLUMNS) {
                    throw new IllegalArgumentException("Invalid column count on CSV line " + lineNumber);
                }

                String hubId = cleanText(row[0]).toUpperCase(Locale.ROOT);
                String province = normalizeProvince(row[1]);
                String sortingCenter = normalizeName(row[2]);

                if (hubId.isEmpty() || sortingCenter.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Hub ID and sorting center are required on CSV line " + lineNumber);
                }

                hubs.add(new Hub(hubId, province, sortingCenter, normalizeActive(row[3])));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + HUBS_FILE, e);
        }

        fillMissingProvinces(hubs);
        return removeDuplicates(hubs);
    }

    private static String normalizeProvince(String value) {
        String province = normalizeName(value);
        return switch (province) {
            case "Kwa-Zulu Natal", "Kwazulu Natal", "Kwazulu-Natal" -> "KwaZulu-Natal";
            default -> province;
        };
    }

    private static String normalizeName(String value) {
        String cleaned = cleanText(value).toLowerCase(Locale.ROOT);
        String[] words = cleaned.split(" ");

        for (int i = 0; i < words.length; i++) {
            words[i] = titleCaseWord(words[i]);
        }

        return String.join(" ", words);
    }

    private static String titleCaseWord(String word) {
        String[] parts = word.split("-");

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                parts[i] = Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1);
            }
        }

        return String.join("-", parts);
    }

    private static boolean normalizeActive(String value) {
        String normalized = cleanText(value).toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(normalized)) {
            return true;
        }
        if (FALSE_VALUES.contains(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Unsupported active value: " + value);
    }

    private static String cleanText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim().replaceAll("\\s+", " ");
        return PLACEHOLDERS.contains(cleaned.toLowerCase(Locale.ROOT)) ? "" : cleaned;
    }

    private static void fillMissingProvinces(List<Hub> hubs) {
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

    private static List<Hub> removeDuplicates(List<Hub> hubs) {
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
