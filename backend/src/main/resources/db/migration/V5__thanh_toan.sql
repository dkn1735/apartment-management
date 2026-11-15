-- Nhóm giao dịch thanh toán và biên nhận (spec 0002).

-- 18. Giao dịch thanh toán phí hóa đơn hoặc phí tiện ích
CREATE SEQUENCE seq_giao_dich_thanh_toan AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE giao_dich_thanh_toan (
    ma_giao_dich VARCHAR(30) PRIMARY KEY
        DEFAULT 'GD' || lpad(nextval('seq_giao_dich_thanh_toan')::text, 8, '0'),
    ma_thong_bao_phi VARCHAR(30) NULL,
    ma_booking VARCHAR(30) NULL,
    ma_cu_dan VARCHAR(30) NOT NULL,
    ma_ke_toan_doi_soat VARCHAR(30) NULL,
    ma_tham_chieu_hoa_don VARCHAR(50) NOT NULL,
    phuong_thuc_thanh_toan VARCHAR(50) NOT NULL,
    so_tien_giao_dich NUMERIC(15,2) NOT NULL,
    thoi_gian_giao_dich TIMESTAMPTZ DEFAULT now() NOT NULL,
    trang_thai_giao_dich VARCHAR(40) DEFAULT 'Đang xác minh' NOT NULL,
    ket_qua_doi_soat VARCHAR(40) DEFAULT 'Chờ đối soát' NOT NULL,
    thoi_gian_doi_soat TIMESTAMPTZ NULL,
    CONSTRAINT chk_giao_dich_thanh_toan_ma CHECK (ma_giao_dich ~ '^GD[0-9]{8}$'),
    CONSTRAINT fk_gdt_tbp FOREIGN KEY (ma_thong_bao_phi)
        REFERENCES thong_bao_phi (ma_thong_bao_phi),
    CONSTRAINT fk_gdt_booking FOREIGN KEY (ma_booking)
        REFERENCES phieu_dat_cho_tien_ich (ma_booking),
    CONSTRAINT fk_gdt_cudan FOREIGN KEY (ma_cu_dan)
        REFERENCES cu_dan (ma_cu_dan),
    CONSTRAINT fk_gdt_ketoan FOREIGN KEY (ma_ke_toan_doi_soat)
        REFERENCES ke_toan (ma_nv),
    CONSTRAINT chk_gdt_sotien CHECK (so_tien_giao_dich > 0),
    CONSTRAINT chk_gdt_phuongthuc CHECK (phuong_thuc_thanh_toan IN ('Trực tuyến (Cổng thanh toán)', 'Tiền mặt', 'Chuyển khoản')),
    CONSTRAINT chk_gdt_ttgiaodich CHECK (trang_thai_giao_dich IN ('Đang xác minh', 'Thành công', 'Thất bại', 'Đã hủy')),
    CONSTRAINT chk_gdt_kqdoisoat CHECK (ket_qua_doi_soat IN ('Chờ đối soát', 'Khớp', 'Chênh lệch', 'Không tìm thấy hóa đơn'))
);

-- 19. Biên nhận điện tử sau thanh toán thành công
CREATE SEQUENCE seq_bien_nhan_dien_tu AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE bien_nhan_dien_tu (
    ma_bien_nhan VARCHAR(30) PRIMARY KEY
        DEFAULT 'BN' || lpad(nextval('seq_bien_nhan_dien_tu')::text, 8, '0'),
    ma_giao_dich VARCHAR(30) NOT NULL UNIQUE,
    ngay_cap TIMESTAMPTZ DEFAULT now() NOT NULL,
    so_tien_xac_nhan NUMERIC(15,2) NOT NULL,
    noi_dung_bien_nhan TEXT NOT NULL,
    CONSTRAINT chk_bien_nhan_dien_tu_ma CHECK (ma_bien_nhan ~ '^BN[0-9]{8}$'),
    CONSTRAINT fk_biennhan_giaodich FOREIGN KEY (ma_giao_dich)
        REFERENCES giao_dich_thanh_toan (ma_giao_dich) ON DELETE RESTRICT,
    CONSTRAINT chk_biennhan_sotien CHECK (so_tien_xac_nhan > 0)
);

-- Index từ file gốc
CREATE INDEX idx_gdt_tbp ON giao_dich_thanh_toan (ma_thong_bao_phi);
CREATE INDEX idx_gdt_cudan ON giao_dich_thanh_toan (ma_cu_dan);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_giao_dich_thanh_toan_ma_booking ON giao_dich_thanh_toan (ma_booking);
CREATE INDEX idx_giao_dich_thanh_toan_ma_ke_toan_doi_soat ON giao_dich_thanh_toan (ma_ke_toan_doi_soat);
