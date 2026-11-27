package vn.edu.hust.chungcu.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.repository.CuTruDinhDanhQuery;

/**
 * Định danh cư dân và nhân viên BQL cho phân hệ cư trú (spec 0014, Kiến trúc phân hệ).
 * Chỉ phụ thuộc repository, không phụ thuộc {@link PhamViTinhToan}, nên {@code cutru} gọi
 * vào không tạo vòng bean.
 */
@Service
public class CuTruDinhDanhService {

	private static final Pattern CHU_SO = Pattern.compile("^[0-9]+$");

	private static final Pattern BO_SDT = Pattern.compile("[\\s.\\-]");

	private static final Pattern BO_CCCD = Pattern.compile("[\\s\\-]");

	private final CuTruDinhDanhQuery query;

	public CuTruDinhDanhService(CuTruDinhDanhQuery query) {
		this.query = query;
	}

	/** Mã {@code BQL...} của người dùng; rỗng khi đã bị gỡ vai trò BQL. */
	@Transactional(readOnly = true)
	public Optional<String> maNvbqlCua(String maNguoiDung) {
		return this.query.maNvbqlCua(maNguoiDung);
	}

	/** Mã {@code CD...} của người dùng; rỗng khi không còn dòng {@code cu_dan}. */
	@Transactional(readOnly = true)
	public Optional<String> maCuDanCua(String maNguoiDung) {
		return this.query.maCuDanCua(maNguoiDung);
	}

	/**
	 * Tìm cư dân để chọn khi tạo hồ sơ (AC-3).
	 * @param q đã đổi NFC và cắt khoảng trắng; {@code null} hoặc rỗng là không lọc
	 */
	@Transactional(readOnly = true)
	public List<ThongTinCuDan> timCuDanLuaChon(String q, int gioiHan) {
		return this.query.timLuaChon(tuKhoa(q), gioiHan);
	}

	/**
	 * Cư dân khớp đồng thời số CCCD và số điện thoại đã chuẩn hóa, mọi trạng thái tài
	 * khoản; nơi gọi tự chặn tài khoản ngừng hoạt động (spec 0015, AC-3).
	 */
	@Transactional(readOnly = true)
	public Optional<ThongTinCuDan> timTheoCccdVaSdt(String soCccd, String soDienThoai) {
		return this.query.theoCccdVaSdt(soCccd, soDienThoai);
	}

	/**
	 * Mã mọi cư dân khớp từ khóa như {@link #timCuDanLuaChon} nhưng không bỏ tài khoản
	 * ngừng hoạt động và không giới hạn số dòng (spec 0015, AC-10).
	 * @param q đã đổi NFC và cắt khoảng trắng; rỗng thì trả tập rỗng
	 */
	@Transactional(readOnly = true)
	public Set<String> maCuDanKhopTuKhoa(String q) {
		if (q == null || q.isEmpty()) {
			return Set.of();
		}
		return this.query.maCuDanKhop(tuKhoa(q));
	}

	/**
	 * Một câu SQL cho cả tập; tập rỗng thì không truy vấn. Mã không có thì vắng khỏi map.
	 */
	@Transactional(readOnly = true)
	public Map<String, ThongTinCuDan> chiTietCuDanTheoMa(Collection<String> maCuDan) {
		if (maCuDan.isEmpty()) {
			return Map.of();
		}
		return this.query.theoMa(maCuDan)
			.stream()
			.collect(Collectors.toUnmodifiableMap(ThongTinCuDan::maCuDan, Function.identity()));
	}

	/** Họ tên hiện tại theo mã nhân viên BQL; tập rỗng thì không truy vấn. */
	@Transactional(readOnly = true)
	public Map<String, String> hoTenNvbqlTheoMa(Collection<String> maNvbql) {
		return maNvbql.isEmpty() ? Map.of() : this.query.hoTenNvbql(maNvbql);
	}

	/**
	 * Số điện thoại: bỏ khoảng trắng, dấu chấm, gạch nối, đổi đầu {@code +84}/{@code 84}
	 * thành {@code 0}. Số CCCD: bỏ khoảng trắng và gạch nối. Mỗi dạng chỉ dùng khi khác
	 * rỗng và toàn chữ số.
	 */
	static CuTruDinhDanhQuery.TuKhoa tuKhoa(String q) {
		if (q == null || q.isEmpty()) {
			return new CuTruDinhDanhQuery.TuKhoa(null, null, null);
		}
		String mau = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
		String sdt = BO_SDT.matcher(q).replaceAll("");
		if (sdt.startsWith("+84")) {
			sdt = "0" + sdt.substring(3);
		}
		else if (sdt.startsWith("84")) {
			sdt = "0" + sdt.substring(2);
		}
		String cccd = BO_CCCD.matcher(q).replaceAll("");
		return new CuTruDinhDanhQuery.TuKhoa(mau, CHU_SO.matcher(sdt).matches() ? sdt : null,
				CHU_SO.matcher(cccd).matches() ? cccd : null);
	}

}
