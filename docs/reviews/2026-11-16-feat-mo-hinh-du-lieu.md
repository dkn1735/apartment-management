# Review, feat/mo-hinh-du-lieu, 2026-11-16

**Reviewed by**: Sonnet 5.5 (author on another model)
**Scope**: 52 files, branch vs main (merge base d7415c4)
**Verdict**: Approve with nits

## Summary

The branch adds the stack skeleton cleanup, the tooling layer (Lefthook, ESLint, Prettier, Spring Java Format, CI) and the full data model: nine Flyway migrations creating 42 tables, a repeatable seed under the `seed` profile, and two Testcontainers test classes. I compared every constraint name and index in the original SQL file against the new migrations by script: nothing is missing, and only the three indexes the spec says to drop are gone. The Java and TypeScript changes outside the migrations are formatting only (I checked with a whitespace insensitive diff). I found no blockers and no majors. The findings below are small gaps in test depth, one stale spec follow up, and a few hardening ideas.

The earlier Dockerfile fix is correct. `--ignore-scripts` skips `prepare: lefthook install`, and the only dependencies with build scripts (`sharp`, `unrs-resolver`) are already turned off in `pnpm-workspace.yaml`, so nothing needed is skipped.

## Minor

### 🟡 Spec 0001 still contains the two sentences that spec 0002 says to fix, `docs/specs/0001-stack-kien-truc/index.md:151` and `:154`
**Problem**: Line 151 still says the overflow guard is a length CHECK and that seed with manual codes needs `setval`. Line 154 still says seed runs only in profile `dev`. Spec 0002 lists both as an open follow up, and the feature is marked done in the scope file.
**Why it matters**: AGENTS.md says the spec wins on conflict, and spec 0001 is the one the next feature authors read. They will read that seed is dev only and that `MAXVALUE` is not the guard, which is the opposite of what the migrations do.
**Suggested fix**: Edit the two sentences in spec 0001 as described in the follow up list of spec 0002, then tick that item.

### 🟡 Most of V3 to V9 has no behavioural test, `backend/src/test/java/vn/edu/hust/chungcu/SchemaMigrationTest.java`
**Problem**: Tests for tables after V2 are structural only (table count, sequence count, foreign key index query, collation list, no `timestamp without time zone`) plus one CHECK case on `thong_bao_phi`. No test inserts a row into any of the V3 to V9 tables. Untested items from spec 0002 section "Sửa so với file SQL gốc": `du_lieu_tra_loi` as JSONB, the new NOT NULL on `tien_ich_noi_khu.trang_thai_phuc_vu` and `phieu_yeu_cau_ho_tro.muc_do_uu_tien`, the `bang_ghi_chi_so.ky_ghi_chi_so` CHECK, and the XOR checks `chk_dongho_xor` and `chk_tep_xor`.
**Why it matters**: A typo in a value list or a default would only show up months later when the first entity for that table is written (the spec itself lists this as a known tradeoff). A few cheap tests would close most of that risk now.
**Suggested fix**: Add a small parameterised test that inserts one valid row per remaining table (using seed rows as parents, rolled back), plus negative cases for the two XOR checks, the `ky_ghi_chi_so` format and the JSONB column.

### 🟡 `mat_khau_hash` CHECK only accepts lowercase and digits in the id, `backend/src/main/resources/db/migration/V1__dinh_danh_xac_thuc.sql:27`
**Problem**: The regex `^\{[a-z0-9]+\}.+` rejects Spring `DelegatingPasswordEncoder` ids that contain uppercase, `@` or `_`, such as `{pbkdf2@SpringSecurity_v5_8}` and `{scrypt@SpringSecurity_v5_8}`.
**Why it matters**: The project uses bcrypt today, so nothing breaks. If someone switches the encoder, inserts fail with a CHECK error. Because V1 must never be edited after it has run, widening the pattern later needs a new migration.
**Suggested fix**: Decide now whether bcrypt or argon2 is the only supported scheme. If yes, record that in the spec. If not, widen the id pattern in a new migration before the auth feature lands.

### 🟡 Check violations will surface as HTTP 409 "xung đột", `backend/src/main/java/vn/edu/hust/chungcu/common/error/GlobalExceptionHandler.java:131`
**Problem**: This branch adds many CHECK constraints (email format, phone format, CCCD, period format). `DataIntegrityViolationException` is mapped to 409 CONFLICT with the message "Dữ liệu xung đột với dữ liệu đã có" for every integrity failure, including CHECK and NOT NULL.
**Why it matters**: Spec 0002 says the backend validates first and the CHECK is the last line of defence, so this should be rare. When it happens, the user sees a misleading conflict message and the real cause is only in the log. Not a bug in this branch, but the new constraints raise the odds.
**Suggested fix**: When the auth features land, make sure bean validation covers every field that has a CHECK. Optionally branch on SQLSTATE 23514 in the handler to return a validation style error.

