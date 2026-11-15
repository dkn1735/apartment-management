-- Nhóm định danh và xác thực (spec 0002).
-- Mô hình bảng con (TPT): mỗi vai trò một bảng con trỏ về nguoi_dung_he_thong.
-- Mã tự sinh: tiền tố + 8 chữ số từ sequence; MAXVALUE chặn tràn, CHECK là lớp phụ.

-- 1. Người dùng hệ thống: định danh gốc và tài khoản đăng nhập
CREATE SEQUENCE seq_nguoi_dung_he_thong AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE nguoi_dung_he_thong (
    ma_nguoi_dung VARCHAR(30) PRIMARY KEY
        DEFAULT 'ND' || lpad(nextval('seq_nguoi_dung_he_thong')::text, 8, '0'),
    ho_ten VARCHAR(100) COLLATE "vi-VN-x-icu" NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    mat_khau_hash VARCHAR(255) NOT NULL,
    trang_thai_tai_khoan VARCHAR(30) DEFAULT 'Hoạt động' NOT NULL,
    so_lan_dang_nhap_sai INTEGER DEFAULT 0 NOT NULL,
    khoa_tam_den TIMESTAMPTZ NULL,
    dang_nhap_cuoi_luc TIMESTAMPTZ NULL,
    doi_mat_khau_luc TIMESTAMPTZ NULL,
    tao_luc TIMESTAMPTZ DEFAULT now() NOT NULL,
    cap_nhat_luc TIMESTAMPTZ DEFAULT now() NOT NULL,
    phien_ban BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_nguoi_dung_he_thong_ma CHECK (ma_nguoi_dung ~ '^ND[0-9]{8}$'),
    CONSTRAINT chk_trangthaitaikhoan CHECK (trang_thai_tai_khoan IN ('Hoạt động', 'Tạm khóa', 'Ngừng hoạt động')),
    CONSTRAINT chk_nguoi_dung_he_thong_so_dien_thoai CHECK (so_dien_thoai ~ '^0[0-9]{9}$'),
    CONSTRAINT chk_nguoi_dung_he_thong_email CHECK (email = lower(btrim(email))),
    CONSTRAINT chk_nguoi_dung_he_thong_mat_khau_hash CHECK (mat_khau_hash ~ '^\{[a-z0-9]+\}.+'),
    CONSTRAINT chk_nguoi_dung_he_thong_so_lan_dang_nhap_sai CHECK (so_lan_dang_nhap_sai >= 0)
);

