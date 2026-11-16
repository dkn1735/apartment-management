-- Nhóm báo cáo thống kê và nhật ký hệ thống (spec 0002).
-- nhat_ky_he_thong giữ nguyên cột gốc (chỉ đổi TIMESTAMPTZ); tính năng 8 sẽ chỉnh.

-- 37. Lịch sử kết xuất báo cáo
CREATE SEQUENCE seq_bao_cao_thong_ke AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE bao_cao_thong_ke (
    ma_bao_cao VARCHAR(30) PRIMARY KEY
        DEFAULT 'BC' || lpad(nextval('seq_bao_cao_thong_ke')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL,
    loai_bao_cao VARCHAR(100) NOT NULL,
    tu_ngay DATE NOT NULL,
    den_ngay DATE NOT NULL,
    pham_vi_thong_ke VARCHAR(100) NOT NULL,
    dinh_dang_ket_xuat VARCHAR(15) NOT NULL,
    duong_dan_file_ket_xuat VARCHAR(500) NULL,
    thoi_gian_ket_xuat TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT chk_bao_cao_thong_ke_ma CHECK (ma_bao_cao ~ '^BC[0-9]{8}$'),
    CONSTRAINT fk_baocao_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung),
    CONSTRAINT chk_baocao_loai CHECK (loai_bao_cao IN ('Báo cáo Doanh thu & Công nợ', 'Báo cáo Hiệu suất xử lý SLA', 'Báo cáo Chi phí OPEX & Bảo dưỡng tài sản')),
    CONSTRAINT chk_baocao_thoigian CHECK (den_ngay >= tu_ngay),
    CONSTRAINT chk_baocao_dinhdang CHECK (dinh_dang_ket_xuat IN ('Excel', 'PDF'))
);

-- 38. Nhật ký kiểm toán an toàn thông tin (audit log)
CREATE SEQUENCE seq_nhat_ky_he_thong AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE;

CREATE TABLE nhat_ky_he_thong (
    ma_log VARCHAR(30) PRIMARY KEY
        DEFAULT 'LOG' || lpad(nextval('seq_nhat_ky_he_thong')::text, 8, '0'),
    ma_nguoi_dung VARCHAR(30) NOT NULL,
    loai_thao_tac VARCHAR(100) NOT NULL,
    doi_tuong_tac_dong VARCHAR(150) NOT NULL,
    thoi_gian_ghi TIMESTAMPTZ DEFAULT now() NOT NULL,
    chi_tiet_thay_doi TEXT NULL,
    CONSTRAINT chk_nhat_ky_he_thong_ma CHECK (ma_log ~ '^LOG[0-9]{8}$'),
    CONSTRAINT fk_nhatky_nguoidung FOREIGN KEY (ma_nguoi_dung)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT
);

-- Index từ file gốc
CREATE INDEX idx_nhatky_nguoidung_thoigian ON nhat_ky_he_thong (ma_nguoi_dung, thoi_gian_ghi);

-- Index mới cho khóa ngoại chưa có index
CREATE INDEX idx_bao_cao_thong_ke_ma_nguoi_dung ON bao_cao_thong_ke (ma_nguoi_dung);
