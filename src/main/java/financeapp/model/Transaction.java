package financeapp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String category;
    private final BigDecimal amount;
    private final TransactionType type;
    private final String note;
    private final LocalDateTime timestamp;
    private final String counterparty;

    public Transaction(String category,
                       BigDecimal amount,
                       TransactionType type,
                       String note,
                       LocalDateTime timestamp,
                       String counterparty) {
        this.category = Objects.requireNonNullElse(category, "Uncategorized");
        this.amount = amount;
        this.type = type;
        this.note = note == null ? "" : note;
        this.timestamp = timestamp;
        this.counterparty = counterparty == null ? "" : counterparty;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public Transaction withCategory(String newCategory) {
        return new Transaction(newCategory, amount, type, note, timestamp, counterparty);
    }
}
