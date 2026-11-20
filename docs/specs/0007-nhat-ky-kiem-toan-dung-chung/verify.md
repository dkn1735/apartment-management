# Verify: Ghi nhật ký kiểm toán dùng chung · spec 0007 · updated 2026-11-20
_Steps derived from spec 0007 acceptance criteria (feature 8 only, AC-1 to AC-10). `/check verify` runs these; `/test` locks the durable ones._

To read the table on the dev DB: `docker compose exec db psql -U <user> -d <db> -c "SELECT * FROM nhat_ky_he_thong ORDER BY ma_log DESC LIMIT 5"` (change the service name to match `docker-compose.yml`).

## UI / manual
- [ ] Sign in at `/dang-nhap` with a seed account → one new `AUTH_DANG_NHAP_THANH_CONG` row with your `ma_nguoi_dung`, `doi_tuong_tac_dong` `NguoiDungHeThong:<ma>`, `chi_tiet_thay_doi` `{"maHoPhien": ...}` equal to the `ma_ho_phien` of your new `phien_dang_nhap` row → AC-1
- [ ] Sign in with the right account and a wrong password → one `AUTH_DANG_NHAP_THAT_BAI` row, `{"lyDo": "SAI_THONG_TIN"}`, your `ma_nguoi_dung` → AC-2
- [ ] Lock a test account (`trang_thai_tai_khoan` not `Hoạt động`), sign in with the right password → 403 as before, one row with `{"lyDo": "TAI_KHOAN_KHOA"}` → AC-2
- [ ] Sign in as `0999999999` and as `abc` → 401 as before, one row each with `ma_nguoi_dung` NULL and `doi_tuong_tac_dong` `NguoiDungHeThong`; no column holds `0999999999`, `abc` or the password you typed → AC-3, AC-8
- [ ] Sign out from the user menu → one `AUTH_DANG_XUAT` row with the same `maHoPhien` as the session; sign out again (or call logout with no cookie) → no new row → AC-4

## Commands
- [ ] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, `openapi.json` unchanged → all ACs
- [ ] `mvnw.cmd test -Dtest=NhatKyDangNhapIntegrationTest` → login rows with IP, request id, `Clock` time cut to micro seconds; 400 validation makes no row → AC-1, AC-2, AC-3
- [ ] `mvnw.cmd test -Dtest=NhatKyPhienIntegrationTest` → logout row; reuse past 30 s grace writes `AUTH_PHAT_HIEN_DUNG_LAI_TOKEN` even with a 401; refresh, grace reuse and dead family reuse write nothing → AC-4, AC-5
- [ ] `mvnw.cmd test -Dtest=NhatKyGiaoDichIntegrationTest` → no tx writes at once; in a tx writes after commit with call time; rollback writes nothing → AC-6
- [ ] `mvnw.cmd test -Dtest=NhatKyChiuLoiIntegrationTest,NhatKyServiceTest` → with a broken writer, login still 200 with both cookies, logout still 200, one ERROR line with `loaiThaoTac`, `doiTuongTacDong`, `maNguoiThucHien`, `requestId` and the exception class, no detail content → AC-7
- [ ] `mvnw.cmd test -Dtest=TenKhoaNhayCamTest,ChiTietNhatKyAnToanTest,NhatKyBiMatIntegrationTest` → sensitive names caught, every `ChiTietNhatKy` record clean, no password, token or hash in any column after all auth flows → AC-8
- [ ] `mvnw.cmd test -Dtest=NhatKyBangIntegrationTest` → UPDATE and DELETE rejected with "Nhật ký hệ thống chỉ được thêm, không được sửa hay xóa"; bad `loai_thao_tac`, `doi_tuong_tac_dong` or non object `chi_tiet_thay_doi` rejected by CHECK → AC-9
- [ ] `mvnw.cmd test -Dtest=LoaiThaoTacTest` → four `AUTH_` values, NFC labels, no duplicate labels → AC-10

## Value sourcing (one step per row, feature 8 rows)
- [ ] `ma_log`: matches `^LOG[0-9]{8}$`, set by the DB sequence, not by the app
- [ ] `ma_nguoi_dung`: login uses the found account (NULL when unknown); logout uses the session owner even when you sign out with an already rotated token; reuse uses `dongCu.maNguoiDung`
- [ ] `loai_thao_tac`: equals the enum name exactly
- [ ] `doi_tuong_tac_dong`: always `NguoiDungHeThong:<ma_nguoi_dung>` here, plain `NguoiDungHeThong` when the user is NULL
- [ ] `chi_tiet_thay_doi`: `jsonb_typeof` is `object`; key order and spacing do not matter
- [ ] `maHoPhien` in detail: login matches the new session family; logout and reuse match the family the cookie belonged to (try a second device: its family is untouched and not logged)
- [ ] `lyDo`: same value as the existing app log line for that branch
- [ ] `thoi_gian_ghi`: comes from the `Clock` at call time; with `MutableClock` advanced inside a tx before commit, the row keeps the earlier time
- [ ] `dia_chi_ip`: equals `getRemoteAddr()`; send `X-Forwarded-For: 198.51.100.4` through the Next.js proxy and see that IP; a 60 char value is cut to 45
- [ ] `request_id`: equals the `X-Request-Id` response header; send your own valid id and see it stored

## Acceptance-criteria coverage
- AC-1 manual 1, commands 2 · AC-2 manual 2 and 3, commands 2 · AC-3 manual 4, commands 2 · AC-4 manual 5, commands 3 · AC-5 commands 3 · AC-6 commands 4 · AC-7 commands 5 · AC-8 manual 4, commands 6 · AC-9 commands 7 · AC-10 commands 8
- AC-11 to AC-14 belong to feature 9 (read API and screen), not checked here
