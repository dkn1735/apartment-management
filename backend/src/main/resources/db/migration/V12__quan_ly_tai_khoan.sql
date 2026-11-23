-- Quản lý tài khoản (spec 0008, AC-18).
-- Cờ bắt đổi mật khẩu tạm ở lần đăng nhập đầu; true khi tạo tài khoản và khi đặt lại mật khẩu.
ALTER TABLE nguoi_dung_he_thong
    ADD COLUMN phai_doi_mat_khau BOOLEAN NOT NULL DEFAULT false;

-- Tìm họ tên không dấu ở danh sách tài khoản (AC-5); có sẵn trong PostgreSQL 18.
CREATE EXTENSION IF NOT EXISTS unaccent;
