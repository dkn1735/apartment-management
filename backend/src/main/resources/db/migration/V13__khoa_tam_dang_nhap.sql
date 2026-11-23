-- Khóa tạm khi đăng nhập sai nhiều lần (spec 0009).

-- Lần giữ lượt gần nhất của tài khoản; bộ đếm so_lan_dang_nhap_sai chỉ còn hiệu lực
-- khi giá trị này nằm trong 15 phút gần nhất. Nullable nên an toàn với dòng đang có.
ALTER TABLE nguoi_dung_he_thong
    ADD COLUMN lan_sai_cuoi_luc TIMESTAMPTZ NULL;

-- Bộ đếm cho định danh không có tài khoản (AC-5), để phản hồi khi bị khóa không lộ
-- số điện thoại hay email nào có tài khoản.
CREATE TABLE dang_nhap_sai_dinh_danh (
    dinh_danh_hash VARCHAR(64) PRIMARY KEY,
    so_lan_sai INTEGER DEFAULT 0 NOT NULL,
    khoa_tam_den TIMESTAMPTZ NULL,
    lan_sai_cuoi_luc TIMESTAMPTZ NULL,
    CONSTRAINT chk_dang_nhap_sai_dinh_danh_dinh_danh_hash CHECK (dinh_danh_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_dang_nhap_sai_dinh_danh_so_lan_sai CHECK (so_lan_sai >= 0)
);

COMMENT ON TABLE dang_nhap_sai_dinh_danh IS
    'Chỉ chứa định danh không có tài khoản. Không cột nào chứa định danh gốc: khóa là SHA-256 hex của định danh đã chuẩn hóa.';

-- Cho job dọn (AC-12)
CREATE INDEX idx_dang_nhap_sai_dinh_danh_lan_sai_cuoi_luc ON dang_nhap_sai_dinh_danh (lan_sai_cuoi_luc);
