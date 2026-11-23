package vn.edu.hust.chungcu.canho.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KY_THUAT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.canho.dto.TangResponse;
import vn.edu.hust.chungcu.canho.dto.ThemTangRequest;
import vn.edu.hust.chungcu.canho.service.TangService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

@Tag(name = "Căn hộ")
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/floors")
public class TangController {

	private final TangService tangService;

	private final PhamViTruyCapService phamViTruyCap;

	public TangController(TangService tangService, PhamViTruyCapService phamViTruyCap) {
		this.tangService = tangService;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Danh sách tầng theo tòa nhà (API-UC01-03)",
			description = "Kèm số căn hộ của mỗi tầng. Tòa ngoài phạm vi trả 404.")
	@ChoPhep({ BAN_QUAN_LY, KY_THUAT })
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<TangResponse>>> danhSach(@PathVariable String buildingId,
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "soTang hoặc maTang, cộng ,asc hoặc ,desc",
					schema = @Schema(type = "string", defaultValue = TangService.SAP_XEP_MAC_DINH)) @RequestParam(
							required = false) String sort) {
		// Tham số (400) trước phạm vi (spec 0006, thứ tự kiểm)
		TrangYeuCau trang = TrangYeuCau.tu(page, size, sort, TangService.TRUONG_SAP_XEP, TangService.SAP_XEP_MAC_DINH);
		String ma = buildingId.toUpperCase(Locale.ROOT);
		this.phamViTruyCap.cuaNguoiDangGoi().yeuCauToaNha(ma);
		return ApiResponse.ok(this.tangService.danhSach(ma, trang), "Lấy danh sách tầng theo tòa nhà thành công");
	}

	@Operation(summary = "Thêm tầng mới vào tòa nhà (API-UC01-04)",
			description = "soTang phải bằng số tầng hiện có cộng một; sai thứ tự hoặc tòa đã đủ 99 tầng trả 422.")
	@ChoPhep(BAN_QUAN_LY)
	@PostMapping
	public ResponseEntity<ApiResponse<TangResponse>> them(@PathVariable String buildingId,
			@RequestBody ThemTangRequest request) {
		return ApiResponse.created(this.tangService.them(this.phamViTruyCap.cuaNguoiDangGoi(),
				buildingId.toUpperCase(Locale.ROOT), request), "Thêm tầng mới vào tòa nhà thành công");
	}

}
