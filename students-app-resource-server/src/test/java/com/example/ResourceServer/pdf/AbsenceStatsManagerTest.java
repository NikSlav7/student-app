package com.example.ResourceServer.pdf;

import com.example.ResourceServer.TestVariables;
import com.example.ResourceServer.dao.AbsenceDAO;
import com.example.ResourceServer.dictionary.Language;
import com.example.ResourceServer.domains.AbsenceStats;
import com.example.ResourceServer.domains.Year;
import com.example.ResourceServer.exceptions.WrongDataSentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AbsenceStatsManagerTest {

    private static final String NL = System.getProperty("line.separator");

    @Mock
    private AbsenceDAO absenceDAO;

    private AbsenceStatsManager absenceStatsManager;

    private final Year year = new Year();

    @BeforeEach
    void setUp() throws WrongDataSentException {
        absenceStatsManager = new AbsenceStatsManager(absenceDAO);
        lenient().when(absenceDAO.getOrCreateNew(any(), anyString()))
                .thenThrow(new WrongDataSentException("There is no such period"));
    }

    @Test
    void readsAbsencesFromARealEstonianSheet() {
        AbsenceStats stats = absenceStatsManager.getAbsentStats(TestVariables.DOTTED_AJA, year, "II", Language.ESTONIAN);

        assertThat(stats.getTotalAbsent()).isEqualTo(26);
        assertThat(stats.getNoReasonAbsent()).isEqualTo(0);
        assertThat(stats.getTotalLate()).isEqualTo(2);
        assertThat(stats.getNoReasonLate()).isEqualTo(2);
    }

    @Test
    void readsAbsencesFromARussianSheet() {
        String sheet = String.join(NL,
                "Табель", "Период: III четверть",
                "Всего пропусков", "12",
                "Пропуски без причины", "3",
                "Всего опозданий", "4",
                "Опоздания без причины", "1",
                "Название курса", "MAT 9. KLASS ,5 4");

        AbsenceStats stats = absenceStatsManager.getAbsentStats(sheet, year, "III", Language.RUSSIAN);

        assertThat(stats.getTotalAbsent()).isEqualTo(12);
        assertThat(stats.getNoReasonAbsent()).isEqualTo(3);
        assertThat(stats.getTotalLate()).isEqualTo(4);
        assertThat(stats.getNoReasonLate()).isEqualTo(1);
    }

    @Test
    void onlyTheFirstFourNumbersAreUsed() {
        String sheet = String.join(NL, "Kokku puudumisi", "10", "x", "1", "x", "5", "x", "0", "99", "Kursuse nimetus");

        AbsenceStats stats = absenceStatsManager.getAbsentStats(sheet, year, "I", Language.ESTONIAN);

        assertThat(stats.getTotalAbsent()).isEqualTo(10);
        assertThat(stats.getNoReasonAbsent()).isEqualTo(1);
        assertThat(stats.getTotalLate()).isEqualTo(5);
        assertThat(stats.getNoReasonLate()).isEqualTo(0);
    }

    @Test
    void createsNewStatsWithAnIdWhenThePeriodIsNew() {
        AbsenceStats stats = absenceStatsManager.getAbsentStats(TestVariables.DOTTED_AJA, year, "II", Language.ESTONIAN);

        assertThat(stats.getAbsenceStatsId()).isNotBlank();
    }

    @Test
    void updatesTheExistingStatsWhenThePeriodWasUploadedBefore() throws WrongDataSentException {
        AbsenceStats existing = new AbsenceStats();
        existing.setAbsenceStatsId("existing-id");
        existing.setTotalAbsent(5);
        doReturn(existing).when(absenceDAO).getOrCreateNew(year, "II");

        AbsenceStats stats = absenceStatsManager.getAbsentStats(TestVariables.DOTTED_AJA, year, "II", Language.ESTONIAN);

        assertThat(stats).isSameAs(existing);                        // same object, so the same database row
        assertThat(stats.getAbsenceStatsId()).isEqualTo("existing-id");
        assertThat(stats.getTotalAbsent()).isEqualTo(26);            // old value 5 replaced by the new sheet
    }

    // Documents current behaviour: a PDF that isn't a marks sheet crashes here.
    // If wrong files are ever rejected more gracefully, update this test.
    @Test
    void crashesWhenTheSheetHasNoAbsenceSection() {
        String notASheet = "Some random PDF text without the expected headings";

        assertThatThrownBy(() -> absenceStatsManager.getAbsentStats(notASheet, year, "I", Language.ESTONIAN))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }
}
