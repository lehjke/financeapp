package financeapp.auth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserAccountStore {
    private final Path storagePath;
    private final Map<String, UserAccount> accounts;

    public UserAccountStore(Path storagePath) {
        this.storagePath = storagePath;
        this.accounts = loadFromDisk();
    }

    public synchronized Optional<UserAccount> find(String login) {
        return Optional.ofNullable(accounts.get(login));
    }

    public synchronized void save(UserAccount account) {
        accounts.put(account.getLogin(), account);
        persist();
    }

    public synchronized boolean exists(String login) {
        return accounts.containsKey(login);
    }

    private Map<String, UserAccount> loadFromDisk() {
        if (Files.notExists(storagePath)) {
            return new HashMap<>();
        }
        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(storagePath))) {
            Object object = inputStream.readObject();
            if (object instanceof Map<?, ?> map) {
                Map<String, UserAccount> result = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof UserAccount value) {
                        result.put(key, value);
                    }
                }
                return result;
            }
        } catch (IOException | ClassNotFoundException ignored) {
            // Fallback to empty map if file is unreadable
        }
        return new HashMap<>();
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(storagePath))) {
                outputStream.writeObject(accounts);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist user accounts", e);
        }
    }
}

