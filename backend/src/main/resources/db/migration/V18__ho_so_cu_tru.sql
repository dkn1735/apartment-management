-- Hồ sơ cư trú căn hộ (spec 0014, AC-9).
-- btree_gist cho phép trộn so sánh bằng (=) trên VARCHAR với giao khoảng (&&) trong một
-- ràng buộc loại trừ; có sẵn trong PostgreSQL 18.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Không đặt CHECK (cap_nhat_luc >= tao_luc): test tích hợp dùng Clock cố định có thể nhỏ
-- hơn thời điểm nạp seed.
-- daterange(ngay_bat_dau, ngay_ket_thuc, '[]') nhận thẳng NULL làm đầu trên mở vô hạn.
ALTER TABLE ho_so_cu_tru
    ADD COLUMN tao_luc TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN cap_nhat_luc TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT chk_hsct_ngay_ket_thuc_bat_buoc CHECK (
        (tinh_trang_cu_tru <> 'Tạm trú' AND trang_thai_ho_so <> 'Hết hiệu lực')
        OR ngay_ket_thuc IS NOT NULL),
    ADD CONSTRAINT ex_hsct_cu_dan_khong_chong_ngay EXCLUDE USING gist (
        ma_can_ho WITH =,
        ma_cu_dan WITH =,
        daterange(ngay_bat_dau, ngay_ket_thuc, '[]') WITH &&
    ) WHERE (trang_thai_ho_so = 'Hợp lệ'),
    ADD CONSTRAINT ex_hsct_mot_chu_can_ho EXCLUDE USING gist (
        ma_can_ho WITH =,
        daterange(ngay_bat_dau, ngay_ket_thuc, '[]') WITH &&
    ) WHERE (trang_thai_ho_so = 'Hợp lệ' AND vai_tro_cu_tru = 'Chủ căn hộ');
