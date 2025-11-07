package financeapp.service;

import financeapp.model.Wallet;
import financeapp.persistence.InMemoryWalletStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletServiceTest {

    private WalletService service;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        service = new WalletService(new InMemoryWalletStore());
        wallet = new Wallet();
    }

    @Test
    void addIncomeShouldIncreaseTotals() {
        service.addIncome(wallet, "Salary", new BigDecimal("1000.50"), "First part");
        service.addIncome(wallet, "Bonus", new BigDecimal("500"), "Bonus");

        BigDecimal total = service.totalIncome(wallet);
        assertEquals(new BigDecimal("1500.50"), total);
    }

    @Test
    void addExpenseShouldIncreaseTotals() {
        service.addExpense(wallet, "Food", new BigDecimal("200.25"), "Lunch");
        service.addExpense(wallet, "Food", new BigDecimal("100.75"), "Dinner");

        BigDecimal total = service.totalExpense(wallet);
        assertEquals(new BigDecimal("301.00"), total);
    }

    @Test
    void setBudgetOverwritesCaseInsensitive() {
        service.setBudget(wallet, "Food", new BigDecimal("1000"));
        service.setBudget(wallet, "food", new BigDecimal("1500"));

        assertEquals(new BigDecimal("1500"), wallet.getBudgetFor("Food"));
    }

    @Test
    void remainingBudgetCalculatedCorrectly() {
        service.setBudget(wallet, "Food", new BigDecimal("1000"));
        service.addExpense(wallet, "Food", new BigDecimal("400"), "Lunch");

        BigDecimal remaining = service.remainingBudget(wallet, "Food");
        assertEquals(new BigDecimal("600.00"), remaining);
    }

    @Test
    void expensesForCategoryCaseInsensitive() {
        service.addExpense(wallet, "Food", new BigDecimal("100"), "1");
        service.addExpense(wallet, "food", new BigDecimal("50"), "2");

        BigDecimal total = service.expensesForCategory(wallet, "FOOD");
        assertEquals(new BigDecimal("150.00"), total);
    }

    @Test
    void registerTransferUpdatesIncomeAndExpense() {
        Wallet second = new Wallet();

        service.registerTransferOut(wallet, "Перевод", new BigDecimal("200"), "b");
        service.registerTransferIn(second, "Перевод", new BigDecimal("200"), "a");

        assertEquals(new BigDecimal("200.00"), service.totalExpense(wallet));
        assertEquals(new BigDecimal("200.00"), service.totalIncome(second));
    }
}
