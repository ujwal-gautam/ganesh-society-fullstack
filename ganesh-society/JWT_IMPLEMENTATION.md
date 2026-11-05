# JWT-only authentication implementation

This patch builds on the previously generated RBAC source implementation, not the older Basic-auth source ZIP. No cookies, no refresh endpoint, no browser token persistence.

## Backend configuration

Set `JWT_SECRET` to Base64 of at least 32 cryptographically random bytes. Keep it outside Git and Docker Compose source. Set `CORS_ORIGIN` to a comma-separated list of exact frontend origins (for example `http://localhost:5173,http://172.18.192.1:5173`). Keep existing DB/BOOTSTRAP variables as needed.

PowerShell generate secret:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Run backend `mvn test` and frontend `npm ci && npm run build` before deploying. Preserve existing MySQL data; review Flyway V3 and back up DB before first deployment.

## API

POST `/api/auth/login` `{"email":"...","password":"..."}` -> `{accessToken,tokenType,expiresIn,...profile}`. Protected requests: `Authorization: Bearer <accessToken>`. GET `/api/auth/me` returns profile. POST `/api/auth/logout` returns 204 but does **not** revoke a previously copied JWT. POST `/api/auth/refresh` is removed (404/denied). Password and role/permission changes invalidate JWTs via a signed security stamp checked against DB. JWT expiry defaults to 15 minutes; users log in again on expiry or page reload.

## Security limitations

Logout only clears frontend memory; a stolen JWT remains usable until expiry unless password or roles change. For immediate per-token logout revocation, add a database-backed JWT denylist. Rate limiting, account recovery, transactional uniqueness of active super-admin, comprehensive tests and production verification remain outstanding. JWT validation uses HS256, issuer, expiry, token type, account status, and security stamp. Do not use this as a production-ready release without tests and security review.
