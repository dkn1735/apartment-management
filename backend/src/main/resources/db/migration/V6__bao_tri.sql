-- Nhóm thiết bị kỹ thuật và bảo trì phòng ngừa (spec 0002).

-- 23. Thiết bị kỹ thuật của tòa nhà
CREATE SEQUENCE seq_thiet_bi_ky_thuat AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE thiet_bi_ky_thuat (
    ma_thiet_bi VARCHAR(30) PRIMARY KEY
        DEFAULT 'TB' || lpad(nextval('seq_thiet_bi_ky_thuat')::text, 8, '0'),
    ma_toa_nha VARCHAR(20) NOT NULL,
    ten_thiet_bi VARCHAR(150) COLLATE "vi-VN-x-icu" NOT NULL,
    nhom_he_thong VARCHAR(50) NOT NULL,
    vi_tri_lap_dat VARCHAR(150) NOT NULL,
    thong_so_tieu_chuan TEXT NOT NULL,
    trang_thai_thiet_bi VARCHAR(40) DEFAULT 'Bình thường' NOT NULL,
    CONSTRAINT chk_thiet_bi_ky_thuat_ma CHECK (ma_thiet_bi ~ '^TB[0-9]{8}$'),
    CONSTRAINT fk_thietbi_toanha FOREIGN KEY (ma_toa_nha)
        REFERENCES toa_nha (ma_toa_nha) ON DELETE RESTRICT,
    CONSTRAINT chk_thietbi_hethong CHECK (nhom_he_thong IN ('PCCC', 'Thang máy', 'Máy phát điện', 'Cấp thoát nước')),
    CONSTRAINT chk_thietbi_trangthai CHECK (trang_thai_thiet_bi IN ('Bình thường', 'Đang bảo trì', 'Cảnh báo sự cố', 'Ngừng hoạt động'))
);

-- 24. Mẫu checklist kiểm tra kỹ thuật
CREATE SEQUENCE seq_mau_checklist AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE mau_checklist (
    ma_checklist VARCHAR(30) PRIMARY KEY
        DEFAULT 'CL' || lpad(nextval('seq_mau_checklist')::text, 8, '0'),
    ten_checklist VARCHAR(200) COLLATE "vi-VN-x-icu" NOT NULL,
    nhom_he_thong_ap_dung VARCHAR(50) NOT NULL,
    CONSTRAINT chk_mau_checklist_ma CHECK (ma_checklist ~ '^CL[0-9]{8}$'),
    CONSTRAINT chk_mauchecklist_hethong CHECK (nhom_he_thong_ap_dung IN ('PCCC', 'Thang máy', 'Máy phát điện', 'Cấp thoát nước'))
);

-- 25. Hạng mục cần kiểm tra trong checklist
CREATE SEQUENCE seq_hang_muc_checklist AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE hang_muc_checklist (
    ma_hang_muc VARCHAR(30) PRIMARY KEY
        DEFAULT 'HM' || lpad(nextval('seq_hang_muc_checklist')::text, 8, '0'),
    ma_checklist VARCHAR(30) NOT NULL,
    ten_hang_muc_kiem_tra VARCHAR(255) COLLATE "vi-VN-x-icu" NOT NULL,
    thong_so_chuan VARCHAR(150) NOT NULL,
    bat_buoc_do_dac BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT chk_hang_muc_checklist_ma CHECK (ma_hang_muc ~ '^HM[0-9]{8}$'),
    CONSTRAINT fk_hangmuc_checklist FOREIGN KEY (ma_checklist)
        REFERENCES mau_checklist (ma_checklist) ON DELETE CASCADE
);

-- 26. Kế hoạch bảo trì định kỳ
CREATE SEQUENCE seq_ke_hoach_bao_tri AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE ke_hoach_bao_tri (
    ma_ke_hoach VARCHAR(30) PRIMARY KEY
        DEFAULT 'KH' || lpad(nextval('seq_ke_hoach_bao_tri')::text, 8, '0'),
    ma_thiet_bi VARCHAR(30) NOT NULL,
    ma_checklist VARCHAR(30) NOT NULL,
    ma_nvbql VARCHAR(30) NOT NULL,
    ten_ke_hoach VARCHAR(200) COLLATE "vi-VN-x-icu" NOT NULL,
    chu_ky_lap VARCHAR(30) NOT NULL,
    ngay_bat_dau DATE NOT NULL,
    ngay_den_han_tiep_theo DATE NOT NULL,
    trang_thai_ke_hoach VARCHAR(40) DEFAULT 'Đang kích hoạt' NOT NULL,
    CONSTRAINT chk_ke_hoach_bao_tri_ma CHECK (ma_ke_hoach ~ '^KH[0-9]{8}$'),
    CONSTRAINT fk_khbt_thietbi FOREIGN KEY (ma_thiet_bi)
        REFERENCES thiet_bi_ky_thuat (ma_thiet_bi) ON DELETE RESTRICT,
    CONSTRAINT fk_khbt_checklist FOREIGN KEY (ma_checklist)
        REFERENCES mau_checklist (ma_checklist) ON DELETE RESTRICT,
    CONSTRAINT fk_khbt_nvbql FOREIGN KEY (ma_nvbql)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT chk_khbt_chuky CHECK (chu_ky_lap IN ('Hàng tuần', 'Hàng tháng', 'Hàng quý', 'Hàng năm')),
    CONSTRAINT chk_khbt_ngay CHECK (ngay_den_han_tiep_theo >= ngay_bat_dau),
    CONSTRAINT chk_khbt_trangthai CHECK (trang_thai_ke_hoach IN ('Đang kích hoạt', 'Tạm dừng', 'Đã kết thúc'))
);

