package financeapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WalletTest {

    @Test
    void renameCategoryUpdatesTransactions() {
        Wallet wallet = new Wallet();
        wallet.addTransaction(new Transaction("food", new BigDecimal("100"), TransactionType.EXPENSE, "",
                LocalDateTime.now(), ""));
        wallet.addTransaction(new Transaction("Food", new BigDecimal("50"), TransactionType.INCOME, "",
                LocalDateTime.now(), ""));

        int updated = wallet.renameCategory("food", "nutrition");

        assertEquals(2, updated);
        assertEquals("nutrition", wallet.getTransactions().get(0).getCategory());
        assertEquals("nutrition", wallet.getTransactions().get(1).getCategory());
    }

    @Test
    void renameCategoryMovesBudget() {
        Wallet wallet = new Wallet();
        wallet.setBudget("food", new BigDecimal("1000"));

        wallet.renameCategory("food", "nutrition");

        assertNull(wallet.getBudgetFor("food"));
        assertEquals(new BigDecimal("1000"), wallet.getBudgetFor("NUTRITION"));
    }
}
