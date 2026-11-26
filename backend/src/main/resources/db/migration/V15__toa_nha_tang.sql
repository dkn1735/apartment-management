-- Tòa nhà và tầng (spec 0011). Không thêm bảng hay cột, chỉ siết ràng buộc để DB tự giữ
-- định dạng mã, giới hạn số tầng và tên tòa không trùng kể cả khác hoa thường.

-- Mã tòa 1 đến 10 chữ cái không dấu hoặc chữ số, không có gạch nối, nên tách mã tầng luôn rõ.
ALTER TABLE toa_nha
    ADD CONSTRAINT chk_toa_nha_ma CHECK (ma_toa_nha ~ '^[A-Z0-9]{1,10}$');

-- Giới hạn 1 đến 99 tầng thay cho chk_toanha_sotang (so_tang > 0) của V2.
ALTER TABLE toa_nha
    DROP CONSTRAINT chk_toanha_sotang;
ALTER TABLE toa_nha
    ADD CONSTRAINT chk_toa_nha_so_tang CHECK (so_tang BETWEEN 1 AND 99);

-- Tên không trùng kể cả khác hoa thường, theo lower() với collation vi-VN-x-icu của cột.
-- Index này chặt hơn và bao trùm UNIQUE viết liền cột ở V2, nên bỏ ràng buộc cũ. Không dùng
-- IF EXISTS: tên sai thì migration báo lỗi rõ ràng.
CREATE UNIQUE INDEX uq_toa_nha_ten_lower ON toa_nha (lower(ten_toa_nha));
ALTER TABLE toa_nha
    DROP CONSTRAINT toa_nha_ten_toa_nha_key;

-- Tầng 1 đến 99, mã do server ghép: <maToaNha>-<số tầng hai chữ số>.
ALTER TABLE tang
    ADD CONSTRAINT chk_tang_so_tang CHECK (so_tang BETWEEN 1 AND 99);
ALTER TABLE tang
    ADD CONSTRAINT chk_tang_ma CHECK (ma_tang = ma_toa_nha || '-' || lpad(so_tang::text, 2, '0'));
