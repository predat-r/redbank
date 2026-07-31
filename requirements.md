# RedBank Backend Requirements

## 1. Project Overview

RedBank is a Spring Boot REST API for a simple banking application.

The backend will provide:

- Public user registration
- Admin approval or rejection of registrations
- Email/password authentication
- OAuth2 login
- Role-based authorization
- Account-holder profile management
- Deposits, withdrawals, and transfers
- Ledger-based balance tracking
- Transaction history
- Administrative account management
- Audit logging
- OpenAPI/Swagger documentation
- Automated tests and Java code-quality checks

The frontend will be developed as a separate application in a separate repository. This repository contains only the backend.

## 2. Technology Stack

- Java Java 25
- Spring Boot 4.1.0
- Maven
- Spring Web
- Spring Security
- Spring OAuth2 Client
- Spring OAuth2 Resource Server
- Spring Data JPA
- Jakarta Validation
- PostgreSQL
- Liquibase
- Spring Boot Actuator
- Springdoc OpenAPI
- JUnit 5
- Mockito
- Spring Boot Test
- Spring Security Test
- Testcontainers
- Cucumber and Selenium or another system-test framework
- JaCoCo
- Checkstyle
- PMD
- SpotBugs
- FindSecBugs

Recommended project metadata:

```text
Group: com.redmath
Artifact: redbank
Package: com.redmath.redbank
Packaging: JAR
```

## 3. High-Level Architecture

```text
Separate frontend application
          |
          | HTTPS / JSON
          v
RedBank Spring Boot REST API
          |
          +-- Authentication and authorization
          +-- User and account-holder management
          +-- Transaction processing
          +-- Ledger and balance management
          +-- Audit logging
          |
          v
PostgreSQL
```

The backend should follow a layered or feature-oriented architecture:

```text
Controller
   |
Service
   |
Repository
   |
PostgreSQL
```

Suggested feature packages:

```text
com.redmath.redbank
├── auth
├── user
├── account
├── transaction
├── balance
├── audit
├── security
└── common
```

## 4. Roles

The system has two roles:

```text
ADMIN
ACCOUNT_HOLDER
```

Common authentication fields are stored in the `user` entity.

Roles are represented by the `ROLES` and `USER_ROLES` tables.

```text
USERS N : M ROLES through USER_ROLES
USERS 1 : 0..1 ACCOUNT_HOLDERS
```

An administrator is a user assigned the `ADMIN` role. An approved customer is assigned the `ACCOUNT_HOLDER` role and linked to an account-holder record.

## 5. Registration and Approval

### 5.1 Public registration

A visitor can register using name, email, password, and address.

New registrations must have:

```text
status = PENDING_APPROVAL
```

A pending user cannot perform banking operations.

### 5.2 Admin approval

An administrator can:

- View pending registrations
- Approve a registration
- Reject a registration
- Provide an optional rejection reason

Approval must:

1. Change the user status to `ACTIVE`.
2. Assign the `ACCOUNT_HOLDER` role.
3. Create an account-holder record.
4. Generate a unique account number.
5. Create an opening balance entry with a running balance of zero.

Rejection must:

1. Change the user status to `REJECTED`.
2. Store the rejection reason.

## 6. Authentication and Security

The backend must support:

- Email/password login
- OAuth2 login, such as Google or GitHub
- JWT-protected REST endpoints
- Role-based authorization
- Password hashing
- Stateless API authentication where practical

OAuth provider information may be stored directly on the user record for this assignment. A separate OAuth identity table is not required.

Passwords must never be stored or returned in plain text.

All actions except public registration, login, OAuth callbacks, health endpoints, and Swagger endpoints must require authentication.

Authorization rules:

```text
ADMIN
- View all users and account holders
- Approve or reject registrations
- Create, update, freeze, activate, and close accounts
- View all transactions and balances
- Perform administrative banking operations
- View audit logs

ACCOUNT_HOLDER
- View and update own profile
- View own account
- View own balance and ledger
- View own transactions
- Deposit funds
- Withdraw funds
- Transfer funds to another account
```

An account holder must never access another holder's private profile, balance, or transaction history.

## 7. Main Modules

### Module 1: Authentication and Account Management

Includes registration, login, OAuth2 login, JWT security, admin approval and rejection, user profile management, account-holder creation, account activation/freezing/closure, tests, Swagger documentation, and system tests.

### Module 2: Transaction Management

Includes deposits, withdrawals, internal transfers, validation, insufficient-funds checks, transaction history, transaction statuses, tests, Swagger documentation, and system tests.

### Module 3: Balance and Ledger Management

Includes immutable balance entries, debit and credit indicators, running-balance calculation, current-balance retrieval, balance history, ledger indexing, concurrency controls, tests, Swagger documentation, and system tests.

Recommended implementation order:

```text
1. Authentication and Account Management
2. Transaction Management
3. Balance and Ledger Management
```

## 8. Data Model

The backend will use these seven tables:

