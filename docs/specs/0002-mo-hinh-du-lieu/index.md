# 0002. Mô hình dữ liệu: chuyển schema 38 bảng sang Flyway và bổ sung cho xác thực

**Date**: 2026-11-16
**Status**: Accepted

## Summary

Spec này đưa 38 bảng trong `sql/database_schema_quan_ly_chung_cu.sql` vào migration Flyway, đổi tên sang snake_case và sửa một số lỗi rõ ràng, đồng thời thêm 4 bảng và vài cột mà phân hệ xác thực cần (vai trò Admin và Bảo vệ, phiên đăng nhập, mã đặt lại mật khẩu, bộ đếm đăng nhập sai). Dữ liệu mẫu (seed, tức dữ liệu nạp sẵn để dùng thử) gồm 8 tài khoản phủ mọi vai trò cùng một tòa nhà nhỏ, chỉ nạp khi backend chạy với profile `seed`, nên có ở dev và bản demo nhưng không có trong test. Tính năng này chỉ giao migration, seed và test; entity JPA do từng tính năng tự viết khi cần.

## Requirements

**User stories**:
- Là thành viên nhóm phát triển, tôi muốn một lệnh khởi động tạo đủ schema trên database trống, để không ai phải chạy SQL bằng tay.
- Là người làm tính năng 5 đến 13, tôi muốn nơi lưu phiên, mã đặt lại mật khẩu, bộ đếm đăng nhập sai và vai trò Admin, Bảo vệ đã có sẵn, để chỉ còn viết logic.
- Là nhóm bảo vệ đồ án, tôi muốn bản demo khởi động là có tài khoản cho mọi vai trò, để trình diễn ngay.

**Acceptance criteria**:
- **AC-1**: Trên một database PostgreSQL 18 trống, khởi động backend làm Flyway tạo đủ 42 bảng (38 bảng gốc và 4 bảng mới) mà không lỗi, và `ddl-auto=validate` vẫn qua.
- **AC-2**: Schema giữ cấu trúc của file SQL gốc (bảng, cột, quan hệ, CHECK), tên theo snake_case, cộng đúng các quy ước ở mục *Quy ước chung cho mọi migration* (mã tự sinh, collation, CHECK định dạng), các bảng và cột ở mục *Bảng mới và bảng thay đổi*, và các chỗ sửa ở mục *Sửa so với file SQL gốc*; không cột thời điểm nào là `timestamp without time zone` (cột giờ trong ngày kiểu `TIME` giữ nguyên, cố ý).
- **AC-3**: Có bảng `quan_tri_vien`, `nhan_vien_bao_ve`, `phien_dang_nhap`, `yeu_cau_dat_lai_mat_khau`, và `nguoi_dung_he_thong` có thêm các cột `so_lan_dang_nhap_sai`, `khoa_tam_den`, `dang_nhap_cuoi_luc`, `doi_mat_khau_luc`, `tao_luc`, `cap_nhat_luc`, `phien_ban`.
- **AC-4**: Chèn một dòng không ghi mã thì DB tự sinh mã đúng dạng tiền tố cộng 8 chữ số (ví dụ `ND00000001`); sequence vượt `99999999` thì báo lỗi chứ không sinh mã trùng. Tòa nhà, tầng, căn hộ không có mã tự sinh.
- **AC-5**: DB từ chối dữ liệu sai: email có chữ hoa hoặc khoảng trắng đầu cuối, số điện thoại không đúng dạng `0` cộng 9 chữ số, CCCD không đủ 12 chữ số, `mat_khau_hash` không có tiền tố `{id}`, kỳ không đúng dạng `YYYY-MM`, và xóa một người dùng còn dòng vai trò.
- **AC-6**: Khi seed bật, database có 8 tài khoản ở mục *Dữ liệu mẫu*, phủ đủ 7 vai trò, có một tài khoản giữ hai vai trò (Cư dân và BQT) và một tài khoản `Tạm khóa`; có 1 tòa, 3 tầng, 3 căn hộ và hồ sơ cư trú `Hợp lệ` cho mỗi cư dân; mật khẩu dev chung khớp với `mat_khau_hash` của mọi tài khoản seed.
- **AC-7**: Seed không chạy khi profile `seed` không bật; chạy khi bật; `backend/.env.example` (`SPRING_PROFILES_ACTIVE=dev,seed`) và `docker-compose.prod.yml` (`prod,seed`) bật nó; test mặc định không có seed (`nguoi_dung_he_thong` trống); bỏ profile `seed` trên một database đã seed thì Flyway vẫn validate và migrate thành công.
- **AC-8**: Chạy lại file seed trên database đã seed không báo lỗi và không đổi số dòng nào.
- **AC-9**: Sắp xếp theo cột tên tiếng Việt cho thứ tự đúng: `ORDER BY ho_ten` với `Anh, Ánh, Bình, Dương, Đào` trả đúng thứ tự đó.

## Decision

**Chosen option**: Option 1: Giữ cấu trúc gốc, sửa lỗi rõ ràng, chia migration theo nhóm, seed bằng Flyway repeatable bật qua profile `seed`.

