package com.ccc.okrtracker.controller;

import com.ccc.okrtracker.dto.HierarchyImportRow;
import com.ccc.okrtracker.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    // Primary date format for internal consistency (ISO standard)
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Secondary, more flexible format to handle common user input (US standard)
    private static final DateTimeFormatter US_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    @PostMapping("/hierarchy")
    public ResponseEntity<String> importHierarchy(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a CSV file to upload.");
        }

        try {
            List<HierarchyImportRow> rows = parseCsv(file);
            importService.importHierarchy(rows);
            return ResponseEntity.ok("Hierarchy imported successfully. Total records processed: " + rows.size());
        } catch (Exception e) {
            // Log the detailed exception
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during import: " + e.getMessage());
        }
    }

    /**
     * A basic, non-robust CSV parser for demonstration purposes.
     * Assumes specific column order and comma delimiter.
     * NOTE: This should be replaced with a robust library (e.g., Apache Commons CSV) in production.
     */
    private List<HierarchyImportRow> parseCsv(MultipartFile file) throws Exception {
        List<HierarchyImportRow> importRows = new ArrayList<>();
        // Using ISO-8859-1 as a general fallback, but UTF-8 is often better if used consistently
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            // Read header (and discard)
            String headerLine = reader.readLine();
            if (headerLine == null) return importRows;

            // Define the expected CSV headers for mapping (order matters here!)
            String[] headers = headerLine.split(",");

            // Expected headers:
            // 0: projectTitle, 1: projectDescription, 2: initiativeTitle, 3: initiativeDescription,
            // 4: goalTitle, 5: goalDescription, 6: objectiveTitle, 7: objectiveDescription,
            // 8: objectiveAssignee, 9: objectiveYear, 10: objectiveQuarter, 11: objectiveDueDate,
            // 12: krTitle, 13: krDescription, 14: krAssignee, 15: krMetricStart,
            // 16: krMetricTarget, 17: krMetricCurrent, 18: krUnit, 19: actionItemTitle,
            // 20: actionItemDescription, 21: actionItemAssignee, 22: actionItemDueDate, 23: actionItemIsCompleted

            String line;
            while ((line = reader.readLine()) != null) {
                // Split line by comma, this is very basic and won't handle embedded commas/quotes well.
                String[] values = line.split(",", -1);
                if (values.length < 24) continue; // Skip incomplete rows

                HierarchyImportRow row = new HierarchyImportRow();
                try {
                    int i = 0;
                    // Project
                    row.setProjectTitle(values[i++].trim());
                    row.setProjectDescription(values[i++].trim());

                    // Strategic Initiative
                    row.setInitiativeTitle(values[i++].trim());
                    row.setInitiativeDescription(values[i++].trim());

                    // Goal
                    row.setGoalTitle(values[i++].trim());
                    row.setGoalDescription(values[i++].trim());

                    // Objective
                    row.setObjectiveTitle(values[i++].trim());
                    row.setObjectiveDescription(values[i++].trim());
                    row.setObjectiveAssignee(values[i++].trim());
                    row.setObjectiveYear(parseInteger(values[i++]));
                    row.setObjectiveQuarter(values[i++].trim());
                    row.setObjectiveDueDate(parseDate(values[i++]));

                    // Key Result
                    row.setKrTitle(values[i++].trim());
                    row.setKrDescription(values[i++].trim());
                    row.setKrAssignee(values[i++].trim());
                    row.setKrMetricStart(parseDouble(values[i++]));
                    row.setKrMetricTarget(parseDouble(values[i++]));
                    row.setKrMetricCurrent(parseDouble(values[i++]));
                    row.setKrUnit(values[i++].trim());

                    // Action Item
                    row.setActionItemTitle(values[i++].trim());
                    row.setActionItemDescription(values[i++].trim());
                    row.setActionItemAssignee(values[i++].trim());
                    row.setActionItemDueDate(parseDate(values[i++]));
                    row.setActionItemIsCompleted(parseBoolean(values[i++]));

                    // Only add if at least a Project is defined
                    if (!row.getProjectTitle().isEmpty()) {
                        importRows.add(row);
                    }
                } catch (NumberFormatException | DateTimeParseException e) {
                    // Log error and continue to next row
                    System.err.println("Skipping row due to parsing error: " + line + ". Error: " + e.getMessage());
                }
            }
        }
        return importRows;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // FIX: Gracefully handle non-numeric input by returning null
            System.err.println("Failed to parse integer value: " + value);
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            // FIX: Added try-catch for NumberFormatException
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            // FIX: Gracefully handle non-numeric input by returning null
            System.err.println("Failed to parse double value: " + value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        String trimmedValue = value.trim();

        // 1. Try ISO Format (YYYY-MM-DD)
        try {
            return LocalDate.parse(trimmedValue, ISO_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Ignore and try next format
        }

        // 2. Try US Format (M/d/yyyy)
        try {
            return LocalDate.parse(trimmedValue, US_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Throw the exception if neither works, as date parsing failed.
            throw e;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String lower = value.trim().toLowerCase();
        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
    }
}