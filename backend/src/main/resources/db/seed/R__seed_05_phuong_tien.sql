-- Phương tiện mẫu (spec 0016, AC-13). Chạy sau R__seed_04_cu_tru.sql: chủ xe là cư dân có hồ sơ
-- hợp lệ tại căn. Phủ đủ bốn loại xe (xe đạp không biển) và năm trạng thái đăng ký.
-- Chạy lại an toàn: bỏ qua xe đã có tại căn (theo loại và biển) và biển đang được xe khác giữ.
INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, bien_so, nhan_hieu_mau_sac,
    trang_thai_dang_ky, ma_cu_dan_dang_ky, ma_nvbql_cap_nhat, ly_do_trang_thai, tao_luc, cap_nhat_luc)
SELECT v.ma_can_ho, c.ma_cu_dan, v.loai, v.bien_so, v.nhan_hieu, v.trang_thai,
    CASE WHEN v.cu_dan_gui THEN c.ma_cu_dan END, CASE WHEN v.bql_xu_ly THEN b.ma_nv END, v.ly_do, v.moc, v.moc
FROM (VALUES
        ('0900000006', 'A-0101', 'Ô tô', '30A12345', 'Toyota Vios trắng', 'Đang hoạt động', false, true,
            NULL, TIMESTAMPTZ '2026-01-05 08:00:00+07'),
        ('0900000006', 'A-0101', 'Xe đạp', NULL, 'Xe đạp Thống Nhất xanh', 'Đang hoạt động', false, true,
            NULL, TIMESTAMPTZ '2026-01-05 08:05:00+07'),
        ('0900000006', 'A-0101', 'Xe máy', '29B112345', 'Honda Vision đỏ', 'Chờ duyệt', true, false,
            NULL, TIMESTAMPTZ '2026-01-06 09:00:00+07'),
        ('0900000009', 'A-0103', 'Xe máy điện', '29MD100234', 'VinFast Klara đen', 'Tạm dừng', false, true,
            'Chủ xe đề nghị tạm ngừng gửi xe', TIMESTAMPTZ '2026-01-07 10:00:00+07'),
        ('0900000009', 'A-0103', 'Ô tô', '30F67890', 'Mazda 3 xám', 'Đã hủy', false, true,
            'Chủ xe đã bán xe', TIMESTAMPTZ '2026-01-08 10:00:00+07'),
        ('0900000010', 'A-0204', 'Xe máy', '29C198765', 'Yamaha Sirius đen', 'Từ chối', true, true,
            'Ảnh đăng ký xe không đọc được', TIMESTAMPTZ '2026-01-09 11:00:00+07'),
        ('0900000010', 'A-0204', 'Xe máy', '29D155555', NULL, 'Đã hủy', true, false,
            NULL, TIMESTAMPTZ '2026-01-10 11:00:00+07'))
    AS v (so_dien_thoai, ma_can_ho, loai, bien_so, nhan_hieu, trang_thai, cu_dan_gui, bql_xu_ly, ly_do, moc)
JOIN nguoi_dung_he_thong n ON n.so_dien_thoai = v.so_dien_thoai
JOIN cu_dan c ON c.ma_nguoi_dung = n.ma_nguoi_dung
CROSS JOIN (
    SELECT q.ma_nv FROM nhan_vien_bql q
    JOIN nguoi_dung_he_thong nq ON nq.ma_nguoi_dung = q.ma_nguoi_dung
    WHERE nq.so_dien_thoai = '0900000002') AS b
WHERE NOT EXISTS (
    SELECT 1 FROM phuong_tien p
    WHERE (p.ma_can_ho = v.ma_can_ho AND p.loai_phuong_tien = v.loai AND p.bien_so IS NOT DISTINCT FROM v.bien_so)
        OR (p.bien_so = v.bien_so AND p.trang_thai_dang_ky IN ('Đang hoạt động', 'Tạm dừng')));
