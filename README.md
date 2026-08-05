# RedBank Backend

RedBank is a Spring Boot backend for a digital banking platform. It handles user authentication,
registration approval workflows, account management, transaction processing (deposits, withdrawals,
transfers), running balance ledgers, scheduled background cleanup jobs, audit logging, and telemetry
observability.

---

## Overall System Flow

The application follows a lifecycle that connects user registration, admin approvals, token
authentication, transaction execution, and background reconciliation.

### 1. User Registration and Onboarding

1. **Submit Registration**: A user submits personal details (name, email, phone, address, password)
   via `POST /api/auth/register`.
2. **Pending Approval**: The user account is saved with `PENDING_APPROVAL` status and assigned the
   `ROLE_PENDING_USER` authority. At this stage, the user cannot execute financial transactions.
3. **Admin Review**: An administrator retrieves pending registration requests using
   `GET /api/admin/registrations/pending`.
4. **Approval / Rejection**:
    - **Approval**: Admin calls `POST /api/admin/registrations/{userId}/approve`. The system updates
      the user status to `ACTIVE`, assigns `ROLE_ACCOUNT_HOLDER`, creates an account holder profile,
      and auto-generates a unique account number (`RB-XXXXXX`).
    - **Rejection**: Admin calls `POST /api/admin/registrations/{userId}/reject` with a rejection
      reason. The user status is set to `REJECTED`.

### 2. Authentication and Token Management

1. **User Login**: The user authenticates via `POST /api/auth/login` using email and password.
2. **JWT Issuance**: Upon successful credential validation, the server returns an **access token**
   (short-lived) and a **refresh token** (longer lived) containing claims such as `userId`, `email`,
   and GrantedAuthorities (`ROLE_ACCOUNT_HOLDER` or `ROLE_ADMIN`).
3. **Authenticated Requests**: Clients attach `Authorization: Bearer <access-token>` header to all
   subsequent API requests.
4. **Token Refresh**: When the access token expires, clients submit the refresh token to
   `POST /api/auth/refresh` to obtain a new access token without re-entering credentials.

### 3. Funding and Transaction Execution

1. **Admin Deposit**: An admin seeds or credits funds to an account holder's account via
   `POST /api/admin/deposits`.
2. **Transaction Processing**:
    - For **withdrawals** (`POST /api/accounts/me/withdrawals`) or **transfers**
      (`POST /api/accounts/me/transfers`):
        1. The system validates request parameters and verifies that initiating/destination accounts
           are `ACTIVE`.
        2. A database pessimistic write lock (`PESSIMISTIC_WRITE`) is acquired on the account holder
           entity to guarantee atomic execution under concurrent requests.
        3. The system queries `BalanceService` to confirm sufficient funds before deducting.
        4. The `BankTransaction` record is saved with status `COMPLETED`.
        5. Corresponding credit and debit entries are recorded in the `Balance` ledger table,
           calculating new running balances.

### 4. Balance and Ledger Monitoring

1. Account holders query their current balance and latest ledger entry using
   `GET /api/balance/me/latest`.
2. Administrators inspect complete ledger history and audit records for any account via
   `GET /api/admin/balance/{accountId}/ledger`.

### 5. Automated Schedulers and Audit

- **Balance Reconciliation**: Cron job calculates account totals from ledger entries and flags any
  balance discrepancies.
- **Registration Cleanup**: Cron job automatically rejects pending registrations that have remained
  unapproved for over 30 days.
- **Stale Transaction Cleanup**: Cron job cancels pending transactions that remain uncompleted after
  30 minutes.
- **Audit Logging**: All administrative deposits and registration decisions write records to the
  `audit_logs` database table.

---

## Detailed Endpoint Reference

### Base URL

- API Base URL: `http://localhost:8080/api`
- OpenAPI Specification: `http://localhost:8080/v3/api-docs`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui/index.html`
- Grafana Telemetry Dashboard: `http://localhost:3000`

---

### 1. Authentication Endpoints (`/api/auth`)

| Method | Endpoint                    | Access Level  | Description                                                          |
|:-------|:----------------------------|:--------------|:---------------------------------------------------------------------|
| `POST` | `/api/auth/register`        | Public        | Registers a new user (creates account in `PENDING_APPROVAL` state).  |
| `POST` | `/api/auth/login`           | Public        | Authenticates credentials and returns JWT access and refresh tokens. |
| `POST` | `/api/auth/refresh`         | Public        | Issues a new access token using a valid refresh token.               |
| `POST` | `/api/auth/change-password` | Authenticated | Allows an authenticated user to change their current password.       |

---

### 2. Account Holder Endpoints (`/api/accounts/me` & `/api/balance/me`)

*Requires `ROLE_ACCOUNT_HOLDER` bearer token.*

| Method | Endpoint                        | Query Parameters                                                           | Description                                                                  |
|:-------|:--------------------------------|:---------------------------------------------------------------------------|:-----------------------------------------------------------------------------|
| `GET`  | `/api/accounts/me/transactions` | `page` (default 0), `size` (default 10), `sort` (default `createdAt,desc`) | Retrieves paginated transaction history for the authenticated user.          |
| `POST` | `/api/accounts/me/withdrawals`  | None                                                                       | Executes a cash withdrawal from the user's account.                          |
| `POST` | `/api/accounts/me/transfers`    | None                                                                       | Transfers funds from the user's account to a destination account number.     |
| `GET`  | `/api/balance/me/latest`        | None                                                                       | Returns the current running balance and latest ledger entry for the account. |

