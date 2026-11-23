package vn.edu.hust.chungcu.auth.repository;

import java.time.Instant;

/**
 * Kết quả của câu giữ lượt (spec 0009, mục Đọc kết quả).
 *
 * @param khoaCu {@code khoa_tam_den} trước lượt này
 * @param vuaKhoa {@code null} khi đang khóa (không cập nhật gì), {@code true} khi lượt
 * này đặt khóa, {@code false} khi là lượt thường
 */
public record KetQuaGiuLuot(Instant khoaCu, Boolean vuaKhoa) {
}
