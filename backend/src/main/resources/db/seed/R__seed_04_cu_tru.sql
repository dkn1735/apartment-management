-- Cư dân và hồ sơ cư trú mẫu (spec 0014, AC-14). Chạy sau R__seed_03_can_ho.sql, để hai căn
-- 'Đang sử dụng' A-0103 và A-0204 có người đang cư trú thật, khớp luật đồng bộ của AC-10.
-- Chạy lại an toàn: mọi INSERT có ON CONFLICT hoặc NOT EXISTS. Mật khẩu chung: ChungCu@2026.

INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash, trang_thai_tai_khoan)
VALUES
    ('Bùi Chủ Hộ', '0900000009', 'chuho@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động'),
    ('Ngô Khách Thuê', '0900000010', 'khachthue@chungcu.test', '{bcrypt}$2a$10$VjcLJeAGTK7tD96AcArQyOIIwobXVsCc//Rdi/1MqhcZUi0ykFExq', 'Hoạt động')
ON CONFLICT (so_dien_thoai) DO NOTHING;

INSERT INTO cu_dan (ma_nguoi_dung, so_cccd)
SELECT n.ma_nguoi_dung, v.so_cccd
FROM (VALUES ('0900000009', '001026000009'), ('0900000010', '001026000010')) AS v (so_dien_thoai, so_cccd)
JOIN nguoi_dung_he_thong n ON n.so_dien_thoai = v.so_dien_thoai
ON CONFLICT (ma_nguoi_dung) DO NOTHING;

-- Người xác minh là BQL 0900000002. Cư dân 0900000009 còn một dòng lịch sử ở A-0101, để cư dân
-- đang ở A-0101 kiểm được rằng mình không thấy lịch sử của người khác.
INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, tao_luc, cap_nhat_luc)
SELECT v.ma_can_ho, c.ma_cu_dan, b.ma_nv, v.vai_tro_cu_tru, v.tinh_trang_cu_tru, v.ngay_bat_dau, v.ngay_ket_thuc,
    v.trang_thai_ho_so, TIMESTAMPTZ '2026-01-01 08:00:00+07', TIMESTAMPTZ '2026-01-01 08:00:00+07'
FROM (VALUES
        ('0900000009', 'A-0103', 'Chủ căn hộ', 'Thường trú', DATE '2026-01-01', NULL::date, 'Hợp lệ'),
        ('0900000009', 'A-0101', 'Khách thuê', 'Tạm trú', DATE '2025-01-01', DATE '2025-12-31', 'Hết hiệu lực'),
        ('0900000010', 'A-0204', 'Khách thuê', 'Tạm trú', DATE '2026-01-01', DATE '2027-12-31', 'Hợp lệ'))
    AS v (so_dien_thoai, ma_can_ho, vai_tro_cu_tru, tinh_trang_cu_tru, ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so)
JOIN nguoi_dung_he_thong n ON n.so_dien_thoai = v.so_dien_thoai
JOIN cu_dan c ON c.ma_nguoi_dung = n.ma_nguoi_dung
CROSS JOIN (
    SELECT q.ma_nv FROM nhan_vien_bql q
    JOIN nguoi_dung_he_thong nq ON nq.ma_nguoi_dung = q.ma_nguoi_dung
    WHERE nq.so_dien_thoai = '0900000002') AS b
WHERE NOT EXISTS (
    SELECT 1 FROM ho_so_cu_tru h WHERE h.ma_can_ho = v.ma_can_ho AND h.ma_cu_dan = c.ma_cu_dan);

-- Hai đề nghị mẫu tại A-0101 do cư dân 0900000006 đứng đơn (spec 0015, AC-14): một đề nghị chờ
-- xác minh cho 0900000010 và một đề nghị bị BQL 0900000002 từ chối cho 0900000009. So theo cả
-- trạng thái để không đụng dòng lịch sử 'Hết hiệu lực' sẵn có của 0900000009 tại A-0101.
INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, tao_luc, cap_nhat_luc, ma_cu_dan_de_nghi, ghi_chu_de_nghi,
    ly_do_tu_choi)
SELECT 'A-0101', c.ma_cu_dan, CASE WHEN v.co_xac_minh THEN b.ma_nv END, v.vai_tro_cu_tru, 'Tạm trú',
    v.ngay_bat_dau, v.ngay_ket_thuc, v.trang_thai_ho_so, v.moc, v.moc, g.ma_cu_dan, v.ghi_chu, v.ly_do
FROM (VALUES
        ('0900000010', 'Khách thuê', DATE '2027-01-01', DATE '2027-06-30', 'Chờ xác minh', false,
            TIMESTAMPTZ '2026-01-02 09:00:00+07', 'Người thuê phòng nhỏ, đã có hợp đồng thuê 6 tháng', NULL),
        ('0900000009', 'Thành viên gia đình', DATE '2026-06-01', DATE '2026-12-31', 'Từ chối', true,
            TIMESTAMPTZ '2026-01-02 10:00:00+07', NULL, 'Chưa nộp bản sao giấy tờ chứng minh quan hệ gia đình'))
    AS v (so_dien_thoai, vai_tro_cu_tru, ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, co_xac_minh, moc, ghi_chu,
        ly_do)
JOIN nguoi_dung_he_thong n ON n.so_dien_thoai = v.so_dien_thoai
JOIN cu_dan c ON c.ma_nguoi_dung = n.ma_nguoi_dung
CROSS JOIN (
    SELECT cg.ma_cu_dan FROM cu_dan cg
    JOIN nguoi_dung_he_thong ng ON ng.ma_nguoi_dung = cg.ma_nguoi_dung
    WHERE ng.so_dien_thoai = '0900000006') AS g
CROSS JOIN (
    SELECT q.ma_nv FROM nhan_vien_bql q
    JOIN nguoi_dung_he_thong nq ON nq.ma_nguoi_dung = q.ma_nguoi_dung
    WHERE nq.so_dien_thoai = '0900000002') AS b
WHERE NOT EXISTS (
    SELECT 1 FROM ho_so_cu_tru h
    WHERE h.ma_can_ho = 'A-0101' AND h.ma_cu_dan = c.ma_cu_dan AND h.trang_thai_ho_so = v.trang_thai_ho_so);
