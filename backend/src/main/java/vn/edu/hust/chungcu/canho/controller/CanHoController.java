package vn.edu.hust.chungcu.canho.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KE_TOAN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.canho.dto.BoLocCanHo;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.DoiTienIchRequest;
import vn.edu.hust.chungcu.canho.dto.SuaCanHoRequest;
import vn.edu.hust.chungcu.canho.dto.ThemCanHoRequest;
import vn.edu.hust.chungcu.canho.service.SuaCanHoService;
import vn.edu.hust.chungcu.canho.service.TamDungTienIchService;
import vn.edu.hust.chungcu.canho.service.ThemCanHoService;
import vn.edu.hust.chungcu.canho.service.TimCanHoService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;

@Tag(name = "Căn hộ")
@RestController
@RequestMapping("/api/v1/apartments")
public class CanHoController {

	private final TimCanHoService timCanHo;

	private final ThemCanHoService themCanHo;

	private final SuaCanHoService suaCanHo;

	private final TamDungTienIchService tamDungTienIch;

	private final PhamViTruyCapService phamViTruyCap;

	public CanHoController(TimCanHoService timCanHo, ThemCanHoService themCanHo, SuaCanHoService suaCanHo,
			TamDungTienIchService tamDungTienIch, PhamViTruyCapService phamViTruyCap) {
		this.timCanHo = timCanHo;
		this.themCanHo = themCanHo;
		this.suaCanHo = suaCanHo;
		this.tamDungTienIch = tamDungTienIch;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Tìm kiếm và lọc danh sách căn hộ (API-UC01-05)",
			description = "Chỉ trả căn thuộc tòa trong phạm vi người gọi. Mọi bộ lọc tùy chọn và kết hợp bằng AND; soTang chỉ dùng được khi có maToaNha.")
	@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN })
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<CanHoResponse>>> timKiem(
			@Parameter(description = "Mã tòa nhà") @RequestParam(required = false) String maToaNha,
			@Parameter(description = "Số tầng 1 đến 99, cần maToaNha",
					schema = @Schema(type = "integer", minimum = "1", maximum = "99")) @RequestParam(
							required = false) String soTang,
			@Parameter(description = "Diện tích từ (m², gồm cả đầu này)",
					schema = @Schema(type = "number", minimum = "0", maximum = "1000")) @RequestParam(
							required = false) String dienTichTu,
			@Parameter(description = "Diện tích đến (m², gồm cả đầu này)",
					schema = @Schema(type = "number", minimum = "0", maximum = "1000")) @RequestParam(
							required = false) String dienTichDen,
			@Parameter(description = "Trạng thái sử dụng, lặp lại được",
					array = @ArraySchema(schema = @Schema(type = "string",
							allowableValues = { "Trống", "Đang sử dụng", "Đang sửa chữa",
									"Đã bàn giao" }))) @RequestParam(required = false) List<String> trangThaiSuDung,
			@Parameter(description = "Một phần mã căn, tối đa 20 ký tự",
					schema = @Schema(type = "string", maxLength = 20)) @RequestParam(required = false) String q,
			@Parameter(description = "true chỉ căn đang bị tạm dừng tiện ích, false chỉ căn bình thường",
					schema = @Schema(type = "boolean")) @RequestParam(required = false) String biTamDungTienIch,
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "maCanHo, dienTich hoặc trangThaiSuDung, cộng ,asc hoặc ,desc",
					schema = @Schema(type = "string", defaultValue = TimCanHoService.SAP_XEP_MAC_DINH)) @RequestParam(
							required = false) String sort) {
		BoLocCanHo boLoc = new BoLocCanHo(maToaNha, soTang, dienTichTu, dienTichDen, trangThaiSuDung, q,
				biTamDungTienIch);
		return ApiResponse.ok(this.timCanHo.timKiem(boLoc, page, size, sort, this.phamViTruyCap.cuaNguoiDangGoi()),
				"Tìm kiếm và lọc danh sách căn hộ thành công");
	}

	@Operation(summary = "Xem chi tiết căn hộ (API-UC01-06)",
			description = "Nhân viên xem căn thuộc tòa trong phạm vi; cư dân chỉ xem căn đang cư trú. Ngoài phạm vi trả 404.")
	@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, CU_DAN })
	@GetMapping("/{apartmentId}")
	public ResponseEntity<ApiResponse<CanHoResponse>> chiTiet(@PathVariable String apartmentId) {
		return ApiResponse.ok(this.timCanHo.chiTiet(apartmentId, this.phamViTruyCap.cuaNguoiDangGoi()),
				"Lấy chi tiết căn hộ thành công");
	}

	@Operation(summary = "Tạo căn hộ (API-UC01-07)",
			description = "Mã căn do server ghép từ maTang và soCan (A-01 và 5 thành A-0105). Mã đã có trả 409.")
	@ChoPhep(BAN_QUAN_LY)
	@PostMapping
	public ResponseEntity<ApiResponse<CanHoResponse>> them(@RequestBody ThemCanHoRequest request) {
		return ApiResponse.created(this.themCanHo.them(this.phamViTruyCap.cuaNguoiDangGoi(), request),
				"Tạo mới căn hộ thành công");
	}

	@Operation(summary = "Cập nhật thông tin căn hộ (API-UC01-08)",
			description = "Chỉ đổi diện tích và trạng thái sử dụng. Lần ghi sau thắng; nhật ký ghi giá trị cũ và mới.")
	@ChoPhep(BAN_QUAN_LY)
	@PutMapping("/{apartmentId}")
	public ResponseEntity<ApiResponse<CanHoResponse>> sua(@PathVariable String apartmentId,
			@RequestBody SuaCanHoRequest request) {
		return ApiResponse.ok(this.suaCanHo.sua(this.phamViTruyCap.cuaNguoiDangGoi(), apartmentId, request),
				"Cập nhật thông tin căn hộ thành công");
	}

	@Operation(summary = "Tạm dừng hoặc mở khóa quyền tiện ích căn hộ (API-UC01-09)",
			description = "Lý do bắt buộc. Yêu cầu trùng trạng thái hiện có trả 409; mỗi lần đổi ghi một dòng nhật ký.")
	@ChoPhep({ BAN_QUAN_LY, KE_TOAN })
	@PatchMapping("/{apartmentId}/facility-suspension")
	public ResponseEntity<ApiResponse<CanHoResponse>> doiTienIch(@PathVariable String apartmentId,
			@RequestBody DoiTienIchRequest request) {
		CanHoResponse canHo = this.tamDungTienIch.doi(this.phamViTruyCap.cuaNguoiDangGoi(), apartmentId, request);
		return ApiResponse.ok(canHo, canHo.biTamDungTienIch() ? "Tạm dừng quyền tiện ích căn hộ thành công"
				: "Mở khóa quyền tiện ích căn hộ thành công");
	}

}
