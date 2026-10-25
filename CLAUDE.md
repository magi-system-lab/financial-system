# CLAUDE.md — financial-system-v3

Spring Boot 3 backend for the Financial Debt Management System. Implements Hexagonal Architecture (Ports & Adapters) with PostgreSQL persistence, multi-provider account statement parsing (PDF/Excel/OCR), and a dual interface: REST API + Thymeleaf web UI.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.3 |
| Language | Java 21 |
| Build | Maven 3.9.6 (mvnw wrapper) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI v2 (Swagger) |
| PDF Parsing | Apache PDFBox 3.0.3 |
| OCR | Tess4j 5.17.0 (Tesseract, Spanish) |
| Excel Parsing | Apache POI 5.4.0 |
| Web UI | Thymeleaf |
| Security | Spring Security (all endpoints permitted — no auth) |

---

## Architecture: Hexagonal (Ports & Adapters)

```
domain/                        ← Pure Java, zero framework dependencies
  model/                       ← Domain entities
  dto/                         ← Data transfer objects
  enums/                       ← Enumerations
  exceptions/                  ← Custom domain exceptions
  utils/                       ← Domain utilities
  application/port/
    in/                        ← Input ports (use case interfaces)
    out/                       ← Output ports (repository interfaces)

application/service/           ← Implement input ports; orchestrate business logic

infrastructure/
  entity/                      ← JPA entities (@Entity, @Table)
  persistence/                 ← JPA repositories (JpaRepository)
  adapter/                     ← Implement output ports; bridge JPA ↔ domain
  mapper/                      ← MapStruct mappers (entity ↔ domain model)
  model/                       ← HTTP request/response models
  web/                         ← REST controllers (@RestController)
  web/view/                    ← Thymeleaf view controllers
  configuration/               ← Spring @Configuration classes
```

**Key invariant:** Domain classes must never import Spring, JPA, or Jackson annotations.

---

## Directory Structure

```
src/main/java/com/jimm0063/magi/debt/management/debtmanagementltesystem/
├── DebtManagementLteSystemApplication.java   # Entry point
├── domain/
│   ├── model/              # Debt, DebtAccount, DebtSysUser, FinancialProvider,
│   │                       # FinancialProviderCatalog, FixedExpense, FixedExpenseCatalog,
│   │                       # Payment, Receivable, ReceivablePayment, StatementExtractionResult, PalacioMsiRowModel
│   ├── dto/                # DebtAccountStatusDto, UserStatusDashboard, AccountStatementPreviewDto, ReceivableStatusDto, ...
│   ├── enums/              # DebtTypeEnum, AccountStatementType
│   ├── exceptions/         # EntityNotFoundException, NoDebtsException
│   ├── utils/              # DebtComparatorUtil, hash utilities
│   └── application/port/
│       ├── in/             # 17 use case interfaces
│       └── out/            # 8 repository interfaces
├── application/service/    # 11 service classes
└── infrastructure/
    ├── entity/             # 8 JPA entity classes (suffix: Entity)
    ├── persistence/        # 8 Spring Data repositories (suffix: JpaRepository or Repository)
    ├── adapter/            # 9 output port adapters (suffix: RepositoryAdapter)
    ├── mapper/             # 9 MapStruct mappers (suffix: Mapper)
    ├── model/              # HTTP request models (prefix: Create, suffix: Req)
    ├── web/                # 8 REST controllers
    ├── web/view/           # 8 Thymeleaf view controllers
    └── configuration/      # SecurityConfig, CorsConfig, etc.

src/main/resources/
├── application.yml         # All Spring configuration
└── templates/              # Thymeleaf HTML templates
```

---

## Development Commands

```bash
# Run locally (requires PostgreSQL running and env vars set)
./mvnw spring-boot:run

# Build JAR
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Build Docker image
docker build -t financial-system:latest .

# Run Docker container
docker run \
  -e SPRING_DB_URL=jdbc:postgresql://host:5432/debt_db \
  -e SPRING_DB_USER=postgres \
  -e SPRING_DB_PASSWORD=secret \
  -p 666:666 \
  financial-system:latest
```

The server runs on **port 666**.

---

## Environment Variables

