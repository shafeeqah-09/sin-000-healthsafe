package co.wethinkcode.healthsafe;

import io.javalin.Javalin;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;



public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        app.get("/wards", ctx -> {
            List<String> rawLines = readCsvLines();
            ctx.json(rawLines);
        });
    }

    /*
    *   Reads the CSV file line by line and returns the raw lines.
    *   Skips the header row
    */

    private static List<String> readCsvLines() {
        List<String> lines = new ArrayList<>();

        try (InputStream is = IngestionServiceApp.class
                .getClassLoader()
                .getResourceAsStream("wards-outdated.csv")) {

            if (is == null) {
                System.out.println("ERROR: wards-outdated.csv not found");
                return lines;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                lines.add(line);
            }

        } catch (Exception e) {
            System.out.println("ERROR reading CSV: " + e.getMessage());
        }

        return lines;
    }
}