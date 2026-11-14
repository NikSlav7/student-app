package com.example.studentes.security;

import com.example.studentes.domains.Account;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TokenGeneratorTest {

    private TokenGenerator tokenGenerator;
    private JwtDecoder accessDecoder;
    private JwtDecoder refreshDecoder;
    private JwtEncoder refreshEncoder;
    private Account account;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair accessKeys = newRsaKeyPair();
        KeyPair refreshKeys = newRsaKeyPair();
        refreshEncoder = encoder(refreshKeys);
        accessDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) accessKeys.getPublic()).build();
        refreshDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) refreshKeys.getPublic()).build();

        tokenGenerator = new TokenGenerator();
        ReflectionTestUtils.setField(tokenGenerator, "accessTokenEncoder", encoder(accessKeys));
        ReflectionTestUtils.setField(tokenGenerator, "refreshTokenEncoder", refreshEncoder);

        account = new Account("claudetest", "secret");
        account.setId("account-id-123");
    }

    @Test
    void accessTokenBelongsToTheUserAndExpiresIn5Hours() {
        TokenDTO tokens = tokenGenerator.createTokens(login(account));

        Jwt access = accessDecoder.decode(tokens.getAccessToken());
        assertThat(access.getSubject()).isEqualTo("account-id-123");
        assertThat(access.getClaimAsString("iss")).isEqualTo("students app");
        assertThat(lifetime(access)).isCloseTo(Duration.ofHours(5).toSeconds(), within(5L));
    }

    @Test
    void refreshTokenBelongsToTheUserAndExpiresIn5Days() {
        TokenDTO tokens = tokenGenerator.createTokens(login(account));

        Jwt refresh = refreshDecoder.decode(tokens.getRefreshToken());
        assertThat(refresh.getSubject()).isEqualTo("account-id-123");
        assertThat(lifetime(refresh)).isCloseTo(Duration.ofDays(5).toSeconds(), within(5L));
    }

    @Test
    void responseContainsTheAccountId() {
        assertThat(tokenGenerator.createTokens(login(account)).getId()).isEqualTo("account-id-123");
    }

    @Test
    void accessAndRefreshTokensAreSignedWithDifferentKeys() {
        TokenDTO tokens = tokenGenerator.createTokens(login(account));

        assertThatThrownBy(() -> refreshDecoder.decode(tokens.getAccessToken())).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> accessDecoder.decode(tokens.getRefreshToken())).isInstanceOf(JwtException.class);
    }

    @Test
    void renewingGivesAFreshRefreshToken() {
        String oldRefresh = tokenGenerator.createTokens(login(account)).getRefreshToken();
        Jwt oldRefreshJwt = refreshDecoder.decode(oldRefresh);

        TokenDTO renewed = tokenGenerator.createTokens(new UsernamePasswordAuthenticationToken(account, oldRefreshJwt));

        Jwt renewedRefresh = refreshDecoder.decode(renewed.getRefreshToken());
        assertThat(renewedRefresh.getSubject()).isEqualTo("account-id-123");
        assertThat(lifetime(renewedRefresh)).isCloseTo(Duration.ofDays(5).toSeconds(), within(5L));
        assertThat(accessDecoder.decode(renewed.getAccessToken()).getSubject()).isEqualTo("account-id-123");
    }

    @Test
    void rejectsALoginThatIsNotAnAccount() {
        Authentication notAnAccount = new UsernamePasswordAuthenticationToken("just-a-string", null);

        assertThatThrownBy(() -> tokenGenerator.createTokens(notAnAccount)).isInstanceOf(BadCredentialsException.class);
    }

    private static Authentication login(Account account) {
        return new UsernamePasswordAuthenticationToken(account, "secret");
    }

    private static long lifetime(Jwt jwt) {
        return Duration.between(Instant.now(), jwt.getExpiresAt()).toSeconds();
    }

    private static KeyPair newRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static JwtEncoder encoder(KeyPair keys) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keys.getPublic()).privateKey((RSAPrivateKey) keys.getPrivate()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }
}
