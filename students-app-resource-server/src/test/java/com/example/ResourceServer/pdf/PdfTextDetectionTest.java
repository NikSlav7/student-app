package com.example.ResourceServer.pdf;

import com.example.ResourceServer.dictionary.Language;
import com.example.ResourceServer.dictionary.LanguageDetector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextDetectionTest {

    private static final String NL = System.getProperty("line.separator");

    private static String sheet(String... lines) {
        return String.join(NL, lines) + NL;
    }

    @Test
    void extractsPeriodFromEstonianSheet() {
        String text = sheet("Hinneteleht", "Periood: II trimester", "TEST STUDENT");

        assertThat(MarksManager.extractPeriod(text, Language.ESTONIAN)).isEqualTo("II");
    }

    @Test
    void extractsPeriodFromRussianSheet() {
        String text = sheet("Табель", "Период: III четверть", "TEST STUDENT");

        assertThat(MarksManager.extractPeriod(text, Language.RUSSIAN)).isEqualTo("III");
    }

    @Test
    void returnsNullWhenSheetHasNoPeriod() {
        String text = sheet("Hinneteleht", "TEST STUDENT");

        assertThat(MarksManager.extractPeriod(text, Language.ESTONIAN)).isNull();
    }

    @Test
    void extractsPeriodWhenSpaceAfterColonIsMissing() {
        String text = sheet("Hinneteleht", "Periood:II trimester");

        assertThat(MarksManager.extractPeriod(text, Language.ESTONIAN)).isEqualTo("II");
    }

    @Test
    void extractsPeriodWhenLineHasLeadingWhitespace() {
        String text = sheet("Hinneteleht", "   Periood: II trimester");

        assertThat(MarksManager.extractPeriod(text, Language.ESTONIAN)).isEqualTo("II");
    }

    @Test
    void detectsSheetLanguage() {
        assertThat(LanguageDetector.detectFileLang("Kokku puudumisi 26")).isEqualTo(Language.ESTONIAN);
        assertThat(LanguageDetector.detectFileLang("Всего пропусков 26")).isEqualTo(Language.RUSSIAN);
        assertThat(LanguageDetector.detectFileLang("Total absences 26")).isEqualTo(Language.ENGLISH);
    }
}
