-- Phương tiện gửi xe căn hộ (spec 0016, AC-1). Biển số lưu ở dạng chuẩn và chỉ cấm trùng
-- giữa các xe đang giữ biển (Đang hoạt động, Tạm dừng), nên biển của xe đã hủy dùng lại
-- được; xe đạp được để trống biển số.
ALTER TABLE phuong_tien
    DROP CONSTRAINT phuong_tien_bien_so_key,
    ALTER COLUMN bien_so DROP NOT NULL;

UPDATE phuong_tien SET bien_so = upper(regexp_replace(normalize(bien_so, NFC), '[[:space:].-]', '', 'g'));

ALTER TABLE phuong_tien
    ADD CONSTRAINT chk_phuongtien_bien_so_bat_buoc CHECK (bien_so IS NOT NULL OR loai_phuong_tien = 'Xe đạp'),
    ADD CONSTRAINT chk_phuongtien_bien_so_dinh_dang CHECK (bien_so IS NULL OR bien_so ~ '^[A-Z0-9]{5,12}$'),
    DROP CONSTRAINT chk_phuongtien_trangthai,
    ADD CONSTRAINT chk_phuongtien_trangthai CHECK (
        trang_thai_dang_ky IN ('Chờ duyệt', 'Đang hoạt động', 'Tạm dừng', 'Từ chối', 'Đã hủy')),
    ADD COLUMN ma_cu_dan_dang_ky VARCHAR(30) NULL,
    ADD COLUMN ma_nvbql_cap_nhat VARCHAR(30) NULL,
    ADD COLUMN ly_do_trang_thai VARCHAR(500) NULL,
    ADD COLUMN tao_luc TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN cap_nhat_luc TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT fk_phuongtien_cudan_dangky FOREIGN KEY (ma_cu_dan_dang_ky)
        REFERENCES cu_dan (ma_cu_dan),
    ADD CONSTRAINT fk_phuongtien_nvbql_capnhat FOREIGN KEY (ma_nvbql_cap_nhat)
        REFERENCES nhan_vien_bql (ma_nv);

CREATE UNIQUE INDEX uq_phuong_tien_bien_so_hieu_luc
    ON phuong_tien (bien_so)
    WHERE (bien_so IS NOT NULL AND trang_thai_dang_ky IN ('Đang hoạt động', 'Tạm dừng'));

CREATE INDEX idx_phuong_tien_ma_cu_dan_dang_ky ON phuong_tien (ma_cu_dan_dang_ky);
CREATE INDEX idx_phuong_tien_ma_nvbql_cap_nhat ON phuong_tien (ma_nvbql_cap_nhat);