| Variable | Required | Description | Example |
|---|---|---|---|
| `SPRING_DB_URL` | Yes | JDBC connection URL | `jdbc:postgresql://localhost:5432/debt_db` |
| `SPRING_DB_USER` | Yes | Database username | `postgres` |
| `SPRING_DB_PASSWORD` | Yes | Database password | `secret` |
| `SPRING_DB_DRIVE` | No | Driver class | `org.postgresql.Driver` |
| `SPRING_DB_SCHEMA` | No | Schema name (default: `public`) | `financial` |

DDL mode is `update` — Hibernate auto-creates and alters tables on startup.

---

## REST API Reference

All endpoints are unauthenticated (`.permitAll()`). User identity is passed via HTTP header `email` or URL path.

### Debts
```
POST   /debt/{debtAccountCode}               Create debt
PUT    /debt                                 Update debt
DELETE /debt/{debtId}                        Soft-delete debt
GET    /debt/all/{debtAccountCode}           Get debts for account
GET    /debt/all                             Get all debts (header: email)
GET    /debt/set/hash/sum                    Compute missing SHA-256 hashes (header: email)
```

### Debt Accounts
```
POST   /debt/account/{financialProviderCode} Create account
PUT    /debt/account                         Update account
PATCH  /debt/account/{code}/statement-type  Change statement type
DELETE /debt/account/{debtAccountCode}       Soft-delete account
GET    /debt/account/status/{code}           Account summary + almost-complete debts
GET    /debt/account/all/{providerCode}      List accounts for provider
```

### Debt Management (Bulk Operations)
```
POST   /debt/management/add/{debtAccountCode}      Bulk-add debts to account
PATCH  /debt/management/payOff/{debtAccountCode}   Mark all debts as paid / increment installments
```

### Account Statement Extraction
```
POST   /account/statement/extract/{debtAccountCode}   Preview extraction (read-only)
POST   /account/statement/sync/{debtAccountCode}      Persist extracted debts

Query param: accountStatementType = UNIVERSAL | MANUAL | RAPPI | PALACIO | LIVERPOOL | MERCADO_PAGO | BBVA
Body: multipart/form-data file upload (PDF or Excel)
```

### Payments
```
GET    /payment/do/{debtAccountCode}         Record payment + increment all installments
GET    /payment/{debtAccountCode}            Payment history
GET    /payment/latest/{debtAccountCode}     Latest payment
```

### Financial Status (Dashboard)
```
GET    /financial/status/{email}             Aggregated financial dashboard
```

### Financial Providers
```
GET    /financial/provider/catalog/all       All provider catalogs
POST   /financial/provider/catalog           Create catalog
PUT    /financial/provider/catalog           Update catalog
DELETE /financial/provider/catalog/{id}      Delete catalog
POST   /financial/provider                   Create provider
PUT    /financial/provider                   Update provider
DELETE /financial/provider/{id}              Delete provider
GET    /financial/provider/all/{email}       List user's providers
```

### Fixed Expenses
```
GET    /fixed/expense/catalog/all            All expense catalogs
POST   /fixed/expense/catalog                Create catalog
PUT    /fixed/expense/catalog                Update catalog
DELETE /fixed/expense/catalog/{id}           Delete catalog
POST   /fixed/expense                        Create expense
PUT    /fixed/expense                        Update expense
DELETE /fixed/expense/{id}                   Delete expense
GET    /fixed/expense/all/{email}            List user's expenses
```

### Receivables (money lent out, e.g. to relatives)
```
POST   /receivable/{userEmail}               Create receivable (record money lent)
PUT    /receivable                           Update receivable
DELETE /receivable/{receivableId}            Soft-delete receivable
GET    /receivable/all/{userEmail}           List user's receivables
POST   /receivable/{receivableId}/payment    Register a repayment (irregular/partial amounts allowed; optional paymentDate, defaults to today)
GET    /receivable/status/{receivableId}     Pending balance + repayment history for one receivable
GET    /receivable/history/{receivableId}    Chronological history with running balance + zero-filled monthly totals
GET    /receivable/status/all/{userEmail}    Pending balance + repayment history for all receivables
```
A `Receivable` auto-closes (`active=false`) once accumulated repayments cover the principal, and
re-opens automatically if repayments are ever removed/adjusted below the principal. Repayments are
dynamic amounts (no fixed installment schedule) — a single payment can cover several months at once.
Each repayment stores the `paymentDate` the money actually arrived (separate from `created_at`, the moment it
was entered); payments recorded before that column existed fall back to their `created_at` date.

