-- Spec 0005 (làm mới phiên và đăng xuất), AC-9.
-- bat_dau_luc: lúc đăng nhập đầu tiên của họ phiên, chép sang mọi dòng xoay vòng, để
-- het_han_luc không bao giờ vượt bat_dau_luc + 30 ngày.
ALTER TABLE phien_dang_nhap ADD COLUMN bat_dau_luc TIMESTAMPTZ;

UPDATE phien_dang_nhap SET bat_dau_luc = tao_luc;

ALTER TABLE phien_dang_nhap ALTER COLUMN bat_dau_luc SET NOT NULL;

ALTER TABLE phien_dang_nhap
    ADD CONSTRAINT chk_phien_dang_nhap_bat_dau CHECK (bat_dau_luc <= tao_luc);

-- Mỗi họ phiên có tối đa một dòng chưa thu hồi: chốt chặn cuối ở DB cho bất biến mà
-- khóa theo họ và transaction của làm mới đã giữ.
CREATE UNIQUE INDEX uq_phien_dang_nhap_ho_con_hieu_luc
    ON phien_dang_nhap (ma_ho_phien) WHERE thu_hoi_luc IS NULL;
