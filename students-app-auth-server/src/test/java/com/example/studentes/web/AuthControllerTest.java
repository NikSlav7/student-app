package com.example.studentes.web;

import com.example.studentes.converters.JwtConverter;
import com.example.studentes.domains.Account;
import com.example.studentes.domains.AccountsManager;
import com.example.studentes.security.KeyUtils;
import com.example.studentes.security.PasswordEncoderConfig;
import com.example.studentes.security.TokenGenerator;
import com.example.studentes.security.WebConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({WebConfig.class, KeyUtils.class, JwtConverter.class, TokenGenerator.class, PasswordEncoderConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AccountsManager accountsManager;

    @BeforeEach
    void setUp() {
        Account account = new Account("claudetest", passwordEncoder.encode("secret123")).activateProfile();
        account.setId("account-1");
        when(accountsManager.loadUserByUsername("claudetest")).thenReturn(account);
        when(accountsManager.loadUserByUsername("nobody")).thenThrow(new UsernameNotFoundException("User wasn't found"));
    }

    @Test
    void correctPasswordGivesBothTokens() throws Exception {
        login("claudetest", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("account-1"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        login("claudetest", "wrong-password")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsRejectedTheSameWayAsAWrongPassword() throws Exception {
        login("nobody", "secret123")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenPassesTheTokenCheck() throws Exception {
        String accessToken = token("$.accessToken");

        mockMvc.perform(post("/api/auth/checktoken").header("Access-Token", accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenCannotBeUsedAsAnAccessToken() throws Exception {
        String refreshToken = token("$.refreshToken");

        mockMvc.perform(post("/api/auth/checktoken").header("Access-Token", refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenGivesNewTokens() throws Exception {
        String refreshToken = token("$.refreshToken");

        renew(refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void accessTokenCannotBeUsedToRenew() throws Exception {
        String accessToken = token("$.accessToken");

        renew(accessToken)
                .andExpect(status().isUnauthorized());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions renew(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/renew")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private String token(String path) throws Exception {
        String body = login("claudetest", "secret123").andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, path);
    }
}