### Upcoming Debts (deferred purchases — "buy now, first payment in N months")
```
POST   /upcoming/debt/{userEmail}            Add a purchase to the upcoming pile (total OR monthlyPayment + installments + firstPaymentMonth "yyyy-MM")
PUT    /upcoming/debt/{id}                   Update it
PATCH  /upcoming/debt/{id}/start             It now shows on a statement → STARTED (leaves the pile)
PATCH  /upcoming/debt/{id}/cancel            Purchase returned / cancelled → CANCELLED
PATCH  /upcoming/debt/{id}/reopen            Put it back on the pile (PENDING)
DELETE /upcoming/debt/{id}                   Soft-delete
GET    /upcoming/debt/all/{userEmail}        Every upcoming debt (any status)
GET    /upcoming/debt/pending/{userEmail}    Only the pile (PENDING)
GET    /upcoming/debt/projection/{userEmail}?months=12   Month-by-month charges: current debts + upcoming + fixed expenses
```
Upcoming debts live in their own `upcoming_debt` table and **never write to `debt`** — they are a planning
reminder only. The real debt arrives through the normal statement import; nothing is matched automatically.
While reviewing a statement (single v2 flow and bulk import) the card's pending upcoming debts are listed so
the user can recognise the purchase and mark it as started. PENDING items are added to the Progression tab
(per card, plus "starts this month" markers), to the projection on `/ui/upcoming-debts` (with a client-side
"what if I buy X" simulator) and to a dashboard KPI — they are NOT included in the dashboard's current totals.

### Spring Data REST (HAL-JSON)
```
/data/*  Auto-generated CRUD for every JPA repository (HAL format with _embedded collections, e.g. /data/debtEntities;
         base path `spring.data.rest.base-path`). Unauthenticated like everything else, including DELETE.
```

### Web UI (Thymeleaf)
```
GET    /ui                   Home / email login (?switch = show the form, skip the default-email auto-login)
POST   /ui/session           Set user email in HTTP session + long-lived `fm_user` cookie
POST   /ui/session/end       "Switch user": forget the session user and the cookie
GET    /ui/insights          Insights tab (home after login): month-by-month payments (?months=6|12|24), finishing soon,
                             per card, payoff advisor, spending by category, salary & savings sheet
PUT    /ui/financials        Update salary & savings (returns to Insights)
GET    /ui/dashboard         Old dashboard URL → redirects to /ui/insights
GET    /ui/more              "More" tab: secondary sections + settings (language, switch user)
GET    /ui/activity          Activity log of the current session
GET    /ui/fixed-expenses    Fixed expenses management
GET    /ui/cards             Cards tab: Wallet-style stack of every card (?provider=code filters one provider);
                             "Add card" sheet (provider, name, pay day, credit, statement type, colour)
GET    /ui/debt-accounts/{code}  One card: totals, record payment, import statement, debts (add/edit/delete sheets),
                             pending upcoming purchases, settings (colour, statement type, CSV upload, pay off, delete)
POST   /ui/debt-accounts/{code}/color  Card colour "#RRGGBB" ("auto" = default from the bank)
GET    /ui/debt-accounts?providerCode=  Old per-provider list → redirects to /ui/cards?provider=
GET    /ui/progression       Old month-by-month page → redirects to /ui/insights?months=24
GET    /ui/statements        Statement upload interface
GET    /ui/payments/{code}   Payment history of one card + record payment
GET    /ui/providers         Providers (banks/stores) + provider types; tap one to see its cards
GET    /ui/receivables       Money lent out (e.g. to relatives) + repayment tracking
GET    /ui/receivables/{id}  One receivable: KPIs, balance-over-time + monthly charts, dated payment history
GET    /ui/upcoming-debts    Upcoming (deferred) purchases pile + month-by-month projection + what-if simulator
GET    /ui/bulk-statements   Bulk statement import (many files → many cards, see below)
```

### UI (iOS-style, mobile first)
Every page is on the iOS-style shell (iPhone first, also iPad and Mac; installed as a Home Screen web app).
No Bootstrap, no CDN scripts — everything is in `app.css` / `app.js` / inline page scripts.
- **Shell:** `templates/layout/app.html` — `head(title)` (Home Screen meta tags, manifest, `app.css`, `app.js`),
  `nav` (floating tab bar below 1024px, sidebar from 1024px, offline banner), `icon(name, cls)` (symbols in
  `static/img/icons.svg`), `langSwitch`. Every page uses `<body class="app">` + `<main class="app-main">`.
