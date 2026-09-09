---
description: Ensure SQLDelight schema migrations are safe, backwards-compatible, and do not risk data loss
paths:
  - "**/*.sq"
  - "**/*.sqm"
  - "**/migrations/**"
---

# SQLDelight Migration Safety

## Rule

All database schema changes must be backwards-compatible and preserve existing user data. Destructive migrations (DROP TABLE, DROP COLUMN) must never be used without an explicit data migration strategy.

## Migration File Requirements

### New migrations
- Every schema change MUST have a corresponding `.sqm` migration file with the correct version number
- Migration version numbers must be sequential and never skip or reuse numbers
- The migration file name must match the pattern: `<version>.sqm` (e.g., `1.sqm`, `2.sqm`)

### Safe operations (allowed without special review)
- `ALTER TABLE ... ADD COLUMN` with a `DEFAULT` value or `NULL`
- `CREATE TABLE` for entirely new tables
- `CREATE INDEX` / `CREATE UNIQUE INDEX`
- `INSERT INTO ... SELECT FROM` for data backfill

### Dangerous operations (MUST BLOCK without migration plan)
- `DROP TABLE` — data is permanently lost
  - **Acceptable**: Only if the table data is recreatable (cache) and documented as such
- `DROP COLUMN` — SQLite does not support this; requires table recreation
  - **Fix**: Create new table → copy data → drop old table → rename new table
- `ALTER TABLE ... RENAME COLUMN` — can break existing queries if not done atomically
- Changing a column type — SQLite doesn't enforce types, but this can break application-level parsing
- Removing a `NOT NULL` constraint or `DEFAULT` value
- Changing a `PRIMARY KEY` — requires table recreation

### Required for every migration
1. **Verify reversibility**: Can the migration be undone if something goes wrong? (Even if you don't implement a down migration, document the rollback strategy)
2. **Data preservation**: If a table is being restructured, the migration MUST copy existing data to the new structure
3. **Null safety**: New columns on existing tables MUST be nullable (`NULL`) or have a `DEFAULT` value — otherwise existing rows will fail constraints
4. **Index consideration**: If adding columns that will be queried, consider adding indexes in the same migration

## Schema Design Review

### In `.sq` files
- Use `INTEGER AS kotlin.Boolean` (NOT `AS Boolean`) — avoids SQLDelight 2.0.2 import bug
- Use `TEXT` for enums with a Kotlin adapter, not raw integer encoding (more readable, safer for migrations)
- Primary keys should use `INTEGER PRIMARY KEY AUTOINCREMENT` for local IDs or `TEXT PRIMARY KEY` for server-assigned UUIDs
- Foreign keys should include `ON DELETE` behavior (`CASCADE`, `SET NULL`, or `RESTRICT`)
- Timestamps should use `INTEGER` (epoch millis) for cross-platform consistency

### Performance
- Tables expected to grow large must have indexes on frequently queried columns
- Composite indexes for multi-column WHERE clauses
- Avoid `SELECT *` in `.sq` queries — select only needed columns

## Testing Requirements

- Migration tests must verify that data inserted before migration is accessible after migration
- Test the full migration chain from version 1 to current (not just the latest migration in isolation)

## Severity

- `🚫 Blocking` — `DROP TABLE`/`DROP COLUMN` without data migration, new non-nullable column without DEFAULT on existing table, missing `.sqm` file for schema change
- `⚠️ Change requested` — Missing migration test, missing indexes on large tables, `SELECT *` usage
- `💡 Suggestion` — Schema design improvements, additional indexes for query optimization
