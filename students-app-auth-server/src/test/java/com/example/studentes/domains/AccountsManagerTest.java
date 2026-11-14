package com.example.studentes.domains;

import com.example.studentes.repos.AccountsRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsManagerTest {

    @Mock
    private AccountsRepo accountsRepo;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @InjectMocks
    private AccountsManager accountsManager;

    @Test
    void newUserPasswordIsStoredHashed() {
        accountsManager.createUser(new Account("claudetest", "secret123"));

        Account saved = savedAccount();
        assertThat(saved.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getPassword())).isTrue();
    }

    @Test
    void newUserGetsAnId() {
        accountsManager.createUser(new Account("claudetest", "secret123"));

        assertThat(savedAccount().getId()).isNotBlank();
    }

    @Test
    void twoUsersGetDifferentIds() {
        Account first = new Account("first", "secret123");
        Account second = new Account("second", "secret123");

        accountsManager.createUser(first);
        accountsManager.createUser(second);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void changedPasswordIsStoredHashed() {
        Account existing = new Account("claudetest", passwordEncoder.encode("old-password"));
        when(accountsRepo.findAccountByUsername("claudetest")).thenReturn(Optional.of(existing));

        accountsManager.changeAccountPassword("claudetest", "new-password");

        Account saved = savedAccount();
        assertThat(saved.getPassword()).isNotEqualTo("new-password");
        assertThat(passwordEncoder.matches("new-password", saved.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("old-password", saved.getPassword())).isFalse();
    }

    @Test
    void unknownUsernameThrows() {
        when(accountsRepo.findAccountByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountsManager.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private Account savedAccount() {
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountsRepo).save(captor.capture());
        return captor.getValue();
    }
}
