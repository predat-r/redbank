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
   `GET /api/admin/registrations`.
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

| Method | Endpoint                        | Access Level                   | Description                                                          |
|:-------|:--------------------------------|:-------------------------------|:---------------------------------------------------------------------|
| `POST` | `/api/auth/register`            | Public                         | Registers a new user (creates account in `PENDING_APPROVAL` state).  |
| `POST` | `/api/auth/login`               | Public                         | Authenticates credentials and returns JWT access and refresh tokens. |
| `POST` | `/api/auth/refresh`             | Public                         | Issues a new access token using a valid refresh token.               |
| `POST` | `/api/auth/logout`              | Public                         | Revokes the supplied refresh token.                                  |
| `PUT`  | `/api/auth/password`            | Authenticated                  | Changes the authenticated user's password.                           |
| `GET`  | `/api/auth/registration-status` | Pending user or account holder | Returns the authenticated user's registration status.                |

---

### 2. Account Holder Endpoints (`/api/accounts` & `/api/balance/me`)

*Requires `ROLE_ACCOUNT_HOLDER` bearer token.*

The account-name lookup also permits `ROLE_ADMIN`.

| Method  | Endpoint                                | Query Parameters                                                           | Description                                                                  |
|:--------|:----------------------------------------|:---------------------------------------------------------------------------|:-----------------------------------------------------------------------------|
| `GET`   | `/api/accounts/me`                      | None                                                                       | Returns the authenticated user's account-holder profile.                     |
| `GET`   | `/api/accounts/name/{accountNumber}`    | None                                                                       | Returns the name and account number for an account holder.                   |
| `PATCH` | `/api/accounts/freeze/me`               | None                                                                       | Freezes the authenticated user's account.                                    |
| `PATCH` | `/api/accounts/deactivate/me`           | None                                                                       | Deactivates the authenticated user's account.                                |
| `GET`   | `/api/accounts/me/transactions`         | `page` (default 0), `size` (default 10), `sort` (default `createdAt,desc`) | Retrieves paginated transaction history for the authenticated user.          |
| `POST`  | `/api/accounts/me/withdrawals`          | None                                                                       | Executes a cash withdrawal from the user's account.                          |
| `POST`  | `/api/accounts/me/transfers`            | None                                                                       | Transfers funds from the user's account to a destination account number.     |
| `GET`   | `/api/balance/me/latest`                | None                                                                       | Returns the current running balance and latest ledger entry for the account. |

---

### 3. Administrative Endpoints (`/api/admin`)

*Requires `ROLE_ADMIN` bearer token.*

#### Registration Management

| Method | Endpoint                                    | Description                                                                          |
|:-------|:--------------------------------------------|:-------------------------------------------------------------------------------------|
| `GET`  | `/api/admin/registrations`                  | Lists all pending user registration requests.                                        |
| `GET`  | `/api/admin/registrations/{userId}`         | Retrieves a pending registration by user ID.                                         |
| `POST` | `/api/admin/registrations/{userId}/approve` | Approves user registration, creates bank account, and assigns `ROLE_ACCOUNT_HOLDER`. |
| `POST` | `/api/admin/registrations/{userId}/reject`  | Rejects user registration with a provided reason string.                             |

#### User & Account Management

| Method  | Endpoint                                     | Description                                    |
|:--------|:---------------------------------------------|:-----------------------------------------------|
| `GET`   | `/api/admin/users`                           | Retrieves a paginated list of users.           |
| `GET`   | `/api/admin/users/{userId}`                  | Retrieves a user by ID.                        |
| `GET`   | `/api/admin/accounts`                        | Retrieves a paginated list of account holders. |
| `GET`   | `/api/admin/accounts/{accountId}`            | Retrieves an account holder by ID.             |
| `PATCH` | `/api/admin/accounts/freeze/{accountId}`     | Freezes an account holder's account.           |
| `PATCH` | `/api/admin/accounts/deactivate/{accountId}` | Deactivates an account holder's account.       |

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
| `GET`  | `/api/admin/audit-logs/{auditLogId}`    | None                   | Retrieves a single audit log by ID.                                     |

---

## Technology Stack

- **Java**: Version 25
- **Framework**: Spring Boot 4.1.0 (Spring Security, Spring Data JPA, Spring WebMVC)
- **Database & Migrations**: PostgreSQL with Liquibase migrations
- **Containerization**: Spring Boot Docker Compose integration, Docker Compose
- **Observability**: OpenTelemetry, Micrometer, Grafana LGTM (Grafana, Loki, Tempo, Mimir)
- **Code Quality Tools**: SonarCloud, PMD 7, Checkstyle (Google Java Style), SpotBugs, JaCoCo, Pitest

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

The development profile requires database credentials, a BCrypt hash for the seeded administrator,
and a Base64-encoded RSA key pair for JWT signing:

```env
SPRING_PROFILES_ACTIVE=dev
POSTGRES_USERNAME=redbank_user
POSTGRES_PASSWORD=redbank_password
ADMIN_PASSWORD_HASH='your-bcrypt-password-hash'
JWT_PRIVATE_KEY='your-base64-pkcs8-private-key'
JWT_PUBLIC_KEY='your-base64-x509-public-key'
```

`POSTGRES_DB` is fixed to `redbank` in `compose.yaml` and is not configurable through an environment
variable.

Docker Compose reads `.env` automatically, but a separately launched Spring Boot process does not.
If the values are stored in `.env`, export them into the current shell before running Maven:

```bash
set -a
source .env
set +a
```

### Running Locally

1. Start the Docker containers:
   ```bash
   docker compose up -d
   ```
2. Export the environment variables as described above, then start the application with the `dev`
   profile active:
   ```bash
   ./mvnw spring-boot:run
   ```

Spring Boot Docker Compose will connect to PostgreSQL, run Liquibase database migrations, and make
the application accessible at `http://localhost:8080`.

---

## Testing and Verification Commands

The GitHub Actions `Verify` workflow runs the Maven verification lifecycle with the `test` profile
on every push or manual dispatch, including SonarCloud analysis while skipping Pitest.

Individual checks can be run locally with the following Maven commands:

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
