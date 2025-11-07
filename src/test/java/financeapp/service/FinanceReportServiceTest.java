package financeapp.service;

import financeapp.model.Transaction;
import financeapp.model.TransactionType;
import financeapp.model.Wallet;
import financeapp.persistence.WalletStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceReportServiceTest {

    private FinanceReportService service;
    private Wallet wallet;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        service = new FinanceReportService();
        wallet = new Wallet();
        walletService = new WalletService(new WalletStore() {
            @Override
            public Wallet load(String login) {
                return wallet;
            }

            @Override
            public void save(String login, Wallet wallet) {
                // no-op for tests
            }
        });
    }

    @Test
    void categoryBreakdownWarnsWhenMissing() {
        String report = service.categoryBreakdown(wallet, walletService, new String[]{"Food"});
        assertTrue(report.contains("Категория не найдена"), "Should warn when category missing");
    }

    @Test
    void categoryBreakdownShowsBudgetAndAmounts() {
        wallet.addTransaction(new Transaction("Food", new BigDecimal("200"),
                TransactionType.EXPENSE, "", LocalDateTime.now(), ""));
        wallet.setBudget("Food", new BigDecimal("1000"));

        String report = service.categoryBreakdown(wallet, walletService, new String[]{"Food"});

        assertTrue(report.contains("Бюджет: 1000"), "Budget information is missing");
        assertTrue(report.contains("Расходы: 200.00"), "Expense information is missing");
    }
}

