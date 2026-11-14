package com.example.ResourceServer.dictionary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DictionaryTest {

    // The words the app really uses to find things in a marks sheet
    // (MarksManager.extractPeriod and AbsenceStatsManager.getAbsentStats)
    @ParameterizedTest(name = "\"{0}\" -> ru \"{1}\", en \"{2}\"")
    @CsvSource({
            "Periood,         Период,          periood",
            "Kokku puudumisi, Всего пропусков, kokku puudumisi",
            "Kursuse nimetus, Название курса,  kursuse nimetus",
    })
    void translatesTheWordsUsedToReadASheet(String estonian, String russian, String english) {
        assertThat(Dictionary.translate(estonian, Language.RUSSIAN)).isEqualTo(russian);
        assertThat(Dictionary.translate(estonian, Language.ENGLISH)).isEqualTo(english);
    }

    @Test
    void estonianReturnsTheWordUnchanged() {
        assertThat(Dictionary.translate("Periood", Language.ESTONIAN)).isEqualTo("Periood");
        assertThat(Dictionary.translate("Tundmatu sõna", Language.ESTONIAN)).isEqualTo("Tundmatu sõna");
    }


    @Test
    void everyWordHasARussianAndEnglishTranslation() {
        Dictionary.init();

        assertThat(Dictionary.dictionary).isNotEmpty();
        Dictionary.dictionary.forEach((estonian, translations) -> {
            assertThat(translations.get(Language.RUSSIAN)).as("Russian for \"%s\"", estonian).isNotBlank();
            assertThat(translations.get(Language.ENGLISH)).as("English for \"%s\"", estonian).isNotBlank();
        });
    }

    @Test
    void unknownWordCrashesForRussianAndEnglish() {
        assertThatThrownBy(() -> Dictionary.translate("Tundmatu sõna", Language.RUSSIAN))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "code \"{0}\" -> {1}")
    @CsvSource({
            "ee, ESTONIAN",
            "ru, RUSSIAN",
            "en, ENGLISH",
            "RU, RUSSIAN",
    })
    void findsLanguageByCode(String code, Language expected) {
        assertThat(Language.getLanguageByCode(code)).isEqualTo(expected);
    }

    @Test
    void unknownLanguageCodeGivesNull() {
        assertThat(Language.getLanguageByCode("de")).isNull();
        assertThat(Language.getLanguageByCode(null)).isNull();
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void periodWordExistsInEveryLanguage(Language language) {
        assertThat(Dictionary.translate("Periood", language)).isNotBlank();
    }
}
