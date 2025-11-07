package financeapp.service;

import financeapp.model.Wallet;

import java.math.BigDecimal;

public class TransferService {
    private final WalletService walletService;

    public TransferService(WalletService walletService) {
        this.walletService = walletService;
    }

    public void transfer(String senderLogin,
                         Wallet senderWallet,
                         String recipientLogin,
                         BigDecimal amount,
                         String category) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть положительной.");
        }
        if (senderLogin.equalsIgnoreCase(recipientLogin)) {
            throw new IllegalArgumentException("Нельзя переводить средства самому себе.");
        }
        Wallet recipientWallet = walletService.loadWallet(recipientLogin);
        walletService.registerTransferOut(senderWallet, category, amount, recipientLogin);
        walletService.registerTransferIn(recipientWallet, category, amount, senderLogin);
        walletService.saveWallet(recipientLogin, recipientWallet);
    }
}