```text
users
roles
user_roles
account_holders
bank_transactions
balances
audit_logs
```

### 8.1 USERS

```text
USERS
-----
id
email
password_hash
name
address
status
oauth_provider
oauth_provider_id
rejection_reason
approved_by_user_id
approved_at
created_at
updated_at
```

Suggested status values:

```text
PENDING_APPROVAL
ACTIVE
REJECTED
DEACTIVATED
```

### 8.2 ROLES

```text
ROLES
-----
id
name
created_at
```

Initial values:

```text
ADMIN
ACCOUNT_HOLDER
```

`name` must be unique.

### 8.3 USER_ROLES

```text
USER_ROLES
----------
user_id
role_id
assigned_at
```

Primary key:

```text
(user_id, role_id)
```

This table implements the many-to-many relationship between users and roles.

### 8.4 ACCOUNT_HOLDERS

```text
ACCOUNT_HOLDERS
---------------
id
user_id
account_number
currency
account_status
approved_at
created_at
updated_at
```

Suggested status values:

```text
ACTIVE
FROZEN
CLOSED
```

Rules:

- `user_id` must be unique.
- `account_number` must be unique and immutable.
- The related user must have the `ACCOUNT_HOLDER` role.
- Current balance is not stored here.

### 8.5 BANK_TRANSACTIONS

```text
BANK_TRANSACTIONS
-----------------
id
transaction_reference
source_account_holder_id
destination_account_holder_id
type
description
amount
status
created_by_user_id
original_transaction_id
created_at
completed_at
```

Suggested types:

```text
DEPOSIT
WITHDRAWAL
TRANSFER
REVERSAL
```

Suggested statuses:

```text
PENDING
COMPLETED
CANCELLED
REVERSED
```

Completed transactions must not be edited or physically deleted.

### 8.6 BALANCES

This table acts as the immutable account ledger.

```text
BALANCES
--------
id
account_holder_id
transaction_id
entry_date
amount
indicator
running_balance
```

Indicator values:

```text
DEBIT
CREDIT
```

Rules:

- A deposit creates one credit row.
- A withdrawal creates one debit row.
- A transfer creates a debit row for the sender and a credit row for the receiver.
- Rows are immutable.
- `running_balance` stores the account balance after the entry.
- Every approved account begins with an opening row whose running balance is zero.

Current balance query:

```sql
SELECT running_balance
FROM balances
WHERE account_holder_id = :accountHolderId
ORDER BY id DESC
LIMIT 1;
```

Required index:

```sql
CREATE INDEX idx_balances_account_latest
ON balances (account_holder_id, id DESC);
```

### 8.7 AUDIT_LOGS

```text
AUDIT_LOGS
----------
id
actor_user_id
action
entity_type
entity_id
details
created_at
```

---

## 9. Entity Relationships

```text
USERS N : M ROLES through USER_ROLES
USERS 1 : 0..1 ACCOUNT_HOLDERS
USERS 1 : N BANK_TRANSACTIONS through created_by_user_id
USERS 1 : N AUDIT_LOGS

ACCOUNT_HOLDERS 1 : N outgoing BANK_TRANSACTIONS
ACCOUNT_HOLDERS 1 : N incoming BANK_TRANSACTIONS
ACCOUNT_HOLDERS 1 : N BALANCES

BANK_TRANSACTIONS 1 : N BALANCES
BANK_TRANSACTIONS 1 : 0..1 original BANK_TRANSACTIONS
```

Simplified ERD:

```text
USERS >------< USER_ROLES >------< ROLES
  |
  | 1 : 0..1
  v
ACCOUNT_HOLDERS
  | \
  |  \ source/destination
  |   v
  |  BANK_TRANSACTIONS
  |          |
  |          | 1 : N
  |          v
  +-------> BALANCES

USERS --------------------------< AUDIT_LOGS
```

---

## 10. REST API Endpoints

