package vn.edu.hust.chungcu.cutru.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.CuDanLuaChonResponse;

/**
 * Tìm cư dân có tài khoản để BQL chọn khi tạo hồ sơ cư trú (spec 0014, AC-3). Tìm trên
 * toàn hệ thống vì người chưa có hồ sơ chưa thuộc tòa nào.
 */
@Service
public class TimCuDanService {

	static final int GIOI_HAN = 20;

	private final CuTruDinhDanhService dinhDanh;

	public TimCuDanService(CuTruDinhDanhService dinhDanh) {
		this.dinhDanh = dinhDanh;
	}

	@Transactional(readOnly = true)
	public List<CuDanLuaChonResponse> tim(String q) {
		String tuKhoa = NhapLieuCuTru.tuKhoa(q);
		String loi = NhapLieuCuTru.doDaiTuKhoa(tuKhoa);
		if (loi != null) {
			throw CuTruLoi.duLieuKhongHopLe(List.of(new FieldError("q", loi)));
		}
		return this.dinhDanh.timCuDanLuaChon(tuKhoa, GIOI_HAN)
			.stream()
			.map(c -> new CuDanLuaChonResponse(c.maCuDan(), c.maNguoiDung(), c.hoTen(), c.soDienThoai(), c.email(),
					c.soCccd(), c.trangThaiTaiKhoan()))
			.toList();
	}

}