-- 2. Nhân viên ban quản lý vận hành
CREATE SEQUENCE seq_nhan_vien_bql AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE nhan_vien_bql (
    ma_nv VARCHAR(30) PRIMARY KEY
        DEFAULT 'BQL' || lpad(nextval('seq_nhan_vien_bql')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    chuc_vu VARCHAR(100) NOT NULL,
    bo_phan VARCHAR(100) NOT NULL,
    CONSTRAINT chk_nhan_vien_bql_ma CHECK (ma_nv ~ '^BQL[0-9]{8}$'),
    CONSTRAINT fk_nvbql_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 3. Nhân viên kỹ thuật bảo trì, ghi chỉ số
CREATE SEQUENCE seq_nhan_vien_ky_thuat AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE nhan_vien_ky_thuat (
    ma_nv VARCHAR(30) PRIMARY KEY
        DEFAULT 'KTV' || lpad(nextval('seq_nhan_vien_ky_thuat')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    chuyen_mon_ky_thuat VARCHAR(150) NOT NULL,
    CONSTRAINT chk_nhan_vien_ky_thuat_ma CHECK (ma_nv ~ '^KTV[0-9]{8}$'),
    CONSTRAINT fk_nvkt_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 4. Kế toán tài chính, công nợ
CREATE SEQUENCE seq_ke_toan AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE ke_toan (
    ma_nv VARCHAR(30) PRIMARY KEY
        DEFAULT 'KT' || lpad(nextval('seq_ke_toan')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    pham_vi_phu_trach VARCHAR(150) NOT NULL,
    CONSTRAINT chk_ke_toan_ma CHECK (ma_nv ~ '^KT[0-9]{8}$'),
    CONSTRAINT fk_ketoan_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 5. Thành viên ban quản trị chung cư
CREATE SEQUENCE seq_thanh_vien_ban_quan_tri AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE thanh_vien_ban_quan_tri (
    ma_thanh_vien VARCHAR(30) PRIMARY KEY
        DEFAULT 'BQT' || lpad(nextval('seq_thanh_vien_ban_quan_tri')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    nhiem_ky VARCHAR(50) NOT NULL,
    vai_tro VARCHAR(100) NOT NULL,
    CONSTRAINT chk_thanh_vien_ban_quan_tri_ma CHECK (ma_thanh_vien ~ '^BQT[0-9]{8}$'),
    CONSTRAINT fk_bqt_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 6. Cư dân
CREATE SEQUENCE seq_cu_dan AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE cu_dan (
    ma_cu_dan VARCHAR(30) PRIMARY KEY
        DEFAULT 'CD' || lpad(nextval('seq_cu_dan')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    so_cccd VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT chk_cu_dan_ma CHECK (ma_cu_dan ~ '^CD[0-9]{8}$'),
    CONSTRAINT chk_cu_dan_so_cccd CHECK (so_cccd ~ '^[0-9]{12}$'),
    CONSTRAINT fk_cudan_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 7. (mới) Quản trị viên hệ thống: vai trò Admin, không có thuộc tính riêng
CREATE SEQUENCE seq_quan_tri_vien AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE quan_tri_vien (
    ma_quan_tri_vien VARCHAR(30) PRIMARY KEY
        DEFAULT 'QTV' || lpad(nextval('seq_quan_tri_vien')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT chk_quan_tri_vien_ma CHECK (ma_quan_tri_vien ~ '^QTV[0-9]{8}$'),
    CONSTRAINT fk_quan_tri_vien_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 8. (mới) Nhân viên bảo vệ
CREATE SEQUENCE seq_nhan_vien_bao_ve AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE nhan_vien_bao_ve (
    ma_nv VARCHAR(30) PRIMARY KEY
        DEFAULT 'BV' || lpad(nextval('seq_nhan_vien_bao_ve')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL UNIQUE,
    vi_tri_truc VARCHAR(150) NOT NULL,
    CONSTRAINT chk_nhan_vien_bao_ve_ma CHECK (ma_nv ~ '^BV[0-9]{8}$'),
    CONSTRAINT fk_nhan_vien_bao_ve_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- 9. (mới) Phiên đăng nhập: một dòng cho mỗi refresh token đã cấp (chỉ lưu SHA-256)
CREATE TABLE phien_dang_nhap (
    ma_phien BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ma_nguoi_dung VARCHAR(30) NOT NULL,
    ma_ho_phien UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    tao_luc TIMESTAMPTZ NOT NULL,
    het_han_luc TIMESTAMPTZ NOT NULL,
    thu_hoi_luc TIMESTAMPTZ NULL,
    ly_do_thu_hoi VARCHAR(40) NULL,
    dia_chi_ip VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    CONSTRAINT fk_phien_dang_nhap_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT,
    CONSTRAINT chk_phien_dang_nhap_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_phien_dang_nhap_het_han CHECK (het_han_luc > tao_luc),
    CONSTRAINT chk_phien_dang_nhap_ly_do_thu_hoi CHECK (ly_do_thu_hoi IN ('Đăng xuất', 'Đã xoay vòng', 'Dùng lại token cũ', 'Đổi mật khẩu', 'Đặt lại mật khẩu', 'Tài khoản bị khóa')),
    CONSTRAINT chk_phien_dang_nhap_thu_hoi CHECK ((thu_hoi_luc IS NULL) = (ly_do_thu_hoi IS NULL))
);

CREATE INDEX idx_phien_dang_nhap_ma_nguoi_dung ON phien_dang_nhap (ma_nguoi_dung);
CREATE INDEX idx_phien_dang_nhap_ma_ho_phien ON phien_dang_nhap (ma_ho_phien);

-- 10. (mới) Yêu cầu đặt lại mật khẩu (chỉ lưu SHA-256 của mã)
CREATE TABLE yeu_cau_dat_lai_mat_khau (
    ma_yeu_cau BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ma_nguoi_dung VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    tao_luc TIMESTAMPTZ NOT NULL,
    het_han_luc TIMESTAMPTZ NOT NULL,
    da_dung_luc TIMESTAMPTZ NULL,
    CONSTRAINT fk_yeu_cau_dat_lai_mat_khau_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT,
    CONSTRAINT chk_yeu_cau_dat_lai_mat_khau_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_yeu_cau_dat_lai_mat_khau_het_han CHECK (het_han_luc > tao_luc)
);

CREATE INDEX idx_yeu_cau_dat_lai_mat_khau_ma_nguoi_dung ON yeu_cau_dat_lai_mat_khau (ma_nguoi_dung);
