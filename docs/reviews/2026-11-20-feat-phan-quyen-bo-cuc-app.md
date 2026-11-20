# Review, feat/phan-quyen-bo-cuc-app, 2026-11-20

**Reviewed by**: Sonnet 5.5 (author on a different model)
**Scope**: 75 files, branch vs origin/main (merge base d927693) plus 6 untracked frontend files
**Verdict**: Approve with nits

## Summary
The change adds role based access to the API with the `@ChoPhep`, `@DaDangNhap` and `@CongKhai` annotations, a Spring MVC interceptor that runs before parameter binding, a shared scope service, a shared paging helper, the first real endpoint `GET /api/v1/buildings`, and a role aware frontend (menu, page guard, overview page, buildings page). The design follows spec 0006 closely and the module boundaries from AGENTS.md. I found no blockers or majors. The remaining points are small robustness and polish items.

## Minor
### 🟡 Repository methods break on an empty scope list, `backend/src/main/java/vn/edu/hust/chungcu/canho/repository/ToaNhaQuery.java:36`
**Problem**: `dem` and `danhSach` bind `IN (:ma)` with the list as given. With an empty list the generated SQL is invalid or version dependent. Only `ToaNhaService.danhSach` guards the empty case.
**Why it matters**: A later caller of the repository, or a refactor of the service, turns an empty scope into a 500 instead of an empty page.
**Suggested fix**: Return early in the repository for an empty list, or state in the Javadoc that the list must not be empty.

### 🟡 Pages in the matrix but not built return HTTP 200 with a "not found" body, `frontend/src/app/(app)/[...duongDan]/page.tsx:14`
**Problem**: For a path such as `/can-ho` (in the matrix, no page yet) the catch all renders `KhongTimThay` as a normal page, so the status is 200. The comment explains why `notFound()` cannot be used.
**Why it matters**: Only a cosmetic and crawler issue for an internal app, but the trade off should stay visible so it is removed once the real pages exist.
**Suggested fix**: Keep as is, and note in the spec Consequences that each new module page replaces its catch all hit.

### 🟡 Ref mutated across calls in the URL updater, `frontend/src/app/(app)/toa-nha/toa-nha-view.tsx:449`
**Problem**: `useCapNhatUrl` keeps accumulated `URLSearchParams` in a ref keyed by the current query string. If a `router.replace` ends up as a no op (same final URL), the ref keeps stale accumulated values until the string changes.
**Why it matters**: Low risk now, but this pattern will be copied to every list page in later modules.
**Suggested fix**: Move it into a shared hook next to `parseTableParams` with its own test, covering two calls in one render and a no op update.

## Nits
- ⚪ `backend/src/main/java/vn/edu/hust/chungcu/auth/phanquyen/PhanQuyenOperationCustomizer.java:64`, the fully qualified `org.springframework.http.MediaType` is used only because of the name clash with the swagger `MediaType`; an alias comment or a constant for the JSON media type string would read better.
- ⚪ `backend/src/main/java/vn/edu/hust/chungcu/auth/phanquyen/VaiTroTrongToken.java:29`, the inner loop over `VaiTro.values()` can be a small lookup map built once.
- ⚪ `backend/src/main/java/vn/edu/hust/chungcu/auth/service/HoSoService.java:9`, import order puts `ChiTietVaiTro` after `HoSoResponse`; `spring-javaformat` does not enforce it but the neighbours are sorted.
- ⚪ `backend/src/main/java/vn/edu/hust/chungcu/auth/phanquyen/ChoPhepInterceptor.java:45`, the `AccessDeniedException` message "FORBIDDEN" is never shown to the user (the handler sets its own message); a clearer internal message would help debugging.

## Strengths
- The check runs in an interceptor before argument binding, so a wrong role always gets 403 before any 400, and the deny log leaves out query string, token and cookie.
- `PhanQuyenCoverage` is a pure class tested with a deliberately wrong sample controller, and it also keeps `@CongKhai` and `PublicPaths.CONG_KHAI` in sync, so a new endpoint cannot ship without a declared rule.
- One scope rule in `PhamViTinhToan` is shared by the profile page and the token based service, and the request scoped cache avoids repeat queries.
- `TrangYeuCau` validates every paging parameter as a string, reports errors per field in a fixed order, and the sort column comes from a whitelist map, so user text never reaches SQL.
- Frontend uses one matrix file for menu, overview and page guard, never renders a guarded page before the role is known, and does not offer "Thử lại" on 403. Axe tests cover the new views.

## Test coverage
Backend: unit tests for `PhamViTruyCapService` (including the Vietnam date boundary), `TrangYeuCau`, the coverage helper and its sample controller, the OpenAPI customizer, and an integration test class for `/buildings` on real PostgreSQL covering the role matrix, 401, 403 before 400, log content, defaults, out of range page and per field 400s. Frontend: tests for the menu logic, `ChanTrang`, `KhongCoQuyen`, `QueryErrorState`, the overview and buildings views, `AppNav`, `AppShell` and the not found pages. Small gap: `ToaNhaQuery` is only exercised through the integration test, and the URL updater hook has no direct test for the no op case.