---

### 3. Administrative Endpoints (`/api/admin`)

*Requires `ROLE_ADMIN` bearer token.*

#### Registration Management

| Method | Endpoint                                    | Description                                                                          |
|:-------|:--------------------------------------------|:-------------------------------------------------------------------------------------|
| `GET`  | `/api/admin/registrations/pending`          | Lists all pending user registration requests.                                        |
| `POST` | `/api/admin/registrations/{userId}/approve` | Approves user registration, creates bank account, and assigns `ROLE_ACCOUNT_HOLDER`. |
| `POST` | `/api/admin/registrations/{userId}/reject`  | Rejects user registration with a provided reason string.                             |

#### Financial & Transaction Management

| Method | Endpoint                                           | Query Parameters       | Description                                                                                   |
|:-------|:---------------------------------------------------|:-----------------------|:----------------------------------------------------------------------------------------------|
| `POST` | `/api/admin/deposits`                              | None                   | Deposits funds directly into a specified account number.                                      |
| `GET`  | `/api/admin/transactions`                          | `page`, `size`, `sort` | Fetches a paginated list of all system transactions.                                          |
| `GET`  | `/api/admin/transactions/{id}`                     | None                   | Retrieves detailed transaction information by ID (includes source/destination owner details). |
| `GET`  | `/api/admin/transactions/reference/{reference}`    | None                   | Fetches transaction details by transaction reference string (e.g. `TXN-XXXXXXXXXXXX`).        |
| `GET`  | `/api/admin/accounts/{accountNumber}/transactions` | `page`, `size`, `sort` | Retrieves transaction history for a specific account number.                                  |

#### Balance & Audit Logs

| Method | Endpoint                                | Query Parameters       | Description                                                             |
|:-------|:----------------------------------------|:-----------------------|:------------------------------------------------------------------------|
| `GET`  | `/api/admin/balance/{accountId}/latest` | None                   | Retrieves the latest balance record for any account ID.                 |
| `GET`  | `/api/admin/balance/{accountId}/ledger` | `page`, `size`, `sort` | Returns the complete balance ledger history for a specified account ID. |
| `GET`  | `/api/admin/audit-logs`                 | `page`, `size`, `sort` | Retrieves system audit logs recording administrative actions.           |

---

## Technology Stack

- **Java**: Version 25
- **Framework**: Spring Boot 4.1.0 (Spring Security, Spring Data JPA, Spring WebMVC)
- **Database & Migrations**: PostgreSQL with Liquibase migrations
- **Containerization**: Spring Boot Docker Compose integration, Docker Compose
- **Observability**: OpenTelemetry, Micrometer, Grafana LGTM (Grafana, Loki, Tempo, Mimir)
- **Code Quality Tools**: PMD 7, Checkstyle (Google Java Style), SpotBugs, JaCoCo, Pitest

---

## Repository Structure

```text
com.redmath.redbank
├── account         # Account holder domain, service, and admin approval controllers
│   └── admin
├── audit           # Audit logging service, repository, and action targets
├── auth            # User registration, login, token refresh, and security filters
├── balance         # Balance entity, running balance ledger, and balance controllers
│   └── admin
├── common          # Custom exception classes, JWT utilities, and shared helpers
├── observability   # Telemetry setup, tracing, and metric instrumentation
├── scheduler       # Cron jobs (balance reconciliation, stale registration & txn cleanup)
├── transaction     # Transaction entity, service, repository, DTOs, and endpoints
│   ├── admin
│   ├── dto
│   └── request
└── user            # User domain model, role definitions, and user repository
    ├── admin
    └── role
```

---

## Getting Started

### Prerequisites

- Java 25 JDK
- Maven 3.9+ (or `./mvnw` wrapper)
- Docker and Docker Compose

### Environment Configuration

Set database credentials and security keys in your environment or in a `.env` file:

```env
POSTGRES_USERNAME=redbank_user
POSTGRES_PASSWORD=redbank_password
POSTGRES_DB=redbank
JWT_SECRET=your-256-bit-secret-key-here
```

### Running Locally

1. Start the Docker containers:
   ```bash
   docker compose up -d
   ```
2. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```

Spring Boot Docker Compose will connect to PostgreSQL, run Liquibase database migrations, and make
the application accessible at `http://localhost:8080`.

---

## Testing and Verification Commands

Execute the test suite and quality plugins with the following Maven commands:

```bash
# Run unit and integration test suite
./mvnw test

# Verify JaCoCo code coverage threshold
./mvnw jacoco:check

# Run PMD static code analysis
./mvnw pmd:check

# Check Google Java Style formatting
./mvnw checkstyle:check

# Run SpotBugs pattern analysis
./mvnw spotbugs:check

# Run Pitest mutation coverage
./mvnw pitest:mutationCoverage
```
