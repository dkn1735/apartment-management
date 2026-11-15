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
