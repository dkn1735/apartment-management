-- Dữ liệu mẫu cho dev và bản demo (spec 0002). Chỉ nạp khi bật profile `seed`.
-- Không ghi mã tự sinh: để DB sinh, rồi tra lại bằng số điện thoại.
-- Chạy lại an toàn: mọi INSERT có ON CONFLICT hoặc NOT EXISTS. Không dùng khối DO (để ScriptUtils chạy lại được trong test).
-- Mật khẩu chung của mọi tài khoản: ChungCu@2026 (bcrypt cost 10, băm sẵn một lần).

-- Tài khoản
INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash, trang_thai_tai_khoan)
VALUES
    ('Nguyễn Quản Trị', '0900000001', 'admin@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Trần Ban Quản Lý', '0900000002', 'bql@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Lê Kỹ Thuật', '0900000003', 'kythuat@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Phạm Kế Toán', '0900000004', 'ketoan@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Hoàng Bảo Vệ', '0900000005', 'baove@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Đặng Ban Quản Trị', '0900000006', 'bqt@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Vũ Cư Dân', '0900000007', 'cudan@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Đỗ Bị Khóa', '0900000008', 'bikhoa@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Tạm khóa')
ON CONFLICT (so_dien_thoai) DO NOTHING;

-- Vai trò
INSERT INTO quan_tri_vien (ma_nguoi_dung)
SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000001'
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

INSERT INTO nhan_vien_bql (ma_nguoi_dung, chuc_vu, bo_phan)
SELECT ma_nguoi_dung, 'Trưởng ban', 'Vận hành' FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000002'
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

INSERT INTO nhan_vien_ky_thuat (ma_nguoi_dung, chuyen_mon_ky_thuat)
SELECT ma_nguoi_dung, 'Điện nước' FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000003'
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

INSERT INTO ke_toan (ma_nguoi_dung, pham_vi_phu_trach)
SELECT ma_nguoi_dung, 'Tòa A' FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000004'
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

INSERT INTO nhan_vien_bao_ve (ma_nguoi_dung, vi_tri_truc)
SELECT ma_nguoi_dung, 'Sảnh tòa A' FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000005'
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

INSERT INTO thanh_vien_ban_quan_tri (ma_nguoi_dung, nhiem_ky, vai_tro)
SELECT ma_nguoi_dung, '2026-2028', 'Trưởng ban' FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000006'
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

INSERT INTO cu_dan (ma_nguoi_dung, so_cccd)
SELECT n.ma_nguoi_dung, v.so_cccd
FROM (VALUES ('0900000006', '001026000006'), ('0900000007', '001026000007'), ('0900000008', '001026000008'))
    AS v (so_dien_thoai, so_cccd)
JOIN nguoi_dung_he_thong n ON n.so_dien_thoai = v.so_dien_thoai
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

-- Không gian: 1 tòa, 3 tầng, 3 căn hộ (mã do người dùng đặt)
INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc)
VALUES ('A', 'Tòa A', 3, 'Khu đô thị mẫu, Hà Nội')
ON CONFLICT (ma_toa_nha) DO NOTHING;

INSERT INTO tang (ma_tang, ma_toa_nha, so_tang)
VALUES ('A-01', 'A', 1), ('A-02', 'A', 2), ('A-03', 'A', 3)
ON CONFLICT (ma_tang) DO NOTHING;

INSERT INTO can_ho (ma_can_ho, ma_tang, dien_tich, trang_thai_su_dung, bi_tam_dung_tien_ich)
VALUES
    ('A-0101', 'A-01', 75.50, 'Đang sử dụng', FALSE),
    ('A-0201', 'A-02', 75.50, 'Đang sử dụng', FALSE),
    ('A-0301', 'A-03', 75.50, 'Đang sử dụng', FALSE)
ON CONFLICT (ma_can_ho) DO NOTHING;

-- Hồ sơ cư trú: không có khóa tự nhiên, nên dùng NOT EXISTS theo (căn hộ, cư dân)
INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so)
SELECT v.ma_can_ho, c.ma_cu_dan, b.ma_nv, v.vai_tro_cu_tru, 'Thường trú', DATE '2026-01-01', NULL, 'Hợp lệ'
FROM (VALUES
        ('0900000006', 'A-0101', 'Chủ căn hộ'),
        ('0900000007', 'A-0201', 'Chủ căn hộ'),
        ('0900000008', 'A-0301', 'Khách thuê'))
    AS v (so_dien_thoai, ma_can_ho, vai_tro_cu_tru)
JOIN nguoi_dung_he_thong n ON n.so_dien_thoai = v.so_dien_thoai
JOIN cu_dan c ON c.ma_nguoi_dung = n.ma_nguoi_dung
CROSS JOIN (
    SELECT q.ma_nv FROM nhan_vien_bql q
    JOIN nguoi_dung_he_thong nq ON nq.ma_nguoi_dung = q.ma_nguoi_dung
    WHERE nq.so_dien_thoai = '0900000002') AS b
WHERE NOT EXISTS (
    SELECT 1 FROM ho_so_cu_tru h WHERE h.ma_can_ho = v.ma_can_ho AND h.ma_cu_dan = c.ma_cu_dan);