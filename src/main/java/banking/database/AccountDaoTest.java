
package banking.database;

import banking.Account;

import java.math.BigDecimal;
import java.sql.SQLException;

public class AccountDaoTest {

    public static void main(String[] args) {

        AccountDao accountDao = new AccountDao();

        // These accounts existed in your earlier Oracle output.
        long senderAccountNumber = 10001;
        long receiverAccountNumber = 10002;

        try {
            System.out.println("========== INITIAL ACCOUNTS ==========");

            Account sender =
                    accountDao.findAccount(senderAccountNumber);

            Account receiver =
                    accountDao.findAccount(receiverAccountNumber);

            if (sender == null || receiver == null) {
                System.out.println(
                        "Test accounts not found. Check your Oracle accounts.");
                return;
            }

            System.out.println("Sender account:");
            sender.displayAccountDetails();

            System.out.println("\nReceiver account:");
            receiver.displayAccountDetails();

            // 1. Test deposit
            System.out.println("\n========== DEPOSIT TEST ==========");

            Account afterDeposit = accountDao.depositMoney(
                    senderAccountNumber,
                    new BigDecimal("500.00")
            );

            System.out.println("Deposit successful!");
            afterDeposit.displayAccountDetails();

            // 2. Test withdrawal
            System.out.println("\n========== WITHDRAWAL TEST ==========");

            Account afterWithdrawal = accountDao.withdrawMoney(
                    senderAccountNumber,
                    new BigDecimal("200.00")
            );

            System.out.println("Withdrawal successful!");
            afterWithdrawal.displayAccountDetails();

            // 3. Test transfer
            System.out.println("\n========== TRANSFER TEST ==========");

            accountDao.transferMoney(
                    senderAccountNumber,
                    receiverAccountNumber,
                    new BigDecimal("300.00")
            );

            System.out.println("Transfer successful!");

            // 4. Verify both balances after transfer
            System.out.println("\n========== VERIFY BALANCES ==========");

            System.out.println("Sender after transfer:");
            accountDao.findAccount(senderAccountNumber)
                    .displayAccountDetails();

            System.out.println("\nReceiver after transfer:");
            accountDao.findAccount(receiverAccountNumber)
                    .displayAccountDetails();

            // 5. List all accounts
            System.out.println("\n========== ALL ACCOUNTS ==========");

            for (Account account : accountDao.findAllAccounts()) {
                account.displayAccountDetails();
                System.out.println("--------------------");
            }

            System.out.println("\nAll database operation tests completed!");

        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("A database operation test failed.");
            System.out.println("Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
