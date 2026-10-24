
package banking;

import java.math.BigDecimal;

public class Account {

    private long accountNumber;
    private String accountHolderName;
    private BigDecimal balance;

    // Constructor
    public Account(long accountNumber,
                   String accountHolderName,
                   BigDecimal balance) {

        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException(
                    "Balance cannot be null or negative.");
        }

        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
    }

    // Getters
    public long getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    // Deposit money
    public boolean deposit(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            System.out.println("Invalid deposit amount.");
            return false;
        }

        balance = balance.add(amount);

        System.out.println("Deposit successful!");
        return true;
    }

    // Withdraw money
    public boolean withdraw(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            System.out.println("Invalid withdrawal amount.");
            return false;
        }

        if (amount.compareTo(balance) > 0) {
            System.out.println("Insufficient balance.");
            return false;
        }

        balance = balance.subtract(amount);

        System.out.println("Withdrawal successful!");
        return true;
    }

    // Display account details
    public void displayAccountDetails() {

        System.out.println("Account Number: " + accountNumber);
        System.out.println("Account Holder: " + accountHolderName);
        System.out.println("Balance: " + balance);
    }
}
