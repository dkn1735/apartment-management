# Verify: Mô hình dữ liệu · spec 0002 · updated 2026-11-16
_Steps derived from spec 0002 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Các lệnh `psql` dưới đây chạy trên database dev: `docker compose exec -T db psql -U chungcu -d chungcu -tAc "<SQL>"`.

## UI / manual
- [ ] Xóa volume (`docker compose down -v`), `docker compose up -d`, chạy backend với `SPRING_PROFILES_ACTIVE=dev,seed` → log có `Successfully applied 10 migrations`, `/actuator/health` trả `UP` → AC-1, AC-7
- [ ] Dừng backend, chạy lại với `SPRING_PROFILES_ACTIVE=dev` (bỏ `seed`) trên cùng database → khởi động không lỗi Flyway, không báo thiếu migration → AC-7
- [ ] `docker compose -f docker-compose.prod.yml up --build` trên volume trống → backend khởi động với `prod,seed`, có 8 tài khoản mẫu → AC-7

## Commands
- [ ] `cd backend && mvnw.cmd verify` → `Tests run: 26, Failures: 0`, `BUILD SUCCESS` → AC-1 đến AC-9
- [ ] `psql`: `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name<>'flyway_schema_history'` → `42` → AC-1
- [ ] `psql`: `SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND data_type='timestamp without time zone' AND table_name<>'flyway_schema_history'` → `0`; `khung_gio_tien_ich.gio_bat_dau` vẫn là `time without time zone` → AC-2
- [ ] `psql`: `\d nguoi_dung_he_thong` → có `so_lan_dang_nhap_sai`, `khoa_tam_den`, `dang_nhap_cuoi_luc`, `doi_mat_khau_luc`, `tao_luc`, `cap_nhat_luc`, `phien_ban`; có bảng `quan_tri_vien`, `nhan_vien_bao_ve`, `phien_dang_nhap`, `yeu_cau_dat_lai_mat_khau` → AC-3
- [ ] `psql`: `INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash) VALUES ('Thử', '0911111111', 'thu@x.vn', '{noop}x') RETURNING ma_nguoi_dung` (trong `BEGIN ... ROLLBACK`) → mã dạng `ND` cộng 8 chữ số → AC-4 (Value sourcing: mã khóa chính từ `seq_<ten_bang>`)
- [ ] `psql`: `SELECT DISTINCT max_value FROM pg_sequences WHERE sequencename LIKE 'seq\_%'` → chỉ `99999999`; đếm được 36 sequence → AC-4
- [ ] `psql`: `INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc) VALUES ('B', 'Tòa B', 1, 'x')` (trong `BEGIN ... ROLLBACK`) → mã `B` giữ nguyên như nhập, không có `DEFAULT` → AC-4 (Value sourcing: mã tòa, tầng, căn hộ do người dùng đặt)
- [ ] `psql`: lần lượt chèn email `An@x.vn`, số `+84900000009`, CCCD `001026000`, hash `abc`, kỳ `11/2026` vào `thong_bao_phi` → mỗi lệnh lỗi `23514` (check_violation) → AC-5
- [ ] `psql`: `DELETE FROM nguoi_dung_he_thong WHERE so_dien_thoai='0900000007'` → lỗi `23001` (restrict_violation) → AC-5
- [ ] `psql`: 8 tài khoản `09000000%`, `0900000006` có cả dòng `cu_dan` và `thanh_vien_ban_quan_tri`, `0900000008` là `Tạm khóa`, 1 tòa, 3 tầng, 3 căn hộ, 3 hồ sơ `Hợp lệ` → AC-6 (Value sourcing: `ma_nguoi_dung`, `ma_cu_dan`, `ma_nv` xác minh tra lại từ số điện thoại)
- [ ] Đăng nhập thử một tài khoản seed bằng `ChungCu@2026` khi tính năng 5 có (hoặc `PasswordEncoderFactories.createDelegatingPasswordEncoder().matches`) → khớp; thử một mật khẩu khác → không khớp → AC-6 (Value sourcing: `mat_khau_hash` viết sẵn trong file seed)
- [ ] `psql`: `SELECT min(tao_luc), max(tao_luc) FROM nguoi_dung_he_thong` → là thời điểm lúc seed chạy, có múi giờ → Value sourcing: `tao_luc`, `cap_nhat_luc` của seed từ `DEFAULT now()`
- [ ] `psql`: `SELECT column_default FROM information_schema.columns WHERE table_name IN ('phien_dang_nhap','yeu_cau_dat_lai_mat_khau') AND column_name IN ('tao_luc','ma_ho_phien')` → đều rỗng (app phải tự ghi từ `Clock` và tự sinh UUID) → Value sourcing: thời điểm do code nghiệp vụ ghi, `ma_ho_phien` do app sinh
- [ ] `psql`: chèn `phien_dang_nhap` với `token_hash` chữ hoa hoặc dài 63 ký tự (trong `BEGIN ... ROLLBACK`) → lỗi `23514` → Value sourcing: `token_hash` là SHA-256 hex chữ thường
- [ ] Chạy lại `db/seed/R__seed_01_xac_thuc.sql` bằng `psql -f` trên database đã seed → không lỗi, số dòng mọi bảng không đổi → AC-8
- [ ] `psql`: `SELECT ho_ten FROM (VALUES ('Đào'),('Bình'),('Ánh'),('Dương'),('Anh')) v(ho_ten) ORDER BY ho_ten COLLATE "vi-VN-x-icu"` → `Anh, Ánh, Bình, Dương, Đào`; `nguoi_dung_he_thong.ho_ten` có `collation_name = 'vi-VN-x-icu'` → AC-9 (Value sourcing: thứ tự từ collation khai báo trên cột)
- [ ] Đổi `SPRING_PROFILES_ACTIVE` trong `backend/.env` giữa `dev` và `dev,seed` rồi chạy `mvnw.cmd verify` → `ChungCuApplicationTests` vẫn thấy `nguoi_dung_he_thong` trống trong cả hai trường hợp → AC-7 (Value sourcing: bật seed chỉ qua profile `seed`)

## Acceptance-criteria coverage
- AC-1 · bước khởi động trên volume trống, đếm 42 bảng, `mvnw.cmd verify`
- AC-2 · không còn `timestamp without time zone`, cột `TIME` giữ nguyên; test `moiCotKhoaNgoaiDeuCoIndex`, `cacCotTenVaTieuDeDungCollationTiengViet`
- AC-3 · `\d nguoi_dung_he_thong` và 4 bảng mới
- AC-4 · mã `ND` tự sinh, `max_value` của 36 sequence, mã tòa do người dùng đặt; tràn sequence do test `sequenceVuotMaxValueThiBaoLoi` kiểm tra
- AC-5 · 5 trường hợp dữ liệu sai trả `23514`, xóa người dùng trả `23001`
- AC-6 · đếm tài khoản, vai trò, không gian, hồ sơ cư trú; khớp mật khẩu
- AC-7 · bỏ profile `seed` trên database đã seed; bản demo `prod,seed`; test không có seed bất kể `.env`
- AC-8 · chạy lại file seed
- AC-9 · thứ tự sắp xếp và collation của `ho_ten`
