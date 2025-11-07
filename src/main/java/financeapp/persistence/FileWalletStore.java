package financeapp.persistence;

import financeapp.model.Wallet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileWalletStore implements WalletStore {
    private final Path storageDir;

    public FileWalletStore(Path storageDir) {
        this.storageDir = storageDir;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to access wallet storage directory", e);
        }
    }

    @Override
    public synchronized Wallet load(String login) {
        Path file = storageDir.resolve(login + "-wallet.dat");
        if (Files.notExists(file)) {
            return new Wallet();
        }
        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(file))) {
            Object object = inputStream.readObject();
            if (object instanceof Wallet wallet) {
                return wallet;
            }
        } catch (IOException | ClassNotFoundException ignored) {
            // Fall through to return empty wallet
        }
        return new Wallet();
    }

    @Override
    public synchronized void save(String login, Wallet wallet) {
        Path file = storageDir.resolve(login + "-wallet.dat");
        try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(file))) {
            outputStream.writeObject(wallet);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist wallet for " + login, e);
        }
    }
}
