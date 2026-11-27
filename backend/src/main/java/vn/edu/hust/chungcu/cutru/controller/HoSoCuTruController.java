package vn.edu.hust.chungcu.cutru.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.cutru.dto.BoLocHoSoCuTru;
import vn.edu.hust.chungcu.cutru.dto.CapNhatHoSoCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.CuDanLuaChonResponse;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.dto.ThemHoSoCuTruRequest;
import vn.edu.hust.chungcu.cutru.service.CapNhatHoSoCuTruService;
import vn.edu.hust.chungcu.cutru.service.DanhSachHoSoCuTruService;
import vn.edu.hust.chungcu.cutru.service.ThemHoSoCuTruService;
import vn.edu.hust.chungcu.cutru.service.TimCuDanService;

@Tag(name = "Cư trú")
@RestController
@RequestMapping("/api/v1/residency-records")
public class HoSoCuTruController {

	private final TimCuDanService timCuDan;

	private final ThemHoSoCuTruService themHoSo;

	private final CapNhatHoSoCuTruService capNhatHoSo;

	private final DanhSachHoSoCuTruService danhSach;

	private final PhamViTruyCapService phamViTruyCap;

	public HoSoCuTruController(TimCuDanService timCuDan, ThemHoSoCuTruService themHoSo,
			CapNhatHoSoCuTruService capNhatHoSo, DanhSachHoSoCuTruService danhSach,
			PhamViTruyCapService phamViTruyCap) {
		this.timCuDan = timCuDan;
		this.themHoSo = themHoSo;
		this.capNhatHoSo = capNhatHoSo;
		this.danhSach = danhSach;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Tra cứu hồ sơ cư trú toàn tòa",
			description = "Chỉ hồ sơ của căn thuộc tòa trong phạm vi người gọi, gồm cả đề nghị. Mọi bộ lọc tùy chọn và "
					+ "kết hợp bằng AND; soTang cần maToaNha. q khớp họ tên không dấu, số điện thoại, CCCD, email, mã "
					+ "cư dân, một phần mã căn hoặc mã hồ sơ.")
	@ChoPhep(BAN_QUAN_LY)
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<HoSoCuTruResponse>>> danhSach(
			@Parameter(description = "Mã tòa nhà") @RequestParam(required = false) String maToaNha,
			@Parameter(description = "Số tầng 1 đến 99, cần maToaNha",
					schema = @Schema(type = "integer", minimum = "1", maximum = "99")) @RequestParam(
							required = false) String soTang,
			@Parameter(description = "Trạng thái hồ sơ, lặp lại được",
					array = @ArraySchema(schema = @Schema(type = "string",
							allowableValues = { "Chờ xác minh", "Hợp lệ", "Hết hiệu lực", "Từ chối",
									"Đã hủy" }))) @RequestParam(required = false) List<String> trangThaiHoSo,
			@Parameter(description = "Vai trò cư trú",
					schema = @Schema(type = "string",
							allowableValues = { "Chủ căn hộ", "Khách thuê", "Thành viên gia đình" })) @RequestParam(
									required = false) String vaiTroCuTru,
			@Parameter(description = "Tình trạng cư trú",
					schema = @Schema(type = "string", allowableValues = { "Thường trú", "Tạm trú" })) @RequestParam(
							required = false) String tinhTrangCuTru,
			@Parameter(description = "Từ khóa, tối đa 100 ký tự",
					schema = @Schema(type = "string", maxLength = 100)) @RequestParam(required = false) String q,
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "Trường,chiều; trường là taoLuc, capNhatLuc, ngayBatDau, ngayKetThuc hoặc maCanHo",
					schema = @Schema(type = "string", defaultValue = "taoLuc,desc")) @RequestParam(
							required = false) String sort) {
		BoLocHoSoCuTru boLoc = new BoLocHoSoCuTru(maToaNha, soTang, trangThaiHoSo, vaiTroCuTru, tinhTrangCuTru, q);
		return ApiResponse.ok(this.danhSach.danhSach(boLoc, page, size, sort, this.phamViTruyCap.cuaNguoiDangGoi()),
				"Tra cứu danh sách hồ sơ cư trú thành công");
	}

	@Operation(summary = "Tìm cư dân để tạo hồ sơ cư trú",
			description = "Tối đa 20 cư dân có tài khoản Hoạt động hoặc Tạm khóa, theo họ tên. q khớp họ tên không dấu, "
					+ "số điện thoại, số CCCD, email hoặc mã cư dân.")
	@ChoPhep(BAN_QUAN_LY)
	@GetMapping("/resident-options")
	public ResponseEntity<ApiResponse<List<CuDanLuaChonResponse>>> timCuDan(
			@Parameter(description = "Từ khóa, tối đa 100 ký tự",
					schema = @Schema(type = "string", maxLength = 100)) @RequestParam(required = false) String q) {
		return ApiResponse.ok(this.timCuDan.tim(q), "Tra cứu danh sách cư dân thành công");
	}

	@Operation(summary = "Thêm mới hồ sơ cư trú (API-UC02-02)",
			description = "Hồ sơ tạo ở trạng thái Hợp lệ; căn tự chuyển sang Đang sử dụng khi có người đang cư trú.")
	@ChoPhep(BAN_QUAN_LY)
	@PostMapping
	public ResponseEntity<ApiResponse<HoSoCuTruResponse>> them(@RequestBody ThemHoSoCuTruRequest request) {
		return ApiResponse.created(this.themHoSo.them(this.phamViTruyCap.cuaNguoiDangGoi(), request),
				"Thêm mới hồ sơ cư trú thành công");
	}

	@Operation(summary = "Cập nhật hoặc kết thúc hồ sơ cư trú (API-UC02-03)",
			description = "Không đổi căn hộ hay cư dân. Đặt Hết hiệu lực để kết thúc; hồ sơ đã hết hiệu lực không sửa được.")
	@ChoPhep(BAN_QUAN_LY)
	@PutMapping("/{recordId}")
	public ResponseEntity<ApiResponse<HoSoCuTruResponse>> capNhat(@PathVariable String recordId,
			@RequestBody CapNhatHoSoCuTruRequest request) {
		return ApiResponse.ok(this.capNhatHoSo.capNhat(this.phamViTruyCap.cuaNguoiDangGoi(), recordId, request),
				"Cập nhật hồ sơ cư trú thành công");
	}

}
