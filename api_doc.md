# FinTracker — Spring Boot API Documentation

> **Base URL:** `http://localhost:8080`  
> **Auth:** All protected routes require an active Google OAuth2 session (Spring Security).  
> Spring injects the logged-in user automatically via `Authentication` — no JWT header needed (unlike the TS version).

---

## Auth

> In Spring Boot, login/logout is handled by Spring Security automatically.  
> No custom `/register` or `/login` needed — Google OAuth2 replaces it.

| Method | Endpoint | Description | Controller |
|--------|----------|-------------|------------|
| `GET` | `/` | Get logged-in user info | `BaseController` |
| `GET` | `/login` | Redirects to Google login (Spring handles) | — |
| `GET` | `/logout` | Logs out user | Spring Security |

### `GET /`
**Response:**
```json
{
  "name": "Sanchit Ingale",
  "email": "sanchitingale09@gmail.com",
  "sub": "104298435213015793587",
  "picture": "https://..."
}
```

---

## Gmail

> **Controller:** `GmailController.java`  
> **Mapping prefix:** `/api/gmail`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/gmail/status` | Check if Gmail is connected |
| `POST` | `/api/gmail/sync` | Sync emails since a date |
| `POST` | `/api/gmail/sync-all` | Sync all emails from start of month |
| `GET` | `/api/gmail/sync/status` | Get last sync info |

---

### `GET /api/gmail/status`
**Response:**
```json
{
  "connected": true,
  "email": "sanchitingale09@gmail.com"
}
```

---

### `POST /api/gmail/sync`
**Request Body:**
```json
{
  "sinceDate": "2026-06-01",
  "maxResults": 50
}
```
**Response:**
```json
{
  "synced": 12,
  "transactions": [
    {
      "amount": "501.00",
      "account": "9791",
      "vpa": "rucha.gaikwad1188-2@okaxis",
      "recipient": "RUCHA SUNIL GAIKWAD",
      "date": "31-05-26",
      "time": "21:20",
      "upiRef": "124002707812"
    }
  ]
}
```

---

### `POST /api/gmail/sync-all`
Same as `/sync` but ignores `sinceDate` — fetches from start of current month.

**Response:** Same as `/sync`

---

### `GET /api/gmail/sync/status`
**Response:**
```json
{
  "lastSyncedAt": "2026-06-08T18:00:00Z",
  "totalSynced": 45
}
```

---

## Transactions

> **Controller:** `TransactionController.java`  
> **Mapping prefix:** `/api/banking`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/banking/transactions/recent` | Get paginated transactions |
| `PUT` | `/api/banking/transactions/{id}/category` | Update transaction category |

---

### `GET /api/banking/transactions/recent`

**Query Params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `limit` | int | 20 | Page size |
| `offset` | int | 0 | Pagination offset |
| `startDate` | String | — | ISO date filter |
| `endDate` | String | — | ISO date filter |
| `type` | String | — | `DEBIT` or `CREDIT` |
| `search` | String | — | Search description/category |
| `categoryId` | Long | — | Filter by category |

**Response:**
```json
{
  "transactions": [
    {
      "id": 1,
      "amount": 501.00,
      "type": "DEBIT",
      "description": "UPI to rucha.gaikwad1188-2@okaxis",
      "transactionDate": "2026-05-31T21:20:00",
      "categoryName": "Food & Dining",
      "bankName": "HDFC Bank",
      "upiRef": "124002707812"
    }
  ],
  "pagination": {
    "total": 45,
    "limit": 20,
    "offset": 0,
    "hasMore": true
  },
  "aggregates": {
    "totalIncome": 0.0,
    "totalExpenses": 4250.00
  }
}
```

---

### `PUT /api/banking/transactions/{id}/category`

**Path Param:** `id` — transaction ID

**Request Body:**
```json
{
  "categoryId": 3
}
```

**Response:**
```json
{
  "message": "Category updated successfully",
  "transaction": {
    "id": 1,
    "categoryId": 3
  }
}
```

---

## Categories

