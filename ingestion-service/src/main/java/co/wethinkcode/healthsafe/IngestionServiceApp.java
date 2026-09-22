package co.wethinkcode.healthsafe;

import io.javalin.Javalin;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.


        app.get("/wards", ctx -> {
            List<Ward> wards = readAndCleanCsv();
            ctx.json(wards);
        });
    }

    /*
    *   Reads the CSV file line by line and returns the raw lines.
    *   Skips the header row
    */

    private static List<Ward> readAndCleanCsv() {
        List<Ward> wards = new ArrayList<>();
        Map<String, Ward> seenWards = new HashMap<>();

        try (InputStream is = IngestionServiceApp.class
                .getClassLoader()
                .getResourceAsStream("wards-outdated.csv")) {

            if (is == null) {
                System.out.println("ERROR: wards-outdated.csv not found");
                return wards;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                Ward ward = cleanRow(line);
                if (ward != null) {
                    String key = ward.getWardId().toUpperCase();
                    if (seenWards.containsKey(key)) {
                        System.out.println("Duplicate detected: " + ward.getWardId());
                        continue;
                    }
                    seenWards.put(key, ward);
                    wards.add(ward);
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR reading CSV: " + e.getMessage());
        }

        return wards;
    }

    private static Ward cleanRow(String rawLine) {
        String[] parts = rawLine.split(",");
        if (parts.length < 4) {
            System.out.println("Skipping malformed row: " + rawLine);
            return null;
        }

        String wardId = cleanWardId(parts[0]);
        String wing = cleanWing(parts[1]);
        String department = cleanDepartment(parts[2]);
        String bedsRaw = cleanText(parts[3]);

        Integer beds = parseBeds(bedsRaw);
        String notes = null;

        if (beds == null && !bedsRaw.isEmpty()) {
            notes = "bedsAvailable was non-numeric ('" + bedsRaw + "') — flagged for follow-up";
        }

        return new Ward(wardId, wing, department, beds, notes);
    }

    private static String cleanText(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    private static String cleanWardId(String input) {
        return cleanText(input).toUpperCase();
    }

    private static String cleanWing(String input) {
        String cleaned = cleanText(input);
        if (cleaned.isEmpty()) return "Unknown Wing";
        return titleCase(cleaned);
    }

    private static String cleanDepartment(String input) {
        String cleaned = cleanText(input);
        if (cleaned.isEmpty()) return "Unknown";
        return titleCase(cleaned);
    }

    private static String titleCase(String input) {
        String[] words = input.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static Integer parseBeds(String input) {
        if (input == null || input.isEmpty()) return null;

        String lower = input.toLowerCase();
        if (lower.equals("n/a") || lower.equals("tbd") || lower.equals("unknown")
                || lower.equals("-") || lower.equals("nan") || lower.equals("full")) {
            return null;
        }

        try {
            int value = Integer.parseInt(input);
            if (value < 0 || value > 1000) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
