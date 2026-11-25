-- Căn hộ demo bộ lọc của tòa A (spec 0012, AC-16). Chạy sau R__seed_01_xac_thuc.sql, nơi đã
-- có tòa A, ba tầng và ba căn A-0101, A-0201, A-0301. Thêm 9 căn để mỗi tầng có 4 căn, đủ cả
-- bốn trạng thái và diện tích khác nhau. Chưa có hồ sơ cư trú cho các căn mới.
INSERT INTO can_ho (ma_can_ho, ma_tang, dien_tich, trang_thai_su_dung, bi_tam_dung_tien_ich)
VALUES
    ('A-0102', 'A-01', 45.00, 'Trống', FALSE),
    ('A-0103', 'A-01', 60.25, 'Đang sử dụng', FALSE),
    ('A-0104', 'A-01', 120.00, 'Đã bàn giao', FALSE),
    ('A-0202', 'A-02', 52.50, 'Đang sửa chữa', FALSE),
    ('A-0203', 'A-02', 68.00, 'Trống', FALSE),
    ('A-0204', 'A-02', 95.75, 'Đang sử dụng', FALSE),
    ('A-0302', 'A-03', 82.00, 'Đã bàn giao', FALSE),
    ('A-0303', 'A-03', 110.40, 'Đang sửa chữa', FALSE),
    ('A-0304', 'A-03', 48.30, 'Trống', FALSE)
ON CONFLICT (ma_can_ho) DO NOTHING;