- **Design tokens / components:** `static/css/app.css` (light + dark via `prefers-color-scheme`); behaviour in `static/js/app.js`.
- **Active tab:** `GlobalModelAdvice` adds `currentPath`, `navSection` (insights | cards | import | plan | more) and `currentUser`.
  `@ModelAttribute` advice does not run for `@ExceptionHandler` models, so `GlobalViewExceptionHandler` adds them itself.
- **Remembered user:** `RememberedUser` (interceptor on `/ui/**`) restores `userEmail` from the `fm_user` cookie —
  iOS drops session cookies whenever it closes a Home Screen app.
- **Language:** `messages.properties` (English, default) / `messages_es.properties`; `?lang=en|es_MX` switches and is kept in
  the `fm_lang` cookie (`WebConfig`). All templates use `#{...}` keys.
- **Offline:** `static/sw.js` saves each `/ui` page you open (network first, 6s timeout) and shows the saved copy with an
  "Offline · updated …" banner when there is no connection; forms are blocked offline; the copies are deleted on login /
  switch user. Service workers need HTTPS (or localhost). Warm-up: a few seconds after a page loads online (at most every
  30 min, `localStorage.fm_warmed`), `app.js` posts the tab URLs from the nav's `data-warm` to the service worker, which
  saves them plus the card / loan / payments pages they link to — so every tab works offline, not only visited ones.
- **Done:** shell + login + More/Activity (phase 1), Insights (phase 2: `InsightsViewController`, view model
  `InsightsView` built from `GetDebtProgressionUseCase` over a 120-month horizon for the debt-free date; month
  panels switch client-side via `data-select` / `data-select-panel`). Money/month formatting for templates:
  `@fmt` (`UiFormat`). Cards (phase 3: `CardsViewController`, card detail, payments, providers; card colour in
  `debt_account.color`, resolved by `@cardColors` / `CardColors` — chosen colour, else bank default, else palette).
  Sheets: `data-open-sheet` + `data-action` / `data-alt-action` / `data-f-<field>` prefill the sheet's form;
  `<form data-action-from="fooAction">` inside a sheet posts to the opener's `data-foo-action` (hidden without it);
  `data-confirm` asks before submitting. Import (phase 4: bulk upload/review/result, single v2 upload/review and the
  older v1 upload/preview incl. CSV, all on the new shell; shared `fragments/statement-diff` (`rows`, `missing`,
  `ignoreSheet`) and `fragments/upcoming-reminder`). `UiFormat` and `CardColors` are registered as beans in `WebConfig`
  so `@WebMvcTest` slices get them. `body.app [hidden]` always hides. Plan (phase 5: `/ui/upcoming-debts` — stacked
  projection chart with salary line, per-month breakdown, "What if I buy…" simulator that also answers "can I afford
  it?", pile with edit/started/cancelled/delete sheet). More pages (phase 6: receivables list/detail with an inline SVG
  balance chart and monthly bars (`ReceivableViewController.MonthBar`), fixed expenses with the by-category breakdown
  (`FixedExpenseViewController.CategoryGroup`) + select mode for bulk edit/delete, ignored debts, error page);
  `layout/navbar` and Bootstrap removed.
- **Local dev note:** with devtools restart enabled, applying a bulk import fails with a ClassCastException because
  `BulkStatementService` copies the batch via `SerializationUtils.clone` (different class loader). Run with
  `-Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=false"`; production (no devtools) is unaffected.
- **Static files** are fingerprinted (`app-<hash>.css`) through `@{...}` and cached for 7 days.

