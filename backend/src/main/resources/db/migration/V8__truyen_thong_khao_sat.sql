-- Nhóm truyền thông, khảo sát và tệp đính kèm (spec 0002). Tệp đính kèm tham chiếu ticket (V7).

-- 32. Thông báo vận hành tòa nhà
CREATE SEQUENCE seq_thong_bao_van_hanh AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE thong_bao_van_hanh (
    ma_thong_bao VARCHAR(30) PRIMARY KEY
        DEFAULT 'TBV' || lpad(nextval('seq_thong_bao_van_hanh')::text, 8, '0'),
    ma_nvbql VARCHAR(30) NOT NULL,
    tieu_de VARCHAR(250) COLLATE "vi-VN-x-icu" NOT NULL,
    phan_loai_tin VARCHAR(50) NOT NULL,
    muc_do_quan_trong VARCHAR(30) DEFAULT 'Bình thường' NOT NULL,
    noi_dung_chi_tiet TEXT NOT NULL,
    pham_vi_nguoi_nhan VARCHAR(150) NOT NULL,
    kenh_phan_phoi VARCHAR(100) NOT NULL,
    thoi_gian_len_lich TIMESTAMPTZ NULL,
    thoi_gian_phat_hanh TIMESTAMPTZ NULL,
    trang_thai_thong_bao VARCHAR(30) DEFAULT 'Bản nháp' NOT NULL,
    CONSTRAINT chk_thong_bao_van_hanh_ma CHECK (ma_thong_bao ~ '^TBV[0-9]{8}$'),
    CONSTRAINT fk_tbvh_nvbql FOREIGN KEY (ma_nvbql)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT chk_tbvh_phanloai CHECK (phan_loai_tin IN ('Lịch cắt điện/nước', 'Kỹ thuật - Bảo trì', 'PCCC', 'Nội quy', 'Khác')),
    CONSTRAINT chk_tbvh_mucdo CHECK (muc_do_quan_trong IN ('Bình thường', 'Quan trọng', 'Khẩn cấp')),
    CONSTRAINT chk_tbvh_trangthai CHECK (trang_thai_thong_bao IN ('Bản nháp', 'Đã lên lịch', 'Đã phát hành'))
);

-- 22. Tệp đính kèm ticket hoặc thông báo
CREATE SEQUENCE seq_tep_dinh_kem AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE tep_dinh_kem (
    ma_tep VARCHAR(30) PRIMARY KEY
        DEFAULT 'TEP' || lpad(nextval('seq_tep_dinh_kem')::text, 8, '0'),
    ma_ticket VARCHAR(30) NULL,
    ma_thong_bao VARCHAR(30) NULL,
    ten_tep VARCHAR(255) COLLATE "vi-VN-x-icu" NOT NULL,
    dinh_dang VARCHAR(20) NOT NULL,
    dung_luong_mb NUMERIC(5,2) NOT NULL,
    duong_dan_luu_tru VARCHAR(500) NOT NULL,
    vai_tro_tep VARCHAR(50) NOT NULL,
    CONSTRAINT chk_tep_dinh_kem_ma CHECK (ma_tep ~ '^TEP[0-9]{8}$'),
    CONSTRAINT fk_tep_ticket FOREIGN KEY (ma_ticket)
        REFERENCES phieu_yeu_cau_ho_tro (ma_ticket) ON DELETE CASCADE,
    CONSTRAINT fk_tep_thongbao FOREIGN KEY (ma_thong_bao)
        REFERENCES thong_bao_van_hanh (ma_thong_bao) ON DELETE CASCADE,
    CONSTRAINT chk_tep_dungluong CHECK (dung_luong_mb > 0 AND dung_luong_mb <= 20.00),
    CONSTRAINT chk_tep_vaitro CHECK (vai_tro_tep IN ('Ảnh hiện trạng sự cố', 'Ảnh nghiệm thu sau xử lý', 'Tệp thông báo')),
    CONSTRAINT chk_tep_xor CHECK ((ma_ticket IS NOT NULL AND ma_thong_bao IS NULL) OR (ma_ticket IS NULL AND ma_thong_bao IS NOT NULL))
);

-- 33. Trạng thái nhận thông báo của cư dân (khóa ghép, không có mã tự sinh)
CREATE TABLE trang_thai_nhan_thong_bao (
    ma_thong_bao VARCHAR(30) NOT NULL,
    ma_cu_dan VARCHAR(30) NOT NULL,
    thoi_gian_phan_phoi TIMESTAMPTZ DEFAULT now() NOT NULL,
    da_doc BOOLEAN DEFAULT FALSE NOT NULL,
    thoi_gian_doc TIMESTAMPTZ NULL,
    PRIMARY KEY (ma_thong_bao, ma_cu_dan),
    CONSTRAINT fk_ttntb_thongbao FOREIGN KEY (ma_thong_bao)
        REFERENCES thong_bao_van_hanh (ma_thong_bao) ON DELETE CASCADE,
    CONSTRAINT fk_ttntb_cudan FOREIGN KEY (ma_cu_dan)
        REFERENCES cu_dan (ma_cu_dan) ON DELETE CASCADE
);

