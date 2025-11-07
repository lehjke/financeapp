package financeapp.auth;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class AuthService {
    private final UserAccountStore store;
    private final PasswordHasher hasher;

    public AuthService(Path storagePath) {
        this.store = new UserAccountStore(storagePath);
        this.hasher = new PasswordHasher();
    }

    public boolean register(String login, String password) {
        validateLogin(login);
        validatePassword(password);
        if (store.exists(login)) {
            return false;
        }
        UserAccount account = new UserAccount(login, hasher.hash(password), LocalDateTime.now());
        store.save(account);
        return true;
    }

    public boolean authenticate(String login, String password) {
        return store.find(login)
                .map(account -> account.getPasswordHash().equals(hasher.hash(password)))
                .orElse(false);
    }

    public boolean exists(String login) {
        return store.exists(login);
    }

    private void validateLogin(String login) {
        if (login == null || login.length() < 3 || login.length() > 32 || login.contains(" ")) {
            throw new IllegalArgumentException("Логин должен содержать от 3 до 32 символов без пробелов.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 6 символов.");
        }
    }
}

