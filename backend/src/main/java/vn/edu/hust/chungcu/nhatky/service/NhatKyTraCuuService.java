package vn.edu.hust.chungcu.nhatky.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.auth.service.NguoiDungTraCuuService;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.BoLocNhatKy;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTacResponse;
import vn.edu.hust.chungcu.nhatky.dto.NhatKyResponse;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyQuery;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyQuery.DongDoc;

/**
 * Tra cứu nhật ký (API-AUTH-05, spec 0007 AC-11 đến AC-14). Chỉ đọc, và việc đọc không tự
 * ghi nhật ký.
 */
@Service
public class NhatKyTraCuuService {

	public static final Set<String> TRUONG_SAP_XEP = NhatKyQuery.TRUONG_SAP_XEP;

	public static final String SAP_XEP_MAC_DINH = "thoiGianGhi,desc";

	private static final TypeReference<Map<String, Object>> CHI_TIET = new TypeReference<>() {
	};

	private final NhatKyQuery query;

	private final NguoiDungTraCuuService nguoiDung;

	private final JsonMapper json;

	private final AppProperties properties;

	public NhatKyTraCuuService(NhatKyQuery query, NguoiDungTraCuuService nguoiDung, JsonMapper json,
			AppProperties properties) {
		this.query = query;
		this.nguoiDung = nguoiDung;
		this.json = json;
		this.properties = properties;
	}

	/**
	 * Một trang nhật ký theo bộ lọc. Tham số sai thì 400 {@code VALIDATION_FAILED}, một
	 * lỗi cho mỗi tham số theo thứ tự {@code page}, {@code size}, {@code sort}, rồi bộ
	 * lọc (AC-12). Tối đa ba truy vấn: đếm, lấy trang, lấy họ tên (AC-13).
	 */
	@Transactional(readOnly = true)
	public PageResponse<NhatKyResponse> danhSach(String page, String size, String sort, String maNguoiDung,
			String loaiThaoTac, String tuNgay, String denNgay) {
		List<FieldError> loi = new ArrayList<>();
		TrangYeuCau trang = TrangYeuCau.tu(page, size, sort, TRUONG_SAP_XEP, SAP_XEP_MAC_DINH, loi);
		BoLocNhatKy boLoc = BoLocNhatKy.tu(maNguoiDung, loaiThaoTac, tuNgay, denNgay, this.properties.timezone(), loi);
		if (!loi.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ", loi);
		}
		long tong = this.query.dem(boLoc);
		List<DongDoc> dong = (trang.offset() < tong) ? this.query.danhSach(boLoc, trang) : List.of();
		Map<String, String> hoTen = this.nguoiDung
			.hoTenTheoMa(dong.stream().map(DongDoc::maNguoiDung).filter(Objects::nonNull).collect(Collectors.toSet()));
		return PageResponse.of(dong.stream().map((d) -> sangResponse(d, hoTen)).toList(), trang, tong);
	}

	/** Danh mục loại thao tác theo thứ tự khai báo của enum (AC-14). */
	public List<LoaiThaoTacResponse> danhMucLoai() {
		return Arrays.stream(LoaiThaoTac.values()).map(LoaiThaoTacResponse::tu).toList();
	}

	private NhatKyResponse sangResponse(DongDoc d, Map<String, String> hoTen) {
		return new NhatKyResponse(d.maLog(), d.maNguoiDung(),
				(d.maNguoiDung() != null) ? hoTen.get(d.maNguoiDung()) : null, d.loaiThaoTac(),
				tenThaoTac(d.loaiThaoTac()), d.doiTuongTacDong(), chiTiet(d.chiTietJson()), d.diaChiIp(), d.requestId(),
				d.thoiGianGhi());
	}

	/** Nhãn của enum; mã có trong DB mà không còn trong enum thì chính mã (AC-11). */
	static String tenThaoTac(String ma) {
		for (LoaiThaoTac loai : LoaiThaoTac.values()) {
			if (loai.name().equals(ma)) {
				return loai.nhan();
			}
		}
		return ma;
	}

	private Map<String, Object> chiTiet(String chiTietJson) {
		return (chiTietJson != null) ? this.json.readValue(chiTietJson, CHI_TIET) : null;
	}

}
