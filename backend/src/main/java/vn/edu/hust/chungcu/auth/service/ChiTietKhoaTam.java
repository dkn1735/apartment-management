package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của khóa tạm và mở khóa sớm (spec 0009, AC-11): thời điểm khóa hết hạn
 * (khi khóa) hoặc giá trị cũ (khi mở). Không bao giờ chứa định danh hay hash của nó.
 */
public record ChiTietKhoaTam(Instant khoaTamDen) implements ChiTietNhatKy {
}