> **Controller:** `CategoryController.java`  
> **Mapping prefix:** `/api/banking`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/banking/categories` | Get all categories |

### `GET /api/banking/categories`
**Response:**
```json
{
  "categories": [
    { "id": 1, "name": "Food & Dining", "color": "#FF6B6B", "icon": "🍔", "isSystem": true },
    { "id": 2, "name": "Transport",     "color": "#4ECDC4", "icon": "🚗", "isSystem": true },
    { "id": 3, "name": "Investment",    "color": "#45B7D1", "icon": "📈", "isSystem": true }
  ]
}
```

---

## Accounts

> **Controller:** `AccountController.java`  
> **Mapping prefix:** `/api/banking`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/banking/accounts` | List linked bank accounts |
| `POST` | `/api/banking/accounts/link` | Link a bank account |

---

### `GET /api/banking/accounts`
**Response:**
```json
{
  "accounts": [
    {
      "id": 1,
      "bankName": "HDFC Bank",
      "accountType": "SAVINGS",
      "maskedAccountNumber": "****9791",
      "isActive": true
    }
  ]
}
```

---

### `POST /api/banking/accounts/link`
**Request Body:**
```json
{
  "bankName": "HDFC Bank",
  "accountType": "SAVINGS"
}
```

**Response:**
```json
{
  "message": "Account linked successfully",
  "account": { "id": 1, "bankName": "HDFC Bank" }
}
```

---

## Dashboard

> **Controller:** `DashboardController.java`  
> **Mapping prefix:** `/api/dashboard`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/dashboard/stats` | Monthly summary stats |

### `GET /api/dashboard/stats`
**Response:**
```json
{
  "totalBalance": 12500.00,
  "monthlyIncome": 0.00,
  "monthlyExpenses": 4250.00,
  "transactionCount": 18,
  "netCashFlow": -4250.00
}
```

---

## Analytics

> **Controller:** `AnalyticsController.java`  
> **Mapping prefix:** `/api/analytics`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/analytics/stats` | Full analytics with category breakdown + daily pattern |

### `GET /api/analytics/stats`

**Query Params:**

| Param | Type | Default |
|-------|------|---------|
| `startDate` | String | Start of current month |
| `endDate` | String | End of current month |

**Response:**
```json
{
  "period": {
    "start": "2026-06-01T00:00:00Z",
    "end": "2026-06-30T23:59:59Z",
    "days": 12
  },
  "summary": {
    "totalIncome": 0.00,
    "totalExpenses": 4250.00,
    "savings": -4250.00,
    "avgDailySpending": 354.16,
    "totalInvested": 1000.00,
    "investmentCount": 2,
    "incomeCount": 0,
    "expenseCount": 18,
    "totalTransactions": 18
  },
  "mostSpentCategory": {
    "category": "Food & Dining",
    "amount": 1800.00,
    "count": 9,
    "percentage": "42.4"
  },
  "categoryBreakdown": [
    { "category": "Food & Dining", "amount": 1800.00, "count": 9, "percentage": "42.4" },
    { "category": "Transport",     "amount": 950.00,  "count": 5, "percentage": "22.4" }
  ],
  "topCategories": [...],
  "dailySpending": [
    { "day": 1, "expense": 200.00, "income": 0.00 },
    { "day": 2, "expense": 0.00,   "income": 0.00 }
  ]
}
```

---

## Spring Boot Controller Build Order

```
1. BaseController          ← done (Google Auth)
2. GmailController         ← current
3. TransactionController   ← next after Gmail
4. CategoryController
5. AccountController
6. DashboardController
7. AnalyticsController
```

---

## Key Differences: TS → Spring Boot

| TS (Express) | Spring Boot |
|---|---|
| JWT in `Authorization` header | Spring Security session (cookie-based) |
| `req.user?.userId` | `(OAuth2User) authentication.getPrincipal()` |
| `authenticateToken` middleware | `@AuthenticationPrincipal` annotation |
| `/api/auth/register` `/login` | Not needed — Google OAuth handles it |
| `pool.query(sql, params)` | Spring Data JPA / JdbcTemplate |
