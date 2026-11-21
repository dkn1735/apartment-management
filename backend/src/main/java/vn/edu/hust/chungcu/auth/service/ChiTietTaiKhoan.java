package vn.edu.hust.chungcu.auth.service;

import java.util.List;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của các thao tác quản lý tài khoản (spec 0008, AC-16). Chỉ có tên
 * trường và mã vai trò, không bao giờ có giá trị số điện thoại, email, số CCCD hay mật
 * khẩu tạm.
 */
final class ChiTietTaiKhoan {

	private ChiTietTaiKhoan() {
	}

	/** {@code AUTH_TAO_TAI_KHOAN}. */
	record Tao(List<VaiTro> vaiTro) implements ChiTietNhatKy {
	}

	/** {@code AUTH_CAP_NHAT_TAI_KHOAN}. */
	record CapNhat(List<String> truongThayDoi) implements ChiTietNhatKy {
	}

	/** {@code AUTH_GAN_VAI_TRO} và {@code AUTH_GO_VAI_TRO}. */
	record VaiTroDon(VaiTro vaiTro) implements ChiTietNhatKy {
	}

	/** {@code AUTH_CAP_NHAT_VAI_TRO}. */
	record CapNhatVaiTro(VaiTro vaiTro, List<String> truongThayDoi) implements ChiTietNhatKy {
	}

	/** {@code AUTH_KHOA_TAI_KHOAN}. */
	record Khoa(String trangThaiCu, String trangThaiMoi, String lyDo, int soPhienThuHoi) implements ChiTietNhatKy {
	}

	/** {@code AUTH_MO_KHOA_TAI_KHOAN}; {@code lyDo} có thể {@code null}. */
	record MoKhoa(String trangThaiCu, String trangThaiMoi, String lyDo) implements ChiTietNhatKy {
	}

	/** {@code AUTH_DAT_LAI_MAT_KHAU}. */
	record DatLai(int soPhienThuHoi) implements ChiTietNhatKy {
	}

}
