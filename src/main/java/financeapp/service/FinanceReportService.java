package financeapp.service;

import financeapp.model.Transaction;
import financeapp.model.TransactionType;
import financeapp.model.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;

public class FinanceReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String buildSummary(String username, Wallet wallet, WalletService walletService) {
        StringBuilder builder = new StringBuilder();
        builder.append("Пользователь: ").append(username).append(System.lineSeparator());
        builder.append("Баланс: ").append(wallet.getBalance()).append(System.lineSeparator());
        builder.append("Общий доход: ").append(walletService.totalIncome(wallet)).append(System.lineSeparator());
        builder.append("Общие расходы: ").append(walletService.totalExpense(wallet)).append(System.lineSeparator());

        builder.append(System.lineSeparator()).append("Доходы по категориям:").append(System.lineSeparator());
        walletService.totalsByCategory(wallet, TransactionType.INCOME)
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append(System.lineSeparator()));

        builder.append(System.lineSeparator()).append("Расходы по категориям:").append(System.lineSeparator());
        walletService.totalsByCategory(wallet, TransactionType.EXPENSE)
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append(System.lineSeparator()));

        builder.append(System.lineSeparator()).append("Бюджеты:").append(System.lineSeparator());
        wallet.getBudgets().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String category = entry.getKey();
                    builder.append(category)
                            .append(": ")
                            .append(entry.getValue())
                            .append(", Остаток: ");
                    var remaining = walletService.remainingBudget(wallet, category);
                    builder.append(remaining == null ? "н/д" : remaining);
                    builder.append(System.lineSeparator());
                });

        builder.append(System.lineSeparator()).append("Последние операции:").append(System.lineSeparator());
        wallet.getTransactions().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .limit(5)
                .forEach(tx -> builder
                        .append(DATE_FORMATTER.format(tx.getTimestamp()))
                        .append(" | ")
                        .append(tx.getType())
                        .append(" | ")
                        .append(tx.getCategory())
                        .append(" | ")
                        .append(tx.getAmount())
                        .append(" | ")
                        .append(tx.getNote())
                        .append(System.lineSeparator()));

        return builder.toString();
    }

    public String categoryBreakdown(Wallet wallet, WalletService service, String[] categories) {
        StringBuilder builder = new StringBuilder();
        for (String category : categories) {
            String normalized = category.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            builder.append(normalized).append(":").append(System.lineSeparator());

            boolean hasTransactions = wallet.getTransactions().stream()
                    .anyMatch(tx -> tx.getCategory().equalsIgnoreCase(normalized));
            boolean hasBudget = wallet.getBudgets().keySet().stream()
                    .anyMatch(key -> key.equalsIgnoreCase(normalized));

            if (!hasTransactions && !hasBudget) {
                builder.append("  Категория не найдена в кошельке.")
                        .append(System.lineSeparator());
                continue;
            }

            var expenses = service.expensesForCategory(wallet, normalized);
            var incomes = service.incomesForCategory(wallet, normalized);
            var budget = wallet.getBudgetFor(normalized);
            builder.append("  Доходы: ").append(incomes).append(System.lineSeparator());
            builder.append("  Расходы: ").append(expenses).append(System.lineSeparator());
            if (budget != null) {
                builder.append("  Бюджет: ").append(budget)
                        .append(", остаток: ").append(service.remainingBudget(wallet, normalized))
                        .append(System.lineSeparator());
            } else {
                builder.append("  Бюджет не задан").append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    public void export(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось записать файл отчета: " + file, e);
        }
    }
}

