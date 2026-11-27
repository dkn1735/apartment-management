package vn.edu.hust.chungcu.cutru.service;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;
import static vn.edu.hust.chungcu.cutru.service.DungHoSoCuTru.CheDo.CU_DAN_DE_NGHI;
import static vn.edu.hust.chungcu.cutru.service.DungHoSoCuTru.CheDo.CU_DAN_NHAN_KHAU;
import static vn.edu.hust.chungcu.cutru.service.DungHoSoCuTru.CheDo.DAY_DU;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruCanHoResponse;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;

/**
 * Nhân khẩu, lịch sử và đề nghị cư trú của một căn (spec 0014 AC-1, AC-2; spec 0015
 * AC-8). Cư dân chỉ thấy đề nghị mình gửi hoặc mình được đề nghị. BQL của tòa thấy đủ; cư
 * dân đang ở căn thấy mọi người đang cư trú nhưng chỉ thấy hồ sơ sắp bắt đầu và lịch sử
 * của chính mình, không kèm dữ liệu cá nhân. Ngoài hai trường hợp đó là 404.
 */
@Service
public class TraCuuHoSoCuTruService {

	private final CanHoCuTruService canHo;

	private final HoSoCuTruQuery query;

	private final CuTruDinhDanhService dinhDanh;

	private final DungHoSoCuTru dungHoSo;

	private final Clock clock;

	public TraCuuHoSoCuTruService(CanHoCuTruService canHo, HoSoCuTruQuery query, CuTruDinhDanhService dinhDanh,
			DungHoSoCuTru dungHoSo, Clock clock) {
		this.canHo = canHo;
		this.query = query;
		this.dinhDanh = dinhDanh;
		this.dungHoSo = dungHoSo;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public HoSoCuTruCanHoResponse xem(String apartmentId, PhamViTruyCap phamVi) {
		String ma = NhapLieuCuTru.maTrenDuongDan(apartmentId);
		String maToaNha = this.canHo.maToaNhaCua(ma).orElseThrow(CuTruLoi::khongThay);
		boolean laBql = phamVi.coVaiTro(BAN_QUAN_LY) && phamVi.maToaNha().contains(maToaNha);
		boolean laCuDan = phamVi.coVaiTro(CU_DAN) && phamVi.maCanHo().contains(ma);
		if (!laBql && !laCuDan) {
			throw CuTruLoi.khongThay();
		}
		LocalDate homNay = LocalDate.now(this.clock);
		PhanLoaiHoSo.Nhom nhom = PhanLoaiHoSo.chia(this.query.theoCanHo(ma), homNay);
		if (laBql) {
			return new HoSoCuTruCanHoResponse(dung(nhom.dangCuTru(), DAY_DU, null),
					dung(nhom.sapBatDau(), DAY_DU, null), dung(nhom.lichSuCuTru(), DAY_DU, null),
					dung(nhom.deNghiCuTru(), DAY_DU, null));
		}
		String cuaToi = this.dinhDanh.maCuDanCua(phamVi.maNguoiDung()).orElse(null);
		return new HoSoCuTruCanHoResponse(dung(nhom.dangCuTru(), CU_DAN_NHAN_KHAU, cuaToi),
				dung(cuaMinh(nhom.sapBatDau(), cuaToi), CU_DAN_NHAN_KHAU, cuaToi),
				dung(cuaMinh(nhom.lichSuCuTru(), cuaToi), CU_DAN_NHAN_KHAU, cuaToi),
				dung(lienQuan(nhom.deNghiCuTru(), cuaToi), CU_DAN_DE_NGHI, cuaToi));
	}

	private List<HoSoCuTruResponse> dung(List<HoSoCuTruDong> dong, DungHoSoCuTru.CheDo cheDo, String cuaToi) {
		return this.dungHoSo.dung(dong, cheDo, cuaToi);
	}

	private static List<HoSoCuTruDong> cuaMinh(List<HoSoCuTruDong> dong, String maCuDan) {
		return dong.stream().filter(d -> d.maCuDan().equals(maCuDan)).toList();
	}

	/** Đề nghị mà người gọi là người gửi hoặc người được đề nghị (spec 0015, AC-8). */
	private static List<HoSoCuTruDong> lienQuan(List<HoSoCuTruDong> dong, String maCuDan) {
		return dong.stream()
			.filter(d -> d.maCuDan().equals(maCuDan) || Objects.equals(d.maCuDanDeNghi(), maCuDan))
			.toList();
	}

}
