package com.example.ResourceServer.security;

import com.example.ResourceServer.exceptions.BadTokenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenFilterTest {

    @Mock
    private AuthManager authManager;

    private TokenFilter tokenFilter;
    private MockHttpServletRequest request;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        tokenFilter = new TokenFilter(authManager);
        request = new MockHttpServletRequest("GET", "/api/year/get-year-names");
        chain = new MockFilterChain();
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenLogsInTheUserFromTheToken() throws Exception {
        when(authManager.checkToken("good-token")).thenReturn("profile-1");
        request.addHeader("Authorization", "Bearer good-token");

        tokenFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(loggedInUser()).isNotNull();
        assertThat(loggedInUser().getPrincipal()).isEqualTo("profile-1");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void invalidTokenLogsNobodyIn() throws Exception {
        when(authManager.checkToken("bad-token")).thenThrow(new BadTokenException("Token is incorrect"));
        request.addHeader("Authorization", "Bearer bad-token");

        tokenFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(loggedInUser()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void noTokenLogsNobodyInAndDoesNotAskTheAuthServer() throws Exception {
        tokenFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(loggedInUser()).isNull();
        verify(authManager, never()).checkToken(anyString());
    }

    @Test
    void headerWithoutBearerIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        tokenFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(loggedInUser()).isNull();
        verify(authManager, never()).checkToken(anyString());
    }

    @Test
    void onlyTheBearerPrefixIsRemovedFromTheToken() throws Exception {
        when(authManager.checkToken("abc.def.ghi")).thenReturn("profile-1");
        request.addHeader("Authorization", "Bearer abc.def.ghi");

        tokenFilter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(authManager).checkToken("abc.def.ghi");
    }

    @Test
    void authServerDownStopsTheRequest() throws Exception {
        when(authManager.checkToken("good-token")).thenThrow(new IOException("Connection refused"));
        request.addHeader("Authorization", "Bearer good-token");

        assertThatThrownBy(() -> tokenFilter.doFilter(request, new MockHttpServletResponse(), chain))
                .isInstanceOf(IOException.class);
        assertThat(loggedInUser()).isNull();
        assertThat(chain.getRequest()).isNull();
    }

    private static Authentication loggedInUser() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
