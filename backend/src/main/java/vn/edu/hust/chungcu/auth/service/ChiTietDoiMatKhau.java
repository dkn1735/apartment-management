package vn.edu.hust.chungcu.auth.service;

import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của đổi mật khẩu (spec 0007): số họ phiên khác đã bị thu hồi. Không
 * bao giờ chứa mật khẩu cũ hay mới.
 */
public record ChiTietDoiMatKhau(int soPhienThuHoi) implements ChiTietNhatKy {

}
