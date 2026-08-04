# RedBank Backend

Spring Boot backend for RedBank, a banking application covering user registration, account
management, balances, transactions, and administrative workflows.

## API and authentication

- API base URL: `http://localhost:8080/api`
- OpenAPI specification: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Authentication: stateless JWT bearer tokens, issued through `/api/auth/login` and renewed through
  `/api/auth/refresh`
- Authorization: role-based access for `ADMIN`, `ACCOUNT_HOLDER`, and `PENDING_USER`

Send authenticated requests with `Authorization: Bearer <access-token>`.

## Main modules

- `auth` and `security` — registration, login, token lifecycle, and access control
- `user` and `account_holder` — users, registration review, and bank accounts
- `balance` — current balances and balance ledger
- `transaction` — deposits, withdrawals, transfers, and transaction history
- `audit` — administrative audit logs
- `scheduler` — reconciliation and scheduled cleanup jobs
- `observability` — OpenTelemetry integration

## Observability

The local Docker Compose stack runs Grafana LGTM for logs, metrics, and traces:

- Grafana: `http://localhost:3000`


