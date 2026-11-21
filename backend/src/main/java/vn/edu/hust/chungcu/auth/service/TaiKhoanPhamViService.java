package vn.edu.hust.chungcu.auth.service;

import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;

/**
 * Nơi duy nhất quyết định phạm vi của người quản lý (spec 0008, AC-15). Admin thấy mọi
 * tài khoản; người chỉ có {@code BAN_QUAN_LY} chỉ thấy tài khoản cư dân thuần, chỉ sửa
 * được trường của {@code CU_DAN}, không gỡ vai trò, không đổi trạng thái tài khoản
 * {@code Ngừng hoạt động}.
 */
@Service
public class TaiKhoanPhamViService {

	static final String KHONG_TIM_THAY = "Không tìm thấy tài khoản";

	/** Danh sách của người này chỉ gồm tài khoản cư dân thuần. */
	public boolean chiCuDanThuan(NguoiQuanLy nguoi) {
		return !nguoi.laAdmin();
	}

	/** Tài khoản có các vai trò này có nằm trong phạm vi của người quản lý không. */
	public boolean trongPhamVi(NguoiQuanLy nguoi, Collection<VaiTro> vaiTroDich) {
		return nguoi.laAdmin() || laCuDanThuan(vaiTroDich);
	}

	/** Ngoài phạm vi thì 404, cùng câu với tài khoản không tồn tại (AC-6). */
	public void kiemThay(NguoiQuanLy nguoi, Collection<VaiTro> vaiTroDich) {
		if (!trongPhamVi(nguoi, vaiTroDich)) {
			throw ApiException.notFound(KHONG_TIM_THAY);
		}
	}

	/** BQL chỉ tạo tài khoản có đúng một vai trò {@code CU_DAN} (AC-15). */
	public void kiemTao(NguoiQuanLy nguoi, Collection<VaiTro> vaiTroMoi) {
		if (!nguoi.laAdmin() && !laCuDanThuan(vaiTroMoi)) {
			throw cam();
		}
	}

	/** BQL chỉ sửa vai trò {@code CU_DAN} (AC-15). */
	public void kiemSuaVaiTro(NguoiQuanLy nguoi, VaiTro vaiTro) {
		if (!nguoi.laAdmin() && vaiTro != VaiTro.CU_DAN) {
			throw cam();
		}
	}

	/** Gỡ vai trò chỉ Admin (AC-15). */
	public void kiemGoVaiTro(NguoiQuanLy nguoi) {
		if (!nguoi.laAdmin()) {
			throw cam();
		}
	}

	/** Đổi trạng thái tài khoản đang {@code Ngừng hoạt động} chỉ Admin (AC-10, AC-15). */
	public void kiemDoiTrangThai(NguoiQuanLy nguoi, String trangThaiHienTai) {
		if (!nguoi.laAdmin() && TrangThaiTaiKhoan.NGUNG_HOAT_DONG.equals(trangThaiHienTai)) {
			throw cam();
		}
	}

	private static boolean laCuDanThuan(Collection<VaiTro> vaiTro) {
		return vaiTro.size() == 1 && List.copyOf(vaiTro).get(0) == VaiTro.CU_DAN;
	}

	private static ApiException cam() {
		return new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
				"Bạn không có quyền thực hiện thao tác này");
	}

}