### 🟡 Public seed password can be enabled on the demo stack with one word, `docker-compose.prod.yml:23`
**Problem**: `SPRING_PROFILES_ACTIVE: prod,seed` loads an Admin account with a password that is public in the repo. The spec accepts this for personal machines and the compose file only publishes port 3000, but nothing in code stops a prod style deployment from keeping `seed`.
**Why it matters**: Spec 0002 itself calls out that enabling seed on a real server exposes every account. A guard is cheap compared with the damage.
**Suggested fix**: Optional hardening: add a startup check (or a Flyway callback) that refuses to start when `seed` is active and a configured public host or non local database URL is used, or at least log a loud warning on startup when the seed profile is on.

## Nits

- ⚪ `backend/src/main/resources/db/migration/V2__khong_gian_cu_tru.sql:58`, `ho_so_cu_tru.ma_nvbql_xac_minh` is NOT NULL while status `Chờ xác minh` exists. Already listed as a follow up for UC02; just keep it visible.
- ⚪ `lefthook.yml:16`, the typecheck hook runs `next typegen` plus a full `tsc` on every commit that touches a `.ts` file. Slow but acceptable; consider it only if commits start to feel heavy.
- ⚪ `.github/workflows/ci.yml:34`, `git diff --exit-code` does not notice an untracked `openapi.json` (for example if the file was never committed). Using `git status --porcelain` on that path would also catch a new file.
- ⚪ `.github/workflows/ci.yml:14`, `cancel-in-progress: true` also cancels runs on `main` when two pushes arrive close together. Scope it to pull requests if you want every main commit to get a full run.
- ⚪ `backend/src/test/java/vn/edu/hust/chungcu/SchemaMigrationTest.java`, about 360 lines, above the roughly 200 line guideline in AGENTS.md. Splitting by concern (seed, constraints, catalog checks) would match the rule and match `BangXacThucSchemaTest`.
- ⚪ AGENTS.md does not yet contain the Java naming rule (abbreviations written as one word, for example `maNvbql`) that spec 0002 asks `/sync` to add. A mismatch fails `ddl-auto=validate` at startup, so it is worth having in the canonical file.
- ⚪ Spec 0002 recommends one shared static container for all test classes. The code uses two (`TestcontainersConfiguration`, `SeedTestcontainersConfiguration`) so the seeded database cannot leak into the empty database test. This is a sound deviation, and it is documented in AGENTS.md, but the spec text could be updated to match.

## Strengths

- The migrations follow the spec closely and consistently: sequences with `MAXVALUE 99999999 NO CYCLE`, matching format CHECKs, `ON DELETE RESTRICT` on role tables, a foreign key index query test that enforces the "every foreign key has an index" rule for all future migrations, and original constraint names preserved.
- The seed is idempotent and avoids `DO` blocks, resolves parents by natural key (phone number) instead of hard coded generated codes, and the rerun test proves row counts do not change.
- `ignore-migration-patterns` correctly keeps `*:future` while adding `repeatable:missing`, and there is a test that drops the `seed` profile on a seeded database and still validates and migrates.
- Error assertions use SQLSTATE rather than exception classes, including the correct 23001 for RESTRICT. The tests also include positive controls (for example `kyDungDangDuocChapNhan`) so a failing negative case cannot be caused by bad parent data.
- Existing tests are pinned to profile `dev` so a developer `.env` containing `seed` cannot pollute them. The format only changes to the shared `common` code and `api.ts` carry no behaviour changes.

## Test coverage

Backend uses JUnit 5 with Testcontainers on `postgres:18.7`, matching the project rule (no H2). Covered well: auto generated codes and overflow, format CHECKs, RESTRICT on user delete, every seed fact in AC-6, seed rerun, dropping the profile, 42 tables, no naive timestamps, collation list and Vietnamese ordering, foreign key indexes, and the new auth tables (token hash format, expiry ordering, revoke pair rule, no defaults for app supplied times, unique token hash) in `BangXacThucSchemaTest`. Not covered: row level behaviour of the V3 to V9 tables beyond the period CHECK, as detailed in the first minor finding. No frontend tests were added; the frontend change is tooling only, so I did not count that as a gap. I did not run the suite.
