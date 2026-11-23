# Review, feat/quen-mat-khau-email (spec 0010 slice), 2026-11-23

**Reviewed by**: Claude Sonnet 5.5 (fresh reviewer; author model not stated)
**Scope**: 82 files (git diff 32f73bb: committed 38c9c96..d2777de plus uncommitted docs/scope/scope.md and verify.md), branch vs main, scoped to the forgot-password slice
**Verdict**: Approve with nits

## Summary
The slice adds a one-time emailed reset link: a 256-bit SecureRandom token stored only as SHA-256, the same response for every branch of `request`, per-account and global rate limits counted from the table itself, token consumption in a single conditional UPDATE under an account row lock, full session revocation, and mail sent AFTER_COMMIT on a bounded executor. I traced the security-sensitive paths (enumeration, single use, lock order, token leakage, header injection, executor bounds, cleanup) and found no blocker or major. What remains is a few narrow races, one spec/code drift and small a11y and tidiness points.

## Minor
### 🟡 Reset link can go to a stale email address, `backend/src/main/java/vn/edu/hust/chungcu/auth/service/QuenMatKhauService.java:55` and `GhiYeuCauDatLaiService.java:45-68`
**Problem**: `timTheoEmail` runs outside the transaction and the event is built from that snapshot (`tk.email()`, `tk.hoTen()`). Inside the transaction only `trang_thai_tai_khoan` is re-read after the lock. If an admin changes the account's email (which runs `huyConMo` and commits) between the lookup and the request transaction, the new row is inserted after the cancel and the link is mailed to the OLD address.
**Why it matters**: The previous mailbox owner receives a valid, live reset link for an account now bound to another email. The window is tiny and needs an email edit at that exact moment, so it is not a blocker, but it is exactly the case AC-10 means to close.
**Suggested fix**: Have `khoaTaiKhoan` return the locked row's current email (and name), and compare with the request email. Abort as `KHONG_HOAT_DONG` or use the locked values for the event.

### 🟡 Global ceiling (100 per hour) is not atomic across accounts, `GhiYeuCauDatLaiService.java:51`
**Problem**: The lock is per account, but the global `count(*)` is read before the insert with no shared lock. Concurrent requests for different accounts can all see 99 and all insert.
**Why it matters**: The cap can be overshot by roughly the concurrency level. It is a soft guard for the Gmail quota, so the impact is small, but the spec words it as a hard ceiling ("không tạo dòng nào nữa").
**Suggested fix**: Accept and say so in the spec or Javadoc, or take a transaction-scoped advisory lock around the global count and insert.

### 🟡 Spec and code disagree on how the listener goes async, `EmailDatLaiMatKhauListener.java:38-46`, `AsyncConfig.java`
**Problem**: Spec 0010 (Luồng yêu cầu, Lớp mới) says `@Async("emailExecutor")`, `@EnableAsync` and a `RejectedExecutionHandler` that logs. The code calls `executor.execute(...)` and catches `TaskRejectedException`, with no `@EnableAsync`. The approach is sound and better, since the log carries `maYeuCau`, but the spec is now wrong, and so is `spring.task.execution.mode: force`, which was added for `@Async` and may now be unnecessary.
**Why it matters**: Misleading source of truth for the next module that sends email, and a stray config line with no clear reason.
**Suggested fix**: Update spec 0010 to match the code (via /sync or /architect). Drop or justify `spring.task.execution.mode: force` in `application.yml`.

### 🟡 Focus is lost after pressing "Gửi lại", `frontend/src/app/quen-mat-khau/quen-mat-khau-form.tsx:86-89`
**Problem**: The "Gửi lại" button unmounts when `daGui` becomes false and the form mounts. Focus falls back to `body`, and the status message disappears with no announcement.
**Why it matters**: WCAG AA keyboard and screen-reader users lose their place. The project requires full keyboard use.
**Suggested fix**: Move focus to the email input (or the h1) after "Gửi lại". Add a test for it.

