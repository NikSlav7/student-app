package com.example.ResourceServer.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Pure parsing logic for one marks line in the format produced by
 * {@link MarksManager#extractMarks}: {@code "<subject name>,<marks cell text>"}.
 *
 * Extracted from MarksManager so it can be unit tested without Spring or a database.
 * The behaviour is identical to the original inline loops.
 */
public final class MarksParser {

    private MarksParser() {
    }

    public static String subjectName(String line) {
        return line.split(",")[0];
    }

    /** Returns every numeric mark on the line. Non-numeric tokens (d, v, +, et, ...) are skipped. */
    public static List<Double> parseMarks(String line, MarksMode mode) {
        return mode == MarksMode.MODE_DOUBLE ? parseDecimalMarks(line) : parseIntegerMarks(line);
    }

    /** Average of the marks, or empty if there are none (so no NaN is ever saved). */
    public static OptionalDouble average(List<Double> marks) {
        return marks.stream().mapToDouble(Double::doubleValue).average();
    }

    // Integer mode: marks separated by commas, slashes or whitespace, e.g. "BIO 9G,5 d 5 4"
    private static List<Double> parseIntegerMarks(String line) {
        String rest = line.replace(subjectName(line), "");
        String[] tokens = rest.split(",|/|\\s+");
        List<Double> marks = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].isBlank()) continue;
            try {
                marks.add(Double.parseDouble(tokens[i]));
            } catch (NumberFormatException ignored) {
                // not a mark (absence, comment, ...)
            }
        }
        return marks;
    }

    // Decimal mode: Estonian decimal commas, marks separated by whitespace or slashes, e.g. "AJA,9,0 8,1 7,3"
    private static List<Double> parseDecimalMarks(String line) {
        String rest = line.replace(subjectName(line), " ");
        List<Double> marks = new ArrayList<>();
        for (String part : rest.split(" ,")) {
            for (String token : part.split("/|\\s+")) {
                token = token.replace(',', '.').replace('*', (char) 0);
                if (token.isBlank()) continue;
                try {
                    marks.add(Double.parseDouble(token));
                } catch (NumberFormatException ignored) {
                    // not a mark
                }
            }
        }
        return marks;
    }
}
