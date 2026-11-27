package vn.edu.hust.chungcu.phuongtien.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.cutru.service.CuTruService;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;

/**
 * Dựng response phương tiện của một căn theo góc nhìn (spec 0016, AC-2 đến AC-4). Cư trú,
 * họ tên cư dân và họ tên BQL được đọc một lần cho cả danh sách.
 */
@Component
class HienThiPhuongTien {

	private final CuTruService cuTru;

	private final CuTruDinhDanhService dinhDanh;

	private final Clock clock;

	HienThiPhuongTien(CuTruService cuTru, CuTruDinhDanhService dinhDanh, Clock clock) {
		this.cuTru = cuTru;
		this.dinhDanh = dinhDanh;
		this.clock = clock;
	}

	/** Dữ liệu tra cứu chung của cả danh sách. */
	private record TraCuu(Set<String> dangCuTru, Map<String, ThongTinCuDan> cuDan, Map<String, String> tenBql) {
	}

	/**
	 * @param maCuDanNguoiGoi {@code null} khi người gọi không có vai trò cư dân
	 */
	List<PhuongTienResponse> dung(String maCanHo, List<PhuongTienDong> dong, GocNhin gocNhin, String maCuDanNguoiGoi) {
		if (dong.isEmpty()) {
			return List.of();
		}
		Set<String> maCuDan = dong.stream()
			.flatMap(d -> Stream.of(d.maCuDanSoHuu(), d.maCuDanDangKy()))
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Set<String> maBql = (gocNhin == GocNhin.NHAN_VIEN)
				? dong.stream().map(PhuongTienDong::maNvbqlCapNhat).filter(Objects::nonNull).collect(Collectors.toSet())
				: Set.of();
		TraCuu traCuu = new TraCuu(Set.copyOf(this.cuTru.maCuDanDangCuTru(maCanHo, LocalDate.now(this.clock))),
				this.dinhDanh.chiTietCuDanTheoMa(maCuDan), this.dinhDanh.hoTenNvbqlTheoMa(maBql));
		return dong.stream().map(d -> mot(d, gocNhin, maCuDanNguoiGoi, traCuu)).toList();
	}

	private static PhuongTienResponse mot(PhuongTienDong d, GocNhin gocNhin, String toi, TraCuu traCuu) {
		boolean nhanVien = gocNhin == GocNhin.NHAN_VIEN;
		boolean laNguoiGui = toi != null && toi.equals(d.maCuDanDangKy());
		boolean lienQuan = laNguoiGui || (toi != null && toi.equals(d.maCuDanSoHuu()));
		String maBql = nhanVien ? d.maNvbqlCapNhat() : null;
		return new PhuongTienResponse(d.maPhuongTien(), d.maCanHo(), d.loaiPhuongTien(), d.bienSo(), d.nhanHieuMauSac(),
				d.trangThaiDangKy(), d.maCuDanSoHuu(), hoTen(traCuu, d.maCuDanSoHuu()),
				traCuu.dangCuTru().contains(d.maCuDanSoHuu()), d.maCuDanDangKy(), hoTen(traCuu, d.maCuDanDangKy()),
				(nhanVien || lienQuan) ? d.lyDoTrangThai() : null, maBql,
				(maBql != null) ? traCuu.tenBql().get(maBql) : null, d.taoLuc(), d.capNhatLuc(),
				laNguoiGui && GiaTriPhuongTien.CHO_DUYET.equals(d.trangThaiDangKy()));
	}

	private static String hoTen(TraCuu traCuu, String maCuDan) {
		ThongTinCuDan cuDan = (maCuDan != null) ? traCuu.cuDan().get(maCuDan) : null;
		return (cuDan != null) ? cuDan.hoTen() : null;
	}

}
