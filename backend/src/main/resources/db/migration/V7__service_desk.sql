-- Nhóm service desk, yêu cầu hỗ trợ và SLA (spec 0002). Tham chiếu thiết bị và phiếu công việc (V6).

-- 20. Quy định SLA theo phân loại và mức ưu tiên
CREATE SEQUENCE seq_quy_dinh_sla AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE quy_dinh_sla (
    ma_sla VARCHAR(30) PRIMARY KEY
        DEFAULT 'SLA' || lpad(nextval('seq_quy_dinh_sla')::text, 8, '0'),
    phan_loai_su_co VARCHAR(50) NOT NULL,
    muc_do_uu_tien VARCHAR(30) NOT NULL,
    thoi_gian_cam_ket_gio NUMERIC(6,2) NOT NULL,
    CONSTRAINT chk_quy_dinh_sla_ma CHECK (ma_sla ~ '^SLA[0-9]{8}$'),
    CONSTRAINT uq_sla_phanloai_mucdo UNIQUE (phan_loai_su_co, muc_do_uu_tien),
    CONSTRAINT chk_sla_phanloai CHECK (phan_loai_su_co IN ('Kỹ thuật', 'An ninh', 'Vệ sinh', 'Dịch vụ')),
    CONSTRAINT chk_sla_mucdo CHECK (muc_do_uu_tien IN ('Khẩn cấp', 'Cao', 'Trung bình', 'Thấp')),
    CONSTRAINT chk_sla_thoigian CHECK (thoi_gian_cam_ket_gio > 0)
);

-- 21. Phiếu yêu cầu hỗ trợ (ticket)
CREATE SEQUENCE seq_phieu_yeu_cau_ho_tro AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE phieu_yeu_cau_ho_tro (
    ma_ticket VARCHAR(30) PRIMARY KEY
        DEFAULT 'TK' || lpad(nextval('seq_phieu_yeu_cau_ho_tro')::text, 8, '0'),
    ma_cu_dan VARCHAR(30) NULL,
    ma_can_ho VARCHAR(20) NULL,
    ma_nvbql_dieu_phoi VARCHAR(30) NULL,
    ma_nv_ky_thuat VARCHAR(30) NULL,
    ma_sla VARCHAR(30) NULL,
    ma_phieu_cv_phat_sinh VARCHAR(30) NULL,
    ma_thiet_bi VARCHAR(30) NULL,
    phan_loai_su_co VARCHAR(50) NOT NULL,
    tieu_de VARCHAR(200) COLLATE "vi-VN-x-icu" NOT NULL,
    mo_ta_chi_tiet TEXT NOT NULL,
    muc_do_uu_tien VARCHAR(30) DEFAULT 'Trung bình' NOT NULL,
    thoi_gian_gui TIMESTAMPTZ DEFAULT now() NOT NULL,
    han_hoan_thanh_sla TIMESTAMPTZ NULL,
    thoi_gian_tam_hoan TIMESTAMPTZ NULL,
    thoi_gian_hoan_thanh TIMESTAMPTZ NULL,
    trang_thai_ticket VARCHAR(40) DEFAULT 'Chờ tiếp nhận' NOT NULL,
    ket_qua_tuan_thu_sla VARCHAR(30) NULL,
    ly_do_tu_choi_hoac_tam_hoan TEXT NULL,
    ket_qua_nghiem_thu TEXT NULL,
    diem_danh_gia_hai_long INTEGER NULL,
    nhan_xet_danh_gia TEXT NULL,
    CONSTRAINT chk_phieu_yeu_cau_ho_tro_ma CHECK (ma_ticket ~ '^TK[0-9]{8}$'),
    CONSTRAINT fk_ticket_cudan FOREIGN KEY (ma_cu_dan)
        REFERENCES cu_dan (ma_cu_dan),
    CONSTRAINT fk_ticket_canho FOREIGN KEY (ma_can_ho)
        REFERENCES can_ho (ma_can_ho),
    CONSTRAINT fk_ticket_nvbql FOREIGN KEY (ma_nvbql_dieu_phoi)
        REFERENCES nhan_vien_bql (ma_nv),
    CONSTRAINT fk_ticket_nvkt FOREIGN KEY (ma_nv_ky_thuat)
        REFERENCES nhan_vien_ky_thuat (ma_nv),
    CONSTRAINT fk_ticket_sla FOREIGN KEY (ma_sla)
        REFERENCES quy_dinh_sla (ma_sla),
    CONSTRAINT fk_ticket_pcv FOREIGN KEY (ma_phieu_cv_phat_sinh)
        REFERENCES phieu_cong_viec_bao_tri (ma_phieu_cv),
    CONSTRAINT fk_ticket_thietbi FOREIGN KEY (ma_thiet_bi)
        REFERENCES thiet_bi_ky_thuat (ma_thiet_bi),
    CONSTRAINT chk_ticket_phanloai CHECK (phan_loai_su_co IN ('Kỹ thuật', 'An ninh', 'Vệ sinh', 'Dịch vụ')),
    CONSTRAINT chk_ticket_mucdo CHECK (muc_do_uu_tien IN ('Khẩn cấp', 'Cao', 'Trung bình', 'Thấp')),
    CONSTRAINT chk_ticket_trangthai CHECK (trang_thai_ticket IN ('Chờ tiếp nhận', 'Từ chối tiếp nhận', 'Đã giao việc', 'Đang xử lý', 'Tạm hoãn / Chờ vật tư', 'Đã hoàn thành')),
    CONSTRAINT chk_ticket_sla_result CHECK (ket_qua_tuan_thu_sla IN ('Đúng hạn', 'Quá hạn SLA')),
    CONSTRAINT chk_ticket_diem CHECK (diem_danh_gia_hai_long BETWEEN 1 AND 5)
);

-- Index từ file gốc
CREATE INDEX idx_ticket_cudan_canho ON phieu_yeu_cau_ho_tro (ma_cu_dan, ma_can_ho);
CREATE INDEX idx_ticket_trangthai_sla ON phieu_yeu_cau_ho_tro (trang_thai_ticket, han_hoan_thanh_sla);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_phieu_yeu_cau_ho_tro_ma_can_ho ON phieu_yeu_cau_ho_tro (ma_can_ho);
CREATE INDEX idx_phieu_yeu_cau_ho_tro_ma_nvbql_dieu_phoi ON phieu_yeu_cau_ho_tro (ma_nvbql_dieu_phoi);
CREATE INDEX idx_phieu_yeu_cau_ho_tro_ma_nv_ky_thuat ON phieu_yeu_cau_ho_tro (ma_nv_ky_thuat);
CREATE INDEX idx_phieu_yeu_cau_ho_tro_ma_sla ON phieu_yeu_cau_ho_tro (ma_sla);
CREATE INDEX idx_phieu_yeu_cau_ho_tro_ma_phieu_cv_phat_sinh ON phieu_yeu_cau_ho_tro (ma_phieu_cv_phat_sinh);
CREATE INDEX idx_phieu_yeu_cau_ho_tro_ma_thiet_bi ON phieu_yeu_cau_ho_tro (ma_thiet_bi);
