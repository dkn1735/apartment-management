-- Spec 0007 (nhật ký kiểm toán dùng chung). Bảng từ V9 đang trống, nên chỉnh một lần
-- tới mô hình đích.

-- Đăng nhập bằng tài khoản không tồn tại không có người thực hiện (AC-3)
ALTER TABLE nhat_ky_he_thong ALTER COLUMN ma_nguoi_dung DROP NOT NULL;

ALTER TABLE nhat_ky_he_thong
    ALTER COLUMN chi_tiet_thay_doi TYPE JSONB USING chi_tiet_thay_doi::jsonb;

ALTER TABLE nhat_ky_he_thong
    ADD COLUMN dia_chi_ip VARCHAR(45) NULL,
    ADD COLUMN request_id VARCHAR(64) NULL;

ALTER TABLE nhat_ky_he_thong
    ADD CONSTRAINT chk_nhat_ky_he_thong_loai_thao_tac CHECK (loai_thao_tac ~ '^[A-Z][A-Z0-9_]*$'),
    ADD CONSTRAINT chk_nhat_ky_he_thong_doi_tuong
        CHECK (doi_tuong_tac_dong ~ '^[A-Z][A-Za-z]*(:[A-Za-z0-9-]+)?$'),
    ADD CONSTRAINT chk_nhat_ky_he_thong_chi_tiet
        CHECK (chi_tiet_thay_doi IS NULL OR jsonb_typeof(chi_tiet_thay_doi) = 'object');

-- Danh sách mặc định kèm tiêu chí phụ, và lọc theo loại thao tác (tính năng 9)
CREATE INDEX idx_nhat_ky_he_thong_thoi_gian ON nhat_ky_he_thong (thoi_gian_ghi DESC, ma_log DESC);
CREATE INDEX idx_nhat_ky_he_thong_loai_thoi_gian ON nhat_ky_he_thong (loai_thao_tac, thoi_gian_ghi);

-- Chỉ thêm (AC-9). TRUNCATE không bị chặn, chỉ dùng trong test.
CREATE FUNCTION chan_sua_xoa_nhat_ky() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Nhật ký hệ thống chỉ được thêm, không được sửa hay xóa';
END;
$$;

CREATE TRIGGER trg_nhat_ky_he_thong_chi_them
    BEFORE UPDATE OR DELETE ON nhat_ky_he_thong
    FOR EACH ROW EXECUTE FUNCTION chan_sua_xoa_nhat_ky();
