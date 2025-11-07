package financeapp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

public class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<String, BigDecimal> budgets = new HashMap<>();

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void setBudget(String category, BigDecimal amount) {
        String existingKey = budgets.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(category))
                .findFirst()
                .orElse(null);
        if (existingKey != null) {
            budgets.put(existingKey, amount);
        } else {
            budgets.put(category, amount);
        }
    }

    public Map<String, BigDecimal> getBudgets() {
        return Collections.unmodifiableMap(budgets);
    }

    public BigDecimal getBudgetFor(String category) {
        return budgets.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(category))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public BigDecimal getBalance() {
        return totalByType(TransactionType.INCOME)
                .add(totalByType(TransactionType.TRANSFER_IN))
                .subtract(totalByType(TransactionType.EXPENSE))
                .subtract(totalByType(TransactionType.TRANSFER_OUT));
    }

    public BigDecimal totalByType(TransactionType type) {
        return transactions.stream()
                .filter(tx -> tx.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> totalsByCategory(TransactionType type) {
        return transactions.stream()
                .filter(tx -> tx.getType() == type)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    public int renameCategory(String original, String target) {
        if (original == null || target == null) {
            throw new IllegalArgumentException("Категории должны быть указаны.");
        }
        if (original.equalsIgnoreCase(target)) {
            return 0;
        }
        int changes = 0;
        ListIterator<Transaction> iterator = transactions.listIterator();
        while (iterator.hasNext()) {
            Transaction tx = iterator.next();
            if (tx.getCategory().equalsIgnoreCase(original)) {
                iterator.set(tx.withCategory(target));
                changes++;
            }
        }
        String originalKey = budgets.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(original))
                .findFirst()
                .orElse(null);
        if (originalKey != null) {
            BigDecimal amount = budgets.remove(originalKey);
            String targetKey = budgets.keySet().stream()
                    .filter(key -> key.equalsIgnoreCase(target))
                    .findFirst()
                    .orElse(target);
            budgets.put(targetKey, amount.add(budgets.getOrDefault(targetKey, BigDecimal.ZERO)));
        }
        return changes;
    }
}
