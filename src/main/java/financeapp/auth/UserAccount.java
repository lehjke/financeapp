package financeapp.auth;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String login;
    private final String passwordHash;
    private final LocalDateTime createdAt;

    public UserAccount(String login, String passwordHash, LocalDateTime createdAt) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

