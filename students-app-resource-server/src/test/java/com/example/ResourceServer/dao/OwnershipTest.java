package com.example.ResourceServer.dao;

import com.example.ResourceServer.PostgresTest;
import com.example.ResourceServer.domains.Period;
import com.example.ResourceServer.domains.Profile;
import com.example.ResourceServer.domains.Upload;
import com.example.ResourceServer.domains.Year;
import com.example.ResourceServer.exceptions.WrongDataSentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({UploadsDao.class, YearsDao.class, PeriodsDao.class})
class OwnershipTest extends PostgresTest {

    @Autowired
    private UploadsDao uploadsDao;

    @Autowired
    private YearsDao yearsDao;

    @Autowired
    private PeriodsDao periodsDao;

    @Autowired
    private TestEntityManager entityManager;

    private Profile me;
    private Profile otherUser;

    @BeforeEach
    void setUp() {
        me = profile("me");
        otherUser = profile("other");
    }

    @Test
    void canDeleteMyOwnUpload() throws WrongDataSentException {
        Upload mine = upload(me);

        uploadsDao.deleteUpload(mine.getUploadId(), me);

        assertThat(reload(Upload.class, mine.getUploadId())).isNull();
    }

    @Test
    void cannotDeleteAnotherUsersUpload() {
        Upload theirs = upload(otherUser);

        assertThatThrownBy(() -> uploadsDao.deleteUpload(theirs.getUploadId(), me))
                .isInstanceOf(WrongDataSentException.class);
        assertThat(reload(Upload.class, theirs.getUploadId())).isNotNull();
    }

    @Test
    void cannotDeleteAnotherUsersYear() {
        Year theirs = year(otherUser, "2023");

        assertThatThrownBy(() -> yearsDao.deleteYear("2023", me))
                .isInstanceOf(WrongDataSentException.class);
        assertThat(reload(Year.class, theirs.getYearId())).isNotNull();
    }

    @Test
    void deletingMyYearKeepsAnotherUsersYearWithTheSameName() throws WrongDataSentException {
        Year mine = year(me, "2023");
        Year theirs = year(otherUser, "2023");

        yearsDao.deleteYear("2023", me);

        assertThat(reload(Year.class, mine.getYearId())).isNull();
        assertThat(reload(Year.class, theirs.getYearId())).isNotNull();
    }

    @Test
    void cannotReachAnotherUsersPeriodWithoutOwningTheYear() {
        Period theirs = period(year(otherUser, "2023"), "II");

        assertThatThrownBy(() -> yearsDao.getYearByNameAndProfile("2023", me))
                .isInstanceOf(WrongDataSentException.class);
        assertThat(reload(Period.class, theirs.getPeriodId())).isNotNull();
    }

    @Test
    void deletingMyPeriodKeepsAnotherUsersPeriodWithTheSameName() throws WrongDataSentException {
        Period mine = period(year(me, "2023"), "II");
        Period theirs = period(year(otherUser, "2023"), "II");

        Year myYear = yearsDao.getYearByNameAndProfile("2023", me);
        periodsDao.deletePeriod("II", myYear);

        assertThat(reload(Period.class, mine.getPeriodId())).isNull();
        assertThat(reload(Period.class, theirs.getPeriodId())).isNotNull();
    }

    private Profile profile(String username) {
        Profile profile = new Profile();
        profile.setProfileId(UUID.randomUUID().toString());
        profile.setUsername(username);
        return entityManager.persist(profile);
    }

    private Upload upload(Profile owner) {
        Upload upload = new Upload();
        upload.setUploadId(UUID.randomUUID().toString());
        upload.setUploadDate(new Date());
        upload.setProfile(owner);
        return entityManager.persistAndFlush(upload);
    }

    private Year year(Profile owner, String name) {
        Year year = new Year();
        year.setYearId(UUID.randomUUID().toString());
        year.setYearName(name);
        year.setProfile(owner);
        return entityManager.persistAndFlush(year);
    }

    private Period period(Year year, String name) {
        Period period = new Period();
        period.setPeriodId(UUID.randomUUID().toString());
        period.setPeriodName(name);
        period.setYear(year);
        return entityManager.persistAndFlush(period);
    }

    private <T> T reload(Class<T> type, String id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(type, id);
    }
}
