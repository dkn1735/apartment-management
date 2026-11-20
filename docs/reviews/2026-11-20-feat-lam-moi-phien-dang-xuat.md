# Review, feat/lam-moi-phien-dang-xuat, 2026-11-20

**Reviewed by**: claude-sonnet-5-5 (author model not stated)
**Scope**: 51 files, branch vs origin/main (merge base 04e6e66, includes uncommitted working tree changes)
**Verdict**: Approve with nits

## Summary
Adds rotating refresh tokens grouped by session family (`ma_ho_phien`), an HMAC-derived next token so a lost refresh response is answered idempotently inside a 30 s grace window, reuse detection that revokes the whole family, logout, a daily purge job, and a frontend single-flight silent refresh on 401 plus a user menu with logout. The design is sound: every write to a family goes through a Postgres advisory lock, the partial unique index backs the "one live row per family" invariant, cookies are set only after commit, and revocation commits despite the thrown `ApiException`. I found no blockers or majors; the findings below are narrow races and maintainability nits.

## Minor
### 🟡 Logout flag is not re-checked after the async 401 body check, `frontend/src/lib/api/refresh.ts:106-113`
**Problem**: `dangXuat` is read at line 106, then the middleware awaits `laUnauthenticated(response)` (line 109) before calling `lamMoiPhien`. If the user clicks "Đăng xuất" inside that await (the `batDauDangXuat()` call happens synchronously in `mutationFn`), a new refresh still starts after logout began.
**Why it matters**: AC-14 says that from the click no new refresh may start. Server side the family lock keeps the data correct (logout revokes whichever row is live), but the refresh response's `Set-Cookie` can land after the logout response's cookie deletion, leaving `access_token` and `phien` in the browser for a revoked session (up to 15 min of access token, and `proxy.ts` sees `phien`). The window is a few microtasks, so it is unlikely.
**Suggested fix**: re-check `dangXuat` immediately before `lamMoiPhien(...)` (and return `undefined` if set). Add a test that flips the flag between the 401 response and the refresh.

### 🟡 A stale 401 starts a redundant rotation, `frontend/src/lib/api/refresh.ts:27-47,103-120`
**Problem**: Single flight only dedupes refreshes that overlap in time. A request that was sent with the old access cookie but whose 401 arrives after the shared refresh finished (the promise is cleared in `finally`) triggers a second refresh and a second rotation, although the cookies are already fresh.
**Why it matters**: Harmless for correctness (the grace window absorbs concurrent users of the previous token), but it needlessly grows the token chain and is the main way a tab can approach the 3-step limit of `timTheoChuoi` under bursts of requests.
**Suggested fix**: remember a "last refresh completed at" counter and, when a 401 belongs to a request that started before it, just retry without refreshing. Optional.

### 🟡 `noRollbackFor = ApiException.class` covers the whole method, `backend/.../auth/service/LamMoiPhienService.java:73`
**Problem**: The rule exists so family revocation (AC-4, AC-6) commits when the method throws. It also applies after the rotation writes (lines 104-110): if `ketQua(...)` (role query, JWT signing) ever threw an `ApiException`, the old row would stay revoked and the new row committed while the client gets an error and cleared cookies.
**Why it matters**: Today nothing in `ketQua` throws an `ApiException`, so there is no live bug; the failure mode is a silently logged-out user, and it is easy to introduce later.
**Suggested fix**: compute role and access token before the rotation writes, or add a one-line comment/test pinning that nothing after the writes may throw `ApiException`.

## Nits
- ⚪ `backend/.../auth/service/LamMoiPhienService.java:1-214`, 214 lines including the record, slightly over the "about 200 lines, one responsibility" rule; the grace-chain walk (`dungLaiTokenDaXoay`, `timTheoChuoi`) could move to its own class.
- ⚪ `backend/.../auth/service/PhienDangNhapService.java:354`, `donDep()` (purge) sits next to `taoPhien()` (login write); a small `PhienDangNhapDonDep`-style service would keep one responsibility per class. Also `donDep` does not truncate to micros like the other time sources (harmless for a `<` cutoff).
- ⚪ `backend/.../auth/repository/PhienDangNhapRepository.java:27-36`, `lock_timeout` is set transaction-wide with `set_config(..., true)`, so it also applies to every later statement in the transaction, not just the advisory lock. Fine today; worth a comment.
- ⚪ `backend/.../auth/repository/PhienDangNhapRepository.java:52-55`, `xoaCu` is one unbounded `DELETE`; fine at this scale, batch it if the table grows.
- ⚪ `frontend/src/lib/api/refresh.ts:98-100`, every non-GET request body is cloned for a possible retry; will double memory for future file uploads. Consider skipping the clone for multipart or large bodies.
- ⚪ `backend/.../auth/service/DangNhapService.java:81-84`, the comment was line-wrapped mid-reference (`(spec` / `0005)`); reflow.

## Strengths
- Concurrency design is correct and well tested: advisory lock per family before any write, re-read of the row under the lock, partial unique index as last-resort guard, cookie set after commit, revocation committed through `noRollbackFor`.
- Reuse detection and grace semantics match the spec precisely, including the "dead family: reject without revoke and without WARN" rule, the 3-step cap, and the not-active-account path inside the grace branch. Anti-oracle behavior (same code and message for every failure) holds.
- HMAC next-token scheme is sound: key derived with a fixed label rather than reusing the JWT key, a fresh `Mac` per call, same format as random tokens, and secrets never logged or in `toString`.
- Cookie flags, `Path`, `SameSite=Strict`, CSRF header requirement on the now public endpoints, and cookie-only token intake (including a test that body, header and query tokens are ignored) are all correct.
- `AppProperties` validates grace (0 to 60 s) and retention >= max lifetime at startup, which is what makes the purge safe for live families.
- Frontend: shared single-flight refresh, body cloning for POST replay, retry outside the middleware (no recursion), logout waiting for an in-flight refresh, and accessible menu with fallback label.

## Test coverage
Strong. Backend integration tests (Testcontainers, `MutableClock`) cover rotation and lifetime caps, micro-second truncation, reuse detection with WARN logging, grace branch (single step, two step chain, non-matching chain, post-logout, locked account, device flag), lost-response scenario, parallel refresh and parallel logout versus refresh, cleanup boundaries (`DonPhienIntegrationTest`), log secrecy, schema constraints, token generator unit tests and `AppProperties` validation. Frontend tests cover single flight, replay of GET and POST bodies, no refresh on auth paths and during logout, failure mapping (invalid, locked, CSRF, network), and the menu with axe. Remaining gaps: the logout-flag race noted above and the stale-401 extra rotation have no test.
