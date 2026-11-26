-- Tạm dừng quyền tiện ích căn hộ (spec 0013). Lưu lần đổi gần nhất ngay trên can_ho; lịch sử
-- đầy đủ ở nhật ký kiểm toán. Ràng buộc để DB tự chặn dữ liệu sai, kể cả khi sửa tay.

-- Trước spec này không có đường ghi cờ nào, nên dòng có cờ bật chỉ có thể do sửa tay trên DB
-- dev và không có lý do để thỏa CHECK mới (AC-10).
UPDATE can_ho SET bi_tam_dung_tien_ich = FALSE WHERE bi_tam_dung_tien_ich;

ALTER TABLE can_ho
    ADD COLUMN tien_ich_ly_do VARCHAR(500) COLLATE "vi-VN-x-icu" NULL,
    ADD COLUMN tien_ich_doi_luc TIMESTAMPTZ NULL,
    ADD COLUMN tien_ich_doi_boi VARCHAR(30) NULL,
    ADD CONSTRAINT fk_can_ho_tien_ich_doi_boi FOREIGN KEY (tien_ich_doi_boi)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_can_ho_tien_ich_dong_bo CHECK (
        (tien_ich_ly_do IS NULL) = (tien_ich_doi_luc IS NULL)
        AND (tien_ich_doi_luc IS NULL) = (tien_ich_doi_boi IS NULL)),
    ADD CONSTRAINT chk_can_ho_tien_ich_tam_dung CHECK (
        NOT bi_tam_dung_tien_ich OR tien_ich_doi_luc IS NOT NULL),
    ADD CONSTRAINT chk_can_ho_tien_ich_ly_do CHECK (
        tien_ich_ly_do IS NULL OR (btrim(tien_ich_ly_do) <> '' AND char_length(tien_ich_ly_do) <= 500));

CREATE INDEX idx_can_ho_tien_ich_doi_boi ON can_ho (tien_ich_doi_boi);
CREATE INDEX idx_can_ho_tam_dung_tien_ich ON can_ho (ma_can_ho) WHERE bi_tam_dung_tien_ich;
