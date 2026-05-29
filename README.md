# Koins Fintech Backend Assessment

A production-grade fintech backend built with Java Spring Boot, implementing a loan and wallet management system for Koins Microfinance Bank.

## Tech Stack

- **Java 21** + **Spring Boot 3.5.0**
- **PostgreSQL 16** — primary database
- **Spring Data JPA** + **Hibernate** — ORM
- **Spring Security** + **JWT** — stateless authentication
- **Argon2** — password hashing (Standard Hashing fintech grade)
- **Paystack** — payment gateway integration
- **Spring Mail** — email notifications
- **Spring Scheduler** — cron jobs
- **Swagger/OpenAPI 3** — API documentation
- **Docker** + **Docker Compose** — containerization
- **JUnit 5** + **Mockito** — unit testing

---

## Features

- User registration with automatic wallet creation
- JWT-based stateless authentication
- Argon2 password hashing
- OTP-based password recovery with expiry
- Wallet funding via Paystack payment gateway
- HMAC-SHA512 verified Paystack webhooks
- Loan application, approval, disbursement, and repayment
- Loan business rules (3× wallet balance cap, funded wallet requirement)
- Double-entry transaction logging in kobo denomination
- Role-based access control (USER / ADMIN)
- Scheduled jobs: overdue loan marking + repayment reminders
- Email notifications: OTP, loan approval, repayment reminder, repayment success
- Swagger UI for interactive API documentation

---

## Prerequisites

- Java 21+
- Docker + Docker Compose
- Maven 3.9+

---

## Setup Instructions

### Option 1 — Docker Compose (recommended)

```bash
git clone https://github.com/chibuike-kt/koins-assessment.git
cd koins-assessment
docker-compose up --build
```

App will be available at `http://localhost:8080`

### Option 2 — Local Development

**1. Start PostgreSQL**

```bash
docker run --name koins-postgres \
  -e POSTGRES_USER=koins \
  -e POSTGRES_PASSWORD=koins1234 \
  -e POSTGRES_DB=koins_db \
  -p 5432:5432 \
  -d postgres:16
```

**2. Configure environment**

Create a `.env` file or export these variables:

```env
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-app-password
JWT_SECRET=koins-super-secret-jwt-key-that-is-long-enough-for-hs256-algorithm
PAYSTACK_SECRET_KEY=sk_test_your_key_here
```

**3. Run the application**

```bash
mvn spring-boot:run
```

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/koins_db` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `koins` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `koins1234` |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | fallback value |
| `PAYSTACK_SECRET_KEY` | Paystack secret key | `sk_test_placeholder` |
| `MAIL_USERNAME` | Gmail address for notifications | — |
| `MAIL_PASSWORD` | Gmail app password | — |

---

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## API Endpoints

### Authentication
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login |
| POST | `/api/v1/auth/logout` | Public | Logout |
| POST | `/api/v1/auth/forgot-password` | Public | Request OTP |
| POST | `/api/v1/auth/reset-password` | Public | Reset password with OTP |
| POST | `/api/v1/auth/resend-otp` | Public | Resend OTP |
| GET | `/api/v1/auth/profile` | USER | Get profile |
| PUT | `/api/v1/auth/profile` | USER | Update profile |

### Wallet
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/wallet/balance` | USER | Get wallet balance |
| POST | `/api/v1/wallet/fund` | USER | Initiate wallet funding |
| GET | `/api/v1/wallet/transactions` | USER | Get transaction history |

### Loans
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/loans/apply` | USER | Apply for loan |
| PUT | `/api/v1/loans/{id}/approve` | ADMIN | Approve loan |
| PUT | `/api/v1/loans/{id}/disburse` | ADMIN | Disburse loan |
| POST | `/api/v1/loans/repay` | USER | Repay loan |
| GET | `/api/v1/loans/my-loans` | USER | Get user loans |
| GET | `/api/v1/loans/{id}` | USER | Get loan by ID |
| GET | `/api/v1/loans` | ADMIN | List all loans |

### Transactions
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/transactions` | USER | List all transactions |
| GET | `/api/v1/transactions/{id}` | USER | Get transaction by ID |

### Webhooks
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/webhooks/paystack` | Public | Paystack payment webhook |

---

## Business Rules

- Wallet is created automatically on signup with zero balance
- All amounts are stored in **kobo** (1 NGN = 100 kobo)
- Loan amount cannot exceed **3× wallet balance**
- Wallet must be funded before loan application
- Only one active (disbursed) loan per user at a time
- Loan interest rate is fixed at **5%**
- Wallet balance cannot go negative
- All financial operations create transaction logs
- Overdue loans are marked `DEFAULTED` daily at midnight
- Repayment reminders sent 3 days before due date at 9am

---

## Running Tests

```bash
mvn test
```

11 unit tests covering auth service and loan service critical paths.

---

## Database

Schema is auto-managed by Hibernate (`ddl-auto: update`).

Tables: `users`, `wallets`, `loans`, `transactions`

---

## Webhook Setup

To test Paystack webhooks locally, expose your local server using [ngrok](https://ngrok.com):

```bash
ngrok http 8080
```

Set the generated URL + `/api/v1/webhooks/paystack` as your Paystack webhook URL in the dashboard.

---

## Project Structure
src/main/java/com/koins/
├── config/          # Security, JWT, OpenAPI, scheduler beans
├── controller/      # REST controllers
├── dto/             # Request and response DTOs
├── entity/          # JPA entities
├── enums/           # Status enums
├── exception/       # Custom exceptions and global handler
├── repository/      # JPA repositories
├── security/        # JWT filter and UserDetails implementation
├── service/         # Business logic interfaces and implementations
├── util/            # OTP and reference generators
└── webhook/         # Paystack webhook handler
