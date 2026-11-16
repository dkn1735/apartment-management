-- Nhóm không gian vật lý, căn hộ và cư trú (spec 0002).
-- Tòa nhà, tầng, căn hộ: mã do người dùng đặt, không có mã tự sinh.

-- 7. Tòa nhà (block)
CREATE TABLE toa_nha (
    ma_toa_nha VARCHAR(20) PRIMARY KEY,
    ten_toa_nha VARCHAR(100) COLLATE "vi-VN-x-icu" NOT NULL UNIQUE,
    so_tang INTEGER NOT NULL,
    dia_chi_khu_vuc VARCHAR(255) NOT NULL,
    CONSTRAINT chk_toanha_sotang CHECK (so_tang > 0)
);

-- 8. Tầng trong tòa nhà
CREATE TABLE tang (
    ma_tang VARCHAR(20) PRIMARY KEY,
    ma_toa_nha VARCHAR(20) NOT NULL,
    so_tang INTEGER NOT NULL,
    CONSTRAINT fk_tang_toanha FOREIGN KEY (ma_toa_nha)
        REFERENCES toa_nha (ma_toa_nha) ON DELETE RESTRICT,
    CONSTRAINT uq_tang_toanha_sotang UNIQUE (ma_toa_nha, so_tang)
);

-- 9. Căn hộ thuộc tầng
CREATE TABLE can_ho (
    ma_can_ho VARCHAR(20) PRIMARY KEY,
    ma_tang VARCHAR(20) NOT NULL,
    dien_tich NUMERIC(8,2) NOT NULL,
    trang_thai_su_dung VARCHAR(40) DEFAULT 'Trống' NOT NULL,
    bi_tam_dung_tien_ich BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT fk_canho_tang FOREIGN KEY (ma_tang)
        REFERENCES tang (ma_tang) ON DELETE RESTRICT,
    CONSTRAINT chk_canho_dientich CHECK (dien_tich > 0),
    CONSTRAINT chk_canho_trangthai CHECK (trang_thai_su_dung IN ('Trống', 'Đang sử dụng', 'Đang sửa chữa', 'Đã bàn giao'))
);

-- 10. Khu vực dùng chung: sảnh, phòng kỹ thuật
CREATE SEQUENCE seq_khu_vuc_dung_chung AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE khu_vuc_dung_chung (
    ma_khu_vuc VARCHAR(30) PRIMARY KEY
        DEFAULT 'KV' || lpad(nextval('seq_khu_vuc_dung_chung')::text, 8, '0'),
    ma_toa_nha VARCHAR(20) NOT NULL,
    ten_khu_vuc VARCHAR(150) COLLATE "vi-VN-x-icu" NOT NULL,
    vi_tri VARCHAR(150) NOT NULL,
    CONSTRAINT chk_khu_vuc_dung_chung_ma CHECK (ma_khu_vuc ~ '^KV[0-9]{8}$'),
    CONSTRAINT fk_khuvuc_toanha FOREIGN KEY (ma_toa_nha)
        REFERENCES toa_nha (ma_toa_nha) ON DELETE RESTRICT
);

-- 11. Hồ sơ cư trú: quan hệ cư trú giữa cư dân và căn hộ
CREATE SEQUENCE seq_ho_so_cu_tru AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE ho_so_cu_tru (
    ma_ho_so VARCHAR(30) PRIMARY KEY
        DEFAULT 'HS' || lpad(nextval('seq_ho_so_cu_tru')::text, 8, '0'),
    ma_can_ho VARCHAR(20) NOT NULL,
    ma_cu_dan VARCHAR(30) NOT NULL,
    ma_nvbql_xac_minh VARCHAR(30) NOT NULL,
    vai_tro_cu_tru VARCHAR(40) NOT NULL,
    tinh_trang_cu_tru VARCHAR(40) NOT NULL,
    ngay_bat_dau DATE NOT NULL,
    ngay_ket_thuc DATE NULL,
    trang_thai_ho_so VARCHAR(40) DEFAULT 'Hợp lệ' NOT NULL,
    CONSTRAINT chk_ho_so_cu_tru_ma CHECK (ma_ho_so ~ '^HS[0-9]{8}$'),
    CONSTRAINT fk_hsct_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho) ON DELETE RESTRICT,
    CONSTRAINT fk_hsct_cudan FOREIGN KEY (ma_cu_dan)
        REFERENCES cu_dan (ma_cu_dan) ON DELETE RESTRICT,
    CONSTRAINT fk_hsct_nvbql FOREIGN KEY (ma_nvbql_xac_minh)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT chk_hsct_vaitro CHECK (vai_tro_cu_tru IN ('Chủ căn hộ', 'Khách thuê', 'Thành viên gia đình')),
    CONSTRAINT chk_hsct_tinhtrang CHECK (tinh_trang_cu_tru IN ('Thường trú', 'Tạm trú')),
    CONSTRAINT chk_hsct_ngay CHECK (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= ngay_bat_dau),
    CONSTRAINT chk_hsct_trangthai CHECK (trang_thai_ho_so IN ('Chờ xác minh', 'Hợp lệ', 'Hết hiệu lực'))
);

-- 12. Phương tiện của cư dân gắn với căn hộ
CREATE SEQUENCE seq_phuong_tien AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE phuong_tien (
    ma_phuong_tien VARCHAR(30) PRIMARY KEY
        DEFAULT 'PT' || lpad(nextval('seq_phuong_tien')::text, 8, '0'),
    ma_can_ho VARCHAR(20) NOT NULL,
    ma_cu_dan_so_huu VARCHAR(30) NOT NULL,
    loai_phuong_tien VARCHAR(40) NOT NULL,
    bien_so VARCHAR(25) NOT NULL UNIQUE,
    nhan_hieu_mau_sac VARCHAR(100) NULL,
    trang_thai_dang_ky VARCHAR(40) DEFAULT 'Đang hoạt động' NOT NULL,
    CONSTRAINT chk_phuong_tien_ma CHECK (ma_phuong_tien ~ '^PT[0-9]{8}$'),
    CONSTRAINT fk_phuongtien_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho) ON DELETE RESTRICT,
    CONSTRAINT fk_phuongtien_cudan FOREIGN KEY (ma_cu_dan_so_huu)
        REFERENCES cu_dan (ma_cu_dan),
    CONSTRAINT chk_phuongtien_loai CHECK (loai_phuong_tien IN ('Ô tô', 'Xe máy', 'Xe máy điện', 'Xe đạp')),
    CONSTRAINT chk_phuongtien_trangthai CHECK (trang_thai_dang_ky IN ('Đang hoạt động', 'Tạm dừng', 'Đã hủy'))
);

-- Index từ file gốc
CREATE INDEX idx_canho_matang ON can_ho (ma_tang);
CREATE INDEX idx_hsct_canho_cudan ON ho_so_cu_tru (ma_can_ho, ma_cu_dan);
CREATE INDEX idx_phuongtien_canho ON phuong_tien (ma_can_ho);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_khu_vuc_dung_chung_ma_toa_nha ON khu_vuc_dung_chung (ma_toa_nha);
CREATE INDEX idx_ho_so_cu_tru_ma_cu_dan ON ho_so_cu_tru (ma_cu_dan);
CREATE INDEX idx_ho_so_cu_tru_ma_nvbql_xac_minh ON ho_so_cu_tru (ma_nvbql_xac_minh);
CREATE INDEX idx_phuong_tien_ma_cu_dan_so_huu ON phuong_tien (ma_cu_dan_so_huu);
