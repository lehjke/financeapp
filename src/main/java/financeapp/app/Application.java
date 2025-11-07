package financeapp.app;

import financeapp.auth.AuthService;
import financeapp.model.Transaction;
import financeapp.model.TransactionType;
import financeapp.model.Wallet;
import financeapp.persistence.FileWalletStore;
import financeapp.service.FinanceReportService;
import financeapp.service.TransactionImportService;
import financeapp.service.TransferService;
import financeapp.service.WalletService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Application {
    private static final BigDecimal BUDGET_THRESHOLD = new BigDecimal("0.80");

    private final Scanner scanner = new Scanner(System.in);
    private final AuthService authService;
    private final WalletService walletService;
    private final FinanceReportService reportService;
    private final TransferService transferService;
    private final TransactionImportService importService;

    public Application() {
        Path dataDir = Path.of("data");
        this.authService = new AuthService(dataDir.resolve("users.dat"));
        this.walletService = new WalletService(new FileWalletStore(dataDir.resolve("wallets")));
        this.reportService = new FinanceReportService();
        this.transferService = new TransferService(walletService);
        this.importService = new TransactionImportService(walletService);
    }

    public void run() {
        System.out.println("=== Консоль управления финансами ===");
        boolean running = true;
        while (running) {
            printWelcomeMenu();
            String choice = prompt("Выберите пункт: ").trim();
            switch (choice) {
                case "1" -> {
                    boolean exit = handleLoginFlow();
                    if (exit) {
                        running = false;
                    }
                }
                case "2" -> handleRegistration();
                case "3", "exit" -> running = false;
                default -> System.out.println("Неизвестный пункт меню, попробуйте снова.");
            }
        }
        System.out.println("До встречи!");
    }

    private void printWelcomeMenu() {
        System.out.println();
        System.out.println("1) Вход");
        System.out.println("2) Регистрация");
        System.out.println("3) Выход");
    }

    private boolean handleLoginFlow() {
        String login = prompt("Логин: ").trim();
        String password = prompt("Пароль: ").trim();
        if (!authService.authenticate(login, password)) {
            System.out.println("Неверный логин или пароль.");
            return false;
        }
        Wallet wallet = walletService.loadWallet(login);
        return runSession(login, wallet);
    }

    private void handleRegistration() {
        String login = prompt("Придумайте логин: ").trim();
        String password = prompt("Придумайте пароль: ").trim();
        try {
            boolean registered = authService.register(login, password);
            if (registered) {
                System.out.println("Аккаунт создан. Теперь можно войти.");
            } else {
                System.out.println("Пользователь уже существует.");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Ошибка регистрации: " + ex.getMessage());
        }
    }

    private boolean runSession(String login, Wallet wallet) {
        System.out.println();
        System.out.println("Добро пожаловать, " + login + "!");
        printHelp();
        boolean exitApplication = false;
        boolean activeSession = true;
        while (activeSession) {
            System.out.print("[" + login + "]> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String command = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (command.isEmpty()) {
                continue;
            }
            try {
                switch (command) {
                    case "help" -> printHelp();
                    case "add-income" -> {
                        addIncome(wallet);
                        autoSave(login, wallet);
                    }
                    case "add-expense" -> {
                        addExpense(wallet);
                        autoSave(login, wallet);
                    }
                    case "set-budget" -> {
                        setBudget(wallet);
                        autoSave(login, wallet);
                    }
                    case "show-summary" -> showSummary(login, wallet);
                    case "view-budgets" -> viewBudgets(wallet);
                    case "list-transactions" -> listTransactions(wallet);
                    case "category-summary" -> showCategorySummary(wallet);
                    case "export-summary" -> exportSummary(login, wallet);
                    case "transfer" -> {
                        performTransfer(login, wallet);
                        autoSave(login, wallet);
                    }
                    case "rename-category" -> {
                        renameCategory(wallet);
                        autoSave(login, wallet);
                    }
                    case "import-transactions" -> {
                        importTransactions(login, wallet);
                        autoSave(login, wallet);
                    }
                    case "save" -> {
                        autoSave(login, wallet);
                        System.out.println("Кошелек сохранен.");
                    }
                    case "logout" -> {
                        autoSave(login, wallet);
                        activeSession = false;
                    }
                    case "exit" -> {
                        autoSave(login, wallet);
                        activeSession = false;
                        exitApplication = true;
                    }
                    default -> System.out.println("Неизвестная команда. Введите 'help' для списка команд.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Ошибка валидации: " + ex.getMessage());
            } catch (IllegalStateException ex) {
                System.out.println("Не удалось выполнить операцию: " + ex.getMessage());
            }
        }
        return exitApplication;
    }

    private void addIncome(Wallet wallet) {
        String category = prompt("Категория дохода: ");
        BigDecimal amount = readAmount("Сумма: ");
        String note = prompt("Комментарий: ");
        walletService.addIncome(wallet, category, amount, note);
        System.out.println("Доход добавлен.");
        warnIfExpensesExceedIncome(wallet);
    }

    private void addExpense(Wallet wallet) {
        String category = prompt("Категория расхода: ");
        BigDecimal amount = readAmount("Сумма: ");
        String note = prompt("Комментарий: ");
        walletService.addExpense(wallet, category, amount, note);
        System.out.println("Расход добавлен.");
        warnBudgetStatus(category, wallet);
        warnIfExpensesExceedIncome(wallet);
        warnLowBalance(wallet);
    }

    private void setBudget(Wallet wallet) {
        String category = prompt("Категория бюджета: ");
        BigDecimal amount = readAmount("Месячный лимит: ");
        walletService.setBudget(wallet, category, amount);
        System.out.println("Бюджет сохранен.");
    }

    private void showSummary(String login, Wallet wallet) {
        String summary = reportService.buildSummary(login, wallet, walletService);
        System.out.println(summary);
        warnOverspend(wallet);
    }

    private void warnOverspend(Wallet wallet) {
        if (walletService.totalExpense(wallet).compareTo(walletService.totalIncome(wallet)) > 0) {
            System.out.println("Внимание: расходы уже превысили доходы.");
        }
    }

    private void viewBudgets(Wallet wallet) {
        if (wallet.getBudgets().isEmpty()) {
            System.out.println("Бюджеты не заданы.");
            return;
        }
        wallet.getBudgets().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    String category = entry.getKey();
                    BigDecimal remaining = walletService.remainingBudget(wallet, category);
                    System.out.println(category + ": " + entry.getValue() +
                            ", остаток: " + (remaining == null ? "н/д" : remaining));
                });
    }

    private void listTransactions(Wallet wallet) {
        if (wallet.getTransactions().isEmpty()) {
            System.out.println("Операций пока нет.");
            return;
        }
        wallet.getTransactions().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .forEach(tx -> System.out.println(tx.getTimestamp() + " | " +
                        tx.getType() + " | " +
                        tx.getCategory() + " | " +
                        tx.getAmount() + " | " +
                        tx.getNote()));
    }

    private void showCategorySummary(Wallet wallet) {
        String categories = prompt("Категории (через запятую): ");
        if (categories.isBlank()) {
            System.out.println("Категории не указаны.");
            return;
        }
        String report = reportService.categoryBreakdown(wallet, walletService, categories.split(","));
        System.out.println(report);
    }

    private void exportSummary(String login, Wallet wallet) {
        String path = prompt("Путь к файлу (например reports/summary.txt): ").trim();
        if (path.isEmpty()) {
            System.out.println("Нужно указать путь.");
            return;
        }
        Path target = Path.of(path);
        String summary = reportService.buildSummary(login, wallet, walletService);
        reportService.export(target, summary);
        System.out.println("Отчет сохранен в " + target.toAbsolutePath());
    }

    private void performTransfer(String login, Wallet wallet) {
        String recipient = prompt("Логин получателя: ").trim();
        if (!authService.exists(recipient)) {
            System.out.println("Пользователь-получатель не найден.");
            return;
        }
        BigDecimal amount = readAmount("Сумма перевода: ");
        String category = prompt("Категория перевода: ");
        transferService.transfer(login, wallet, recipient, amount, category);
        System.out.println("Перевод выполнен.");
        warnBudgetStatus(category, wallet);
        warnIfExpensesExceedIncome(wallet);
        warnLowBalance(wallet);
    }

    private void warnBudgetStatus(String category, Wallet wallet) {
        BigDecimal budget = wallet.getBudgetFor(category);
        if (budget == null || budget.signum() <= 0) {
            return;
        }
        BigDecimal spent = walletService.expensesForCategory(wallet, category);
        BigDecimal remaining = budget.subtract(spent).setScale(2, RoundingMode.HALF_UP);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("Превышен бюджет по категории: " + category +
                    ". Перерасход: " + remaining.abs());
            return;
        }
        if (budget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = spent.divide(budget, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(BUDGET_THRESHOLD) >= 0) {
                BigDecimal percent = ratio.multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP);
                System.out.println("Предупреждение: категория '" + category + "' израсходована на " +
                        percent + "%, остаток " + remaining);
            }
        }
    }

    private void warnIfExpensesExceedIncome(Wallet wallet) {
        if (walletService.totalExpense(wallet).compareTo(walletService.totalIncome(wallet)) > 0) {
            System.out.println("Предупреждение: расходы превышают доходы.");
        }
    }

    private void warnLowBalance(Wallet wallet) {
        if (wallet.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Баланс опустился до нуля или стал отрицательным. Пополните кошелек.");
        }
    }

    private void autoSave(String login, Wallet wallet) {
        walletService.saveWallet(login, wallet);
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private BigDecimal readAmount(String message) {
        while (true) {
            String input = prompt(message);
            try {
                BigDecimal value = new BigDecimal(input.replace(',', '.'));
                if (value.signum() <= 0) {
                    System.out.println("Сумма должна быть положительной.");
                    continue;
                }
                return value.setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ex) {
                System.out.println("Введите числовое значение.");
            }
        }
    }

    private void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  help              - показать этот список");
        System.out.println("  add-income        - добавить запись о доходе");
        System.out.println("  add-expense       - добавить запись о расходе");
        System.out.println("  set-budget        - задать или обновить бюджет категории");
        System.out.println("  show-summary      - вывести суммарную статистику");
        System.out.println("  view-budgets      - показать бюджеты и остатки");
        System.out.println("  list-transactions - вывести все операции");
        System.out.println("  category-summary  - посчитать выбранные категории");
        System.out.println("  export-summary    - сохранить отчет в файл");
        System.out.println("  transfer          - перевести средства другому пользователю");
        System.out.println("  rename-category   - переименовать категорию в кошельке");
        System.out.println("  import-transactions - загрузить операции из CSV/TSV файла");
        System.out.println("  save              - сохранить текущее состояние");
        System.out.println("  logout            - выйти в главное меню");
        System.out.println("  exit              - сохранить и закрыть приложение");
    }

    private void renameCategory(Wallet wallet) {
        String from = prompt("Текущее название категории: ").trim();
        String to = prompt("Новое название: ").trim();
        if (from.isEmpty() || to.isEmpty()) {
            System.out.println("Названия не должны быть пустыми.");
            return;
        }
        int updated = walletService.renameCategory(wallet, from, to);
        if (updated == 0) {
            System.out.println("Категория не найдена, операции не изменены.");
        } else {
            System.out.println("Категория переименована, обновлено операций: " + updated);
        }
    }

    private void importTransactions(String login, Wallet wallet) {
        String path = prompt("CSV/TSV файл (type,category,amount,note): ").trim();
        if (path.isEmpty()) {
            System.out.println("Путь не указан.");
            return;
        }
        try {
            int count = importService.importFromCsv(wallet, Path.of(path));
            System.out.println("Импортировано операций: " + count);
            warnLowBalance(wallet);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Ошибка импорта: " + ex.getMessage());
        }
    }
}
