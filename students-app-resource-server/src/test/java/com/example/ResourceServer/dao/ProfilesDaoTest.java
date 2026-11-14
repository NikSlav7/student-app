package com.example.ResourceServer.dao;

import com.example.ResourceServer.PostgresTest;
import com.example.ResourceServer.domains.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ProfilesDao.class)
class ProfilesDaoTest extends PostgresTest {

    @Autowired
    private ProfilesDao profilesDao;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.persistAndFlush(profile("anna@example.test", "anna"));
        entityManager.persistAndFlush(profile("ben@example.test", "ben"));
    }

    @Test
    void newEmailAndUsernameCanRegister() {
        assertThat(profilesDao.canAddProfile(profile("carl@example.test", "carl"))).isTrue();
    }

    @Test
    void takenUsernameIsRefused() {
        assertThat(profilesDao.canAddProfile(profile("new@example.test", "anna"))).isFalse();
    }

    @Test
    void takenEmailIsRefused() {
        assertThat(profilesDao.canAddProfile(profile("anna@example.test", "newname"))).isFalse();
    }

    @Test
    void emailOfOneUserAndUsernameOfAnotherIsRefused() {
        assertThat(profilesDao.canAddProfile(profile("anna@example.test", "ben"))).isFalse();
    }

    private static Profile profile(String email, String username) {
        Profile profile = new Profile();
        profile.setProfileId(UUID.randomUUID().toString());
        profile.setEmail(email);
        profile.setUsername(username);
        return profile;
    }
}