### Bulk Statement Import (Web UI)
```
GET    /ui/bulk-statements                         Pick files, associate each with a card, choose default mode
POST   /ui/bulk-statements/extract                 Extract + diff every file (no DB writes); batch kept in session
GET    /ui/bulk-statements/preview                 Review per-card diff (new / updated / completed / not in statement)
POST   /ui/bulk-statements/items/{i}/action        Per-card action: UPDATE | SOURCE_OF_TRUTH ("rebuild from 0") | SKIP
POST   /ui/bulk-statements/action-all              Same action for every successfully extracted card
POST   /ui/bulk-statements/items/{i}/mark-ignorable Mark a debt ignorable and recompute that card
POST   /ui/bulk-statements/items/{i}/retry         Re-upload a failed file (e.g. with PDF password)
POST   /ui/bulk-statements/execute                 Persist all cards in ONE transaction (all-or-nothing)
POST   /ui/bulk-statements/cancel                  Discard the pending batch
```
Each file uses the parser linked to its card (`DebtAccount.accountStatementType`) and the same diff/sync
logic as the single-statement v2 flow (`StatementDiffPreviewService` / `StatementDiffSyncService`).
Only one file per card per batch. A file that fails to extract never blocks the others.

### API Documentation
```
GET    /swagger-ui.html      Interactive Swagger UI
GET    /v3/api-docs          OpenAPI JSON spec
```

---

## Database Schema

Hibernate manages DDL automatically (`ddl-auto: update`). Key relationships:

```
debt_sys_user (PK: email)
├── 1:N → financial_provider
│         └── 1:N → debt_account
│                   ├── 1:N → debt
│                   └── 1:N → payment
├── 1:N → fixed_expense
├── 1:N → upcoming_debt (M:1 optional → debt_account; status PENDING | STARTED | CANCELLED)
└── 1:N → receivable
          └── 1:N → receivable_payment

financial_provider_catalog  ← M:1 from financial_provider
fixed_expense_catalog       ← M:1 from fixed_expense
```

**Common entity fields:**
- `active` (boolean) — soft delete flag; all queries filter `WHERE active = true`
- `created_at`, `updated_at` — managed by `@CreationTimestamp` / `@UpdateTimestamp`
- Primary keys: `code` (String UUID) for accounts/providers, auto-increment Long for debts/expenses

---

## Naming Conventions

| Artifact | Convention | Example |
|---|---|---|
| JPA entities | PascalCase + `Entity` suffix | `DebtEntity`, `DebtAccountEntity` |
| Domain models | PascalCase, no suffix | `Debt`, `DebtAccount` |
| Request models | `Create` prefix + `Req` suffix | `CreateDebtReq`, `DebtReq` |
| DTOs | PascalCase + `Dto` suffix (or descriptive name) | `DebtAccountStatusDto`, `UserStatusDashboard` |
| Services | PascalCase + `Service` suffix | `DebtService`, `PaymentService` |
| Repository interfaces (port/out) | PascalCase + `Repository` | `DebtRepository` |
| Repository adapters | PascalCase + `RepositoryAdapter` | `DebtRepositoryAdapter` |
| JPA repositories | PascalCase + `JpaRepository` | `DebtJpaRepository` |
| Mappers | PascalCase + `Mapper` | `DebtMapper` |
| Controllers | PascalCase + `Controller` | `DebtController` |
| View controllers | PascalCase + `ViewController` | `DebtViewController` |
| Enums | PascalCase + `Enum` or descriptive | `DebtTypeEnum`, `AccountStatementType` |
| Exceptions | Descriptive + `Exception` | `EntityNotFoundException` |
| Packages | lowercase, dot-separated | `com.jimm0063.magi...` |

---

## Account Statement Extraction

The most complex subsystem. Uses Factory + Strategy patterns.

**Supported providers and their parsing strategies:**

| `AccountStatementType` | Parser | File Format |
|---|---|---|
| `UNIVERSAL` | Regex over PDF text | PDF |
| `MANUAL` | Manual entry | — |
| `RAPPI` | Regex over PDF text | PDF |
| `BBVA` | Regex over PDF text | PDF |
| `LIVERPOOL` | Tesseract OCR (Spanish) | Scanned PDF |
| `PALACIO` | Apache POI | Excel (.xlsx) |
| `MERCADO_PAGO` | Regex over PDF text | PDF |

**Deduplication:** SHA-256 hash of `{debtAccountCode}|{monthlyPayment}|{maxFinancingTerm}` stored on `Debt`. The `/debt/set/hash/sum` endpoint backfills missing hashes.

**Preview vs. Sync:**
- `POST /account/statement/extract` — reads file, returns `AccountStatementPreviewDto` (no DB writes)
- `POST /account/statement/sync` — reads file, persists new/updated debts

