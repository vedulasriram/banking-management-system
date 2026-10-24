
package banking;

import java.math.BigDecimal;
import java.util.Scanner;

public class BankingApplication {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        BankService bankService = new BankService();

        while (true) {

            System.out.println("\n===== BANKING MANAGEMENT SYSTEM =====");
            System.out.println("1. Create Account");
            System.out.println("2. Display All Accounts");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. Check Account");
            System.out.println("6. Transfer Money");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();

            switch (choice) {

                case 1 -> {
                    System.out.print("Enter account number: ");
                    long accountNumber = scanner.nextLong();
                    scanner.nextLine();

                    System.out.print("Enter account holder name: ");
                    String name = scanner.nextLine();

                    System.out.print("Enter initial balance: ");
                    BigDecimal initialBalance = scanner.nextBigDecimal();

                    if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
                        System.out.println("Balance cannot be negative.");
                        break;
                    }

                    Account account = new Account(
                            accountNumber, name, initialBalance
                    );

                    bankService.createAccount(account);
                }

                case 2 -> {
                    bankService.displayAllAccounts();
                }

                case 3 -> {
                    System.out.print("Enter account number: ");
                    long depositAccount = scanner.nextLong();

                    System.out.print("Enter deposit amount: ");
                    BigDecimal depositAmount = scanner.nextBigDecimal();

                    bankService.depositMoney(
                            depositAccount, depositAmount
                    );
                }

                case 4 -> {
                    System.out.print("Enter account number: ");
                    long withdrawAccount = scanner.nextLong();

                    System.out.print("Enter withdrawal amount: ");
                    BigDecimal withdrawAmount = scanner.nextBigDecimal();

                    bankService.withdrawMoney(
                            withdrawAccount, withdrawAmount
                    );
                }

                case 5 -> {
                    System.out.print("Enter account number: ");
                    long searchAccount = scanner.nextLong();

                    Account foundAccount =
                            bankService.findAccount(searchAccount);

                    if (foundAccount != null) {
                        foundAccount.displayAccountDetails();
                    } else {
                        System.out.println("Account not found.");
                    }
                }

                case 6 -> {
                    System.out.print("Enter sender account number: ");
                    long sender = scanner.nextLong();

                    System.out.print("Enter receiver account number: ");
                    long receiver = scanner.nextLong();

                    System.out.print("Enter transfer amount: ");
                    BigDecimal transferAmount = scanner.nextBigDecimal();

                    bankService.transferMoney(
                            sender, receiver, transferAmount
                    );
                }

                case 7 -> {
                    System.out.println(
                            "Thank you for using our bank!"
                    );
                    scanner.close();
                    return;
                }

                default -> {
                    System.out.println(
                            "Invalid choice. Try again."
                    );
                }
            }
        }
    }
}
