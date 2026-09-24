package com.ecommerce.identity.domain;

import com.ecommerce.identity.domain.model.AccountStatus;
import com.ecommerce.identity.domain.model.AccountType;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    @Test
    @DisplayName("New customer account is created with ACTIVE status and immutable account type")
    void shouldCreateActiveCustomerAccount() {
        UserAccount account = new UserAccount("test@example.com", "hashed_pwd", AccountType.CUSTOMER, "John Doe");

        assertThat(account.getId()).isNotNull();
        assertThat(account.getEmail()).isEqualTo("test@example.com");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getAccountType()).isEqualTo(AccountType.CUSTOMER);
        assertThat(account.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Roles can be assigned and revoked from user account")
    void shouldAssignAndRevokeRoles() {
        UserAccount account = new UserAccount("admin@example.com", "hashed_pwd", AccountType.BACK_OFFICE, "Super Admin");
        Role superAdmin = new Role("SUPER_ADMIN");

        account.getRoles().add(superAdmin);
        assertThat(account.getRoles()).hasSize(1);

        account.getRoles().remove(superAdmin);
        assertThat(account.getRoles()).isEmpty();
    }
}
