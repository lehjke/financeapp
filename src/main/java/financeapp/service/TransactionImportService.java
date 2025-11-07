package financeapp.service;

import financeapp.model.TransactionType;
import financeapp.model.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TransactionImportService {
    private final WalletService walletService;

    public TransactionImportService(WalletService walletService) {
        this.walletService = walletService;
    }

    public int importFromCsv(Wallet wallet, Path source) {
        if (wallet == null) {
            throw new IllegalArgumentException("Кошелек не найден.");
        }
        if (source == null || source.toString().isBlank()) {
            throw new IllegalArgumentException("Путь к файлу обязателен.");
        }
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Файл не найден: " + source);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл: " + source, e);
        }

        int imported = 0;
        int lineNumber = 0;
        for (String rawLine : lines) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] tokens = splitCsvLine(line);
            if (lineNumber == 1 && isHeader(tokens)) {
                continue;
            }
            if (tokens.length < 3) {
                throw new IllegalArgumentException(errorMessage(lineNumber, "Нужно минимум три столбца: type, category, amount."));
            }
            String typeToken = tokens[0].trim().toUpperCase();
            String category = tokens[1].trim();
            String amountValue = tokens[2].trim().replace(",", ".");
            String note = tokens.length > 3 ? tokens[3].trim().replace("\"", "") : "";

            TransactionType type = parseType(typeToken, lineNumber);
            BigDecimal amount = parseAmount(amountValue, lineNumber);

            if (type == TransactionType.INCOME) {
                walletService.addIncome(wallet, category, amount, note);
            } else if (type == TransactionType.EXPENSE) {
                walletService.addExpense(wallet, category, amount, note);
            } else {
                throw new IllegalArgumentException(errorMessage(lineNumber, "Разрешены только типы INCOME или EXPENSE."));
            }
            imported++;
        }
        return imported;
    }

    private String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && (c == ';' || c == ',')) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private boolean isHeader(String[] tokens) {
        return tokens.length >= 3
                && tokens[0].equalsIgnoreCase("type")
                && tokens[1].equalsIgnoreCase("category")
                && tokens[2].equalsIgnoreCase("amount");
    }

    private TransactionType parseType(String token, int lineNumber) {
        return switch (token) {
            case "INCOME" -> TransactionType.INCOME;
            case "EXPENSE" -> TransactionType.EXPENSE;
            default -> throw new IllegalArgumentException(errorMessage(lineNumber,
                    "Тип должен быть INCOME или EXPENSE."));
        };
    }

    private BigDecimal parseAmount(String token, int lineNumber) {
        try {
            BigDecimal value = new BigDecimal(token);
            if (value.signum() <= 0) {
                throw new IllegalArgumentException();
            }
            return value.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            throw new IllegalArgumentException(errorMessage(lineNumber, "Сумма должна быть положительным числом. Значение: " + token));
        }
    }

    private String errorMessage(int line, String message) {
        return "Строка " + line + ": " + message;
    }
}

