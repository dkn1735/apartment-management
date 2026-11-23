package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanChiTietResponse;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanTomTat;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroTaiKhoan;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery.BoLocTaiKhoan;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery.DongTaiKhoan;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Danh sách và chi tiết tài khoản (spec 0008, AC-5, AC-6). Chỉ đọc; phạm vi theo
 * {@link TaiKhoanPhamViService}.
 */
@Service
public class TaiKhoanTraCuuService {

	public static final Set<String> TRUONG_SAP_XEP = Set.of("taoLuc");

	public static final String SAP_XEP_MAC_DINH = "taoLuc,desc";

	static final int Q_TOI_DA = 100;

	private static final Pattern MA_NGUOI_DUNG = Pattern.compile("^ND[0-9]{8}$");

	private static final Pattern CHI_SO = Pattern.compile("^[0-9]+$");

	private final TaiKhoanQuery query;

	private final TaiKhoanPhamViService phamVi;

	private final Clock clock;

	public TaiKhoanTraCuuService(TaiKhoanQuery query, TaiKhoanPhamViService phamVi, Clock clock) {
		this.query = query;
		this.phamVi = phamVi;
		this.clock = clock;
	}

	/**
	 * Một trang tài khoản trong phạm vi người quản lý. Tham số sai thì 400, một lỗi mỗi
	 * tham số theo thứ tự {@code page}, {@code size}, {@code sort}, {@code q},
	 * {@code vaiTro}, {@code trangThai}. Tối đa ba truy vấn.
	 */
	@Transactional(readOnly = true)
	public PageResponse<TaiKhoanTomTat> danhSach(NguoiQuanLy nguoi, String page, String size, String sort, String q,
			String vaiTro, String trangThai) {
		List<FieldError> loi = new ArrayList<>();
		TrangYeuCau trang = TrangYeuCau.tu(page, size, sort, TRUONG_SAP_XEP, SAP_XEP_MAC_DINH, loi);
		String tuKhoa = (q != null) ? q.strip() : "";
		if (tuKhoa.length() > Q_TOI_DA) {
			loi.add(new FieldError("q", "Từ khóa tối đa 100 ký tự"));
		}
		VaiTro locVaiTro = null;
		if (vaiTro != null && !vaiTro.isEmpty()) {
			locVaiTro = TaiKhoanHopLe.docVaiTro(vaiTro);
			if (locVaiTro == null) {
				loi.add(new FieldError("vaiTro", "Vai trò không tồn tại"));
			}
		}
		String locTrangThai = (trangThai != null && !trangThai.isEmpty()) ? trangThai : null;
		if (locTrangThai != null && !TrangThaiTaiKhoan.TAT_CA.contains(locTrangThai)) {
			loi.add(new FieldError("trangThai", "Trạng thái không tồn tại"));
		}
		if (!loi.isEmpty()) {
			throw TaiKhoanLoi.duLieuKhongHopLe(loi);
		}
		String qSo = null;
		if (!tuKhoa.isEmpty()) {
			String chuanHoa = SoDienThoaiChuanHoa.boPhanCach(tuKhoa);
			qSo = CHI_SO.matcher(chuanHoa).matches() ? chuanHoa : null;
		}
		BoLocTaiKhoan boLoc = new BoLocTaiKhoan(this.phamVi.chiCuDanThuan(nguoi), tuKhoa.isEmpty() ? null : tuKhoa, qSo,
				locVaiTro, locTrangThai);
		long tong = this.query.dem(boLoc);
		List<DongTaiKhoan> dong = (trang.offset() < tong) ? this.query.danhSach(boLoc, trang) : List.of();
		Map<String, List<VaiTroTaiKhoan>> vaiTroTheoMa = this.query
			.vaiTroCua(dong.stream().map(DongTaiKhoan::maNguoiDung).toList());
		List<TaiKhoanTomTat> items = dong.stream()
			.map((d) -> new TaiKhoanTomTat(d.maNguoiDung(), d.hoTen(), d.soDienThoai(), d.email(),
					d.trangThaiTaiKhoan(), d.phaiDoiMatKhau(),
					vaiTroTheoMa.get(d.maNguoiDung()).stream().map(VaiTroTaiKhoan::vaiTro).toList(),
					d.dangNhapCuoiLuc(), d.taoLuc()))
			.toList();
		return PageResponse.of(items, trang, tong);
	}

	/** Chi tiết trong phạm vi; sai dạng, không có, hay ngoài phạm vi đều 404 (AC-6). */
	@Transactional(readOnly = true)
	public TaiKhoanChiTietResponse chiTiet(NguoiQuanLy nguoi, String maNguoiDung) {
		TaiKhoanChiTietResponse chiTiet = timChiTiet(maNguoiDung);
		this.phamVi.kiemThay(nguoi, chiTiet.vaiTro().stream().map(VaiTroTaiKhoan::vaiTro).toList());
		return chiTiet;
	}

	/**
	 * Chi tiết không xét phạm vi, cho service ghi đọc lại sau khi ghi; không có thì 404.
	 */
	public TaiKhoanChiTietResponse timChiTiet(String maNguoiDung) {
		if (maNguoiDung == null || !MA_NGUOI_DUNG.matcher(maNguoiDung).matches()) {
			throw ApiException.notFound(TaiKhoanPhamViService.KHONG_TIM_THAY);
		}
		TaiKhoanQuery.DongChiTiet chiTiet = this.query.tim(maNguoiDung)
			.orElseThrow(() -> ApiException.notFound(TaiKhoanPhamViService.KHONG_TIM_THAY));
		DongTaiKhoan d = chiTiet.dong();
		List<VaiTroTaiKhoan> vaiTro = this.query.vaiTroCua(List.of(maNguoiDung)).get(maNguoiDung);
		// Khóa đã tới hạn thì coi như không khóa (spec 0009, AC-9)
		Instant khoaTamDen = chiTiet.khoaTamDen();
		if (khoaTamDen != null && !khoaTamDen.isAfter(Instant.now(this.clock))) {
			khoaTamDen = null;
		}
		return new TaiKhoanChiTietResponse(d.maNguoiDung(), d.hoTen(), d.soDienThoai(), d.email(),
				d.trangThaiTaiKhoan(), d.phaiDoiMatKhau(), d.dangNhapCuoiLuc(), d.doiMatKhauLuc(), d.taoLuc(),
				d.capNhatLuc(), d.phienBan(), vaiTro, khoaTamDen);
	}

}
