package financeapp.persistence;

import financeapp.model.Wallet;

import java.util.HashMap;
import java.util.Map;

public class InMemoryWalletStore implements WalletStore {
    private final Map<String, Wallet> storage = new HashMap<>();

    @Override
    public Wallet load(String login) {
        return storage.computeIfAbsent(login, key -> new Wallet());
    }

    @Override
    public void save(String login, Wallet wallet) {
        storage.put(login, wallet);
    }
}

