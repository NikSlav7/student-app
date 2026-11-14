package com.example.ResourceServer.rest;

import com.example.ResourceServer.dao.PasswordResetTokenDAO;
import com.example.ResourceServer.dao.ProfilesDao;
import com.example.ResourceServer.dao.YearsDao;
import com.example.ResourceServer.domains.PasswordResetToken;
import com.example.ResourceServer.domains.Profile;
import com.example.ResourceServer.exceptions.NoSuchEntityException;
import com.example.ResourceServer.exceptions.WrongDataSentException;
import com.example.ResourceServer.mail.PasswordResetMailSender;
import com.example.ResourceServer.security.AuthManager;
import com.example.ResourceServer.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {YearService.class, ResetPasswordService.class})
@Import(SecurityConfig.class)
class PasswordResetEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "authManager")
    private AuthManager authManager;

    @MockBean
    private YearsDao yearsDao;

    @MockBean
    private ProfilesDao profilesDao;

    @MockBean
    private PasswordResetTokenDAO passwordResetTokenDAO;

    @MockBean
    private PasswordResetMailSender passwordResetMailSender;

    private Profile profile;

    @BeforeEach
    void setUp() {
        profile = new Profile();
        profile.setProfileId("profile-1");
        profile.setUsername("claudetest1");
        profile.setEmail("claudetest1@example.test");
    }

    @Test
    void requestingAResetNeverReturnsTheToken() throws Exception {
        PasswordResetToken token = new PasswordResetToken("secret-token", LocalDateTime.now().plusMinutes(15), profile);
        when(profilesDao.getProfileByEmailOrUsername("claudetest1")).thenReturn(profile);
        when(passwordResetTokenDAO.generateToken(profile)).thenReturn(token);

        mockMvc.perform(post("/api/password-reset/require-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cred\":\"claudetest1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("secret-token"))));

        verify(passwordResetMailSender).sendPasswordResetEmail("claudetest1@example.test", "claudetest1", token);
    }

    @Test
    void requestingAResetForAnUnknownUserFails() throws Exception {
        when(profilesDao.getProfileByEmailOrUsername("nobody"))
                .thenThrow(new WrongDataSentException("No user with such credentials existing"));

        mockMvc.perform(post("/api/password-reset/require-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cred\":\"nobody\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resettingWithAValidTokenChangesThePasswordAndDeletesTheToken() throws Exception {
        when(passwordResetTokenDAO.isValid("secret-token")).thenReturn(true);
        when(passwordResetTokenDAO.getProfileByToken("secret-token")).thenReturn(profile);

        mockMvc.perform(reset("secret-token"))
                .andExpect(status().isOk());

        verify(authManager).resetPassword(profile, "newpass123", "secret-token");
        verify(passwordResetTokenDAO).removeToken("secret-token");
    }

    @Test
    void reusingATokenIsRejected() throws Exception {
        when(passwordResetTokenDAO.isValid("used-token")).thenThrow(new NoSuchEntityException("The token is invalid"));

        mockMvc.perform(reset("used-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("The token is invalid"));

        verify(authManager, never()).resetPassword(any(), anyString(), anyString());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        when(passwordResetTokenDAO.isValid("old-token")).thenReturn(false);

        mockMvc.perform(reset("old-token"))
                .andExpect(status().isBadRequest());

        verify(authManager, never()).resetPassword(any(), anyString(), anyString());
        verify(passwordResetTokenDAO, never()).removeToken(eq("old-token"));
    }

    private static org.springframework.test.web.servlet.RequestBuilder reset(String token) {
        return post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"newpass123\",\"passwordRepeat\":\"newpass123\"}");
    }
}
