# Verify: Ghi nhật ký kiểm toán dùng chung · spec 0007 · updated 2026-11-20
_Steps derived from spec 0007 acceptance criteria (feature 8 only, AC-1 to AC-10). `/check verify` runs these; `/test` locks the durable ones._

To read the table on the dev DB: `docker compose exec db psql -U <user> -d <db> -c "SELECT * FROM nhat_ky_he_thong ORDER BY ma_log DESC LIMIT 5"` (change the service name to match `docker-compose.yml`).

## UI / manual
- [x] Sign in at `/dang-nhap` with a seed account → one new `AUTH_DANG_NHAP_THANH_CONG` row with your `ma_nguoi_dung`, `doi_tuong_tac_dong` `NguoiDungHeThong:<ma>`, `chi_tiet_thay_doi` `{"maHoPhien": ...}` equal to the `ma_ho_phien` of your new `phien_dang_nhap` row → AC-1
- [x] Sign in with the right account and a wrong password → one `AUTH_DANG_NHAP_THAT_BAI` row, `{"lyDo": "SAI_THONG_TIN"}`, your `ma_nguoi_dung` → AC-2
- [x] Lock a test account (`trang_thai_tai_khoan` not `Hoạt động`), sign in with the right password → 403 as before, one row with `{"lyDo": "TAI_KHOAN_KHOA"}` → AC-2
- [x] Sign in as `0999999999` and as `abc` → 401 as before, one row each with `ma_nguoi_dung` NULL and `doi_tuong_tac_dong` `NguoiDungHeThong`; no column holds `0999999999`, `abc` or the password you typed → AC-3, AC-8
- [x] Sign out from the user menu → one `AUTH_DANG_XUAT` row with the same `maHoPhien` as the session; sign out again (or call logout with no cookie) → no new row → AC-4

## Commands
- [x] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, `openapi.json` unchanged → all ACs
- [x] `mvnw.cmd test -Dtest=NhatKyDangNhapIntegrationTest` → login rows with IP, request id, `Clock` time cut to micro seconds; 400 validation makes no row → AC-1, AC-2, AC-3
- [x] `mvnw.cmd test -Dtest=NhatKyPhienIntegrationTest` → logout row; reuse past 30 s grace writes `AUTH_PHAT_HIEN_DUNG_LAI_TOKEN` even with a 401; refresh, grace reuse and dead family reuse write nothing → AC-4, AC-5
- [x] `mvnw.cmd test -Dtest=NhatKyGiaoDichIntegrationTest` → no tx writes at once; in a tx writes after commit with call time; rollback writes nothing → AC-6
- [x] `mvnw.cmd test -Dtest=NhatKyChiuLoiIntegrationTest,NhatKyServiceTest` → with a broken writer, login still 200 with both cookies, logout still 200, one ERROR line with `loaiThaoTac`, `doiTuongTacDong`, `maNguoiThucHien`, `requestId` and the exception class, no detail content → AC-7
- [x] `mvnw.cmd test -Dtest=TenKhoaNhayCamTest,ChiTietNhatKyAnToanTest,NhatKyBiMatIntegrationTest` → sensitive names caught, every `ChiTietNhatKy` record clean, no password, token or hash in any column after all auth flows → AC-8
- [x] `mvnw.cmd test -Dtest=NhatKyBangIntegrationTest` → UPDATE and DELETE rejected with "Nhật ký hệ thống chỉ được thêm, không được sửa hay xóa"; bad `loai_thao_tac`, `doi_tuong_tac_dong` or non object `chi_tiet_thay_doi` rejected by CHECK → AC-9
- [x] `mvnw.cmd test -Dtest=LoaiThaoTacTest` → four `AUTH_` values, NFC labels, no duplicate labels → AC-10

## Value sourcing (one step per row, feature 8 rows)
- [x] `ma_log`: matches `^LOG[0-9]{8}$`, set by the DB sequence, not by the app
- [x] `ma_nguoi_dung`: login uses the found account (NULL when unknown); logout uses the session owner even when you sign out with an already rotated token; reuse uses `dongCu.maNguoiDung`
- [x] `loai_thao_tac`: equals the enum name exactly
- [x] `doi_tuong_tac_dong`: always `NguoiDungHeThong:<ma_nguoi_dung>` here, plain `NguoiDungHeThong` when the user is NULL
- [x] `chi_tiet_thay_doi`: `jsonb_typeof` is `object`; key order and spacing do not matter
- [x] `maHoPhien` in detail: login matches the new session family; logout and reuse match the family the cookie belonged to (try a second device: its family is untouched and not logged)
- [x] `lyDo`: same value as the existing app log line for that branch
- [x] `thoi_gian_ghi`: comes from the `Clock` at call time; with `MutableClock` advanced inside a tx before commit, the row keeps the earlier time
- [x] `dia_chi_ip`: equals `getRemoteAddr()`; send `X-Forwarded-For: 198.51.100.4` through the Next.js proxy and see that IP; a 60 char value is cut to 45
- [x] `request_id`: equals the `X-Request-Id` response header; send your own valid id and see it stored

