-- Nhóm tiện ích nội khu và đặt chỗ (spec 0002). Tạo trước V5 vì giao dịch tham chiếu phiếu đặt chỗ.

-- 29. Tiện ích nội khu: bể bơi, sân thể thao, BBQ
CREATE SEQUENCE seq_tien_ich_noi_khu AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE tien_ich_noi_khu (
    ma_tien_ich VARCHAR(30) PRIMARY KEY
        DEFAULT 'TI' || lpad(nextval('seq_tien_ich_noi_khu')::text, 8, '0'),
    ma_toa_nha VARCHAR(20) NOT NULL,
    ten_tien_ich VARCHAR(150) COLLATE "vi-VN-x-icu" NOT NULL,
    noi_quy_su_dung TEXT NOT NULL,
    suc_chua_toi_da INTEGER NOT NULL,
    dinh_ngach_luot_tuan INTEGER NOT NULL,
    muc_phi_su_dung NUMERIC(15,2) DEFAULT 0 NOT NULL,
    tien_coc_yeu_cau NUMERIC(15,2) DEFAULT 0 NOT NULL,
    trang_thai_phuc_vu VARCHAR(40) DEFAULT 'Đang hoạt động' NOT NULL,
    CONSTRAINT chk_tien_ich_noi_khu_ma CHECK (ma_tien_ich ~ '^TI[0-9]{8}$'),
    CONSTRAINT fk_tienich_toanha FOREIGN KEY (ma_toa_nha)
        REFERENCES toa_nha (ma_toa_nha) ON DELETE RESTRICT,
    CONSTRAINT chk_tienich_succhua CHECK (suc_chua_toi_da > 0),
    CONSTRAINT chk_tienich_quota CHECK (dinh_ngach_luot_tuan > 0),
    CONSTRAINT chk_tienich_phi CHECK (muc_phi_su_dung >= 0),
    CONSTRAINT chk_tienich_tiencoc CHECK (tien_coc_yeu_cau >= 0),
    CONSTRAINT chk_tienich_trangthai CHECK (trang_thai_phuc_vu IN ('Đang hoạt động', 'Đang bảo trì', 'Tạm ngưng'))
);

-- 30. Khung giờ hoạt động của tiện ích.
-- gio_bat_dau, gio_ket_thuc giữ kiểu TIME: giờ trong ngày theo giờ địa phương, không phải thời điểm.
CREATE SEQUENCE seq_khung_gio_tien_ich AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE khung_gio_tien_ich (
    ma_khung_gio VARCHAR(30) PRIMARY KEY
        DEFAULT 'KG' || lpad(nextval('seq_khung_gio_tien_ich')::text, 8, '0'),
    ma_tien_ich VARCHAR(30) NOT NULL,
    ngay_su_dung DATE NOT NULL,
    gio_bat_dau TIME NOT NULL,
    gio_ket_thuc TIME NOT NULL,
    so_slot_con_trong INTEGER NOT NULL,
    trang_thai_khung_gio VARCHAR(40) DEFAULT 'Còn trống' NOT NULL,
    CONSTRAINT chk_khung_gio_tien_ich_ma CHECK (ma_khung_gio ~ '^KG[0-9]{8}$'),
    CONSTRAINT fk_khunggio_tienich FOREIGN KEY (ma_tien_ich)
        REFERENCES tien_ich_noi_khu (ma_tien_ich) ON DELETE CASCADE,
    CONSTRAINT uq_khunggio_tienich_slot UNIQUE (ma_tien_ich, ngay_su_dung, gio_bat_dau),
    CONSTRAINT chk_khunggio_thoigian CHECK (gio_ket_thuc > gio_bat_dau),
    CONSTRAINT chk_khunggio_slot CHECK (so_slot_con_trong >= 0),
    CONSTRAINT chk_khunggio_trangthai CHECK (trang_thai_khung_gio IN ('Còn trống', 'Đang giữ chỗ tạm thời', 'Đã khóa (Occupied)', 'Tạm dừng bảo trì'))
);

-- 31. Phiếu đặt chỗ tiện ích của cư dân
CREATE SEQUENCE seq_phieu_dat_cho_tien_ich AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE phieu_dat_cho_tien_ich (
    ma_booking VARCHAR(30) PRIMARY KEY
        DEFAULT 'BK' || lpad(nextval('seq_phieu_dat_cho_tien_ich')::text, 8, '0'),
    ma_khung_gio VARCHAR(30) NOT NULL,
    ma_cu_dan VARCHAR(30) NOT NULL,
    ma_can_ho VARCHAR(20) NOT NULL,
    so_nguoi_tham_gia INTEGER DEFAULT 1 NOT NULL,
    chi_phi_phat_sinh NUMERIC(15,2) DEFAULT 0 NOT NULL,
    ma_qr_check_in VARCHAR(100) NULL UNIQUE,
    thoi_gian_dat TIMESTAMPTZ DEFAULT now() NOT NULL,
    han_giu_cho_tam_thoi TIMESTAMPTZ NULL,
    trang_thai_booking VARCHAR(40) DEFAULT 'Đã xác nhận' NOT NULL,
    CONSTRAINT chk_phieu_dat_cho_tien_ich_ma CHECK (ma_booking ~ '^BK[0-9]{8}$'),
    CONSTRAINT fk_booking_khunggio FOREIGN KEY (ma_khung_gio)
        REFERENCES khung_gio_tien_ich (ma_khung_gio) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_cudan FOREIGN KEY (ma_cu_dan)
        REFERENCES cu_dan (ma_cu_dan),
    CONSTRAINT fk_booking_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho),
    CONSTRAINT chk_booking_songuoi CHECK (so_nguoi_tham_gia > 0),
    CONSTRAINT chk_booking_chiphi CHECK (chi_phi_phat_sinh >= 0),
    CONSTRAINT chk_booking_trangthai CHECK (trang_thai_booking IN ('Chờ thanh toán cọc', 'Đã xác nhận', 'Đã sử dụng (Checked-in)', 'Đã hủy'))
);

-- Index từ file gốc (bỏ idx_khunggio_tienich_ngay: trùng UNIQUE)
CREATE INDEX idx_booking_khunggio ON phieu_dat_cho_tien_ich (ma_khung_gio);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_tien_ich_noi_khu_ma_toa_nha ON tien_ich_noi_khu (ma_toa_nha);
CREATE INDEX idx_phieu_dat_cho_tien_ich_ma_cu_dan ON phieu_dat_cho_tien_ich (ma_cu_dan);
CREATE INDEX idx_phieu_dat_cho_tien_ich_ma_can_ho ON phieu_dat_cho_tien_ich (ma_can_ho);
