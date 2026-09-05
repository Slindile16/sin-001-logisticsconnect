package co.wethinkcode.logisticsconnect;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CsvReader implements DataReader {

    private static final String HUBS_FILE = "hubs-global.csv";
    private static final int EXPECTED_COLUMNS = 4;
    private static final Set<String> TRUE_VALUES = Set.of("true", "yes", "y", "1");
    private static final Set<String> FALSE_VALUES = Set.of(
            "false", "no", "n", "0", "", "n/a", "tbd", "unknown", "-", "nan");

    @Override
    public List<Hub> readHubs() {
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

                hubs.add(new Hub(row[0], row[1], row[2], parseActive(row[3])));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + HUBS_FILE, e);
        }

        return hubs;
    }

    private boolean parseActive(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(normalized)) {
            return true;
        }
        if (FALSE_VALUES.contains(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Unsupported active value: " + value);
    }
}
