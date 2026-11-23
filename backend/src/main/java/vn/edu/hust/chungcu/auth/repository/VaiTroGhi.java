package vn.edu.hust.chungcu.auth.repository;

import java.util.Map;
import vn.edu.hust.chungcu.auth.dto.VaiTro;

/**
 * Một vai trò đã chuẩn hóa và kiểm, sẵn sàng ghi vào bảng con (spec 0008, AC-1). Có đúng
 * các trường của {@link BangVaiTro#truong()}.
 */
public record VaiTroGhi(VaiTro vaiTro, Map<TruongVaiTro, String> giaTri) {

	public VaiTroGhi {
		giaTri = Map.copyOf(giaTri);
	}

	@Override
	public String toString() {
		return "VaiTroGhi[vaiTro=" + this.vaiTro + ", ***]";
	}

}
