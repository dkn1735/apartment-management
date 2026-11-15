-- Nhóm điện nước, tài chính và công nợ (spec 0002).

-- 13. Đồng hồ đo điện, nước tại căn hộ hoặc khu vực chung
CREATE SEQUENCE seq_dong_ho_tieu_thu AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE dong_ho_tieu_thu (
    ma_dong_ho VARCHAR(30) PRIMARY KEY
        DEFAULT 'DH' || lpad(nextval('seq_dong_ho_tieu_thu')::text, 8, '0'),
    ma_can_ho VARCHAR(20) NULL,
    ma_khu_vuc VARCHAR(30) NULL,
    loai_dong_ho VARCHAR(20) NOT NULL,
    vi_tri_lap_dat VARCHAR(150) NOT NULL,
    trang_thai_dong_ho VARCHAR(40) DEFAULT 'Đang hoạt động' NOT NULL,
    CONSTRAINT chk_dong_ho_tieu_thu_ma CHECK (ma_dong_ho ~ '^DH[0-9]{8}$'),
    CONSTRAINT fk_dongho_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho),
    CONSTRAINT fk_dongho_khuvuc FOREIGN KEY (ma_khu_vuc)
        REFERENCES khu_vuc_dung_chung (ma_khu_vuc),
    CONSTRAINT chk_dongho_loai CHECK (loai_dong_ho IN ('Điện', 'Nước')),
    CONSTRAINT chk_dongho_trangthai CHECK (trang_thai_dong_ho IN ('Đang hoạt động', 'Đã ghi nhận', 'Chưa ghi nhận/Đồng hồ lỗi', 'Ngừng sử dụng')),
    CONSTRAINT chk_dongho_xor CHECK ((ma_can_ho IS NOT NULL AND ma_khu_vuc IS NULL) OR (ma_can_ho IS NULL AND ma_khu_vuc IS NOT NULL))
);

