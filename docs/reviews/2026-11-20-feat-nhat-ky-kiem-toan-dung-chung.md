# Review, feat/nhat-ky-kiem-toan-dung-chung, 2026-11-20

**Reviewed by**: Claude Sonnet 5.5 (author on a different model)
**Scope**: 38 files, branch vs eaebed1
**Verdict**: Approve with nits

## Summary
Adds the `nhatky` module: `NhatKyService.ghi` writes through `JdbcClient` in a `REQUIRES_NEW` transaction, deferred to `afterCommit` when the caller is in a transaction. It also adds migration V11 (JSONB, CHECKs, append-only trigger, indexes) and wires four auth events. The implementation follows spec 0007 closely: time, IP and request id are captured at call time, every failure is swallowed into an ERROR log without payload, and the sensitive-key guard is enforced at write time and by a classpath scan test. I found no correctness, security or contract defects. Remaining items are minor doc and robustness points.

## Minor
### 🟡 Spec follow-up for AGENTS.md rule not done, `AGENTS.md:36`
**Problem**: The AGENTS.md diff only adds the spec 0006 "Phân quyền" rule. The rule that spec 0007 Follow-up asks for ("audit logging only via `NhatKyService.ghi`, detail is a `ChiTietNhatKy` record, new type = `LoaiThaoTac` value with module prefix") is absent, and the Follow-up checkboxes in the spec are still open.
**Why it matters**: Later modules (features 10, 11) will not be told the convention by the canonical context file.
**Suggested fix**: Add the line (probably via /sync) and tick the Follow-up items closed by this feature.

### 🟡 Scope entry 8 marked done but text still lists later events, `docs/scope/scope.md` (feature 8 description)
**Problem**: The description still says the batch records "đổi mật khẩu, khóa và mở khóa tài khoản", but those belong to features 10 and 11 per the spec. Marking it `done` is misleading.
**Suggested fix**: Trim the description to the four auth events actually delivered.

### 🟡 Denylist of sensitive key names is a heuristic, `nhatky/service/TenKhoaNhayCam.java:15`
**Problem**: Words like `pwd`, `passwd`, `passphrase`, `jwt`, `authorization`, `apikey`, `credential` are not matched. The guard only inspects key names, never values.
**Why it matters**: It is the stated last line of defense for AC-8; the scan test only catches what the list knows.
**Suggested fix**: Extend the word set cheaply (`passwd`, `pwd`, `passphrase`, `jwt`, `authorization`, `credential`, `apikey`) and note in the Javadoc that values are not inspected.

### 🟡 Pool-exhaustion path of AC-7 untested and blocks the response, `nhatky/service/NhatKyService.java:86-93`
**Problem**: With `afterCommit`, the second connection is requested while the first is still held. Under pool starvation `them` waits up to `connection-timeout` (30 s) before logging and dropping the row. No test covers the "hết thời gian chờ kết nối" case listed in AC-7, and nothing bounds the wait on the request thread.
**Why it matters**: Spec accepts this as a tradeoff, but a bounded wait (or at least a test with a tiny pool) would show the main response is still correct.
**Suggested fix**: Optionally add a test with a pool of 1 and an unavailable second connection, or document the 30 s worst case in verify.md.

### 🟡 Failed-login path writes one unbounded row per attempt synchronously, `auth/service/DangNhapService.java:73,79,84`
**Problem**: Every unauthenticated failed login does an extra insert on the request thread, and rows cannot be deleted (trigger). This is acknowledged in spec Consequences and Follow-up (feature 12).
**Why it matters**: Cheap table-growth and write-amplification vector until IP rate limiting exists.
**Suggested fix**: No change needed now; keep the feature 12 follow-up visible.

## Nits
- ⚪ `auth/service/DangNhapService.java:57`, `LamMoiPhienService.java:69`: `this.nhatKy = nhatKy;` is assigned first in the constructor, out of declaration order.
- ⚪ `auth/service/LamMoiPhienService.java:138-140`: comment wrapped mid-sentence ("ghi sau" / "commit"); reflow.
- ⚪ `nhatky/service/NhatKyService.java:142`: the private exception subclass could be a plain return value or sealed result, but it is fine as is.

## Strengths
- Faithful implementation of the hard parts: `afterCommit` deferral with a captured snapshot (time, IP, request id), `REQUIRES_NEW` via `TransactionTemplate` (correctly avoiding self-invocation proxy trap), and logs that carry only the exception class name because PostgreSQL CHECK messages echo the row.
- Defense in depth for immutability and secrecy: DB trigger plus CHECKs plus a repository with a single insert method, a tested `lopGhiChiCoHamThem`, a classpath-scanning test over all `ChiTietNhatKy` records, and a full-column secret sweep after real auth flows.
- Migration V11 is a new file, indexes every FK path, and keeps the existing `(ma_nguoi_dung, thoi_gian_ghi)` index.
- Module boundaries respected: `nhatky` has no dependency on `auth`; auth only calls the public service.

## Test coverage
Good. Unit tests for `NhatKyService` use fakes through the constructor (immediate write, after-commit, rollback, repository failure, post-commit failure, context failure, sensitive key). Integration tests run on real PostgreSQL for login success/failure (wrong password, locked, unknown account, malformed input, validation 400 writes nothing), logout (single device, rotated token, other devices untouched), reuse detection, other refresh outcomes writing nothing, failure tolerance with unchanged HTTP responses, secret sweep, table trigger/CHECKs, and transaction semantics. Gaps: the pool-exhaustion timeout case from AC-7, and no concurrency test of the second-connection pattern.