**`AccountStatementPreviewDto` structure:**
```java
List<Debt> newDebts          // Debts not yet in DB
List<Debt> installmentUpdates // Existing debts with changed installment count
List<Debt> completedDebts    // Debts where currentInstallment >= maxFinancingTerm
```

---

## Payment Flow

`GET /payment/do/{debtAccountCode}`:
1. Fetches all active debts for the account
2. Saves a backup JSON snapshot to `payment.backup_data`
3. Increments `currentInstallment` on each debt
4. Marks debts as inactive when `currentInstallment >= maxFinancingTerm`
5. Saves the `Payment` record

This is transactional — a failure rolls back all changes.

---

## Financial Status Aggregation

`GET /financial/status/{email}` returns `UserStatusDashboard`:

```
salary
savings
totalMonthlyPayments   = sum of active debt monthly payments
fixedExpenses          = sum of active fixed expense costs
availableIncome        = salary - totalMonthlyPayments - fixedExpenses
breakdownByType        = CARD / LOAN / PEOPLE / FOR_LIFE_PLAN sums
breakdownByAccount     = per-account payment distribution
totalPendingReceivables = sum of pending balances across active receivables (money lent out, not yet repaid)
```

---

## Adding a New Feature (Checklist)

1. **Domain model** → add class in `domain/model/`
2. **Output port** → add interface in `domain/application/port/out/`
3. **Input port** → add use case interface in `domain/application/port/in/`
4. **JPA entity** → add `*Entity.java` in `infrastructure/entity/`
5. **JPA repository** → add `*JpaRepository` in `infrastructure/persistence/`
6. **MapStruct mapper** → add `*Mapper` in `infrastructure/mapper/`
7. **Repository adapter** → implement output port in `infrastructure/adapter/`
8. **Service** → implement input port(s) in `application/service/`
9. **Controller** → add `@RestController` in `infrastructure/web/`
10. **Request model** (if needed) → add in `infrastructure/model/`

---

## Security

Spring Security is configured with **all requests permitted** and CSRF disabled. This is intentional for the current internal deployment.

CORS is configured for:
- `http://localhost:3000`
- `http://192.168.50.180:3000`
- `http://localhost:666`

Do not add authentication unless explicitly requested.

---

## Multipart Upload Limits

Configured in `application.yml`:
- Max file size: **50MB**
- Max request size: **300MB** (bulk statement import sends several files in one request)
- Tomcat `max-part-count`: **500** (each bulk file adds ~3 multipart parts)

These exist to support large PDF/Excel statement files from financial institutions.

---

## Docker

Multi-stage Dockerfile:
- **Stage 1:** Maven 3.9.6 + JDK 21 — builds the fat JAR
- **Stage 2:** JRE 21 + Tesseract OCR (Spanish language data) — minimal runtime image
- Exposes **port 666**
- Entrypoint: `java -jar app.jar`

The Tesseract installation in Docker is required for `LIVERPOOL` statement parsing (OCR on scanned PDFs).

---

## Key Files to Know

| File | Purpose |
|---|---|
| `DebtManagementLteSystemApplication.java` | Spring Boot entry point |
| `infrastructure/configuration/SecurityConfig.java` | CORS + auth (all permitted) |
| `application/service/DebtService.java` | Core debt operations + deduplication |
| `application/service/FinancialStatusService.java` | Dashboard aggregation |
| `application/service/PaymentService.java` | Payment recording + installment increment |
| `application/service/BulkStatementService.java` | Bulk statement import: per-file extraction/diff + single-transaction sync |
| `application/service/UpcomingDebtService.java` | Upcoming (deferred) purchases pile + payment projection |
| `application/service/ReceivableService.java` | Money lent out: pending-balance calculation, auto open/close on repayment |
| `infrastructure/web/AccountStatementController.java` | Statement extraction endpoints |
| `domain/utils/DebtComparatorUtil.java` | Diff logic: new vs. existing debts |
| `domain/enums/AccountStatementType.java` | Provider type enum (drives factory selection) |
| `src/main/resources/application.yml` | All Spring/DB configuration |
| `templates/layout/app.html` | Shared shell for the redesigned UI (head, tab bar / sidebar, icons) |
| `static/css/app.css` | iOS-style design tokens and components (light + dark) |
| `static/sw.js` | Service worker: offline copies of visited pages |
