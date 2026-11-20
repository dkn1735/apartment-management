package vn.edu.hust.chungcu.nhatky.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.QUAN_TRI_VIEN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTacResponse;
import vn.edu.hust.chungcu.nhatky.dto.NhatKyResponse;
import vn.edu.hust.chungcu.nhatky.service.NhatKyTraCuuService;

/**
 * Tra cứu nhật ký kiểm toán (spec 0007, AC-11 đến AC-14). Không có endpoint sửa hay xóa.
 */
@Tag(name = "Nhật ký hệ thống")
@RestController
@RequestMapping("/api/v1/system/audit-logs")
public class NhatKyController {

	private final NhatKyTraCuuService traCuu;

	public NhatKyController(NhatKyTraCuuService traCuu) {
		this.traCuu = traCuu;
	}

	@Operation(operationId = "traCuuNhatKy", summary = "Tra cứu nhật ký kiểm toán (API-AUTH-05)",
			description = "Mới nhất trước. Bộ lọc kết hợp bằng AND; ngày theo giờ Việt Nam, gồm cả ngày denNgay.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY, BAN_QUAN_TRI })
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<NhatKyResponse>>> danhSach(
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "thoiGianGhi hoặc loaiThaoTac, cộng ,asc hoặc ,desc",
					schema = @Schema(type = "string",
							defaultValue = NhatKyTraCuuService.SAP_XEP_MAC_DINH)) @RequestParam(
									required = false) String sort,
			@Parameter(description = "Mã người thực hiện, dạng ND + 8 chữ số",
					schema = @Schema(type = "string", pattern = "^ND[0-9]{8}$")) @RequestParam(
							required = false) String maNguoiDung,
			@Parameter(description = "Mã một loại thao tác, lấy từ /action-types") @RequestParam(
					required = false) String loaiThaoTac,
			@Parameter(description = "Từ ngày, yyyy-MM-dd",
					schema = @Schema(type = "string", format = "date")) @RequestParam(required = false) String tuNgay,
			@Parameter(description = "Đến ngày (gồm cả ngày này), yyyy-MM-dd",
					schema = @Schema(type = "string", format = "date")) @RequestParam(
							required = false) String denNgay) {
		return ApiResponse.ok(this.traCuu.danhSach(page, size, sort, maNguoiDung, loaiThaoTac, tuNgay, denNgay),
				"Tra cứu nhật ký kiểm toán thành công");
	}

	@Operation(operationId = "danhMucLoaiThaoTac", summary = "Danh mục loại thao tác của nhật ký")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY, BAN_QUAN_TRI })
	@GetMapping("/action-types")
	public ResponseEntity<ApiResponse<List<LoaiThaoTacResponse>>> danhMucLoai() {
		return ApiResponse.ok(this.traCuu.danhMucLoai(), "Lấy danh mục loại thao tác thành công");
	}

}
