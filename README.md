
# Banking Management System

A database-backed banking application built with Core Java, JDBC, Oracle Database, and REST APIs using Java's built-in HttpServer.

## Features

- Create new bank accounts
- Retrieve an account by account number
- List all accounts
- Deposit money
- Withdraw money
- Transfer money between accounts
- Validate requests and handle API errors
- Persist account data in Oracle Database
- Handle fund transfers using database transactions

## Tech Stack

- Java 25
- Maven
- Oracle Database 21c
- JDBC
- Java HttpServer
- Jackson for JSON processing
- Postman for API testing

## Architecture

The application separates responsibilities into three main packages:

- `banking` — Account model, application entry point, API server, and business service
- `banking.database` — Database connection, data access operations, and database test utilities
- `banking.handler` — HTTP routing, request processing, validation, and JSON responses

## Prerequisites

- JDK 25
- Maven support
- Oracle Database with a configured pluggable database
- Postman (for API testing)

## Database Configuration

Configure these environment variables before starting the application:

- `BANKING_DB_URL`
- `BANKING_DB_USERNAME`
- `BANKING_DB_PASSWORD`

Example URL format:

`jdbc:oracle:thin:@//localhost:1521/your-service-name`

Replace `your-service-name` with your configured Oracle service name.

Do not commit database passwords or other secrets to GitHub.

The database user must have the necessary permissions and the required account table and sequence must exist.

## Build

From the project root, run:

```bash
mvn clean compile
```

## Run

Start the API using the configured main class:

`banking.BankingApiServer`

The server currently uses port `8080`.

## REST API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/accounts` | List all accounts |
| POST | `/accounts` | Create an account |
| GET | `/accounts/{id}` | Retrieve an account |
| POST | `/accounts/{id}/deposit` | Deposit money |
| POST | `/accounts/{id}/withdraw` | Withdraw money |
| POST | `/transfers` | Transfer money |

Base URL for local testing:

`http://localhost:8080`

Use Postman to send requests and inspect the JSON responses and HTTP status codes.

## Testing

The API has been manually tested with Postman for account operations, deposits, withdrawals, transfers, validation errors, missing accounts, and insufficient balances.

## Security Notes

- Database credentials are read from environment variables.
- Do not commit passwords, tokens, or private configuration.
- This learning project does not currently implement API authentication.
- Do not expose the API to untrusted networks or use it for real banking transactions.

## Future Improvements

- Add automated unit and integration tests
- Add authentication and authorization
- Improve API documentation
- Add structured logging

## Author

Developed as a Java backend learning and portfolio project.