Toàn bộ schema vào 9 file migration theo nhóm phân hệ; các bổ sung cho xác thực theo đúng mẫu bảng con (TPT, mỗi vai trò một bảng con trỏ về `nguoi_dung_he_thong`); seed nằm trong `db/seed` và chỉ được nạp khi profile `seed` bật.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`), cho cấu hình Flyway và profile.

## Feature design

### Quy ước chung cho mọi migration

- **Đặt tên**: bảng và cột snake_case không dấu. Chữ viết tắt viết như một từ: `NhanVienBQL` thành `nhan_vien_bql`, `maNVBQLXacMinh` thành `ma_nvbql_xac_minh`, `soCCCD` thành `so_cccd`, `maQRCheckIn` thành `ma_qr_check_in`, `dungLuongMB` thành `dung_luong_mb`, `maSLA` thành `ma_sla`, `maPhieuCV` thành `ma_phieu_cv`, `maNV` thành `ma_nv`.
- **Tên field Java phải theo cùng cách viết** (`maNvbqlXacMinh`, `soCccd`, `maNv`, entity `NhanVienBql`). Naming strategy mặc định của Spring chỉ chèn gạch dưới ở chỗ chữ thường đứng trước chữ hoa và chữ thường đứng sau, nên field `maNVBQLXacMinh` sẽ ánh xạ thành `manvbqlxac_minh` và `ddl-auto=validate` báo lỗi. Không dùng `@Column(name=...)` để chữa.
- **Mã tự sinh**: mỗi bảng một sequence `seq_<ten_bang>` khai báo `MINVALUE 1 MAXVALUE 99999999 NO CYCLE`; cột khóa `DEFAULT '<TIỀN TỐ>' || lpad(nextval('seq_<ten_bang>')::text, 8, '0')` và `CHECK (<cot> ~ '^<TIỀN TỐ>[0-9]{8}$')`. `MAXVALUE` là thứ chặn tràn: `lpad` cắt số 9 chữ số thành 8 chữ số, nên chỉ CHECK độ dài không đủ (sửa lại cách mô tả ở spec 0001).
- **Thời điểm**: mọi cột thời điểm là `TIMESTAMPTZ`. Giữ `DEFAULT now()` ở chỗ file gốc có `DEFAULT CURRENT_TIMESTAMP` (cho SQL viết tay và seed), nhưng code nghiệp vụ luôn tự gán giá trị lấy từ bean `Clock`.
- **Collation**: các cột tên và tiêu đề sau khai báo `COLLATE "vi-VN-x-icu"` (đã kiểm tra có trong image `postgres:18.7`): `ho_ten`, `ten_toa_nha`, `ten_khu_vuc`, `ten_bieu_phi`, `ten_khoan_muc`, `ten_tien_ich`, `ten_thiet_bi`, `ten_checklist`, `ten_hang_muc_kiem_tra`, `ten_ke_hoach`, `ten_tep`, và `tieu_de` của `phieu_yeu_cau_ho_tro`, `thong_bao_van_hanh`, `dot_khao_sat`.
- **Tên ràng buộc**: giữ tên trong file gốc, viết thường (`chk_canho_trangthai`, `fk_hsct_canho`). Ràng buộc mới đặt `chk_<bang>_<y_nghia>`, index `idx_<bang>_<cot>`.
- **File**: UTF-8 không BOM, NFC. Không sửa file đã chạy; thay đổi sau này là file `V<n>` mới. Đặt `spring.flyway.placeholder-replacement: false` trong `application.yml`, để chuỗi `${` trong SQL không bị Flyway hiểu là biến.
- **Cột `TIME`** (`khung_gio_tien_ich.gio_bat_dau`, `gio_ket_thuc`) giữ `time without time zone`: đó là giờ trong ngày theo giờ địa phương, không phải thời điểm.
- **Image PostgreSQL**: dùng cùng một tag `postgres:18.7` (bản Debian) ở `docker-compose.yml`, `docker-compose.prod.yml` và Testcontainers. Collation ICU có phiên bản; đổi image sang bản có ICU khác có thể làm hỏng UNIQUE trên cột có collation. Khi nâng image, chạy `REINDEX` các index đó và `ALTER COLLATION "vi-VN-x-icu" REFRESH VERSION`.

### Danh sách bảng, file migration và tiền tố mã

| File | Bảng (tên gốc → tên mới) | Cột khóa | Tiền tố |
|---|---|---|---|
| `V1__dinh_danh_xac_thuc.sql` | NguoiDungHeThong → `nguoi_dung_he_thong` | `ma_nguoi_dung` | `ND` |
| | NhanVienBQL → `nhan_vien_bql` | `ma_nv` | `BQL` |
| | NhanVienKyThuat → `nhan_vien_ky_thuat` | `ma_nv` | `KTV` |
| | KeToan → `ke_toan` | `ma_nv` | `KT` |
| | ThanhVienBanQuanTri → `thanh_vien_ban_quan_tri` | `ma_thanh_vien` | `BQT` |
| | CuDan → `cu_dan` | `ma_cu_dan` | `CD` |
| | (mới) `quan_tri_vien` | `ma_quan_tri_vien` | `QTV` |
| | (mới) `nhan_vien_bao_ve` | `ma_nv` | `BV` |
| | (mới) `phien_dang_nhap` | `ma_phien` | số tự tăng |
| | (mới) `yeu_cau_dat_lai_mat_khau` | `ma_yeu_cau` | số tự tăng |
| `V2__khong_gian_cu_tru.sql` | ToaNha → `toa_nha` | `ma_toa_nha` | người dùng đặt |
| | Tang → `tang` | `ma_tang` | người dùng đặt |
| | CanHo → `can_ho` | `ma_can_ho` | người dùng đặt |
| | KhuVucDungChung → `khu_vuc_dung_chung` | `ma_khu_vuc` | `KV` |
| | HoSoCuTru → `ho_so_cu_tru` | `ma_ho_so` | `HS` |
| | PhuongTien → `phuong_tien` | `ma_phuong_tien` | `PT` |
| `V3__dien_nuoc_tai_chinh.sql` | DongHoTieuThu → `dong_ho_tieu_thu` | `ma_dong_ho` | `DH` |
| | BangGhiChiSo → `bang_ghi_chi_so` | `ma_ban_ghi` | `BG` |
| | BieuPhiDichVu → `bieu_phi_dich_vu` | `ma_bieu_phi` | `BP` |
| | ThongBaoPhi → `thong_bao_phi` | `ma_thong_bao_phi` | `TBP` |
| | ChiTietKhoanPhi → `chi_tiet_khoan_phi` | `ma_chi_tiet` | `CTP` |
| `V4__tien_ich.sql` | TienIchNoiKhu → `tien_ich_noi_khu` | `ma_tien_ich` | `TI` |
| | KhungGioTienIch → `khung_gio_tien_ich` | `ma_khung_gio` | `KG` |
| | PhieuDatChoTienIch → `phieu_dat_cho_tien_ich` | `ma_booking` | `BK` |
| `V5__thanh_toan.sql` | GiaoDichThanhToan → `giao_dich_thanh_toan` | `ma_giao_dich` | `GD` |
| | BienNhanDienTu → `bien_nhan_dien_tu` | `ma_bien_nhan` | `BN` |
| `V6__bao_tri.sql` | ThietBiKyThuat → `thiet_bi_ky_thuat` | `ma_thiet_bi` | `TB` |
| | MauChecklist → `mau_checklist` | `ma_checklist` | `CL` |
| | HangMucChecklist → `hang_muc_checklist` | `ma_hang_muc` | `HM` |
| | KeHoachBaoTri → `ke_hoach_bao_tri` | `ma_ke_hoach` | `KH` |
| | PhieuCongViecBaoTri → `phieu_cong_viec_bao_tri` | `ma_phieu_cv` | `PCV` |
| | KetQuaHangMucChecklist → `ket_qua_hang_muc_checklist` | `ma_ket_qua` | `KQ` |
| `V7__service_desk.sql` | QuyDinhSLA → `quy_dinh_sla` | `ma_sla` | `SLA` |
| | PhieuYeuCauHoTro → `phieu_yeu_cau_ho_tro` | `ma_ticket` | `TK` |
| `V8__truyen_thong_khao_sat.sql` | ThongBaoVanHanh → `thong_bao_van_hanh` | `ma_thong_bao` | `TBV` |
| | TepDinhKem → `tep_dinh_kem` | `ma_tep` | `TEP` |
| | TrangThaiNhanThongBao → `trang_thai_nhan_thong_bao` | (`ma_thong_bao`, `ma_cu_dan`) | không có |
| | DotKhaoSat → `dot_khao_sat` | `ma_khao_sat` | `KS` |
| | CauHoiKhaoSat → `cau_hoi_khao_sat` | `ma_cau_hoi` | `CH` |
| | PhieuPhanHoiKhaoSat → `phieu_phan_hoi_khao_sat` | `ma_phan_hoi` | `PH` |
| `V9__bao_cao_nhat_ky.sql` | BaoCaoThongKe → `bao_cao_thong_ke` | `ma_bao_cao` | `BC` |
| | NhatKyHeThong → `nhat_ky_he_thong` | `ma_log` | `LOG` |

Thứ tự file theo phụ thuộc khóa ngoại: V7 tham chiếu thiết bị và phiếu công việc (V6), V8 tham chiếu ticket (V7), V5 tham chiếu phiếu đặt chỗ (V4).

### Bảng mới và bảng thay đổi (chi tiết)

**`nguoi_dung_he_thong`** (giữ mọi cột gốc, thêm cột và CHECK):

| Cột | Kiểu | Null | Ghi chú |
|---|---|---|---|
| `ma_nguoi_dung` | VARCHAR(30) | không | PK, mã tự sinh `ND` |
| `ho_ten` | VARCHAR(100) | không | collation `vi-VN-x-icu` |
| `so_dien_thoai` | VARCHAR(15) | không | UNIQUE, `CHECK (so_dien_thoai ~ '^0[0-9]{9}$')` |
| `email` | VARCHAR(150) | không | UNIQUE, `CHECK (email = lower(btrim(email)))` |
| `mat_khau_hash` | VARCHAR(255) | không | định dạng `{id}hash` của DelegatingPasswordEncoder, `CHECK (mat_khau_hash ~ '^\{[a-z0-9]+\}.+')` |
| `trang_thai_tai_khoan` | VARCHAR(30) | không | như gốc, mặc định `'Hoạt động'` |
| `so_lan_dang_nhap_sai` | INTEGER | không | mặc định 0, `CHECK (>= 0)` |
| `khoa_tam_den` | TIMESTAMPTZ | có | khóa tạm do đăng nhập sai (tính năng 12); độc lập với `'Tạm khóa'` do Admin đặt |
| `dang_nhap_cuoi_luc` | TIMESTAMPTZ | có | |
| `doi_mat_khau_luc` | TIMESTAMPTZ | có | |
| `tao_luc`, `cap_nhat_luc` | TIMESTAMPTZ | không | mặc định `now()` |
| `phien_ban` | BIGINT | không | mặc định 0, dùng cho `@Version` |

**Bảng vai trò** (`nhan_vien_bql`, `nhan_vien_ky_thuat`, `ke_toan`, `thanh_vien_ban_quan_tri`, `cu_dan`, và hai bảng mới): mỗi bảng có `ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE`, khóa ngoại tới `nguoi_dung_he_thong` với `ON DELETE RESTRICT` (gốc là CASCADE). `cu_dan.so_cccd` thêm `CHECK (so_cccd ~ '^[0-9]{12}$')`.
- `quan_tri_vien`: `ma_quan_tri_vien` (PK, `QTV`), `ma_nguoi_dung`. Không có thuộc tính riêng.
- `nhan_vien_bao_ve`: `ma_nv` (PK, `BV`), `ma_nguoi_dung`, `vi_tri_truc VARCHAR(150) NOT NULL`.

**`phien_dang_nhap`** (một dòng cho mỗi refresh token đã cấp):

| Cột | Kiểu | Null | Ghi chú |
|---|---|---|---|
| `ma_phien` | BIGINT | không | `GENERATED ALWAYS AS IDENTITY`, PK |
| `ma_nguoi_dung` | VARCHAR(30) | không | FK RESTRICT, có index |
| `ma_ho_phien` | UUID | không | chung cho mọi token nối tiếp của một lần đăng nhập, có index; app sinh (`UUID.randomUUID()`) lúc đăng nhập, không có `DEFAULT` |
| `token_hash` | VARCHAR(64) | không | SHA-256 dạng hex chữ thường của refresh token, UNIQUE, `CHECK (token_hash ~ '^[0-9a-f]{64}$')` |
| `tao_luc` | TIMESTAMPTZ | không | không có `DEFAULT`: chỉ app ghi bảng này, giờ lấy từ `Clock` |
| `het_han_luc` | TIMESTAMPTZ | không | `CHECK (het_han_luc > tao_luc)` |
| `thu_hoi_luc` | TIMESTAMPTZ | có | |
| `ly_do_thu_hoi` | VARCHAR(40) | có | `CHECK IN ('Đăng xuất', 'Đã xoay vòng', 'Dùng lại token cũ', 'Đổi mật khẩu', 'Đặt lại mật khẩu', 'Tài khoản bị khóa')`; `CHECK ((thu_hoi_luc IS NULL) = (ly_do_thu_hoi IS NULL))` |
| `dia_chi_ip` | VARCHAR(45) | có | đủ cho IPv6 |
| `user_agent` | VARCHAR(500) | có | app cắt bớt nếu dài hơn |

**`yeu_cau_dat_lai_mat_khau`**:

| Cột | Kiểu | Null | Ghi chú |
|---|---|---|---|
| `ma_yeu_cau` | BIGINT | không | `GENERATED ALWAYS AS IDENTITY`, PK |
| `ma_nguoi_dung` | VARCHAR(30) | không | FK RESTRICT, có index |
| `token_hash` | VARCHAR(64) | không | SHA-256 hex chữ thường, UNIQUE, `CHECK (token_hash ~ '^[0-9a-f]{64}$')` |
| `tao_luc` | TIMESTAMPTZ | không | không có `DEFAULT`, như `phien_dang_nhap` |
| `het_han_luc` | TIMESTAMPTZ | không | `CHECK (het_han_luc > tao_luc)` |
| `da_dung_luc` | TIMESTAMPTZ | có | |

Quan hệ: `nguoi_dung_he_thong` 1:0..1 với mỗi bảng vai trò (một người giữ được nhiều vai trò cùng lúc), 1:N với `phien_dang_nhap` và `yeu_cau_dat_lai_mat_khau`.

### Sửa so với file SQL gốc

1. Mọi `TIMESTAMP` thành `TIMESTAMPTZ`.
2. `phieu_phan_hoi_khao_sat.du_lieu_tra_loi` từ TEXT thành `JSONB`.
3. Thêm `NOT NULL` cho `tien_ich_noi_khu.trang_thai_phuc_vu` và `phieu_yeu_cau_ho_tro.muc_do_uu_tien` (cả hai đã có giá trị mặc định).
4. `bang_ghi_chi_so.ky_ghi_chi_so` và `thong_bao_phi.ky_tinh_phi` thành `VARCHAR(7)` với `CHECK (<cot> ~ '^[0-9]{4}-(0[1-9]|1[0-2])$')`.
5. Khóa ngoại từ các bảng vai trò tới `nguoi_dung_he_thong`: CASCADE thành RESTRICT.
6. Bỏ 3 index trùng với ràng buộc UNIQUE đã có: `idx_bgcs_dongho_ky`, `idx_tbp_canho_ky`, `idx_khunggio_tienich_ngay`.
7. Thêm index cho mọi cột khóa ngoại chưa có PK, UNIQUE hay index nào bắt đầu bằng nó. Cụ thể: `khu_vuc_dung_chung.ma_toa_nha`; `ho_so_cu_tru.ma_cu_dan`, `ma_nvbql_xac_minh`; `phuong_tien.ma_cu_dan_so_huu`; `bang_ghi_chi_so.ma_nv_ky_thuat`, `ma_nvbql_xac_minh`; `bieu_phi_dich_vu.ma_ke_toan_cau_hinh`; `thong_bao_phi.ma_ke_toan`; `chi_tiet_khoan_phi.ma_bieu_phi`; `tien_ich_noi_khu.ma_toa_nha`; `phieu_dat_cho_tien_ich.ma_cu_dan`, `ma_can_ho`; `giao_dich_thanh_toan.ma_booking`, `ma_ke_toan_doi_soat`; `hang_muc_checklist.ma_checklist`; `ke_hoach_bao_tri.ma_thiet_bi`, `ma_checklist`, `ma_nvbql`; `phieu_cong_viec_bao_tri.ma_nv_ky_thuat`, `ma_nvbql_nghiem_thu`; `ket_qua_hang_muc_checklist.ma_hang_muc`; `phieu_yeu_cau_ho_tro.ma_can_ho`, `ma_nvbql_dieu_phoi`, `ma_nv_ky_thuat`, `ma_sla`, `ma_phieu_cv_phat_sinh`, `ma_thiet_bi`; `thong_bao_van_hanh.ma_nvbql`; `tep_dinh_kem.ma_ticket`, `ma_thong_bao`; `trang_thai_nhan_thong_bao.ma_cu_dan`; `dot_khao_sat.ma_nvbql`; `cau_hoi_khao_sat.ma_khao_sat`; `phieu_phan_hoi_khao_sat.ma_cu_dan`, `ma_can_ho`; `bao_cao_thong_ke.ma_nguoi_dung`.
8. Mã tự sinh, collation và CHECK định dạng như ở *Quy ước chung*.

Mọi thứ khác (cột, kiểu, CHECK giá trị trạng thái, quan hệ, `ON DELETE` của các bảng khác) giữ nguyên. `nhat_ky_he_thong` giữ nguyên cột (chỉ đổi `TIMESTAMPTZ`); tính năng 8 sẽ chỉnh.

### Dữ liệu mẫu

**Cơ chế**: file repeatable `backend/src/main/resources/db/seed/R__seed_01_xac_thuc.sql` (phân hệ sau thêm `R__seed_02_<phan_he>.sql`, Flyway chạy theo thứ tự tên). Flyway chạy file repeatable sau mọi file `V`, và chạy lại khi nội dung file đổi. File seed không ghi mã tự sinh: để DB sinh, rồi tìm lại bằng khóa tự nhiên (số điện thoại). Quy tắc viết file seed:
- `nguoi_dung_he_thong`: `ON CONFLICT (so_dien_thoai) DO NOTHING`. Luôn ghi rõ cột xung đột, không dùng `ON CONFLICT DO NOTHING` trần, để một email trùng với dữ liệu có sẵn làm seed báo lỗi chứ không bị bỏ qua âm thầm.
- Bảng vai trò: `INSERT ... SELECT ... FROM nguoi_dung_he_thong WHERE so_dien_thoai = '...' ON CONFLICT (ma_nguoi_dung) DO NOTHING`.
- `toa_nha`, `tang`, `can_ho`: `ON CONFLICT` theo khóa chính.
- `ho_so_cu_tru` (không có khóa tự nhiên): `INSERT ... SELECT ... WHERE NOT EXISTS (cùng ma_can_ho và ma_cu_dan)`.
- Không dùng khối `DO $$ ... $$`, để file chạy lại được bằng `ScriptUtils` trong test.
- Mỗi lần chạy lại tốn vài giá trị sequence cho các dòng bị xung đột; điều này vô hại.

Nhờ vậy không cần `setval`, và chạy lại an toàn.

**Bật tắt**: profile `seed`. File `application-seed.yml` chỉ có `spring.flyway.locations: classpath:db/migration,classpath:db/seed`. Dev bật bằng `SPRING_PROFILES_ACTIVE=dev,seed` trong `backend/.env.example`; bản demo bằng `SPRING_PROFILES_ACTIVE=prod,seed` trong service `backend` của `docker-compose.prod.yml`. Test dùng `@ActiveProfiles` riêng (ví dụ `dev`), nên ghi đè giá trị trong `.env` của máy và không có seed; riêng `SchemaMigrationTest` bật thêm `seed`. `application.yml` đặt `spring.flyway.ignore-migration-patterns: "*:future,repeatable:missing"`, để bỏ profile `seed` trên database đã seed không làm Flyway báo lỗi "thiếu migration". Thuộc tính này thay hẳn giá trị mặc định của Flyway chứ không cộng thêm, nên luôn giữ `*:future` trong đó.

**Mật khẩu dev chung**: `ChungCu@2026`. Băm một lần bằng `BCryptPasswordEncoder` của Spring Security (cost 10), rồi ghi chuỗi cố định `{bcrypt}$2a$10$<hash>` vào file seed, dùng chung cho cả 8 tài khoản. Không sinh lại hash mỗi lần chạy; test ở AC-6 kiểm tra hash khớp mật khẩu. Ghi mật khẩu này trong `README.md`.

| Số điện thoại | Email | Họ tên | Vai trò | Trạng thái |
|---|---|---|---|---|
| 0900000001 | admin@chungcu.test | Nguyễn Quản Trị | Admin | Hoạt động |
| 0900000002 | bql@chungcu.test | Trần Ban Quản Lý | BQL (chức vụ Trưởng ban, bộ phận Vận hành) | Hoạt động |
| 0900000003 | kythuat@chungcu.test | Lê Kỹ Thuật | Kỹ thuật (Điện nước) | Hoạt động |
| 0900000004 | ketoan@chungcu.test | Phạm Kế Toán | Kế toán (phạm vi Tòa A) | Hoạt động |
| 0900000005 | baove@chungcu.test | Hoàng Bảo Vệ | Bảo vệ (vị trí Sảnh tòa A) | Hoạt động |
| 0900000006 | bqt@chungcu.test | Đặng Ban Quản Trị | Cư dân và BQT (`nhiem_ky` `2026-2028`, `vai_tro` `Trưởng ban`) | Hoạt động |
| 0900000007 | cudan@chungcu.test | Vũ Cư Dân | Cư dân | Hoạt động |
| 0900000008 | bikhoa@chungcu.test | Đỗ Bị Khóa | Cư dân | Tạm khóa |

Thuộc tính vai trò khác: BQL `chuc_vu` `Trưởng ban`, `bo_phan` `Vận hành`; kỹ thuật `chuyen_mon_ky_thuat` `Điện nước`; kế toán `pham_vi_phu_trach` `Tòa A`; bảo vệ `vi_tri_truc` `Sảnh tòa A`. CCCD: 0900000006 là `001026000006`, 0900000007 là `001026000007`, 0900000008 là `001026000008`.

Không gian:
- Tòa `A`: `ten_toa_nha` `Tòa A`, `so_tang` 3, `dia_chi_khu_vuc` `Khu đô thị mẫu, Hà Nội`.
- Tầng `A-01`, `A-02`, `A-03` với `so_tang` lần lượt 1, 2, 3.
- Căn hộ `A-0101`, `A-0201`, `A-0301` (một căn mỗi tầng): `dien_tich` 75.50, `trang_thai_su_dung` `Đang sử dụng`, `bi_tam_dung_tien_ich` false.

Hồ sơ cư trú: `Hợp lệ`, `Thường trú`, `ngay_bat_dau` `2026-01-01`, `ngay_ket_thuc` rỗng, xác minh bởi BQL 0900000002. Cụ thể: 0900000006 là `Chủ căn hộ` `A-0101`, 0900000007 là `Chủ căn hộ` `A-0201`, 0900000008 là `Khách thuê` `A-0301`.

### API surface

Tính năng này không thêm endpoint. Các API xác thực dùng schema này được thiết kế ở spec tính năng 5, 6, 10 đến 13.

### Value sourcing

| Hành động | Giá trị | Nguồn |
|---|---|---|
| Chèn dòng mới (mọi bảng có tiền tố) | mã khóa chính | `DEFAULT` của cột, từ `seq_<ten_bang>` |
| Chèn tòa nhà, tầng, căn hộ | mã khóa chính | input của request (UC01); trong seed là giá trị cố định ở trên |
| Seed tài khoản | `mat_khau_hash` | hash bcrypt của `ChungCu@2026`, viết sẵn trong file seed |
| Seed vai trò, hồ sơ cư trú | `ma_nguoi_dung`, `ma_cu_dan`, `ma_nv` xác minh | tra lại từ `so_dien_thoai` của tài khoản seed |
| Seed mọi cột thời điểm | `tao_luc`, `cap_nhat_luc` | `DEFAULT now()` |
| Code nghiệp vụ ghi thời điểm | `tao_luc`, `cap_nhat_luc`, `khoa_tam_den`, ... | bean `Clock` (spec 0001), không dựa vào `DEFAULT` |
| Bật seed | có hay không nạp `db/seed` | profile `seed` trong `SPRING_PROFILES_ACTIVE` |
| Đăng nhập (tính năng 5) | `ma_ho_phien` | app sinh UUID |
| Lưu refresh token, mã đặt lại | `token_hash` | SHA-256 hex chữ thường của token, app tính |
| Test thứ tự tiếng Việt | thứ tự sắp xếp | collation `vi-VN-x-icu` khai báo trên cột |

### Key invariants

- Có dòng trong bảng vai trò nào thì người dùng có vai trò đó; không có nguồn vai trò nào khác. Một người giữ được nhiều vai trò.
- Tài khoản không bao giờ bị xóa; ngừng dùng thì đặt `trang_thai_tai_khoan = 'Ngừng hoạt động'`. DB chặn xóa bằng RESTRICT.
- Email lưu viết thường, đã cắt khoảng trắng; số điện thoại lưu dạng `0` cộng 9 chữ số. Backend chuẩn hóa trước khi lưu và trước khi tra cứu đăng nhập; CHECK là lớp bảo vệ cuối.
- Không lưu refresh token hay mã đặt lại mật khẩu dạng thô, chỉ lưu SHA-256.
- `khoa_tam_den` và `trang_thai_tai_khoan = 'Tạm khóa'` là hai cơ chế độc lập: cái đầu tự hết hạn, cái sau chỉ Admin mở.
- Entity Java khớp tên cột nhờ naming strategy mặc định; `ddl-auto=validate` bắt mọi lệch khi entity đầu tiên của mỗi bảng được viết.

### Security model

- Không có endpoint mới, nên không có quy tắc quyền mới. Mọi bảng dùng chung một user database của backend.
- `mat_khau_hash`, `token_hash`, `so_cccd` là dữ liệu nhạy cảm: không bao giờ ghi vào log hay trả trong API (spec 0001 đã cấm ghi CCCD, mật khẩu, token vào log).
- Mật khẩu seed công khai trong repo, nên seed chỉ được bật ở dev và bản demo chạy trên máy cá nhân, không bao giờ trên một máy chủ mở ra Internet.

### Configuration required

- Không có biến mới. `SPRING_PROFILES_ACTIVE` có sẵn nhận thêm `seed`: `dev,seed` trong `backend/.env.example`, `prod,seed` trong service `backend` của `docker-compose.prod.yml`. Thiếu `seed` thì không nạp dữ liệu mẫu.

### Critical test scenarios

Một lớp `SchemaMigrationTest` (Testcontainers, `postgres:18.7`, `@ActiveProfiles({"dev", "seed"})`). Để các test không làm bẩn dữ liệu của nhau:
- Mỗi kịch bản ghi dữ liệu chạy trong `@Transactional` (tự rollback).
- Lỗi được kiểm tra theo SQLSTATE, không theo lớp exception: 23514 cho CHECK, 23001 cho RESTRICT (`restrict_violation`; 23503 chỉ xuất hiện với `NO ACTION`), 2200H cho tràn sequence.
- Không đổi sequence thật (`setval` không rollback được).

Các kịch bản:
- Happy path: database trống khởi động xong có đúng 42 bảng trong schema `public` (không tính `flyway_schema_history`, đọc từ `information_schema.tables`) và không cột nào kiểu `timestamp without time zone`, verifies **AC-1**, **AC-2**, **AC-3**.
- Mã tự sinh: chèn một `nguoi_dung_he_thong` không ghi mã nhận mã khớp `^ND[0-9]{8}$`. Kiểm tra tràn trên một sequence tạm tạo trong test với cùng khai báo (`MAXVALUE 99999999 NO CYCLE`, `START 99999999`): `nextval` lần hai trả 2200H. Thêm một truy vấn `pg_sequences` xác nhận mọi `seq_*` có `max_value = 99999999`, verifies **AC-4**.
- Dữ liệu sai: mỗi trường hợp trả 23514. Các trường hợp là email `An@x.vn`, số `+84900000009`, CCCD 9 chữ số, hash không tiền tố và kỳ `11/2026` trên `thong_bao_phi` (căn hộ `A-0101` và kế toán seed làm cha). `DELETE` người dùng 0900000007 (có dòng `cu_dan`) trả 23001, verifies **AC-5**.
- Seed: tài khoản có số `09000000%` đúng 8; mỗi bảng vai trò ít nhất 1 dòng; 0900000006 có cả `cu_dan` và `thanh_vien_ban_quan_tri`; 0900000008 là `Tạm khóa`; có 3 hồ sơ cư trú `Hợp lệ`; `PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("ChungCu@2026", hash)` đúng với mọi tài khoản, verifies **AC-6**.
- Chạy lại: đọc `db/seed/R__seed_01_xac_thuc.sql` bằng `EncodedResource` UTF-8, chạy bằng `ScriptUtils.executeSqlScript`, rồi so số dòng của mọi bảng trong `public` trước và sau: không đổi, verifies **AC-8**.
- Collation: tạo bảng tạm có cột `ho_ten ... COLLATE "vi-VN-x-icu"`, chèn `Đào, Bình, Ánh, Dương, Anh`, `ORDER BY ho_ten` ra `Anh, Ánh, Bình, Dương, Đào`. Thêm một truy vấn `information_schema.columns` xác nhận `nguoi_dung_he_thong.ho_ten` có `collation_name = 'vi-VN-x-icu'`, verifies **AC-9**.
- Không có seed mặc định: `ChungCuApplicationTests` (profile `dev`, không `seed`) thấy `nguoi_dung_he_thong` trống, verifies **AC-7**.
- Bỏ profile trên database đã seed: trong `SchemaMigrationTest`, dùng `Flyway.configure()` trên cùng `DataSource`, với `locations("classpath:db/migration")` và cùng `ignoreMigrationPatterns`; `validate()` rồi `migrate()` không lỗi, verifies **AC-7**.

Nên dùng một container dùng chung (static) cho mọi lớp test, vì mỗi bộ profile khác nhau tạo một Spring context mới.

## Build plan

Cách làm Tracer Bullet áp vào một tính năng chỉ có tầng dữ liệu: dựng trước một đường mỏng chạy thật từ migration qua profile seed tới test (nhóm định danh), rồi làm dày bằng các nhóm bảng còn lại.

1. **Đường mỏng định danh.** `V1__dinh_danh_xac_thuc.sql` (10 bảng, sequence, CHECK, collation); `application-seed.yml`, `ignore-migration-patterns`, `placeholder-replacement: false`; `R__seed_01_xac_thuc.sql` phần tài khoản và vai trò; `SPRING_PROFILES_ACTIVE` có `seed` trong `backend/.env.example` và `docker-compose.prod.yml`; kiểm tra các test hiện có không bật `seed`; container test dùng chung; khung `SchemaMigrationTest` với các kịch bản mã tự sinh, dữ liệu sai trên bảng V1, seed tài khoản, bỏ profile trên database đã seed. Satisfies **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-7**.
2. **Không gian và cư trú.** `V2__khong_gian_cu_tru.sql`; thêm tòa nhà, tầng, căn hộ, hồ sơ cư trú vào seed; test seed đầy đủ, chạy lại, và xóa người dùng bị RESTRICT. Satisfies **AC-2**, **AC-5**, **AC-6**, **AC-8**.
3. **Các nhóm còn lại.** `V3` đến `V9` theo bảng ở trên, áp đủ 8 mục sửa; test kỳ `11/2026` trên `thong_bao_phi`. Satisfies **AC-1**, **AC-2**, **AC-5**.
4. **Khóa lại và ghi tài liệu.** Test đếm 42 bảng, không còn `timestamp without time zone`, collation; chạy `mvnw.cmd verify`; ghi mật khẩu seed và cách làm sạch database (`docker compose down -v`) vào `README.md`. Satisfies **AC-1**, **AC-2**, **AC-9**.

## Consequences

**Positive**:
- Một lần khởi động là có đủ schema và tài khoản; mọi tính năng sau chỉ viết entity và logic.
- Vai trò chỉ có một nguồn (bảng con), nên tính năng 7 đọc vai trò bằng một quy tắc duy nhất.
- DB tự chặn dữ liệu sai định dạng và việc xóa tài khoản, kể cả khi ai đó chạy SQL tay.
- Seed chạy lại an toàn và tắt bật không làm hỏng database.

**Negative / tradeoffs**:
- Schema lệch tài liệu nộp ở tên (snake_case), 4 bảng mới, 7 cột mới và 8 nhóm sửa; bạn cần cập nhật file SQL và xlsx trước khi nộp.
- Tên field Java phải viết chữ viết tắt như một từ (`maNvbql`), khác cách viết trong tài liệu (`maNVBQL`); quên là `validate` báo lỗi lúc khởi động.
- Seed dùng `ON CONFLICT DO NOTHING`, nên sửa giá trị một dòng seed đã có không cập nhật được dòng đó; muốn áp dụng phải xóa volume (`docker compose down -v`).
- Mật khẩu seed công khai; bật seed nhầm trên máy chủ thật là lộ mọi tài khoản.
- Collation đặt theo từng cột: cột tên mới thêm sau này phải nhớ khai báo `COLLATE`.
- 38 bảng có sẵn nhưng chưa có entity; một bảng có thể nằm nhiều tháng mới được code chạm tới, lỗi thiết kế trong đó chỉ lộ ra lúc đó.

**Neutral**:
- `phien_dang_nhap` sẽ tích lũy dòng hết hạn; việc dọn dẹp chốt ở tính năng 6.
- Cột kỳ `YYYY-MM` cần một converter sang `YearMonth` khi UC03 hoặc UC04 viết entity.

## Follow-up

- [ ] Sửa hai câu trong spec 0001, mục *Quy ước database*. (1) Ở phần sinh mã khóa chính: chặn tràn bằng `MAXVALUE 99999999` của sequence, CHECK định dạng chỉ là lớp phụ. (2) Ở phần seed: "seed chỉ chạy ở profile `dev`" thành "seed chạy khi bật profile `seed` (dev và bản demo), xem spec 0002".
- [ ] Tạo tài khoản Admin đầu tiên khi database không có seed (ví dụ từ biến môi trường lúc khởi động): tính năng 11. Tới lúc đó, database không seed không có ai đăng nhập được; bản demo không bị ảnh hưởng vì có seed.
- [ ] Cập nhật `sql/database_schema_quan_ly_chung_cu.sql` và file xlsx cho khớp schema mới trước khi nộp (tên, bảng mới, các mục sửa).
- [ ] Ghi quy tắc "chữ viết tắt trong tên field Java viết như một từ" vào `AGENTS.md` `## Rules`: `/sync` sau khi tính năng này xong.
- [ ] Tính năng 8: chỉnh `nhat_ky_he_thong` (cho `ma_nguoi_dung` rỗng khi đăng nhập sai với tài khoản không tồn tại, thêm IP, `request_id`, chi tiết dạng JSONB).
- [ ] Tính năng 6: dọn dòng `phien_dang_nhap` đã hết hạn hoặc thu hồi (giữ bao lâu, chạy lúc nào); xem lại danh sách `ly_do_thu_hoi`.
- [ ] UC02: `ho_so_cu_tru.ma_nvbql_xac_minh` đang `NOT NULL` trong khi có trạng thái `'Chờ xác minh'` (hồ sơ chờ thì chưa có người xác minh); spec UC02 quyết có cho rỗng không.
- [ ] Mỗi spec phân hệ quyết bảng nào cần `tao_luc`, `cap_nhat_luc`, `phien_ban`, và tìm kiếm không dấu (`unaccent`) nếu cần.

## Rationale

Lý do và các phương án đã cân nhắc: xem [rationale.md](rationale.md).
