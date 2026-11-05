# Member mobile and contribution history enhancement

- V5 adds nullable, unique `members.mobile_number` (10 digits) and an append-only `contribution_changes` table. Existing members retain NULL until updated. Existing rows are not deleted.
- V6 adds `CONTRIBUTION_UPDATE` and grants it to existing ADMIN and SUPER_ADMIN roles. Check any custom roles manually.
- Admin endpoints: GET `/api/admin/members`, GET `/api/admin/members/{id}/contributions`, GET `/api/admin/members/{id}/contribution-changes`, GET `/api/admin/contributions/{id}/history`, PUT `/api/admin/contributions/{id}`.
- Correction body: `{ "contributionMonth":"2026-11", "amount":750.00, "reason":"Correcting entry" }`.
- Existing member POST/PUT now require mobile number; duplicates return 409. Existing members can have NULL until they are edited.
- Public snapshot excludes member mobile numbers; authorized admin member list includes them.
- Frontend member View/Edit requires login and permissions. Public users do not see mobile numbers.
- Monthly contributions may contain multiple entries per member/month in the existing schema; this enhancement preserves that behavior.

## Verification limits
This environment could not run the full backend Maven tests (Maven unavailable), or the frontend Vite build (the uploaded Windows node_modules lacks Linux Rollup native package). Run `mvn clean test` in backend and `npm ci && npm run build` in frontend on your machine before deployment.

## Production considerations
- For sensitive mobile data, ensure all admin endpoints are HTTPS-only and use proper access controls.
- Duplicate check is backed by a unique index, but simultaneous duplicate submissions can still return a database conflict; map SQL constraint violations to HTTP 409 if needed.
- The contribution edit audit captures month/amount corrections; member profile edits currently do not have a dedicated before/after audit table.
