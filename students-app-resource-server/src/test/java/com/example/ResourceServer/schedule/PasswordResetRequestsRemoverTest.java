package com.example.ResourceServer.schedule;

import com.example.ResourceServer.PostgresTest;
import com.example.ResourceServer.domains.PasswordResetToken;
import com.example.ResourceServer.domains.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetRequestsRemoverTest extends PostgresTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PasswordResetRequestsRemover remover;
    private Profile profile;

    @BeforeEach
    void setUp() {
        remover = new PasswordResetRequestsRemover();
        ReflectionTestUtils.setField(remover, "jdbcTemplate", jdbcTemplate);

        profile = new Profile();
        profile.setProfileId(UUID.randomUUID().toString());
        profile.setUsername("claudetest");
        entityManager.persist(profile);
    }

    @Test
    void deletesExpiredTokens() {
        String expired = token(LocalDateTime.now().minusDays(1));

        remover.deleteOldRequests();

        assertThat(reload(expired)).isNull();
    }

    @Test
    void keepsTokensThatAreStillValid() {
        String valid = token(LocalDateTime.now().plusDays(1));

        remover.deleteOldRequests();

        assertThat(reload(valid)).isNotNull();
    }

    @Test
    void onlyTheExpiredOnesAreDeleted() {
        String expired = token(LocalDateTime.now().minusMinutes(30));
        String valid = token(LocalDateTime.now().plusHours(1));

        remover.deleteOldRequests();

        assertThat(reload(expired)).isNull();
        assertThat(reload(valid)).isNotNull();
    }

    private String token(LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(expiresAt);
        token.setProfile(profile);
        entityManager.persistAndFlush(token);
        return token.getToken();
    }

    private PasswordResetToken reload(String token) {
        entityManager.clear();
        return entityManager.find(PasswordResetToken.class, token);
    }
}
