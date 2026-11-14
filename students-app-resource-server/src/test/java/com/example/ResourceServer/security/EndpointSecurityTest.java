package com.example.ResourceServer.security;

import com.example.ResourceServer.dao.PasswordResetTokenDAO;
import com.example.ResourceServer.dao.ProfilesDao;
import com.example.ResourceServer.dao.YearsDao;
import com.example.ResourceServer.mail.PasswordResetMailSender;
import com.example.ResourceServer.rest.ResetPasswordService;
import com.example.ResourceServer.rest.YearService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {YearService.class, ResetPasswordService.class})
@Import(SecurityConfig.class)
class EndpointSecurityTest {

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

    @Test
    void readingDataWithoutATokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/year/get-year-names"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingDataWithoutATokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/year/delete-year/2023"))
                .andExpect(status().isForbidden());
    }

    @Test
    void withAValidTokenTheRequestGoesThrough() throws Exception {
        when(authManager.checkToken("good-token")).thenReturn("profile-1");

        mockMvc.perform(get("/api/year/get-year-names").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());

        verify(yearsDao).getSimplifiedYearsByProfileId("profile-1");
    }

    @Test
    void passwordResetWorksWithoutALogin() throws Exception {
        mockMvc.perform(get("/api/password-reset/check-token").param("token", "abc"))
                .andExpect(status().isOk());
    }
}