-- 14. Bảng ghi chỉ số điện nước theo kỳ
CREATE SEQUENCE seq_bang_ghi_chi_so AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE bang_ghi_chi_so (
    ma_ban_ghi VARCHAR(30) PRIMARY KEY
        DEFAULT 'BG' || lpad(nextval('seq_bang_ghi_chi_so')::text, 8, '0'),
    ma_dong_ho VARCHAR(30) NOT NULL,
    ma_nv_ky_thuat VARCHAR(30) NOT NULL,
    ma_nvbql_xac_minh VARCHAR(30) NULL,
    ky_ghi_chi_so VARCHAR(7) NOT NULL,
    chi_so_ky_truoc NUMERIC(12,2) NOT NULL,
    chi_so_hien_tai NUMERIC(12,2) NULL,
    luong_tieu_thu NUMERIC(12,2) NULL,
    anh_minh_chung VARCHAR(255) NULL,
    ghi_chu_bat_thuong TEXT NULL,
    ly_do_chinh_sua TEXT NULL,
    thoi_gian_ghi TIMESTAMPTZ DEFAULT now() NOT NULL,
    trang_thai_ghi VARCHAR(40) DEFAULT 'Đã ghi nhận' NOT NULL,
    CONSTRAINT chk_bang_ghi_chi_so_ma CHECK (ma_ban_ghi ~ '^BG[0-9]{8}$'),
    CONSTRAINT fk_bgcs_dongho FOREIGN KEY (ma_dong_ho)
        REFERENCES dong_ho_tieu_thu (ma_dong_ho),
    CONSTRAINT fk_bgcs_nvkt FOREIGN KEY (ma_nv_ky_thuat)
        REFERENCES nhan_vien_ky_thuat (ma_nv),
    CONSTRAINT fk_bgcs_nvbql FOREIGN KEY (ma_nvbql_xac_minh)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT uq_bgcs_dongho_ky UNIQUE (ma_dong_ho, ky_ghi_chi_so),
    CONSTRAINT chk_bang_ghi_chi_so_ky CHECK (ky_ghi_chi_so ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT chk_bgcs_chisokytruoc CHECK (chi_so_ky_truoc >= 0),
    CONSTRAINT chk_bgcs_chisohientai CHECK (chi_so_hien_tai IS NULL OR chi_so_hien_tai >= 0),
    CONSTRAINT chk_bgcs_luongtieuthu CHECK (luong_tieu_thu IS NULL OR luong_tieu_thu >= 0),
    CONSTRAINT chk_bgcs_trangthai CHECK (trang_thai_ghi IN ('Đã ghi nhận', 'Chờ xác minh ngoại lệ', 'Chưa ghi nhận/Đồng hồ lỗi'))
);

-- 15. Biểu phí dịch vụ: đơn giá, định mức
CREATE SEQUENCE seq_bieu_phi_dich_vu AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE bieu_phi_dich_vu (
    ma_bieu_phi VARCHAR(30) PRIMARY KEY
        DEFAULT 'BP' || lpad(nextval('seq_bieu_phi_dich_vu')::text, 8, '0'),
    ma_ke_toan_cau_hinh VARCHAR(30) NOT NULL,
    ten_bieu_phi VARCHAR(150) COLLATE "vi-VN-x-icu" NOT NULL,
    loai_dich_vu VARCHAR(50) NOT NULL,
    don_gia_dinh_muc NUMERIC(15,2) NOT NULL,
    don_vi_tinh VARCHAR(30) NOT NULL,
    ngay_hieu_luc DATE NOT NULL,
    trang_thai_hieu_luc VARCHAR(30) DEFAULT 'Đang áp dụng' NOT NULL,
    CONSTRAINT chk_bieu_phi_dich_vu_ma CHECK (ma_bieu_phi ~ '^BP[0-9]{8}$'),
    CONSTRAINT fk_bieuphi_ketoan FOREIGN KEY (ma_ke_toan_cau_hinh)
        REFERENCES ke_toan (ma_nv),
    CONSTRAINT chk_bieuphi_dongia CHECK (don_gia_dinh_muc >= 0),
    CONSTRAINT chk_bieuphi_trangthai CHECK (trang_thai_hieu_luc IN ('Đang áp dụng', 'Ngừng áp dụng'))
);

-- 16. Thông báo phí hằng tháng cho từng căn hộ
CREATE SEQUENCE seq_thong_bao_phi AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE thong_bao_phi (
    ma_thong_bao_phi VARCHAR(30) PRIMARY KEY
        DEFAULT 'TBP' || lpad(nextval('seq_thong_bao_phi')::text, 8, '0'),
    ma_can_ho VARCHAR(20) NOT NULL,
    ma_ke_toan VARCHAR(30) NOT NULL,
    ky_tinh_phi VARCHAR(7) NOT NULL,
    ngay_phat_hanh DATE NULL,
    han_thanh_toan DATE NOT NULL,
    tong_tien_phai_thu NUMERIC(15,2) DEFAULT 0 NOT NULL,
    so_tien_da_thu NUMERIC(15,2) DEFAULT 0 NOT NULL,
    du_no_con_lai NUMERIC(15,2) DEFAULT 0 NOT NULL,
    trang_thai_phat_hanh VARCHAR(40) DEFAULT 'Bản nháp' NOT NULL,
    trang_thai_thanh_toan VARCHAR(40) DEFAULT 'Chưa thanh toán' NOT NULL,
    CONSTRAINT chk_thong_bao_phi_ma CHECK (ma_thong_bao_phi ~ '^TBP[0-9]{8}$'),
    CONSTRAINT fk_tbp_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho),
    CONSTRAINT fk_tbp_ketoan FOREIGN KEY (ma_ke_toan)
        REFERENCES ke_toan (ma_nv),
    CONSTRAINT uq_tbp_canho_kytinhphi UNIQUE (ma_can_ho, ky_tinh_phi),
    CONSTRAINT chk_thong_bao_phi_ky CHECK (ky_tinh_phi ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT chk_tbp_tongtien CHECK (tong_tien_phai_thu >= 0),
    CONSTRAINT chk_tbp_dathu CHECK (so_tien_da_thu >= 0),
    CONSTRAINT chk_tbp_duno CHECK (du_no_con_lai >= 0),
    CONSTRAINT chk_tbp_ttphathanh CHECK (trang_thai_phat_hanh IN ('Bản nháp', 'Đã phát hành', 'Gửi thông báo thất bại')),
    CONSTRAINT chk_tbp_ttthanhtoan CHECK (trang_thai_thanh_toan IN ('Chưa thanh toán', 'Đã thanh toán một phần', 'Đã thanh toán', 'Nợ đọng'))
);

-- 17. Chi tiết khoản phí trong thông báo phí
CREATE SEQUENCE seq_chi_tiet_khoan_phi AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE chi_tiet_khoan_phi (
    ma_chi_tiet VARCHAR(30) PRIMARY KEY
        DEFAULT 'CTP' || lpad(nextval('seq_chi_tiet_khoan_phi')::text, 8, '0'),
    ma_thong_bao_phi VARCHAR(30) NOT NULL,
    ma_bieu_phi VARCHAR(30) NOT NULL,
    ma_ban_ghi_chi_so VARCHAR(30) NULL UNIQUE,
    ten_khoan_muc VARCHAR(150) COLLATE "vi-VN-x-icu" NOT NULL,
    so_luong_tieu_thu NUMERIC(12,2) NOT NULL,
    don_gia_ap_dung NUMERIC(15,2) NOT NULL,
    thanh_tien NUMERIC(15,2) NOT NULL,
    CONSTRAINT chk_chi_tiet_khoan_phi_ma CHECK (ma_chi_tiet ~ '^CTP[0-9]{8}$'),
    CONSTRAINT fk_ctkp_tbp FOREIGN KEY (ma_thong_bao_phi)
        REFERENCES thong_bao_phi (ma_thong_bao_phi) ON DELETE CASCADE,
    CONSTRAINT fk_ctkp_bieuphi FOREIGN KEY (ma_bieu_phi)
        REFERENCES bieu_phi_dich_vu (ma_bieu_phi) ON DELETE RESTRICT,
    CONSTRAINT fk_ctkp_bgcs FOREIGN KEY (ma_ban_ghi_chi_so)
        REFERENCES bang_ghi_chi_so (ma_ban_ghi),
    CONSTRAINT chk_ctkp_soluong CHECK (so_luong_tieu_thu >= 0),
    CONSTRAINT chk_ctkp_dongia CHECK (don_gia_ap_dung >= 0),
    CONSTRAINT chk_ctkp_thanhtien CHECK (thanh_tien >= 0)
);

-- Index từ file gốc (bỏ idx_bgcs_dongho_ky, idx_tbp_canho_ky: trùng UNIQUE)
CREATE INDEX idx_dongho_canho ON dong_ho_tieu_thu (ma_can_ho);
CREATE INDEX idx_dongho_khuvuc ON dong_ho_tieu_thu (ma_khu_vuc);
CREATE INDEX idx_tbp_trangthai ON thong_bao_phi (trang_thai_thanh_toan, trang_thai_phat_hanh);
CREATE INDEX idx_ctkp_tbp ON chi_tiet_khoan_phi (ma_thong_bao_phi);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_bang_ghi_chi_so_ma_nv_ky_thuat ON bang_ghi_chi_so (ma_nv_ky_thuat);
CREATE INDEX idx_bang_ghi_chi_so_ma_nvbql_xac_minh ON bang_ghi_chi_so (ma_nvbql_xac_minh);
CREATE INDEX idx_bieu_phi_dich_vu_ma_ke_toan_cau_hinh ON bieu_phi_dich_vu (ma_ke_toan_cau_hinh);
CREATE INDEX idx_thong_bao_phi_ma_ke_toan ON thong_bao_phi (ma_ke_toan);
CREATE INDEX idx_chi_tiet_khoan_phi_ma_bieu_phi ON chi_tiet_khoan_phi (ma_bieu_phi);