## Acceptance-criteria coverage
- AC-1 manual 1, commands 2 · AC-2 manual 2 and 3, commands 2 · AC-3 manual 4, commands 2 · AC-4 manual 5, commands 3 · AC-5 commands 3 · AC-6 commands 4 · AC-7 commands 5 · AC-8 manual 4, commands 6 · AC-9 commands 7 · AC-10 commands 8
- AC-11 to AC-14 belong to feature 9 (read API and screen), not checked here

# Verify: Màn hình tra cứu nhật ký kiểm toán · spec 0007 · updated 2026-11-21
_Steps for feature 9 (AC-11 to AC-14). Run with the `seed` profile; every seed account uses password `ChungCu@2026`. `/check verify` runs these; `/test` locks the durable ones._

## UI / manual
- [x] Sign in as `0900000001` (Admin), `0900000002` (BQL) or `0900000006` (BQT): the menu shows "Nhật ký hệ thống", and `/nhat-ky` shows a table with the newest row first → AC-11
- [x] Sign in as `0900000004` (Kế toán) or `0900000007` (Cư dân): there is no menu item, and opening `/nhat-ky` directly shows the "no permission" page → AC-11
- [x] Sign in with a wrong password for an account that doesn't exist, then open `/nhat-ky` as Admin: that row shows "Không xác định" as the actor, with IP and request id filled in → AC-11, AC-13
- [x] The "Thời gian" column shows Vietnam time (`dd/MM/yyyy HH:mm`); click its header and the order flips; click "Thao tác" and rows group by code → AC-11
- [x] The "Loại thao tác" select lists the four types in declaration order, plus "Tất cả loại thao tác" → AC-14
- [x] Filter by user code, by type, then by from and to dates: the URL gets the parameters, the page resets to 1, and Back restores the old filter in the form → AC-12
- [x] Type `nd1` as the user code, or a to date before the from date: the error shows under that field and the URL does not change → AC-12
- [x] A filter that matches nothing shows "Không có dòng nhật ký nào khớp bộ lọc" and a "Xóa bộ lọc" button that clears the URL → scope Done when
- [x] Edit the URL by hand to `/nhat-ky?tuNgay=2026-02-30`: an alert lists "Ngày phải có dạng yyyy-MM-dd" and offers "Xóa bộ lọc" → AC-12
- [x] Stop the backend and reload: the table shows the error state with "Thử lại" → scope Done when
- [x] Do all of the above with only the keyboard: focus is visible on every control, and every field has a label → AGENTS.md WCAG AA

## Commands
- [x] `cd backend && mvnw.cmd test -Dtest=NhatKyTraCuuIntegrationTest` → 18 tests pass → AC-11 to AC-14
- [x] `cd frontend && pnpm vitest run "src/app/(app)/nhat-ky"` → all pass, including the axe checks → AC-11, AC-12, AC-14
- [x] `GET /api/v1/system/audit-logs?page=0&size=500&sort=maLog,asc&maNguoiDung=x&loaiThaoTac=y&tuNgay=z&denNgay=w` as Admin → 400 `VALIDATION_FAILED` with `errors` in the order page, size, sort, maNguoiDung, loaiThaoTac, tuNgay, denNgay → AC-12
- [x] Count rows (`SELECT max(ma_log) FROM nhat_ky_he_thong`) before and after reading the list a few times → unchanged, because reading never writes → AC-11

## Value sourcing
- [x] `hoTen`: change a user's `ho_ten` in the DB, then reload → the list shows the new name for old rows too (current name, not the name at the time) → AC-13
- [x] `tenThaoTac`: insert a row with `loai_thao_tac` `AUTH_DA_BO` → the list shows the code itself and does not fail → AC-11
- [x] `tuNgay`/`denNgay` boundaries: rows at `2026-03-09T17:00:00Z` and `2026-03-10T16:59:59Z` show for `tuNgay=denNgay=2026-03-10`, while `2026-03-09T16:59:59Z` and `2026-03-10T17:00:00Z` do not (Vietnam day, `app.timezone`) → AC-12
- [x] `totalItems`/`totalPages`: with a filter on, go to `page=99` → empty items, totals match the filtered count → AC-11
- [x] `ma`/`nhan` of the type list: compare with the `LoaiThaoTac` enum order → AC-14

## Acceptance-criteria coverage
- AC-11 manual 1, 2, 4, 9, commands 1, 4, value sourcing 2, 4 · AC-12 manual 6, 7, 9, commands 3, value sourcing 3 · AC-13 manual 3, value sourcing 1 (query count of at most three is by design, not yet asserted by a test) · AC-14 manual 5, value sourcing 5