-- 34. Đợt khảo sát, lấy ý kiến, biểu quyết
CREATE SEQUENCE seq_dot_khao_sat AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE dot_khao_sat (
    ma_khao_sat VARCHAR(30) PRIMARY KEY
        DEFAULT 'KS' || lpad(nextval('seq_dot_khao_sat')::text, 8, '0'),
    ma_nvbql VARCHAR(30) NOT NULL,
    tieu_de VARCHAR(250) COLLATE "vi-VN-x-icu" NOT NULL,
    muc_dich_mo_ta TEXT NOT NULL,
    pham_vi_doi_tuong VARCHAR(150) NOT NULL,
    ngay_bat_dau TIMESTAMPTZ NOT NULL,
    ngay_ket_thuc TIMESTAMPTZ NOT NULL,
    quy_tac_gioi_han_phieu VARCHAR(50) DEFAULT '1 phiếu/căn hộ' NOT NULL,
    che_do_danh_tinh VARCHAR(30) DEFAULT 'Công khai' NOT NULL,
    trang_thai_khao_sat VARCHAR(30) DEFAULT 'Đã lên lịch' NOT NULL,
    CONSTRAINT chk_dot_khao_sat_ma CHECK (ma_khao_sat ~ '^KS[0-9]{8}$'),
    CONSTRAINT fk_khaosat_nvbql FOREIGN KEY (ma_nvbql)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT chk_khaosat_thoigian CHECK (ngay_ket_thuc > ngay_bat_dau),
    CONSTRAINT chk_khaosat_quytac CHECK (quy_tac_gioi_han_phieu IN ('1 phiếu/căn hộ', '1 phiếu/tài khoản')),
    CONSTRAINT chk_khaosat_chedo CHECK (che_do_danh_tinh IN ('Công khai', 'Ẩn danh')),
    CONSTRAINT chk_khaosat_trangthai CHECK (trang_thai_khao_sat IN ('Đã lên lịch', 'Đang mở', 'Đã đóng'))
);

-- 35. Câu hỏi trong đợt khảo sát
CREATE SEQUENCE seq_cau_hoi_khao_sat AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE cau_hoi_khao_sat (
    ma_cau_hoi VARCHAR(30) PRIMARY KEY
        DEFAULT 'CH' || lpad(nextval('seq_cau_hoi_khao_sat')::text, 8, '0'),
    ma_khao_sat VARCHAR(30) NOT NULL,
    noi_dung_cau_hoi TEXT NOT NULL,
    dang_cau_hoi VARCHAR(50) NOT NULL,
    bat_buoc_tra_loi BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT chk_cau_hoi_khao_sat_ma CHECK (ma_cau_hoi ~ '^CH[0-9]{8}$'),
    CONSTRAINT fk_cauhoi_khaosat FOREIGN KEY (ma_khao_sat)
        REFERENCES dot_khao_sat (ma_khao_sat) ON DELETE CASCADE,
    CONSTRAINT chk_cauhoi_dang CHECK (dang_cau_hoi IN ('Trắc nghiệm đơn lựa chọn', 'Trắc nghiệm nhiều lựa chọn', 'Thang điểm 1-5 sao', 'Ý kiến tự do'))
);

-- 36. Phiếu phản hồi khảo sát của cư dân
CREATE SEQUENCE seq_phieu_phan_hoi_khao_sat AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE phieu_phan_hoi_khao_sat (
    ma_phan_hoi VARCHAR(30) PRIMARY KEY
        DEFAULT 'PH' || lpad(nextval('seq_phieu_phan_hoi_khao_sat')::text, 8, '0'),
    ma_khao_sat VARCHAR(30) NOT NULL,
    ma_cu_dan VARCHAR(30) NOT NULL,
    ma_can_ho VARCHAR(20) NOT NULL,
    thoi_gian_nop TIMESTAMPTZ DEFAULT now() NOT NULL,
    du_lieu_tra_loi JSONB NOT NULL,
    CONSTRAINT chk_phieu_phan_hoi_khao_sat_ma CHECK (ma_phan_hoi ~ '^PH[0-9]{8}$'),
    CONSTRAINT fk_phanhoi_khaosat FOREIGN KEY (ma_khao_sat)
        REFERENCES dot_khao_sat (ma_khao_sat) ON DELETE RESTRICT,
    CONSTRAINT fk_phanhoi_cudan FOREIGN KEY (ma_cu_dan)
        REFERENCES cu_dan (ma_cu_dan),
    CONSTRAINT fk_phanhoi_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho),
    CONSTRAINT uq_phanhoi_khaosat_cudan UNIQUE (ma_khao_sat, ma_cu_dan)
);

-- Index từ file gốc
CREATE INDEX idx_tbvh_trangthai ON thong_bao_van_hanh (trang_thai_thong_bao, thoi_gian_phat_hanh);
CREATE INDEX idx_khaosat_trangthai ON dot_khao_sat (trang_thai_khao_sat, ngay_ket_thuc);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_thong_bao_van_hanh_ma_nvbql ON thong_bao_van_hanh (ma_nvbql);
CREATE INDEX idx_tep_dinh_kem_ma_ticket ON tep_dinh_kem (ma_ticket);
CREATE INDEX idx_tep_dinh_kem_ma_thong_bao ON tep_dinh_kem (ma_thong_bao);
CREATE INDEX idx_trang_thai_nhan_thong_bao_ma_cu_dan ON trang_thai_nhan_thong_bao (ma_cu_dan);
CREATE INDEX idx_dot_khao_sat_ma_nvbql ON dot_khao_sat (ma_nvbql);
CREATE INDEX idx_cau_hoi_khao_sat_ma_khao_sat ON cau_hoi_khao_sat (ma_khao_sat);
CREATE INDEX idx_phieu_phan_hoi_khao_sat_ma_cu_dan ON phieu_phan_hoi_khao_sat (ma_cu_dan);
CREATE INDEX idx_phieu_phan_hoi_khao_sat_ma_can_ho ON phieu_phan_hoi_khao_sat (ma_can_ho);
