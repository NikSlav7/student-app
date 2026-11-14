package com.example.ResourceServer.pdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MarksParserTest {

    private static List<Double> marks(String semicolonSeparated) {
        return Arrays.stream(semicolonSeparated.split(";")).map(Double::valueOf).toList();
    }

    @ParameterizedTest(name = "integer mode: \"{0}\" -> {1}, avg {2}")
    @CsvSource(delimiter = '|', value = {
            // line                  | expected marks | expected average
            "BIO 9G,5 d 5 4 1        | 5;5;4;1        | 3.75",
            "BIO 9G,5,d,5,4,1        | 5;5;4;1        | 3.75",
            "FÜ 9G,4/5 3             | 4;5;3          | 4.0",
            "EK 9G,5 v + et 4        | 5;4            | 4.5",
            "MU 9G,5 5 1             | 5;5;1          | 3.6667",
    })
    void parsesIntegerMarks(String line, String expectedMarks, double expectedAverage) {
        List<Double> parsed = MarksParser.parseMarks(line, MarksMode.MODE_INTEGER);

        assertThat(parsed).containsExactlyElementsOf(marks(expectedMarks));
        assertThat(MarksParser.average(parsed).getAsDouble()).isCloseTo(expectedAverage, within(0.001));
    }

    @ParameterizedTest(name = "decimal mode: \"{0}\" -> {1}, avg {2}")
    @CsvSource(delimiter = '|', value = {
            "AJA,9,0 8,1 7,3 7,2 8,0 | 9.0;8.1;7.3;7.2;8.0 | 7.92", // Estonian decimal commas
            "MAT,8,5* 9,0            | 8.5;9.0             | 8.75", // '*' after a mark is ignored
            "KK,7,0/8,0              | 7.0;8.0             | 7.5",  // slash separated
            "IK,10 9,5 d             | 10;9.5              | 9.75", // whole number + absence
    })
    void parsesDecimalMarks(String line, String expectedMarks, double expectedAverage) {
        List<Double> parsed = MarksParser.parseMarks(line, MarksMode.MODE_DOUBLE);

        assertThat(parsed).containsExactlyElementsOf(marks(expectedMarks));
        assertThat(MarksParser.average(parsed).getAsDouble()).isCloseTo(expectedAverage, within(0.001));
    }

    @ParameterizedTest(name = "no marks: \"{0}\"")
    @CsvSource(delimiter = '|', value = {
            "MAT 9. KLASS | MODE_INTEGER", // subject row with no marks cell
            "KLT 9G,      | MODE_INTEGER", // empty marks cell
            "BIO 9G,d v + | MODE_INTEGER", // only non-numeric tokens
            "VK,d d       | MODE_DOUBLE",
    })
    @DisplayName("subject without numeric marks has no average (so no NaN is saved)")
    void subjectWithoutMarksHasNoAverage(String line, MarksMode mode) {
        List<Double> parsed = MarksParser.parseMarks(line, mode);

        assertThat(parsed).isEmpty();
        assertThat(MarksParser.average(parsed)).isEmpty();
    }

    @Test
    void averageOfMarks() {

        assertThat(MarksParser.average(List.of(5.0, 4.0, 3.0)).getAsDouble()).isEqualTo(4.0);

        assertThat(MarksParser.average(List.of(9.0, 8.1, 7.3, 7.2, 8.0)).getAsDouble())
                .isCloseTo(7.92, within(0.001));

        assertThat(MarksParser.average(List.of(5.0, 5.0, 1.0)).getAsDouble())
                .isCloseTo(3.667, within(0.001));

        assertThat(MarksParser.average(List.of(5.0)).getAsDouble()).isEqualTo(5.0);

        assertThat(MarksParser.average(List.of())).isEmpty();
    }

    @Test
    void subjectNameIsEverythingBeforeTheFirstComma() {
        assertThat(MarksParser.subjectName("BIO 9G,5 4 3")).isEqualTo("BIO 9G");
        assertThat(MarksParser.subjectName("KK 9G+9V (P),5")).isEqualTo("KK 9G+9V (P)");
    }
}
