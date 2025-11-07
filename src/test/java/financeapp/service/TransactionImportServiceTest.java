package financeapp.service;

import financeapp.model.Wallet;
import financeapp.persistence.InMemoryWalletStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionImportServiceTest {

    private WalletService walletService;
    private TransactionImportService importService;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(new InMemoryWalletStore());
        importService = new TransactionImportService(walletService);
        wallet = new Wallet();
    }

    @Test
    void importIncomeAndExpense() throws IOException {
        Path file = Files.createTempFile("transactions", ".csv");
        Files.writeString(file, """
                type,category,amount,note
                INCOME,Salary,1000,Monthly pay
                EXPENSE,Food,250,Lunch
                """);

        int imported = importService.importFromCsv(wallet, file);

        assertEquals(2, imported);
        assertEquals(new BigDecimal("750.00"), wallet.getBalance());
    }

    @Test
    void headerSkippedAutomatically() throws IOException {
        Path file = Files.createTempFile("transactions", ".csv");
        Files.writeString(file, "TYPE;CATEGORY;AMOUNT\nEXPENSE;Travel;100;Taxi");

        int imported = importService.importFromCsv(wallet, file);

        assertEquals(1, imported);
    }

    @Test
    void invalidTypeThrows() throws IOException {
        Path file = Files.createTempFile("transactions", ".csv");
        Files.writeString(file, "UNKNOWN,Food,100");

        assertThrows(IllegalArgumentException.class, () ->
                importService.importFromCsv(wallet, file));
    }
}
