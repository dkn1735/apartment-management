# Review (re-review), feat/dang-nhap-ho-so (spec 0004), 2026-11-16

**Reviewed by**: Sonnet 5.5 (fresh review; author model not stated)
**Scope**: ~95 files (generated files skipped), branch vs main, scoped to 32fd1b6..working tree, plus fix commit 459f44d
**Verdict**: Approve with nits

## Previous review
- Major "CSRF header check skippable with percent-encoded path": **resolved** in 459f44d (see below).
- The 5 minors and 4 nits from the first review are unchanged in the code and still stand (listed again below, not blocking).

## Summary
The fix for the earlier major is correct. `CsrfHeaderFilter` now matches `/api/**` with `PathPatternRequestMatcher`, the same mechanism as `PublicPaths`, so the decoded and normalised path is used, and the regression test sends `POST /%61pi/v1/auth/login` without the header and gets 403 `CSRF_REJECTED` with no cookies set. No new blockers or majors. What remains are the earlier minors (none security-critical) and small nits.

## Verification of the CSRF fix
- Path semantics: `PathPatternRequestMatcher` parses the request path through `RequestPath` (context path stripped by the container-aware parse) and matches against decoded path segments, as Spring MVC does. `/%61pi/...` therefore matches. A context path is handled by `RequestPath`, which the old `substring(contextPath.length())` did by hand.
- Superset of the old check: `/api/**` also matches bare `/api`, so nothing the old `startsWith("/api/")` covered is lost.
- Other encodings: encoded `. / \ % ;` and `//` are rejected by `StrictHttpFirewall` before the filter chain. Case is matched case-sensitively, the same as MVC's default, so `/API/...` does not reach a handler. I found no remaining way for the filter and the handler to disagree.
- The filter is still not a bean (test `csrfHeaderFilterKhongLaBean`), so there is no double registration.
- Test is meaningful: asserts status, error code and that no `Set-Cookie` was issued. It does not run against a real servlet container (MockMvc with `URI.create` keeps the raw URI, so it does exercise the raw vs decoded difference), which is adequate here.

## Minor
### 🟡 Empty `COOKIE_SECURE` may silently turn off the Secure flag, `backend/.env.example:13` with `backend/src/main/resources/application.yml:63`
**Problem**: `.env.example` still ships `COOKIE_SECURE=` (empty). `${COOKIE_SECURE:true}` falls back only when the variable is unset, not when empty; an empty string bound to a primitive `boolean` is most likely `false`. Not run.
**Why it matters**: a non-docker production start from a copied `.env` could issue cookies without `Secure`. Docker prod is safe (`${COOKIE_SECURE:-true}`).
**Suggested fix**: comment the line out or set `COOKIE_SECURE=true`; optionally fail startup on blank, and test the empty-string case in `AppPropertiesTest`.

### 🟡 Service layer builds web headers, `backend/src/main/java/vn/edu/hust/chungcu/auth/service/HoSoService.java:23-39` and `common/error/ApiException.java`
**Problem**: `HoSoService` depends on `AuthCookies` and passes `HttpHeaders` into `ApiException`, putting transport detail in the service.
**Why it matters**: blurs the service/controller split and splits cookie-clearing policy between service and `GlobalExceptionHandler`. Spec accepts it; design smell only.
**Suggested fix**: throw a marker subclass or flag and let the handler map it to `AuthCookies.xoa()`.

### 🟡 CSRF filter uses a block-list of methods, `backend/src/main/java/vn/edu/hust/chungcu/common/security/CsrfHeaderFilter.java:30,56`
**Problem**: only `POST/PUT/PATCH/DELETE` are checked; other non-safe methods are not.
**Why it matters**: conforms to the spec text, but an allow-list of safe methods (`GET`, `HEAD`, `OPTIONS`) is the stricter default.
**Suggested fix**: require the header for everything except those three under `/api/**`.

### 🟡 Profile page can stay on the skeleton forever, `frontend/src/app/(app)/ho-so/ho-so-view.tsx:21-27,40`
**Problem**: on `UNAUTHENTICATED` or `AUTH_ACCOUNT_LOCKED` the page keeps the skeleton and relies on the global redirect. If that redirect is suppressed, the user sees a spinner with no action.
**Suggested fix**: after a delay, or if the redirect did not happen, show the error state with a link to `/dang-nhap`.

### 🟡 Profile read not in a read-only transaction, `HoSoService.java:33`
**Problem**: `cua` does 3 to 4 reads without a shared `@Transactional(readOnly = true)`, so no single snapshot. Practical risk is low.
**Suggested fix**: annotate `cua` with `@Transactional(readOnly = true)`.

## Nits
- ⚪ `auth/controller/AuthController.java:20`, `AuthCookies` import placed after `DangNhapService`.
- ⚪ `auth/service/DangNhapService.java:80`, `clock.instant()` is shorter than `Instant.now(this.clock)`.
- ⚪ Access tokens carry `sid` but nothing checks the session row; by design until feature 6, add a spec note so feature 7 does not assume revocation.
- ⚪ `ThongTinThietBi`, `forward-headers-strategy: framework` trusts `X-Forwarded-For` from any caller, so `dia_chi_ip` is spoofable if the backend is directly reachable; informational only.

## Strengths
- The CSRF fix reuses the same decoded-path matcher as `PublicPaths`, ships with a test that proves the bypass is closed, and its Javadoc explains why.
- Login flow follows the spec closely: constant-work fake-hash compare for unknown account, malformed account and over-72-byte password; same message and code for all; lock checked only after a correct password.
- Hash upgrade is outside the transaction with a conditional `WHERE mat_khau_hash = :hashCu`, so a concurrent password change is not overwritten.
- JWT config is tight (HS256 pinned, issuer and timestamp validators with Clock, startup secret checks, cookie-only source); refresh token is 32 `SecureRandom` bytes stored only as SHA-256.
- Roles and account status are read from the DB per profile call; cross-module access only through public services.

## Test coverage
Strong and now includes the encoded-path CSRF regression test. The newly added working-tree tests (`HoSoIntegrationTest` residency edge dates including Vietnam-time "today", all-building scope for staff, `vai-tro.test.ts` label and detail rows, updated profile view and form tests) close the earlier gaps on residency edge dates. Remaining gaps: empty `COOKIE_SECURE`, issuer-mismatch token, and the stuck-skeleton path on the profile page.
