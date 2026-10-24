
package banking.handler;

import banking.Account;
import banking.BankService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class BankingApiHandler implements HttpHandler {

    private final BankService bankService = new BankService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            String method = exchange.getRequestMethod();
            String path = URLDecoder.decode(
                    exchange.getRequestURI().getPath(),
                    StandardCharsets.UTF_8);

            String[] parts = path.split("/");

            // Reject unsupported HTTP methods on known endpoints.
            if (isKnownPath(path, parts)
                    && !isAllowedMethod(path, parts, method)) {

                exchange.getResponseHeaders().set(
                        "Allow", allowedMethod(path, parts));

                sendError(exchange, 405,
                        "HTTP method not allowed for this endpoint.");
                return;
            }

            // GET /accounts
            if (path.equals("/accounts") && method.equals("GET")) {

                List<Account> accounts =
                        bankService.findAllAccountsFromDatabase();

                sendJson(exchange, 200, accounts);
                return;
            }

            // POST /accounts
            if (path.equals("/accounts") && method.equals("POST")) {

                CreateAccountRequest request =
                        readBody(exchange, CreateAccountRequest.class);

                validateAccountRequest(request);

                Account account = bankService.createAccountInDatabase(
                        request.accountHolderName,
                        request.openingBalance);

                sendJson(exchange, 201, account);
                return;
            }

            // GET /accounts/{accountNumber}
            if (parts.length == 3
                    && parts[1].equals("accounts")
                    && method.equals("GET")) {

                long accountNumber = parseAccountNumber(parts[2]);

                Account account =
                        bankService.findAccountInDatabase(accountNumber);

                if (account == null) {
                    sendError(exchange, 404, "Account not found.");
                    return;
                }

                sendJson(exchange, 200, account);
                return;
            }

            // POST /accounts/{accountNumber}/deposit
            if (parts.length == 4
                    && parts[1].equals("accounts")
                    && parts[3].equals("deposit")
                    && method.equals("POST")) {

                long accountNumber = parseAccountNumber(parts[2]);

                MoneyRequest request =
                        readBody(exchange, MoneyRequest.class);

                validateMoneyRequest(request);

                Account account = bankService.depositMoneyInDatabase(
                        accountNumber, request.amount);

                sendJson(exchange, 200, account);
                return;
            }

            // POST /accounts/{accountNumber}/withdraw
            if (parts.length == 4
                    && parts[1].equals("accounts")
                    && parts[3].equals("withdraw")
                    && method.equals("POST")) {

                long accountNumber = parseAccountNumber(parts[2]);

                MoneyRequest request =
                        readBody(exchange, MoneyRequest.class);

                validateMoneyRequest(request);

                Account account = bankService.withdrawMoneyInDatabase(
                        accountNumber, request.amount);

                sendJson(exchange, 200, account);
                return;
            }

            // POST /transfers
            if (path.equals("/transfers") && method.equals("POST")) {

                TransferRequest request =
                        readBody(exchange, TransferRequest.class);

                validateTransferRequest(request);

                bankService.transferMoneyInDatabase(
                        request.senderAccountNumber,
                        request.receiverAccountNumber,
                        request.amount);

                sendJson(exchange, 200, Map.of(
                        "message", "Transfer successful.",
                        "senderAccountNumber",
                        request.senderAccountNumber,
                        "receiverAccountNumber",
                        request.receiverAccountNumber,
                        "amount", request.amount));

                return;
            }

            sendError(exchange, 404, "Endpoint not found.");

        } catch (JsonProcessingException e) {
            sendError(exchange, 400, "Invalid JSON request body.");

        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());

        } catch (SQLException e) {

            String message = e.getMessage() == null
                    ? ""
                    : e.getMessage();

            if (message.equals("Account not found.")
                    || message.equals(
                    "Sender account or receiver account not found.")) {

                sendError(exchange, 404, message);

            } else if (message.equals("Insufficient balance.")) {

                sendError(exchange, 409, message);

            } else {
                // Log database details on the server, not in the response.
                e.printStackTrace();
                sendError(exchange, 500, "Database operation failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal server error.");

        } finally {
            exchange.close();
        }
    }

    // Validate account creation request
    private void validateAccountRequest(CreateAccountRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required.");
        }

        if (request.accountHolderName == null
                || request.accountHolderName.isBlank()) {
            throw new IllegalArgumentException(
                    "Account holder name cannot be empty.");
        }

        if (request.accountHolderName.length() > 100) {
            throw new IllegalArgumentException(
                    "Account holder name cannot exceed 100 characters.");
        }

        if (request.openingBalance == null) {
            throw new IllegalArgumentException(
                    "Opening balance is required.");
        }

        if (request.openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Opening balance cannot be negative.");
        }

        validateDecimalPlaces(request.openingBalance);
    }

    // Validate deposit and withdrawal request
    private void validateMoneyRequest(MoneyRequest request) {

        if (request == null || request.amount == null) {
            throw new IllegalArgumentException(
                    "Amount is required.");
        }

        validatePositiveAmount(request.amount);
    }

    // Validate transfer request
    private void validateTransferRequest(TransferRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required.");
        }

        if (request.senderAccountNumber <= 0
                || request.receiverAccountNumber <= 0) {
            throw new IllegalArgumentException(
                    "Account numbers must be greater than zero.");
        }

        if (request.senderAccountNumber
                == request.receiverAccountNumber) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same account.");
        }

        if (request.amount == null) {
            throw new IllegalArgumentException(
                    "Transfer amount is required.");
        }

        validatePositiveAmount(request.amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero.");
        }

        validateDecimalPlaces(amount);
    }

    private void validateDecimalPlaces(BigDecimal amount) {

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places.");
        }
    }

    private boolean isKnownPath(String path, String[] parts) {

        if (path.equals("/accounts") || path.equals("/transfers")) {
            return true;
        }

        if (parts.length == 3 && parts[1].equals("accounts")) {
            return true;
        }

        return parts.length == 4
                && parts[1].equals("accounts")
                && (parts[3].equals("deposit")
                || parts[3].equals("withdraw"));
    }

    private boolean isAllowedMethod(
            String path, String[] parts, String method) {

        String allowed = allowedMethod(path, parts);

        for (String allowedMethod : allowed.split(",\\s*")) {
            if (allowedMethod.equals(method)) {
                return true;
            }
        }

        return false;
    }

    private String allowedMethod(String path, String[] parts) {

        if (path.equals("/accounts")) {
            return "GET, POST";
        }

        if (path.equals("/transfers")) {
            return "POST";
        }

        if (parts.length == 3 && parts[1].equals("accounts")) {
            return "GET";
        }

        if (parts.length == 4 && parts[1].equals("accounts")) {
            return "POST";
        }

        return "";
    }

    private long parseAccountNumber(String value) {

        try {
            long accountNumber = Long.parseLong(value);

            if (accountNumber <= 0) {
                throw new IllegalArgumentException(
                        "Account number must be positive.");
            }

            return accountNumber;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid account number.");
        }
    }

    private <T> T readBody(
            HttpExchange exchange, Class<T> requestClass)
            throws IOException {

        try (var inputStream = exchange.getRequestBody()) {
            return objectMapper.readValue(inputStream, requestClass);
        }
    }

    private void sendJson(
            HttpExchange exchange, int status, Object response)
            throws IOException {

        byte[] responseBytes =
                objectMapper.writeValueAsBytes(response);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(status, responseBytes.length);

        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private void sendError(
            HttpExchange exchange, int status, String message)
            throws IOException {

        sendJson(exchange, status, Map.of("error", message));
    }

    public static class CreateAccountRequest {
        public String accountHolderName;
        public BigDecimal openingBalance;
    }

    public static class MoneyRequest {
        public BigDecimal amount;
    }

    public static class TransferRequest {
        public long senderAccountNumber;
        public long receiverAccountNumber;
        public BigDecimal amount;
    }
}
