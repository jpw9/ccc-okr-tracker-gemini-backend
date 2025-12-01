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

// ADDED IMPORTS for Apache Commons CSV
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    // Primary date format for internal consistency (ISO standard)
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Secondary, more flexible format to handle common user input (US standard)
    private static final DateTimeFormatter US_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    // Define the expected CSV headers in order
    private static final String[] CSV_HEADERS = {
            "Project Title", "Project Description",
            "Initiative Title", "Initiative Description",
            "Goal Title", "Goal Description",
            "Objective Title", "Objective Description", "Objective Assignee", "Objective Year", "Objective Quarter", "Objective Due Date",
            "KR Title", "KR Description", "KR Assignee", "KR Metric Start", "KR Metric Target", "KR Metric Current", "KR Unit",
            "Action Item Title", "Action Item Description", "Action Item Assignee", "Action Item Due Date", "Action Item Is Completed"
    };

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
            // IMPROVEMENT: Provide a more informative error for the user
            String errorMessage = e.getMessage() != null && e.getMessage().contains("expected 24")
                    ? "Import failed. The CSV file must contain exactly 24 columns in the correct order, starting with the header row. Check for missing columns or extra delimiters in the file."
                    : "Error during import: " + e.getMessage();
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Robust CSV parser using Apache Commons CSV library.
     * Assumes the first row contains headers defined in CSV_HEADERS.
     */
    private List<HierarchyImportRow> parseCsv(MultipartFile file) throws Exception {
        List<HierarchyImportRow> importRows = new ArrayList<>();

        // Use UTF-8 for reading the file content
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withHeader(CSV_HEADERS)
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .withSkipHeaderRecord() // Skip the first line after reading it as headers
                     .withAllowMissingColumnNames()
                     .withNullString("") // Treat empty strings as null
                     .withIgnoreEmptyLines()
             )) {

            for (CSVRecord csvRecord : csvParser) {
                // Ensure all expected columns are present. Commons CSV ensures 24 fields if headers were read correctly.
                if (csvRecord.size() < CSV_HEADERS.length) {
                    System.err.println("Skipping row due to insufficient columns: " + csvRecord.toString());
                    continue;
                }

                HierarchyImportRow row = new HierarchyImportRow();
                try {
                    // Project
                    row.setProjectTitle(csvRecord.get("Project Title"));
                    row.setProjectDescription(csvRecord.get("Project Description"));

                    // Strategic Initiative
                    row.setInitiativeTitle(csvRecord.get("Initiative Title"));
                    row.setInitiativeDescription(csvRecord.get("Initiative Description"));

                    // Goal
                    row.setGoalTitle(csvRecord.get("Goal Title"));
                    row.setGoalDescription(csvRecord.get("Goal Description"));

                    // Objective
                    row.setObjectiveTitle(csvRecord.get("Objective Title"));
                    row.setObjectiveDescription(csvRecord.get("Objective Description"));
                    row.setObjectiveAssignee(csvRecord.get("Objective Assignee"));
                    row.setObjectiveYear(parseInteger(csvRecord.get("Objective Year")));
                    row.setObjectiveQuarter(csvRecord.get("Objective Quarter"));
                    row.setObjectiveDueDate(parseDate(csvRecord.get("Objective Due Date")));

                    // Key Result
                    row.setKrTitle(csvRecord.get("KR Title"));
                    row.setKrDescription(csvRecord.get("KR Description"));
                    row.setKrAssignee(csvRecord.get("KR Assignee"));
                    row.setKrMetricStart(parseDouble(csvRecord.get("KR Metric Start")));
                    row.setKrMetricTarget(parseDouble(csvRecord.get("KR Metric Target")));
                    row.setKrMetricCurrent(parseDouble(csvRecord.get("KR Metric Current")));
                    row.setKrUnit(csvRecord.get("KR Unit"));

                    // Action Item
                    row.setActionItemTitle(csvRecord.get("Action Item Title"));
                    row.setActionItemDescription(csvRecord.get("Action Item Description"));
                    row.setActionItemAssignee(csvRecord.get("Action Item Assignee"));
                    row.setActionItemDueDate(parseDate(csvRecord.get("Action Item Due Date")));
                    // NOTE: Commons CSV handles "NULL" strings, but we still need to derive boolean
                    row.setActionItemIsCompleted(parseBoolean(csvRecord.get("Action Item Is Completed")));

                    // Only add if at least a Project Title is defined
                    if (row.getProjectTitle() != null && !row.getProjectTitle().trim().isEmpty()) {
                        importRows.add(row);
                    }
                } catch (IllegalArgumentException e) {
                    // This catches errors like missing headers not caught by the parser setup.
                    System.err.println("Error processing row: " + csvRecord.toString() + ". Error: " + e.getMessage());
                } catch (DateTimeParseException e) {
                    // Log error and continue to next row
                    System.err.println("Skipping row due to parsing error: " + csvRecord.toString() + ". Error: " + e.getMessage());
                }
            }
        }
        return importRows;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            // FIX: Gracefully handle non-numeric input by returning null
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse integer value: " + value);
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            // FIX: Gracefully handle non-numeric input by returning null
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
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