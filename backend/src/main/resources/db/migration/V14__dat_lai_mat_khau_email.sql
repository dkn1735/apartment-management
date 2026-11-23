-- Quên mật khẩu qua email (spec 0010).

-- Lúc yêu cầu bị một yêu cầu mới hoặc một sự kiện ở AC-10 thay thế. Nullable nên an toàn
-- với dòng đang có (bảng chưa được dùng trước spec này).
ALTER TABLE yeu_cau_dat_lai_mat_khau
    ADD COLUMN huy_luc TIMESTAMPTZ NULL;

-- Đã dùng và đã hủy là hai trạng thái cuối, không thể cùng lúc.
ALTER TABLE yeu_cau_dat_lai_mat_khau
    ADD CONSTRAINT chk_yeu_cau_dat_lai_mat_khau_ket_thuc CHECK (da_dung_luc IS NULL OR huy_luc IS NULL);

-- Mỗi tài khoản có nhiều nhất một yêu cầu mở (AC-5).
CREATE UNIQUE INDEX uq_yeu_cau_dat_lai_mat_khau_mo ON yeu_cau_dat_lai_mat_khau (ma_nguoi_dung)
    WHERE da_dung_luc IS NULL AND huy_luc IS NULL;

-- Đếm 60 giây và 5 lần mỗi giờ theo tài khoản (AC-4). Vẫn bắt đầu bằng cột khóa ngoại
-- nên thay được index cũ.
DROP INDEX idx_yeu_cau_dat_lai_mat_khau_ma_nguoi_dung;
CREATE INDEX idx_yeu_cau_dat_lai_mat_khau_nguoi_dung_tao ON yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, tao_luc DESC);

-- Trần chung mỗi giờ (AC-4) và job dọn (AC-12).
CREATE INDEX idx_yeu_cau_dat_lai_mat_khau_tao_luc ON yeu_cau_dat_lai_mat_khau (tao_luc);

COMMENT ON COLUMN yeu_cau_dat_lai_mat_khau.token_hash IS
    'SHA-256 hex của token trong link. Token gốc chỉ nằm trong email, không bao giờ lưu.';
