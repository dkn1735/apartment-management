package vn.edu.hust.chungcu.phuongtien.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của thao tác trên phương tiện (spec 0016, AC-12). Chỉ mã định danh và
 * trường nghiệp vụ xe, không bao giờ có họ tên, số điện thoại, email hay số CCCD.
 */
record ChiTietPhuongTien(String maCanHo, String maCuDanSoHuu, String loaiPhuongTien, String bienSo, TrangThai trangThai,
		@JsonInclude(JsonInclude.Include.NON_NULL) String lyDo) implements ChiTietNhatKy {

	/** Trạng thái trước và sau; {@code cu} là {@code null} khi đăng ký. */
	record TrangThai(String cu, String moi) {
	}

}