-- 27. Phiếu công việc bảo trì định kỳ
CREATE SEQUENCE seq_phieu_cong_viec_bao_tri AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE phieu_cong_viec_bao_tri (
    ma_phieu_cv VARCHAR(30) PRIMARY KEY
        DEFAULT 'PCV' || lpad(nextval('seq_phieu_cong_viec_bao_tri')::text, 8, '0'),
    ma_ke_hoach VARCHAR(30) NOT NULL,
    ma_nv_ky_thuat VARCHAR(30) NOT NULL,
    ma_nvbql_nghiem_thu VARCHAR(30) NULL,
    thoi_gian_giao_viec TIMESTAMPTZ DEFAULT now() NOT NULL,
    thoi_gian_tiep_nhan TIMESTAMPTZ NULL,
    ngay_den_han DATE NOT NULL,
    ngay_hoan_thanh TIMESTAMPTZ NULL,
    anh_minh_chung VARCHAR(255) NULL,
    trang_thai_phieu VARCHAR(40) DEFAULT 'Mới khởi tạo' NOT NULL,
    ly_do_yeu_cau_kiem_tra_lai TEXT NULL,
    nhan_xet_nghiem_thu TEXT NULL,
    CONSTRAINT chk_phieu_cong_viec_bao_tri_ma CHECK (ma_phieu_cv ~ '^PCV[0-9]{8}$'),
    CONSTRAINT fk_pcv_kehoach FOREIGN KEY (ma_ke_hoach)
        REFERENCES ke_hoach_bao_tri (ma_ke_hoach) ON DELETE RESTRICT,
    CONSTRAINT fk_pcv_nvkt FOREIGN KEY (ma_nv_ky_thuat)
        REFERENCES nhan_vien_ky_thuat (ma_nv),
    CONSTRAINT fk_pcv_nvbql FOREIGN KEY (ma_nvbql_nghiem_thu)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT chk_pcv_trangthai CHECK (trang_thai_phieu IN ('Mới khởi tạo', 'Đang thực hiện', 'Chờ nghiệm thu', 'Yêu cầu kiểm tra bổ sung', 'Đã hoàn thành', 'Hoàn thành có cảnh báo'))
);

-- 28. Kết quả đo đạc từng hạng mục checklist
CREATE SEQUENCE seq_ket_qua_hang_muc_checklist AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE ket_qua_hang_muc_checklist (
    ma_ket_qua VARCHAR(30) PRIMARY KEY
        DEFAULT 'KQ' || lpad(nextval('seq_ket_qua_hang_muc_checklist')::text, 8, '0'),
    ma_phieu_cv VARCHAR(30) NOT NULL,
    ma_hang_muc VARCHAR(30) NOT NULL,
    thong_so_thuc_te VARCHAR(150) NULL,
    danh_gia_dat BOOLEAN DEFAULT TRUE NOT NULL,
    nguy_co_hu_hong_cao BOOLEAN DEFAULT FALSE NOT NULL,
    ghi_chu_hien_truong TEXT NULL,
    CONSTRAINT chk_ket_qua_hang_muc_checklist_ma CHECK (ma_ket_qua ~ '^KQ[0-9]{8}$'),
    CONSTRAINT fk_kqhm_phieucv FOREIGN KEY (ma_phieu_cv)
        REFERENCES phieu_cong_viec_bao_tri (ma_phieu_cv) ON DELETE CASCADE,
    CONSTRAINT fk_kqhm_hangmuc FOREIGN KEY (ma_hang_muc)
        REFERENCES hang_muc_checklist (ma_hang_muc),
    CONSTRAINT uq_kqhm_phieucv_hangmuc UNIQUE (ma_phieu_cv, ma_hang_muc)
);

-- Index từ file gốc
CREATE INDEX idx_thietbi_toanha ON thiet_bi_ky_thuat (ma_toa_nha);
CREATE INDEX idx_pcv_kehoach ON phieu_cong_viec_bao_tri (ma_ke_hoach);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_hang_muc_checklist_ma_checklist ON hang_muc_checklist (ma_checklist);
CREATE INDEX idx_ke_hoach_bao_tri_ma_thiet_bi ON ke_hoach_bao_tri (ma_thiet_bi);
CREATE INDEX idx_ke_hoach_bao_tri_ma_checklist ON ke_hoach_bao_tri (ma_checklist);
CREATE INDEX idx_ke_hoach_bao_tri_ma_nvbql ON ke_hoach_bao_tri (ma_nvbql);
CREATE INDEX idx_phieu_cong_viec_bao_tri_ma_nv_ky_thuat ON phieu_cong_viec_bao_tri (ma_nv_ky_thuat);
CREATE INDEX idx_phieu_cong_viec_bao_tri_ma_nvbql_nghiem_thu ON phieu_cong_viec_bao_tri (ma_nvbql_nghiem_thu);
CREATE INDEX idx_ket_qua_hang_muc_checklist_ma_hang_muc ON ket_qua_hang_muc_checklist (ma_hang_muc);
