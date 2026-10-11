# REST API Specification

All JSON responses follow a uniform contract:

### Standard Success Response (200 / 201)
```json
{
  "success": true,
  "message": "Human-readable confirmation message",
  "data": { ... },
  "errors": []
}
```

### Standard Error Response (400 / 401 / 403 / 404 / 409 / 500)
```json
{
  "success": false,
  "message": "Human-readable error description",
  "errors": [
    "Error description 1",
    "Error description 2"
  ]
}
```

---

## 1. Authentication Endpoints (`/api/auth/*`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticates user; establishes secure `JSESSIONID` session. |
| `POST` | `/api/auth/logout` | Authenticated | Terminates active session. |
| `GET` | `/api/auth/me` | Authenticated | Returns identity profile of currently logged-in principal. |
| `POST` | `/api/auth/change-password` | Authenticated | Updates account password. |

#### `POST /api/auth/login`
```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

---

## 2. Books & Catalog Endpoints (`/api/books/*`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/books` | Public | Search, filter, and paginate catalog books. |
| `GET` | `/api/books/{id}` | Public | Get single book details with author and copy availability. |
| `POST` | `/api/books` | Staff | Register new book title with authors and category. |
| `PUT` | `/api/books/{id}` | Staff | Update book metadata. |
| `DELETE`| `/api/books/{id}` | Staff | Deactivate book title. |
| `GET` | `/api/books/{id}/copies` | Staff | List physical copies for a title. |
| `POST` | `/api/books/{id}/copies` | Staff | Add physical copy with accession number/barcode. |
| `DELETE`| `/api/books/copies/{copyId}` | Staff | Delete physical copy. |

#### Query Parameters for `GET /api/books`
- `search`: Case-insensitive text search matching title, ISBN, or author name.
- `categoryId`: Filter by category ID.
- `authorId`: Filter by author ID.
- `availability`: `AVAILABLE` or `UNAVAILABLE`.
- `sortBy`: `b.title`, `b.publication_year`, `total_copies`, `available_copies`.
- `sortOrder`: `ASC` or `DESC`.
- `page`: Page index (default: 1).
- `pageSize`: Number of items per page (default: 10).

---

## 3. Circulation & Loan Endpoints (`/api/loans/*`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/loans` | Staff | List and filter borrowing transactions. |
| `GET` | `/api/loans/overdue` | Staff | List all currently overdue loans with accrued fines. |
| `GET` | `/api/loans/my` | Member | List current and past loans for logged-in member. |
| `GET` | `/api/loans/{id}` | Staff/Member | Retrieve single loan details. |
| `POST` | `/api/loans/issue` | Staff | Issue book copy to member (concurrency locked). |
| `POST` | `/api/loans/{id}/return` | Staff | Accept return, assess overdue fines, check reservations. |

#### `POST /api/loans/issue`
```json
{
  "memberId": 1,
  "copyId": 4,
  "loanDays": 14,
  "notes": "Borrowed for research"
}
```

---

## 4. Fine Management Endpoints (`/api/fines/*`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/fines` | Staff | List fines with status and member filtering. |
| `GET` | `/api/fines/{id}` | Staff | Get fine details. |
| `POST` | `/api/fines/{id}/pay` | Staff | Record payment (CASH, CARD, ONLINE, UPI). |
| `POST` | `/api/fines/{id}/waive` | Admin | Waive fine penalty with justification reason. |

---

## 5. Reservation Endpoints (`/api/reservations/*`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/reservations` | Staff | List active and historical reservations. |
| `POST` | `/api/reservations` | Authenticated | Place reservation hold on a book title. |
| `DELETE`| `/api/reservations/{id}` | Authenticated | Cancel an existing reservation. |
| `PATCH` | `/api/reservations/{id}/cancel` | Authenticated | Cancel an existing reservation. |

---

## 6. Member Endpoints (`/api/members/*`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/members` | Staff | Paginated list of registered members. |
| `GET` | `/api/members/{id}` | Staff | Single member record with current active loans count. |
| `POST` | `/api/members` | Staff | Register new library patron. |
| `PUT` | `/api/members/{id}` | Staff | Update member contact or membership dates. |
| `PATCH` | `/api/members/{id}/status` | Staff | Toggle status (`ACTIVE`, `SUSPENDED`, `EXPIRED`). |

---

## 7. Metadata Endpoints (`Categories`, `Authors`, `Publishers`)

- `GET|POST|PUT|DELETE /api/categories`
- `GET|POST|PUT|DELETE /api/authors`
- `GET|POST|PUT|DELETE /api/publishers`

---

## 8. Administrative Endpoints (`/api/users/*`, `/api/dashboard/*`, `/api/reports/*`)

- `GET /api/dashboard/stats`: Metric cards (total books, copies, members, active loans, overdues, fines).
- `GET /api/dashboard/charts`: 7-day circulation issue & return activity.
- `GET /api/reports/loans`, `/fines`, `/audit`: Formatted historical reports.
- `GET|POST|PUT|PATCH /api/users`: Staff and administrator user management.
- `POST /api/users/{id}/reset-password`: Administrator-initiated credential reset.
