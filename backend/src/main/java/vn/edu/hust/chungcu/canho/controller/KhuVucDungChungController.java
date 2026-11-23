package vn.edu.hust.chungcu.canho.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KY_THUAT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.canho.dto.KhuVucDungChungResponse;
import vn.edu.hust.chungcu.canho.service.KhuVucDungChungService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;

@Tag(name = "Căn hộ")
@RestController
@RequestMapping("/api/v1/common-areas")
public class KhuVucDungChungController {

	private final KhuVucDungChungService khuVuc;

	private final PhamViTruyCapService phamViTruyCap;

	public KhuVucDungChungController(KhuVucDungChungService khuVuc, PhamViTruyCapService phamViTruyCap) {
		this.khuVuc = khuVuc;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Danh mục khu vực dùng chung (API-UC01-10)",
			description = "Chỉ khu vực thuộc tòa trong phạm vi người gọi. q tìm chứa trong tên, không phân biệt hoa thường, không bỏ dấu.")
	@ChoPhep({ BAN_QUAN_LY, KY_THUAT })
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<KhuVucDungChungResponse>>> danhSach(
			@Parameter(description = "Mã tòa nhà; bỏ trống là mọi tòa trong phạm vi") @RequestParam(
					required = false) String maToaNha,
			@Parameter(description = "Từ khóa trong tên khu vực, tối đa 100 ký tự",
					schema = @Schema(type = "string", maxLength = 100)) @RequestParam(required = false) String q,
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "tenKhuVuc, maKhuVuc hoặc tenToaNha, cộng ,asc hoặc ,desc",
					schema = @Schema(type = "string",
							defaultValue = KhuVucDungChungService.SAP_XEP_MAC_DINH)) @RequestParam(
									required = false) String sort) {
		return ApiResponse.ok(this.khuVuc.danhSach(this.phamViTruyCap.cuaNguoiDangGoi(), maToaNha, q, page, size, sort),
				"Lấy danh mục khu vực dùng chung thành công");
	}

}