### Authentication

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
PUT  /api/auth/password
```

OAuth2:

```http
GET /oauth2/authorization/google
GET /oauth2/authorization/github
GET /login/oauth2/code/{provider}
```

### Current user profile

```http
GET   /api/users/me
PATCH /api/users/me
```

### Admin registration management

```http
GET  /api/admin/registrations
GET  /api/admin/registrations/{userId}
POST /api/admin/registrations/{userId}/approve
POST /api/admin/registrations/{userId}/reject
```

### Admin user management

```http
GET  /api/admin/users
GET  /api/admin/users/{userId}
POST /api/admin/users/{userId}/activate
POST /api/admin/users/{userId}/deactivate
```

### Account management

```http
GET /api/accounts/me
```

Admin:

```http
GET  /api/accounts
GET  /api/accounts/{accountNumber}
POST /api/accounts
POST /api/accounts/{accountNumber}/activate
POST /api/accounts/{accountNumber}/freeze
POST /api/accounts/{accountNumber}/close
```

### Balance and ledger

```http
GET /api/accounts/me/balance
GET /api/accounts/me/balance-entries
GET /api/accounts/{accountNumber}/balance
GET /api/accounts/{accountNumber}/balance-entries
```

### Transactions

```http
GET /api/accounts/me/transactions
GET /api/accounts/me/transactions/{transactionReference}
GET /api/accounts/{accountNumber}/transactions
GET /api/accounts/{accountNumber}/transactions/{transactionReference}
```

### Deposits

```http
POST /api/accounts/me/deposits
```

### Withdrawals

```http
POST /api/accounts/me/withdrawals
```

### Transfers

```http
POST /api/accounts/me/transfers
```

### Reversal

```http
POST /api/admin/transactions/{transactionReference}/reverse
```

### Audit logs

```http
GET /api/admin/audit-logs
GET /api/admin/audit-logs/{id}
```

## 11. Financial Rules

- Money must use `BigDecimal`.
- Amounts must be greater than zero.
- Negative balances are not allowed.
- Deposits, withdrawals, transfers, reversals, and ledger entries must execute within database transactions.
- A completed financial transaction must never be edited or deleted.
- Financial corrections must use reversal transactions.
- Frozen or closed accounts cannot initiate banking operations.
- Closed accounts cannot receive new transactions.
- Initial implementation supports one currency, preferably PKR.
- Transaction references and account numbers must be unique.
- Ledger entries must be immutable.

## 12. Balance Calculation

The current balance is obtained from the latest ledger entry:

```sql
SELECT running_balance
FROM balance_entry
WHERE account_holder_id = :accountHolderId
ORDER BY id DESC
LIMIT 1;
```

New running balance:

```text
Credit: new balance = previous balance + amount
Debit:  new balance = previous balance - amount
```

For transfers, sender and receiver entries must be committed atomically.

Concurrency must be controlled so two simultaneous transactions cannot calculate from the same previous balance.

## 13. Validation

Registration:

- Name is required.
- Email is required and valid.
- Email must be unique.
- Password must satisfy the configured password policy.
- Address is required.

Transactions:

- Amount must be positive.
- Source and destination accounts must be valid for the transaction type.
- Source and destination must differ for transfers.
- Source account must have sufficient funds.
- Accounts must be active.
- Description must have a reasonable maximum length.

## 14. OpenAPI and Swagger

- Swagger UI must be available in non-production environments.
- Every endpoint must document request and response models.
- Authentication requirements must be visible.
- Role restrictions should be described.
- Standard error responses must be documented.

## 15. Database Migrations

Liquibase must:

- Create all tables.
- Create foreign keys and unique constraints.
- Create ledger indexes.
- Seed the predefined administrator.
- Never store a plain-text admin password.
- Version all schema changes.

## 16. Testing Requirements

### Unit tests

Cover service logic, validation, authorization, balance calculations, deposits, withdrawals, transfers, and reversals.

### Integration tests

Use PostgreSQL Testcontainers for repositories, queries, constraints, rollbacks, and latest-balance lookup.

### API tests

Cover validation, success responses, error responses, authentication, authorization, and ownership restrictions.

### System tests

At minimum:

1. User registers.
2. Admin approves registration.
3. User logs in.
4. User deposits funds.
5. User withdraws funds.
6. User transfers funds.
7. User views balance and transaction history.
8. Unauthorized access is rejected.
9. Insufficient-funds transfer is rejected.
10. Admin freezes or closes an account.

Cucumber/Selenium tests should be isolated from normal unit tests using tags or a separate Maven profile.

## 17. Code Quality

The Maven build must include:

- Checkstyle
- PMD
- SpotBugs
- FindSecBugs
- JaCoCo

Recommended command:

```bash
./mvnw verify
```

## 18. Non-Functional Requirements

- Use HTTPS in deployed environments.
- Do not expose persistence entities directly when DTOs are appropriate.
- Do not log passwords, tokens, or keys.
- Use pagination for list endpoints.
- Use UTC timestamps.
- Use database constraints in addition to application validation.
- Return consistent error responses.
- Add indexes for common lookup fields.
- Keep transaction and ledger history immutable.
- Separate local, test, CI, and production configuration.
- Do not expose sensitive Actuator endpoints publicly.

## 19. Initial Delivery Scope

- Public registration
- Admin approval and rejection
- Predefined administrator
- Email/password login
- OAuth2 login
- JWT-protected APIs
- Account-holder profile
- Account status management
- Deposits
- Withdrawals
- Transfers
- Transaction history
- Ledger-based running balance
- Admin reversal
- Audit logging
- Swagger/OpenAPI
- Liquibase migrations
- Unit, integration, and system tests
- Java quality checks
- CI-compatible Maven build

## 20. Out of Scope

- Real payment-rail integration
- Real external bank deposits or withdrawals
- Currency conversion
- Interest calculations
- Loans
- Cards
- Multiple accounts per user unless later required
- Separate OAuth identity table
- Reversal-request workflow
- Physical deletion of completed financial history
- Frontend implementation in this repository
