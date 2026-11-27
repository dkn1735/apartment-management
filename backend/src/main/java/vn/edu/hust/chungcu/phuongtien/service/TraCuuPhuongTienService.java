package vn.edu.hust.chungcu.phuongtien.service;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienCanHoResponse;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienQuery;

/** Danh sách phương tiện của một căn (spec 0016, AC-2 đến AC-4). */
@Service
public class TraCuuPhuongTienService {

	static final int LICH_SU_TOI_DA = 50;

	private final CanHoCuTruService canHo;

	private final PhuongTienQuery query;

	private final CuTruDinhDanhService dinhDanh;

	private final HienThiPhuongTien hienThi;

	public TraCuuPhuongTienService(CanHoCuTruService canHo, PhuongTienQuery query, CuTruDinhDanhService dinhDanh,
			HienThiPhuongTien hienThi) {
		this.canHo = canHo;
		this.query = query;
		this.dinhDanh = dinhDanh;
		this.hienThi = hienThi;
	}

	@Transactional(readOnly = true)
	public PhuongTienCanHoResponse xem(String apartmentId, PhamViTruyCap phamVi) {
		String ma = NhapLieuPhuongTien.maTrenDuongDan(apartmentId);
		String maToaNha = this.canHo.maToaNhaCua(ma).orElseThrow(PhuongTienLoi::khongThay);
		GocNhin gocNhin = GocNhin.cua(phamVi, maToaNha, ma).orElseThrow(PhuongTienLoi::khongThay);
		String toi = phamVi.coVaiTro(CU_DAN) ? this.dinhDanh.maCuDanCua(phamVi.maNguoiDung()).orElse(null) : null;
		// Góc nhìn cư dân mà không có mã cư dân thì "" không khớp dòng nào
		String loc = (gocNhin == GocNhin.NHAN_VIEN) ? null : (toi != null) ? toi : "";
		List<PhuongTienDong> dangGui = this.query.dangGui(ma);
		// Dựng một lần cho cả hai mảng để mỗi loại tra cứu chỉ chạy một lần (AC-2, AC-4)
		List<PhuongTienDong> tatCa = Stream
			.of(dangGui, this.query.choDuyet(ma, loc), this.query.lichSu(ma, loc, LICH_SU_TOI_DA))
			.flatMap(List::stream)
			.toList();
		List<PhuongTienResponse> ketQua = this.hienThi.dung(ma, tatCa, gocNhin, toi);
		return new PhuongTienCanHoResponse(ketQua.subList(0, dangGui.size()),
				ketQua.subList(dangGui.size(), ketQua.size()));
	}

}
