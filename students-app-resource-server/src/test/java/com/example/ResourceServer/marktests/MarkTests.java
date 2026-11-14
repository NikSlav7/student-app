package com.example.ResourceServer.marktests;


import com.example.ResourceServer.dao.AbsenceDAO;
import com.example.ResourceServer.dao.PeriodsDao;
import com.example.ResourceServer.dao.ProfilesDao;
import com.example.ResourceServer.dao.YearsDao;
import com.example.ResourceServer.domains.AbsenceStats;
import com.example.ResourceServer.domains.Mark;
import com.example.ResourceServer.domains.Period;
import com.example.ResourceServer.domains.Profile;
import com.example.ResourceServer.domains.Subject;
import com.example.ResourceServer.pdf.AbsenceStatsManager;
import com.example.ResourceServer.pdf.MarksManager;
import com.example.ResourceServer.pdf.MarksMode;
import com.example.ResourceServer.repositories.AbsenceStatsRepository;
import com.example.ResourceServer.repositories.PeriodRepository;
import com.example.ResourceServer.repositories.ProfilesRepository;
import com.example.ResourceServer.repositories.YearsRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MarkTests {


    private ProfilesDao profilesDao;


    @Mock
    private YearsRepository yearsRepository;
    @Mock
    private PeriodRepository periodRepository;

    @Mock
    private ProfilesRepository profilesRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AbsenceStatsRepository absenceStatsRepository;


    private MarksManager marksManager;

    private AutoCloseable autoCloseable;

    private AbsenceStatsManager absenceStatsManager;

    private Profile profile;

    private AbsenceStats absenceStats;



    @BeforeEach
    void setup(){
        autoCloseable = MockitoAnnotations.openMocks(this);
        YearsDao yearsDao = new YearsDao(yearsRepository, jdbcTemplate);
        PeriodsDao periodsDao = new PeriodsDao(periodRepository);
        AbsenceDAO absenceDAO = new AbsenceDAO(periodsDao, yearsDao, absenceStatsRepository);
        marksManager = new MarksManager(yearsDao, periodsDao, profilesRepository, absenceDAO);
        absenceStatsManager = new AbsenceStatsManager(absenceDAO);

        profile = new Profile();
        profile.setProfileId("test-profile");
        profile.setYearList(new ArrayList<>());
        profile.setUploadList(new ArrayList<>());
        absenceStats = new AbsenceStats();
    }

    @Test
    void calculateAverage(){
        List<Subject> subjects = upload("BIO 9G,5 4 3", MarksMode.MODE_INTEGER);

        Mark mark = subjects.get(0).getMarkList().get(0);
        assertThat(mark.getMarkList()).containsExactly(5.0, 4.0, 3.0);
        assertThat(mark.getMarkAvg()).isEqualTo(4.0);
    }

    @AfterEach
    void destroy() throws Exception {
        autoCloseable.close();
    }


    @Test
    void canCalculateDottedAvg() throws Exception{
        List<Subject> subjects = upload("AJA,9,0 8,1 7,3 7,2 8,0", MarksMode.MODE_DOUBLE);

        assertThat(subjects.get(0).getMarkList().get(0).getMarkAvg()).isCloseTo(7.92, within(0.001));
    }

    @Test
    void createsOneSubjectPerLineAndSkipsSubjectsWithoutMarks() {
        List<Subject> subjects = upload("BIO 9G,5 4 3\nKLT 9G,\nMU 9G,5 5 1\n", MarksMode.MODE_INTEGER);

        assertThat(subjects).extracting(Subject::getSubjectName).containsExactly("BIO 9G", "MU 9G");
    }

    @Test
    void linksEverythingTogether() {
        List<Subject> subjects = upload("BIO 9G,5 4 3", MarksMode.MODE_INTEGER);

        Subject subject = subjects.get(0);
        Period period = subject.getPeriod();
        assertThat(period.getPeriodName()).isEqualTo("II");
        assertThat(period.getSubjectList()).containsExactly(subject);
        assertThat(period.getAbsenceStats()).isSameAs(absenceStats);
        assertThat(absenceStats.getPeriod()).isSameAs(period);
        assertThat(period.getYear().getYearName()).isEqualTo("2023");
        assertThat(period.getYear().getProfile()).isSameAs(profile);
        assertThat(profile.getYearList()).containsExactly(period.getYear());
    }

    @Test
    void allMarksOfOneUploadShareTheSameUpload() {
        List<Subject> subjects = upload("BIO 9G,5 4 3\nMU 9G,5 5 1", MarksMode.MODE_INTEGER);

        Mark bio = subjects.get(0).getMarkList().get(0);
        Mark mu = subjects.get(1).getMarkList().get(0);
        assertThat(bio.getUpload()).isSameAs(mu.getUpload());
        assertThat(bio.getUpload().getUploadId()).isNotBlank();
        assertThat(bio.getUpload().getUploadDate()).isNotNull();
        assertThat(profile.getUploadList()).containsExactly(bio.getUpload());
    }

    @Test
    void savesTheProfileOnce() {
        upload("BIO 9G,5 4 3\nMU 9G,5 5 1", MarksMode.MODE_INTEGER);

        verify(profilesRepository, times(1)).save(profile);
    }

    @Test
    void reuploadAddsTheNewMarkToTheExistingSubject() {
        Subject existingSubject = new Subject();
        existingSubject.setSubjectName("BIO 9G");
        existingSubject.setMarkList(new ArrayList<>(List.of(new Mark())));
        Period existingPeriod = new Period();
        existingPeriod.setPeriodName("II");
        existingPeriod.setSubjectList(new ArrayList<>(List.of(existingSubject)));
        when(periodRepository.getPeriodByYearAndPeriodName(any(), eq("II"))).thenReturn(Optional.of(existingPeriod));

        List<Subject> subjects = upload("BIO 9G,5 4 3", MarksMode.MODE_INTEGER);

        Mark newMark = subjects.get(0).getMarkList().get(0);
        assertThat(existingPeriod.getSubjectList()).containsExactly(existingSubject);
        assertThat(existingSubject.getMarkList()).hasSize(2).contains(newMark);
        assertThat(newMark.getSubject()).isSameAs(existingSubject);
    }

    private List<Subject> upload(String marks, MarksMode mode) {
        return marksManager.createAndSaveMarks(marks, absenceStats, profile, "2023", "II", mode);
    }
}
