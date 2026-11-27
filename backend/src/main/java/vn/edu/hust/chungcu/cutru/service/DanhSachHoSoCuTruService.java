package vn.edu.hust.chungcu.cutru.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.BoLocHoSoCuTru;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruTraCuuQuery;

/**
 * Tra cứu hồ sơ cư trú toàn tòa cho BQL (spec 0015, AC-9, AC-10). Tham số sai (400) trước
 * phạm vi tòa (404); phạm vi hay tập căn rỗng thì trả trang rỗng mà không truy vấn hồ sơ.
 */
@Service
public class DanhSachHoSoCuTruService {

	public static final Set<String> TRUONG_SAP_XEP = HoSoCuTruTraCuuQuery.COT_SAP_XEP.keySet();

	public static final String SAP_XEP_MAC_DINH = "taoLuc,desc";

	private static final Pattern SO_TANG = Pattern.compile("^[1-9][0-9]?$");

	private final CanHoCuTruService canHo;

	private final CuTruDinhDanhService dinhDanh;

	private final HoSoCuTruTraCuuQuery query;

	private final DungHoSoCuTru dungHoSo;

	public DanhSachHoSoCuTruService(CanHoCuTruService canHo, CuTruDinhDanhService dinhDanh, HoSoCuTruTraCuuQuery query,
			DungHoSoCuTru dungHoSo) {
		this.canHo = canHo;
		this.dinhDanh = dinhDanh;
		this.query = query;
		this.dungHoSo = dungHoSo;
	}

	@Transactional(readOnly = true)
	public PageResponse<HoSoCuTruResponse> danhSach(BoLocHoSoCuTru boLoc, String page, String size, String sort,
			PhamViTruyCap phamVi) {
		List<FieldError> loi = new ArrayList<>();
		TrangYeuCau trang = TrangYeuCau.tu(gon(page), gon(size), gon(sort), TRUONG_SAP_XEP, SAP_XEP_MAC_DINH, loi);
		BoLoc loc = kiem(boLoc, loi);
		if (!loi.isEmpty()) {
			throw CuTruLoi.duLieuKhongHopLe(loi);
		}
		if (loc.maToaNha() != null) {
			phamVi.yeuCauToaNha(loc.maToaNha());
		}
		Set<String> maCanHo = this.canHo.maCanHoThuocBoLoc(phamVi.maToaNha(), loc.maToaNha(), loc.soTang());
		if (maCanHo.isEmpty()) {
			return PageResponse.of(List.of(), trang, 0);
		}
		HoSoCuTruTraCuuQuery.DieuKien dk = new HoSoCuTruTraCuuQuery.DieuKien(maCanHo, loc.trangThai(), loc.vaiTro(),
				loc.tinhTrang(), (loc.q() != null) ? this.dinhDanh.maCuDanKhopTuKhoa(loc.q()) : null,
				(loc.q() != null) ? mau(loc.q()) : null);
		long tong = this.query.dem(dk);
		List<HoSoCuTruResponse> items = (trang.offset() < tong)
				? this.dungHoSo.dung(this.query.tim(dk, trang), DungHoSoCuTru.CheDo.DAY_DU, null) : List.of();
		return PageResponse.of(items, trang, tong);
	}

	/**
	 * Lỗi theo thứ tự soTang, trangThaiHoSo, vaiTroCuTru, tinhTrangCuTru, q (AC-10), sau
	 * ba tham số trang.
	 */
	static BoLoc kiem(BoLocHoSoCuTru boLoc, List<FieldError> loi) {
		String toa = gon(boLoc.maToaNha());
		String maToaNha = (toa != null) ? toa.toUpperCase(Locale.ROOT) : null;
		Integer soTang = null;
		String soTangNhap = gon(boLoc.soTang());
		if (soTangNhap != null) {
			if (!SO_TANG.matcher(soTangNhap).matches()) {
				loi.add(new FieldError("soTang", "Số tầng phải là số nguyên từ 1 đến 99"));
			}
			else if (maToaNha == null) {
				loi.add(new FieldError("soTang", "Chọn tòa nhà trước khi lọc theo tầng"));
			}
			else {
				soTang = Integer.valueOf(soTangNhap);
			}
		}
		Set<String> trangThai = new LinkedHashSet<>();
		boolean trangThaiSai = false;
		for (String raw : (boLoc.trangThaiHoSo() != null) ? boLoc.trangThaiHoSo() : List.<String>of()) {
			String gon = gon(raw);
			if (gon == null) {
				continue;
			}
			if (GiaTriCuTru.TRANG_THAI.contains(gon)) {
				trangThai.add(gon);
			}
			else {
				trangThaiSai = true;
			}
		}
		if (trangThaiSai) {
			loi.add(new FieldError("trangThaiHoSo", "Trạng thái hồ sơ không hợp lệ"));
		}
		String vaiTro = mot(boLoc.vaiTroCuTru(), GiaTriCuTru.VAI_TRO, "vaiTroCuTru", "Vai trò cư trú không hợp lệ",
				loi);
		String tinhTrang = mot(boLoc.tinhTrangCuTru(), GiaTriCuTru.TINH_TRANG, "tinhTrangCuTru",
				"Tình trạng cư trú không hợp lệ", loi);
		String q = gon(boLoc.q());
		String loiQ = (q != null) ? NhapLieuCuTru.doDaiTuKhoa(q) : null;
		if (loiQ != null) {
			loi.add(new FieldError("q", loiQ));
		}
		return new BoLoc(maToaNha, soTang, List.copyOf(trangThai), vaiTro, tinhTrang, q);
	}

	/** Mẫu LIKE "chứa"; dấu gạch chéo ngược, phần trăm, gạch dưới được thoát. */
	static String mau(String q) {
		return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}

	private static String mot(String raw, List<String> hopLe, String truong, String cauSai, List<FieldError> loi) {
		String gon = gon(raw);
		if (gon != null && !hopLe.contains(gon)) {
			loi.add(new FieldError(truong, cauSai));
			return null;
		}
		return gon;
	}

	/** NFC rồi cắt khoảng trắng hai đầu; rỗng tính như vắng. */
	private static String gon(String raw) {
		if (raw == null) {
			return null;
		}
		String value = NhapLieuCuTru.nfc(raw);
		return value.isEmpty() ? null : value;
	}

	/** Bộ lọc đã chuẩn hóa và hợp lệ; {@code null} là không lọc. */
	record BoLoc(String maToaNha, Integer soTang, List<String> trangThai, String vaiTro, String tinhTrang, String q) {
	}

}
