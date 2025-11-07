package financeapp.service;

import financeapp.model.Wallet;
import financeapp.persistence.InMemoryWalletStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferServiceTest {

    private WalletService walletService;
    private TransferService transferService;
    private InMemoryWalletStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryWalletStore();
        walletService = new WalletService(store);
        transferService = new TransferService(walletService);
    }

    @Test
    void selfTransferForbidden() {
        Wallet wallet = walletService.loadWallet("alice");
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer("alice", wallet, "alice", new BigDecimal("10"), "Any"));
    }

    @Test
    void transferPersistsRecipientWallet() {
        Wallet sender = walletService.loadWallet("alice");
        walletService.saveWallet("bob", new Wallet());

        transferService.transfer("alice", sender, "bob", new BigDecimal("50"), "Gift");

        Wallet recipient = store.load("bob");
        assertEquals(new BigDecimal("50.00"), walletService.totalIncome(recipient));
        assertEquals(new BigDecimal("50.00"), walletService.totalExpense(sender));
    }
}

