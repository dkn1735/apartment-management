package vn.edu.hust.chungcu.auth.service;

import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của đặt lại mật khẩu qua email (spec 0010, AC-11): số họ phiên đã bị
 * thu hồi. Không bao giờ chứa token, link hay email.
 */
public record ChiTietDatLaiQuaEmail(int soPhienThuHoi) implements ChiTietNhatKy {

}
