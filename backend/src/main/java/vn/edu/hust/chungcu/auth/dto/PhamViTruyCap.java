package vn.edu.hust.chungcu.auth.dto;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import vn.edu.hust.chungcu.common.error.ApiException;

/**
 * Phạm vi dữ liệu của người đang gọi (spec 0006, AC-6). Dữ liệu ngoài phạm vi trả cùng
 * 404 như dữ liệu không tồn tại.
 *
 * @param maNguoiDung claim {@code sub}
 * @param vaiTro vai trò trong access token
 * @param maToaNha tòa nhà được xem, tăng dần
 * @param maCanHo căn hộ đang cư trú hôm nay, tăng dần
 */
public record PhamViTruyCap(String maNguoiDung, Set<VaiTro> vaiTro, List<String> maToaNha, List<String> maCanHo) {

	public static final String KHONG_TIM_THAY = "Không tìm thấy dữ liệu yêu cầu";

	public PhamViTruyCap {
		// EnumSet giữ thứ tự khai báo của VaiTro
		vaiTro = Collections.unmodifiableSet(vaiTro.isEmpty() ? EnumSet.noneOf(VaiTro.class) : EnumSet.copyOf(vaiTro));
		maToaNha = List.copyOf(maToaNha);
		maCanHo = List.copyOf(maCanHo);
	}

	public boolean coVaiTro(VaiTro value) {
		return this.vaiTro.contains(value);
	}

	/** Ném 404 {@code NOT_FOUND} khi tòa nhà nằm ngoài phạm vi. */
	public void yeuCauToaNha(String ma) {
		if (!this.maToaNha.contains(ma)) {
			throw ApiException.notFound(KHONG_TIM_THAY);
		}
	}

	/** Ném 404 {@code NOT_FOUND} khi căn hộ nằm ngoài phạm vi. */
	public void yeuCauCanHo(String ma) {
		if (!this.maCanHo.contains(ma)) {
			throw ApiException.notFound(KHONG_TIM_THAY);
		}
	}

}
