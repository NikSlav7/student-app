package com.example.ResourceServer.pdf;

import com.example.ResourceServer.TestVariables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SampleSheetTest {

    private static final Map<String, Double> averages = new LinkedHashMap<>();

    @BeforeAll
    static void parseTheWholeSheet() {
        String marksSection = TestVariables.DOTTED_AJA
                .split("Aasta/Kursus Periood Kursuse hinded\n")[1]
                .split("Selgitus")[0];

        for (String line : marksSection.split("\n")) {
            if (line.isBlank()) continue;
            List<Double> marks = MarksParser.parseMarks(line, MarksMode.MODE_INTEGER);
            OptionalDouble average = MarksParser.average(marks);
            if (average.isPresent()) averages.put(MarksParser.subjectName(line).trim(), average.getAsDouble());
        }
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "AJA          | 7.92",   // decimal marks with dots: (9.0 + 8.1 + 7.3 + 7.2 + 8.0) / 5
            "BIO 9G       | 4.444",  // 40 / 9
            "EK 9G        | 4.133",  // 62 / 15, absences (d) and 'v' skipped
            "FÜ 9G        | 4.667",  // 28 / 6, slash separated
            "GG 9G        | 4.875",  // 39 / 8
            "IK 9G+9V     | 4.5",    // 45 / 10, '+' inside the subject name
            "KEE 9G       | 5.0",    // '+' as a mark is skipped
            "KIR 9G       | 5.0",
            "KK 9G+9V (P) | 4.3",    // 43 / 10, brackets in the subject name
            "KU 9G        | 4.417",  // 53 / 12
            "MAjET 9. KLASS | 5.0",  // the '9.' in the name is not a mark
            "MAT 9. KLASS | 5.0",
            "MU 9G        | 3.286",  // 23 / 7
            "ÜÕ 9G        | 5.0",    // non-ASCII letters in the name
            "VK 9G        | 4.571",
    })
    void eachSubjectHasTheRightAverage(String subject, double expectedAverage) {
        assertThat(averages).containsKey(subject);
        assertThat(averages.get(subject)).isCloseTo(expectedAverage, within(0.001));
    }

    @Test
    void subjectsWithoutMarksAreSkipped() {
        // "KLT 9G" has no marks; the second "MAT 9. KLASS" line is empty and must not overwrite the first
        assertThat(averages).doesNotContainKey("KLT 9G");
        assertThat(averages.get("MAT 9. KLASS")).isEqualTo(5.0);
    }

    @Test
    void findsEverySubjectThatHasMarks() {
        assertThat(averages).hasSize(15);
    }
}
