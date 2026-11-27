-- Đề nghị cư trú do cư dân gửi (spec 0015, AC-11). Vòng đời đề nghị lưu ngay trên
-- ho_so_cu_tru: dòng 'Chờ xác minh' và 'Đã hủy' chưa có người xác minh, dòng 'Từ chối' bắt
-- buộc có lý do. Một cặp (căn, cư dân) có tối đa một đề nghị đang chờ.
ALTER TABLE ho_so_cu_tru
    ALTER COLUMN ma_nvbql_xac_minh DROP NOT NULL,
    DROP CONSTRAINT fk_hsct_nvbql,
    ADD CONSTRAINT fk_hsct_nvbql FOREIGN KEY (ma_nvbql_xac_minh)
        REFERENCES nhan_vien_bql (ma_nv) ON DELETE RESTRICT,
    ADD COLUMN ma_cu_dan_de_nghi VARCHAR(30) NULL,
    ADD COLUMN ghi_chu_de_nghi VARCHAR(500) NULL,
    ADD COLUMN ly_do_tu_choi VARCHAR(500) NULL,
    ADD CONSTRAINT fk_hsct_cudan_denghi FOREIGN KEY (ma_cu_dan_de_nghi)
        REFERENCES cu_dan (ma_cu_dan) ON DELETE RESTRICT,
    DROP CONSTRAINT chk_hsct_trangthai,
    ADD CONSTRAINT chk_hsct_trangthai CHECK (
        trang_thai_ho_so IN ('Chờ xác minh', 'Hợp lệ', 'Hết hiệu lực', 'Từ chối', 'Đã hủy')),
    ADD CONSTRAINT chk_hsct_xac_minh_theo_trang_thai CHECK (
        (trang_thai_ho_so IN ('Chờ xác minh', 'Đã hủy') AND ma_nvbql_xac_minh IS NULL)
        OR (trang_thai_ho_so IN ('Hợp lệ', 'Hết hiệu lực', 'Từ chối') AND ma_nvbql_xac_minh IS NOT NULL)),
    ADD CONSTRAINT chk_hsct_nguon_de_nghi CHECK (
        trang_thai_ho_so NOT IN ('Chờ xác minh', 'Từ chối', 'Đã hủy')
        OR (ma_cu_dan_de_nghi IS NOT NULL AND vai_tro_cu_tru IN ('Thành viên gia đình', 'Khách thuê'))),
    ADD CONSTRAINT chk_hsct_ly_do_tu_choi CHECK (
        ((trang_thai_ho_so = 'Từ chối' AND ly_do_tu_choi IS NOT NULL AND btrim(ly_do_tu_choi) <> '')
         OR (trang_thai_ho_so <> 'Từ chối' AND ly_do_tu_choi IS NULL))
        AND (ghi_chu_de_nghi IS NULL OR btrim(ghi_chu_de_nghi) <> ''));

CREATE UNIQUE INDEX uq_hsct_cho_xac_minh_canho_cudan
    ON ho_so_cu_tru (ma_can_ho, ma_cu_dan)
    WHERE (trang_thai_ho_so = 'Chờ xác minh');

CREATE INDEX idx_ho_so_cu_tru_ma_cu_dan_de_nghi ON ho_so_cu_tru (ma_cu_dan_de_nghi);
CREATE INDEX idx_ho_so_cu_tru_trangthai_taoluc ON ho_so_cu_tru (trang_thai_ho_so, tao_luc DESC);
