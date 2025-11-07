package financeapp.service;

import financeapp.model.Transaction;
import financeapp.model.TransactionType;
import financeapp.model.Wallet;
import financeapp.persistence.WalletStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WalletService {
    private final WalletStore repository;

    public WalletService(WalletStore repository) {
        this.repository = repository;
    }

    public Wallet loadWallet(String login) {
        return repository.load(login);
    }

    public void saveWallet(String login, Wallet wallet) {
        repository.save(login, wallet);
    }

    public void addIncome(Wallet wallet, String category, BigDecimal amount, String note) {
        wallet.addTransaction(new Transaction(
                category,
                amount,
                TransactionType.INCOME,
                note,
                LocalDateTime.now(),
                ""
        ));
    }

    public void addExpense(Wallet wallet, String category, BigDecimal amount, String note) {
        wallet.addTransaction(new Transaction(
                category,
                amount,
                TransactionType.EXPENSE,
                note,
                LocalDateTime.now(),
                ""
        ));
    }

    public void registerTransferOut(Wallet wallet, String category, BigDecimal amount, String recipient) {
        wallet.addTransaction(new Transaction(
                category,
                amount,
                TransactionType.TRANSFER_OUT,
                "Перевод пользователю " + recipient,
                LocalDateTime.now(),
                recipient
        ));
    }

    public void registerTransferIn(Wallet wallet, String category, BigDecimal amount, String sender) {
        wallet.addTransaction(new Transaction(
                category,
                amount,
                TransactionType.TRANSFER_IN,
                "Перевод от пользователя " + sender,
                LocalDateTime.now(),
                sender
        ));
    }

    public void setBudget(Wallet wallet, String category, BigDecimal amount) {
        wallet.setBudget(category, amount);
    }

    public BigDecimal totalIncome(Wallet wallet) {
        return wallet.totalByType(TransactionType.INCOME)
                .add(wallet.totalByType(TransactionType.TRANSFER_IN))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalExpense(Wallet wallet) {
        return wallet.totalByType(TransactionType.EXPENSE)
                .add(wallet.totalByType(TransactionType.TRANSFER_OUT))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> totalsByCategory(Wallet wallet, TransactionType type) {
        Map<String, BigDecimal> totals = wallet.totalsByCategory(type);
        Map<String, BigDecimal> scaled = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            scaled.put(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP));
        }
        return Collections.unmodifiableMap(scaled);
    }

    public BigDecimal expensesForCategory(Wallet wallet, String category) {
        return wallet.getTransactions().stream()
                .filter(tx -> tx.getType() == TransactionType.EXPENSE || tx.getType() == TransactionType.TRANSFER_OUT)
                .filter(tx -> tx.getCategory().equalsIgnoreCase(category))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal incomesForCategory(Wallet wallet, String category) {
        return wallet.getTransactions().stream()
                .filter(tx -> tx.getType() == TransactionType.INCOME || tx.getType() == TransactionType.TRANSFER_IN)
                .filter(tx -> tx.getCategory().equalsIgnoreCase(category))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal remainingBudget(Wallet wallet, String category) {
        BigDecimal budget = wallet.getBudgetFor(category);
        if (budget == null) {
            return null;
        }
        BigDecimal spent = expensesForCategory(wallet, category);
        return budget.subtract(spent).setScale(2, RoundingMode.HALF_UP);
    }

    public int renameCategory(Wallet wallet, String from, String to) {
        return wallet.renameCategory(from, to);
    }
}
