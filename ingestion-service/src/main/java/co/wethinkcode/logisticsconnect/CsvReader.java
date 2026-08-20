package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;



/*This method reads the package data from the CSV file and converts each row into package information that can be used by the rest of the LogisticsConnect system.

public class CsvReader {

    public static List<Hub> readHubs() {
        List<Hub> hubs = new ArrayList<>();

        try {
            CSVReader reader = new CSVReader(
                    new FileReader("src/main/resources/hubs-global.csv")
            );

            String[] row;


            reader.readNext();

            while ((row = reader.readNext()) != null) {

                String hubId = row[0];
                String province = row[1];
                String sortingCenter = row[2];
                boolean active = Boolean.parseBoolean(row[3]);

                Hub hub = new Hub(
                        hubId,
                        province,
                        sortingCenter,
                        active
                );

                hubs.add(hub);
            }

            reader.close();

        } catch (Exception e) {
            System.out.println("Could not read hubs CSV file.");
        }

        return hubs;
    }
}