-- Căn hộ (spec 0012). Không thêm bảng hay cột, chỉ siết ràng buộc để DB tự giữ định dạng mã
-- căn và giới hạn diện tích, kể cả khi chèn tay.

-- Mã căn do server ghép: <ma_tang><số căn hai chữ số 01 đến 99>. Vì ma_tang đã theo
-- chk_tang_ma, mã căn luôn chỉ ra đúng tòa, tầng và số căn.
ALTER TABLE can_ho
    ADD CONSTRAINT chk_can_ho_ma CHECK (
        ma_can_ho ~ '^[A-Z0-9]{1,10}-[0-9]{4}$'
        AND left(ma_can_ho, length(ma_tang)) = ma_tang
        AND length(ma_can_ho) = length(ma_tang) + 2
        AND right(ma_can_ho, 2) <> '00');

-- Diện tích lớn hơn 0 và tối đa 1000 m², thay cho chk_canho_dientich (dien_tich > 0) của V2.
-- Không dùng IF EXISTS: tên sai thì migration báo lỗi rõ ràng.
ALTER TABLE can_ho
    DROP CONSTRAINT chk_canho_dientich;
ALTER TABLE can_ho
    ADD CONSTRAINT chk_can_ho_dien_tich CHECK (dien_tich > 0 AND dien_tich <= 1000);