### 🟡 `check` and `request` have no throttle of their own, `QuenMatKhauController.java:307,316`
**Problem**: `request` has rate limits per account and globally but nothing per client. `check` is a DB lookup per call for any well-formed token. The spec lists per-IP limiting as deferred, so this is not a regression.
**Why it matters**: An anonymous caller who knows about 20 real emails can exhaust the global 100 per hour and block everyone's reset emails for the hour (the spec accepts this). It also makes timing probes cheap. I raise it only to keep the follow-up visible.
**Suggested fix**: Keep the spec follow-up on per-IP limiting and make sure it covers `/password-reset/request`.

## Nits
- ⚪ `backend/src/main/java/vn/edu/hust/chungcu/auth/service/TaiKhoanHopLe.java:32` depends on the constant `YeuCauDatLaiRequest.EMAIL_REGEX` in a DTO. A shared constant in a neutral place (for example a validation helper in `common`) would avoid service-to-DTO coupling.
- ⚪ `frontend/src/proxy.ts:28` the matcher exclusions are prefix matches, so `/quen-mat-khau-xyz` would also bypass the proxy. This already applies to `dang-nhap`. Anchor with `(?:/|$)` if you care.
- ⚪ `frontend/src/app/dat-lai-mat-khau/dat-lai-mat-khau-form.tsx:85` `(data as { hetHanLuc: string })` casts away the generated type. Type the `api()` result from the schema instead.
- ⚪ `backend/.../auth/controller/QuenMatKhauController.java:327` `xacNhan` returns `int soHo` that the controller ignores. Either make the service return void or leave a comment for why it returns the count (tests?).
- ⚪ `backend/.../auth/service/QuenMatKhauService.java:51-53` the comment was wrapped mid-sentence by the formatter ("đúng" on its own line). Reword it so the formatter keeps it readable.
- ⚪ `backend/.../auth/service/EmailDatLaiMatKhauListener.java:53` only `RuntimeException` is caught in the background task. An `Error` (for example `NoClassDefFoundError` from the mail library) would escape without the `maYeuCau` log line. Catching `Exception | LinkageError` or `Throwable` here is cheap.

## Strengths
- Token handling is textbook: 32 bytes from `SecureRandom`, only the SHA-256 in the DB, fragment link (never query or path), body-only API, `Referrer-Policy: no-referrer`, `toString()` of requests, events and `EmailGui` redacted, and log lines carry only `maYeuCau` or `maNguoiDung` and the exception class name.
- Single use and the race are handled in the right place: account `FOR NO KEY UPDATE`, then a conditional `UPDATE ... WHERE da_dung_luc IS NULL AND huy_luc IS NULL AND het_han_luc > :now`, with rollback on any later failure, so the token is spent only if the password changed. Bcrypt runs outside the transaction and not at all for a bad token. Lock order is the same in all six flows.
- The DB backs the invariants: partial unique index for one open request, CHECK against used and cancelled together, new indexes with the FK-leading column kept, and a new `V14` file rather than editing an old one.
- Email goes after commit on a bounded executor (2 threads, queue 100, SMTP timeouts 10 s), with the rejection handled and logged with the request id. HTML escaping of the name and link is correct, and the link is built only from `app.public-url` (no Host header). Prod fails fast on missing `MAIL_HOST`, `MAIL_FROM` and `APP_PUBLIC_URL`.
- All cancel and revoke paths required by AC-10 and AC-7 are wired, including the session family revoke and cookie clearing. The frontend reads the token once into a `useRef`, strips the hash and survives StrictMode, and the error UI keys on `errorCode`.

## Test coverage
Good and mostly behavior-driven, on real PostgreSQL with GreenMail: happy path, four-case response equality, SMTP failure with a log-secret scan, 60 s and 5 per hour limits, 10 parallel requests, global cap, partial unique index, all token-invalid branches with bcrypt not invoked, parallel confirms, password rules not consuming the token, cancel from each of four flows plus a deadlock test, audit rows, cleanup job (8-day and 6-day rows, error logged), prod config failures, listener unit test with a rejected executor, and Vitest for both pages and the login link. Gaps: no test for the stale-email race above, no test of focus after "Gửi lại", and the global-ceiling test is sequential, so it cannot show the overshoot under concurrency. The secret-in-log scan covers the main paths; there is no assertion on Tomcat or access logs, which is out of scope here.
