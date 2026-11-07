# Monthly contribution deletion

- Admin and Super Admin with `CONTRIBUTION_DELETE` can delete a contribution from Member Details.
- A confirmation prompt is shown before `DELETE /api/admin/contributions/{id}`.
- Deletion is **soft**: `deleted_at` and `deleted_by` are recorded; existing contribution rows and correction history are retained.
- Deleted entries are excluded from member contribution history and public snapshot totals.
- Flyway migration `V8__contribution_soft_delete.sql` adds columns and assigns `CONTRIBUTION_DELETE` to ADMIN and SUPER_ADMIN.
- Back up production MySQL before applying V8; ensure Flyway V1-V7 are already applied.
- Deploy both backend and frontend. Sign out/in to refresh UI permissions.
- Existing audit/history endpoints return 404 for deleted contribution IDs, while the underlying audit rows remain stored.
