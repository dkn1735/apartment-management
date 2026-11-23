-- Khu vực dùng chung của tòa A (spec 0011, AC-12). Chạy sau R__seed_01_xac_thuc.sql, nơi đã
-- có tòa A. Mã để DB tự sinh, nên chống nhân đôi bằng NOT EXISTS theo (tòa, tên).
INSERT INTO khu_vuc_dung_chung (ma_toa_nha, ten_khu_vuc, vi_tri)
SELECT v.ma_toa_nha, v.ten_khu_vuc, v.vi_tri
FROM (VALUES
        ('A', 'Sảnh chính', 'Tầng 1'),
        ('A', 'Hành lang tầng 2', 'Tầng 2'),
        ('A', 'Phòng kỹ thuật điện nước', 'Tầng 1'))
    AS v (ma_toa_nha, ten_khu_vuc, vi_tri)
WHERE NOT EXISTS (
    SELECT 1 FROM khu_vuc_dung_chung k
    WHERE k.ma_toa_nha = v.ma_toa_nha AND k.ten_khu_vuc = v.ten_khu_vuc
);
