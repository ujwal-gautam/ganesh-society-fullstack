# Shree Ganesh Society — Spring Boot + MySQL + React

## Architecture
- `backend/`: Java 17, Spring Boot 3.5.7, Spring Data JPA, Flyway, Spring Security.
- `frontend/`: React 18 + Vite (JavaScript). Public passbook and separate admin area.
- `backend/src/main/resources/db/migration/V1__schema.sql`: normalized MySQL schema.
- `V2__seed.sql`: original HTML's 24 members, August/September 2026 contributions, 4 loans and 3 repayments.

## Local setup
1. Install Java 17, Maven, Node.js 20+ and MySQL 8.
2. Create MySQL database/user, or allow JDBC to create DB using a sufficiently privileged local account. Recommended:
   ```sql
   CREATE DATABASE ganesh_society CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'society_app'@'localhost' IDENTIFIED BY 'change-me';
   GRANT ALL PRIVILEGES ON ganesh_society.* TO 'society_app'@'localhost';
   ```
3. Set environment variables `DB_URL=jdbc:mysql://localhost:3306/ganesh_society`, `DB_USER=society_app`, `DB_PASSWORD=change-me`, `ADMIN_EMAIL=admin@yourdomain.com`, `ADMIN_PASSWORD=<strong-unique-password>`, `CORS_ORIGIN=http://localhost:5173`.
4. Run `cd backend && mvn spring-boot:run` (port 8080). Flyway creates schema and imports seed only once. Admin account is created on first boot if missing.
5. Run `cd frontend && cp .env.example .env && npm install && npm run dev` (port 5173). Open `http://localhost:5173`.

## API
Public: `GET /api/public/snapshot`.
Admin (HTTP Basic auth): `GET /api/admin/me`; `POST /api/admin/members`; `PUT /api/admin/members/{id}`; `POST /api/admin/contributions`; `POST /api/admin/loans`; `POST /api/admin/loans/{id}/repayments`; `PUT /api/admin/settings`.

## Security and financial notes
- Public API is read-only. Writes require a database-backed enabled admin account with BCrypt password hash and ADMIN role. Add a second admin through the database with a securely generated BCrypt hash (not plaintext).
- This starter uses HTTPS-compatible HTTP Basic with credentials held in React memory only. **Use HTTPS in deployment**; for production, prefer short-lived session/JWT + refresh or server-side HttpOnly session cookie, CSRF protection, login rate limiting, audit trail, backups and a reverse proxy.
- Public API exposes member names and financial records to anyone with the URL. Add member-specific permissions if privacy is required.
- Contributions can have multiple payments per member/month (partial installments). Repayment `received_amount` may exceed principal+interest allocated, preserving the source's unresolved ₹700 case; reconcile manually.
- Cash balance includes all received repayments (including unallocated amounts). It excludes expenses, fees, opening balance and bank reconciliation, which are not present in the original page.
- No hard deletes are provided for ledger entries. Add auditable correction/reversal transactions rather than deleting historical financial data.
- Import is a one-time initial snapshot; verify figures with the treasurer before production use.
