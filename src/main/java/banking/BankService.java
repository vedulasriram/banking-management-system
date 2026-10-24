
package banking;

import banking.database.AccountDao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BankService {

    // Existing in-memory storage for the console application
    private final ArrayList<Account> accounts = new ArrayList<>();

    // DAO for Oracle database operations
    private final AccountDao accountDao = new AccountDao();


    // ==================== EXISTING CONSOLE METHODS ====================

    // Create an account in memory
    public void createAccount(Account account) {

        if (account == null) {
            System.out.println("Account cannot be null.");
            return;
        }

        if (findAccount(account.getAccountNumber()) != null) {
            System.out.println("Account already exists.");
            return;
        }

        accounts.add(account);

        System.out.println("Account created successfully!");
    }


    // Find an account in memory
    public Account findAccount(long accountNumber) {

        for (Account account : accounts) {
            if (account.getAccountNumber() == accountNumber) {
                return account;
            }
        }

        return null;
    }


    // Display all in-memory accounts
    public void displayAllAccounts() {

        if (accounts.isEmpty()) {
            System.out.println("No accounts available.");
            return;
        }

        for (Account account : accounts) {
            account.displayAccountDetails();
            System.out.println("------------------------");
        }
    }


    // Deposit into an in-memory account
    public void depositMoney(
            long accountNumber, BigDecimal amount) {

        Account account = findAccount(accountNumber);

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        if (!isValidPositiveAmount(amount)) {
            System.out.println("Invalid deposit amount.");
            return;
        }

        account.deposit(amount);
    }


    // Withdraw from an in-memory account
    public void withdrawMoney(
            long accountNumber, BigDecimal amount) {

        Account account = findAccount(accountNumber);

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        if (!isValidPositiveAmount(amount)) {
            System.out.println("Invalid withdrawal amount.");
            return;
        }

        account.withdraw(amount);
    }


    // Transfer between in-memory accounts
    public void transferMoney(
            long senderAccountNumber,
            long receiverAccountNumber,
            BigDecimal amount) {

        Account sender = findAccount(senderAccountNumber);
        Account receiver = findAccount(receiverAccountNumber);

        if (sender == null) {
            System.out.println("Sender account not found.");
            return;
        }

        if (receiver == null) {
            System.out.println("Receiver account not found.");
            return;
        }

        if (senderAccountNumber == receiverAccountNumber) {
            System.out.println(
                    "Cannot transfer to the same account.");
            return;
        }

        if (!isValidPositiveAmount(amount)) {
            System.out.println("Invalid transfer amount.");
            return;
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            System.out.println("Insufficient balance.");
            return;
        }

        boolean withdrawn = sender.withdraw(amount);

        if (withdrawn) {
            boolean deposited = receiver.deposit(amount);

            if (deposited) {
                System.out.println("Transfer successful!");
            } else {
                // Restore the sender's balance if deposit fails.
                sender.deposit(amount);
                System.out.println("Transfer failed.");
            }
        }
    }


    // ==================== ORACLE DATABASE METHODS ====================

    // Create an account in Oracle
    public Account createAccountInDatabase(
            String accountHolderName,
            BigDecimal openingBalance) throws SQLException {

        if (accountHolderName == null
                || accountHolderName.isBlank()) {
            throw new IllegalArgumentException(
                    "Account holder name cannot be empty.");
        }

        validateNonNegativeBalance(openingBalance);

        return accountDao.createAccount(
                accountHolderName, openingBalance);
    }


    // Find one account in Oracle
    public Account findAccountInDatabase(
            long accountNumber) throws SQLException {

        validateAccountNumber(accountNumber);

        return accountDao.findAccount(accountNumber);
    }


    // Retrieve all accounts from Oracle
    public List<Account> findAllAccountsFromDatabase()
            throws SQLException {

        return accountDao.findAllAccounts();
    }


    // Deposit money into an Oracle account
    public Account depositMoneyInDatabase(
            long accountNumber,
            BigDecimal amount) throws SQLException {

        validateAccountNumber(accountNumber);
        validatePositiveAmount(amount);

        return accountDao.depositMoney(accountNumber, amount);
    }


    // Withdraw money from an Oracle account
    public Account withdrawMoneyInDatabase(
            long accountNumber,
            BigDecimal amount) throws SQLException {

        validateAccountNumber(accountNumber);
        validatePositiveAmount(amount);

        return accountDao.withdrawMoney(accountNumber, amount);
    }


    // Transfer money between Oracle accounts
    public void transferMoneyInDatabase(
            long senderAccountNumber,
            long receiverAccountNumber,
            BigDecimal amount) throws SQLException {

        validateAccountNumber(senderAccountNumber);
        validateAccountNumber(receiverAccountNumber);
        validatePositiveAmount(amount);

        if (senderAccountNumber == receiverAccountNumber) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same account.");
        }

        accountDao.transferMoney(
                senderAccountNumber,
                receiverAccountNumber,
                amount);
    }


    // ==================== VALIDATION METHODS ====================

    private boolean isValidPositiveAmount(BigDecimal amount) {

        return amount != null
                && amount.compareTo(BigDecimal.ZERO) > 0;
    }


    private void validateAccountNumber(long accountNumber) {

        if (accountNumber <= 0) {
            throw new IllegalArgumentException(
                    "Account number must be greater than zero.");
        }
    }


    private void validatePositiveAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero.");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places.");
        }
    }


    private void validateNonNegativeBalance(BigDecimal balance) {

        if (balance == null
                || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Opening balance cannot be negative.");
        }

        if (balance.scale() > 2) {
            throw new IllegalArgumentException(
                    "Opening balance cannot have more than two decimal places.");
        }
    }
}
