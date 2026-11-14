package com.example.ResourceServer.dao;

import com.example.ResourceServer.PostgresTest;
import com.example.ResourceServer.domains.Mark;
import com.example.ResourceServer.domains.Profile;
import com.example.ResourceServer.domains.Upload;
import com.example.ResourceServer.domains.UploadWithAvg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Import(UploadsDao.class)
class UploadsDaoTest extends PostgresTest {

    @Autowired
    private UploadsDao uploadsDao;

    @Autowired
    private TestEntityManager entityManager;

    private Profile profile;

    @BeforeEach
    void setUp() {
        profile = new Profile();
        profile.setProfileId("profile-1");
        profile.setUsername("claudetest");
        entityManager.persist(profile);
    }

    @Test
    void uploadAverageIsTheAverageOfItsSubjectAverages() {
        Upload upload = upload(profile, 4.0, 5.0, 3.0);

        List<UploadWithAvg> uploads = uploadsDao.getUploads("profile-1", 10, 0);

        assertThat(uploads).hasSize(1);
        assertThat(uploads.get(0).getUploadId()).isEqualTo(upload.getUploadId());
        assertThat(uploads.get(0).getOverallAvg()).isEqualTo(4.0);
    }

    @Test
    void decimalAveragesAreAveragedCorrectly() {
        upload(profile, 7.92, 4.444, 3.286);

        List<UploadWithAvg> uploads = uploadsDao.getUploads("profile-1", 10, 0);

        assertThat(uploads.get(0).getOverallAvg()).isCloseTo(5.2167, within(0.001));
    }

    @Test
    void eachUploadHasItsOwnAverage() {
        Upload first = upload(profile, 5.0, 5.0);
        Upload second = upload(profile, 3.0, 4.0);

        List<UploadWithAvg> uploads = uploadsDao.getUploads("profile-1", 10, 0);

        assertThat(uploads).hasSize(2);
        assertThat(averageOf(uploads, first)).isEqualTo(5.0);
        assertThat(averageOf(uploads, second)).isEqualTo(3.5);
    }

    @Test
    void uploadWithoutMarksHasNoAverage() {
        upload(profile);

        List<UploadWithAvg> uploads = uploadsDao.getUploads("profile-1", 10, 0);

        assertThat(uploads.get(0).getOverallAvg()).isNull();
    }

    @Test
    void onlyReturnsTheUploadsOfThatProfile() {
        Profile otherUser = new Profile();
        otherUser.setProfileId("profile-2");
        otherUser.setUsername("someone-else");
        entityManager.persist(otherUser);
        Upload mine = upload(profile, 5.0);
        upload(otherUser, 1.0);

        List<UploadWithAvg> uploads = uploadsDao.getUploads("profile-1", 10, 0);

        assertThat(uploads).extracting(UploadWithAvg::getUploadId).containsExactly(mine.getUploadId());
    }

    @Test
    void limitAndOffsetPageThroughUploadsOldestFirst() {
        Upload first = uploadAt(profile, new Date(1_000_000L), 5.0);
        Upload second = uploadAt(profile, new Date(2_000_000L), 4.0);
        Upload third = uploadAt(profile, new Date(3_000_000L), 3.0);

        List<UploadWithAvg> pageOne = uploadsDao.getUploads("profile-1", 2, 0);
        List<UploadWithAvg> pageTwo = uploadsDao.getUploads("profile-1", 2, 2);

        assertThat(pageOne).extracting(UploadWithAvg::getUploadId).containsExactly(first.getUploadId(), second.getUploadId());
        assertThat(pageTwo).extracting(UploadWithAvg::getUploadId).containsExactly(third.getUploadId());
    }

    @Test
    void unknownProfileHasNoUploads() {
        upload(profile, 5.0);

        assertThat(uploadsDao.getUploads("does-not-exist", 10, 0)).isEmpty();
    }

    private Upload upload(Profile owner, double... subjectAverages) {
        return uploadAt(owner, new Date(), subjectAverages);
    }

    private Upload uploadAt(Profile owner, Date date, double... subjectAverages) {
        Upload upload = new Upload();
        upload.setUploadId(UUID.randomUUID().toString());
        upload.setUploadDate(date);
        upload.setProfile(owner);
        upload.setMarkList(new ArrayList<>());
        entityManager.persist(upload);

        for (double average : subjectAverages) {
            Mark mark = new Mark();
            mark.setMarkId(UUID.randomUUID().toString());
            mark.setMarkList(List.of(average));
            mark.setMarkAvg(average);
            mark.setUpload(upload);
            entityManager.persist(mark);
        }
        entityManager.flush();
        return upload;
    }

    private static Double averageOf(List<UploadWithAvg> uploads, Upload upload) {
        return uploads.stream()
                .filter(u -> u.getUploadId().equals(upload.getUploadId()))
                .findFirst()
                .orElseThrow()
                .getOverallAvg();
    }
}
