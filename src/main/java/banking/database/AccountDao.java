
package banking.database;

import banking.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccountDao {

    // Create a new account in Oracle
    public Account createAccount(
            String accountHolderName,
            BigDecimal openingBalance) throws SQLException {

        if (accountHolderName == null
                || accountHolderName.isBlank()) {
            throw new IllegalArgumentException(
                    "Account holder name cannot be empty.");
        }

        if (openingBalance == null
                || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Opening balance cannot be negative.");
        }

        if (openingBalance.scale() > 2) {
            throw new IllegalArgumentException(
                    "Opening balance cannot have more than two decimal places.");
        }

        String sequenceSql =
                "SELECT BANKING_APP.ACCOUNTS_SEQ.NEXTVAL FROM DUAL";

        String insertSql = """
                INSERT INTO BANKING_APP.ACCOUNTS
                    (ACCOUNT_NUMBER, ACCOUNT_HOLDER_NAME, BALANCE)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                long accountNumber;

                try (Statement statement = connection.createStatement();
                     ResultSet resultSet =
                             statement.executeQuery(sequenceSql)) {

                    if (!resultSet.next()) {
                        throw new SQLException(
                                "Could not generate account number.");
                    }

                    accountNumber = resultSet.getLong(1);
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(insertSql)) {

                    statement.setLong(1, accountNumber);
                    statement.setString(2, accountHolderName);
                    statement.setBigDecimal(3, openingBalance);

                    statement.executeUpdate();
                }

                Account account =
                        findAccountByNumber(connection, accountNumber);

                connection.commit();

                return account;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }


    // Find one account by account number
    public Account findAccount(long accountNumber)
            throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {
            return findAccountByNumber(connection, accountNumber);
        }
    }


    // Find an account using an existing connection
    private Account findAccountByNumber(
            Connection connection,
            long accountNumber) throws SQLException {

        String sql = """
                SELECT ACCOUNT_NUMBER, ACCOUNT_HOLDER_NAME, BALANCE
                FROM BANKING_APP.ACCOUNTS
                WHERE ACCOUNT_NUMBER = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Account(
                            resultSet.getLong("ACCOUNT_NUMBER"),
                            resultSet.getString("ACCOUNT_HOLDER_NAME"),
                            resultSet.getBigDecimal("BALANCE")
                    );
                }

                return null;
            }
        }
    }


    // Find all accounts
    public List<Account> findAllAccounts() throws SQLException {

        List<Account> accounts = new ArrayList<>();

        String sql = """
                SELECT ACCOUNT_NUMBER, ACCOUNT_HOLDER_NAME, BALANCE
                FROM BANKING_APP.ACCOUNTS
                ORDER BY ACCOUNT_NUMBER
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Account account = new Account(
                        resultSet.getLong("ACCOUNT_NUMBER"),
                        resultSet.getString("ACCOUNT_HOLDER_NAME"),
                        resultSet.getBigDecimal("BALANCE")
                );

                accounts.add(account);
            }
        }

        return accounts;
    }


    // Deposit money into an account
    public Account depositMoney(
            long accountNumber,
            BigDecimal amount) throws SQLException {

        validateAmount(amount);

        String sql = """
                UPDATE BANKING_APP.ACCOUNTS
                SET BALANCE = BALANCE + ?
                WHERE ACCOUNT_NUMBER = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setLong(2, accountNumber);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SQLException("Account not found.");
            }
        }

        return findAccount(accountNumber);
    }


    // Withdraw money only when sufficient balance is available
    public Account withdrawMoney(
            long accountNumber,
            BigDecimal amount) throws SQLException {

        validateAmount(amount);

        String sql = """
                UPDATE BANKING_APP.ACCOUNTS
                SET BALANCE = BALANCE - ?
                WHERE ACCOUNT_NUMBER = ?
                  AND BALANCE >= ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, amount);
            statement.setLong(2, accountNumber);
            statement.setBigDecimal(3, amount);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                if (findAccount(accountNumber) == null) {
                    throw new SQLException("Account not found.");
                }

                throw new SQLException("Insufficient balance.");
            }
        }

        return findAccount(accountNumber);
    }


    // Transfer money safely between two accounts
    public void transferMoney(
            long senderAccountNumber,
            long receiverAccountNumber,
            BigDecimal amount) throws SQLException {

        validateAmount(amount);

        if (senderAccountNumber <= 0 || receiverAccountNumber <= 0) {
            throw new IllegalArgumentException(
                    "Account numbers must be greater than zero.");
        }

        if (senderAccountNumber == receiverAccountNumber) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same account.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Lock accounts in ascending order to reduce deadlock risk.
                long firstNumber = Math.min(
                        senderAccountNumber, receiverAccountNumber);
                long secondNumber = Math.max(
                        senderAccountNumber, receiverAccountNumber);

                Account firstAccount =
                        findAccountForUpdate(connection, firstNumber);
                Account secondAccount =
                        findAccountForUpdate(connection, secondNumber);

                if (firstAccount == null || secondAccount == null) {
                    throw new SQLException(
                            "Sender account or receiver account not found.");
                }

                Account sender = senderAccountNumber == firstNumber
                        ? firstAccount : secondAccount;

                if (sender.getBalance().compareTo(amount) < 0) {
                    throw new SQLException("Insufficient balance.");
                }

                String debitSql = """
                        UPDATE BANKING_APP.ACCOUNTS
                        SET BALANCE = BALANCE - ?
                        WHERE ACCOUNT_NUMBER = ?
                        """;

                String creditSql = """
                        UPDATE BANKING_APP.ACCOUNTS
                        SET BALANCE = BALANCE + ?
                        WHERE ACCOUNT_NUMBER = ?
                        """;

                try (PreparedStatement debit =
                             connection.prepareStatement(debitSql);
                     PreparedStatement credit =
                             connection.prepareStatement(creditSql)) {

                    debit.setBigDecimal(1, amount);
                    debit.setLong(2, senderAccountNumber);

                    if (debit.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not debit sender account.");
                    }

                    credit.setBigDecimal(1, amount);
                    credit.setLong(2, receiverAccountNumber);

                    if (credit.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not credit receiver account.");
                    }
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }


    // Lock an account row during a transaction
    private Account findAccountForUpdate(
            Connection connection,
            long accountNumber) throws SQLException {

        String sql = """
                SELECT ACCOUNT_NUMBER, ACCOUNT_HOLDER_NAME, BALANCE
                FROM BANKING_APP.ACCOUNTS
                WHERE ACCOUNT_NUMBER = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Account(
                            resultSet.getLong("ACCOUNT_NUMBER"),
                            resultSet.getString("ACCOUNT_HOLDER_NAME"),
                            resultSet.getBigDecimal("BALANCE")
                    );
                }

                return null;
            }
        }
    }


    // Validate deposit, withdrawal, and transfer amounts
    private void validateAmount(BigDecimal amount) {

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
}
