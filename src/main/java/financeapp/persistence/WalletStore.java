package financeapp.persistence;

import financeapp.model.Wallet;

public interface WalletStore {
    Wallet load(String login);

    void save(String login, Wallet wallet);
}

