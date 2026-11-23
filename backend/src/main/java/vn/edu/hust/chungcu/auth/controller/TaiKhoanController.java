package vn.edu.hust.chungcu.auth.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.QUAN_TRI_VIEN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.CapNhatTaiKhoanRequest;
import vn.edu.hust.chungcu.auth.dto.DatLaiMatKhauResponse;
import vn.edu.hust.chungcu.auth.dto.DoiTrangThaiRequest;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanChiTietResponse;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanTomTat;
import vn.edu.hust.chungcu.auth.dto.TaoTaiKhoanRequest;
import vn.edu.hust.chungcu.auth.dto.TaoTaiKhoanResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTroCapNhatRequest;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.CapNhatTaiKhoanService;
import vn.edu.hust.chungcu.auth.service.DatLaiMatKhauService;
import vn.edu.hust.chungcu.auth.service.MoKhoaDangNhapService;
import vn.edu.hust.chungcu.auth.service.NguoiQuanLy;
import vn.edu.hust.chungcu.auth.service.TaiKhoanTraCuuService;
import vn.edu.hust.chungcu.auth.service.TaoTaiKhoanService;
import vn.edu.hust.chungcu.auth.service.TrangThaiTaiKhoanService;
import vn.edu.hust.chungcu.auth.service.VaiTroTaiKhoanService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;

/**
 * Quản lý tài khoản (spec 0008). Admin quản lý mọi tài khoản; người chỉ có BQL quản lý
 * tài khoản cư dân thuần. Phạm vi chi tiết do service quyết định.
 */
@Tag(name = "Tài khoản")
@RestController
@RequestMapping("/api/v1/accounts")
public class TaiKhoanController {

	private final TaiKhoanTraCuuService traCuu;

	private final TaoTaiKhoanService tao;

	private final CapNhatTaiKhoanService capNhat;

	private final VaiTroTaiKhoanService vaiTro;

	private final TrangThaiTaiKhoanService trangThai;

	private final DatLaiMatKhauService datLai;

	private final MoKhoaDangNhapService moKhoa;

	public TaiKhoanController(TaiKhoanTraCuuService traCuu, TaoTaiKhoanService tao, CapNhatTaiKhoanService capNhat,
			VaiTroTaiKhoanService vaiTro, TrangThaiTaiKhoanService trangThai, DatLaiMatKhauService datLai,
			MoKhoaDangNhapService moKhoa) {
		this.moKhoa = moKhoa;
		this.traCuu = traCuu;
		this.tao = tao;
		this.capNhat = capNhat;
		this.vaiTro = vaiTro;
		this.trangThai = trangThai;
		this.datLai = datLai;
	}

	@Operation(operationId = "traCuuTaiKhoan", summary = "Tra cứu tài khoản",
			description = "Mới tạo trước. q tìm họ tên không dấu, số điện thoại, email, mã. BQL chỉ thấy tài khoản cư dân thuần.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<TaiKhoanTomTat>>> danhSach(Authentication authentication,
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "taoLuc, cộng ,asc hoặc ,desc",
					schema = @Schema(type = "string",
							defaultValue = TaiKhoanTraCuuService.SAP_XEP_MAC_DINH)) @RequestParam(
									required = false) String sort,
			@Parameter(description = "Từ khóa, tối đa 100 ký tự",
					schema = @Schema(type = "string", maxLength = 100)) @RequestParam(required = false) String q,
			@Parameter(description = "Mã một vai trò") @RequestParam(required = false) String vaiTro,
			@Parameter(description = "Hoạt động, Tạm khóa hoặc Ngừng hoạt động") @RequestParam(
					required = false) String trangThai) {
		return ApiResponse.ok(
				this.traCuu.danhSach(NguoiQuanLy.tu(authentication), page, size, sort, q, vaiTro, trangThai),
				"Tra cứu tài khoản thành công");
	}

	@Operation(operationId = "taoTaiKhoan", summary = "Tạo tài khoản",
			description = "Sinh mật khẩu tạm, chỉ hiện một lần; người dùng phải đổi ở lần đăng nhập đầu. BQL chỉ tạo tài khoản cư dân.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@PostMapping
	public ResponseEntity<ApiResponse<TaoTaiKhoanResponse>> tao(Authentication authentication,
			@RequestBody TaoTaiKhoanRequest request) {
		return khongLuu(
				ApiResponse.created(this.tao.tao(NguoiQuanLy.tu(authentication), request), "Tạo tài khoản thành công"));
	}

	@Operation(operationId = "chiTietTaiKhoan", summary = "Chi tiết tài khoản")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@GetMapping("/{maNguoiDung}")
	public ResponseEntity<ApiResponse<TaiKhoanChiTietResponse>> chiTiet(Authentication authentication,
			@PathVariable String maNguoiDung) {
		return ApiResponse.ok(this.traCuu.chiTiet(NguoiQuanLy.tu(authentication), maNguoiDung),
				"Lấy thông tin tài khoản thành công");
	}

	@Operation(operationId = "capNhatTaiKhoan", summary = "Sửa họ tên, số điện thoại, email",
			description = "Gửi lại phienBan nhận ở chi tiết; lệch thì 409 CONCURRENT_MODIFICATION.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@PutMapping("/{maNguoiDung}")
	public ResponseEntity<ApiResponse<TaiKhoanChiTietResponse>> capNhat(Authentication authentication,
			@PathVariable String maNguoiDung, @RequestBody CapNhatTaiKhoanRequest request) {
		return ApiResponse.ok(this.capNhat.capNhat(NguoiQuanLy.tu(authentication), maNguoiDung, request),
				"Cập nhật tài khoản thành công");
	}

	@Operation(operationId = "datVaiTroTaiKhoan", summary = "Thêm hoặc sửa một vai trò",
			description = "Chưa có vai trò thì thêm, đã có thì sửa trường của nó. BQL chỉ sửa được CU_DAN.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@PutMapping("/{maNguoiDung}/roles/{vaiTro}")
	public ResponseEntity<ApiResponse<TaiKhoanChiTietResponse>> datVaiTro(Authentication authentication,
			@PathVariable String maNguoiDung, @Parameter(description = "Mã vai trò") @PathVariable String vaiTro,
			@RequestBody VaiTroCapNhatRequest request) {
		return ApiResponse.ok(this.vaiTro.dat(NguoiQuanLy.tu(authentication), maNguoiDung, vaiTro, request),
				"Cập nhật vai trò thành công");
	}

	@Operation(operationId = "goVaiTroTaiKhoan", summary = "Gỡ một vai trò",
			description = "Chỉ Admin. Vai trò cuối hoặc vai trò đang bị dữ liệu khác tham chiếu thì không gỡ được.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@DeleteMapping("/{maNguoiDung}/roles/{vaiTro}")
	public ResponseEntity<ApiResponse<TaiKhoanChiTietResponse>> goVaiTro(Authentication authentication,
			@PathVariable String maNguoiDung, @Parameter(description = "Mã vai trò") @PathVariable String vaiTro,
			@Parameter(description = "phienBan nhận ở chi tiết tài khoản", required = true,
					schema = @Schema(type = "integer", minimum = "0")) @RequestParam(
							required = false) String phienBan) {
		return ApiResponse.ok(this.vaiTro.go(NguoiQuanLy.tu(authentication), maNguoiDung, vaiTro, phienBan),
				"Gỡ vai trò thành công");
	}

	@Operation(operationId = "doiTrangThaiTaiKhoan", summary = "Khóa, ngừng hoạt động hoặc mở lại tài khoản",
			description = "Khóa hay ngừng thì thu hồi mọi phiên; lyDo bắt buộc. Rời Ngừng hoạt động chỉ Admin.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@PutMapping("/{maNguoiDung}/status")
	public ResponseEntity<ApiResponse<TaiKhoanChiTietResponse>> doiTrangThai(Authentication authentication,
			@PathVariable String maNguoiDung, @RequestBody DoiTrangThaiRequest request) {
		return ApiResponse.ok(this.trangThai.doi(NguoiQuanLy.tu(authentication), maNguoiDung, request),
				"Cập nhật trạng thái thành công");
	}

	@Operation(operationId = "datLaiMatKhauTaiKhoan", summary = "Đặt lại mật khẩu",
			description = "Sinh mật khẩu tạm, chỉ hiện một lần, thu hồi mọi phiên; người dùng phải đổi ở lần đăng nhập tới.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@PostMapping("/{maNguoiDung}/reset-password")
	public ResponseEntity<ApiResponse<DatLaiMatKhauResponse>> datLaiMatKhau(Authentication authentication,
			@PathVariable String maNguoiDung) {
		return khongLuu(ApiResponse.ok(this.datLai.datLai(NguoiQuanLy.tu(authentication), maNguoiDung),
				"Đặt lại mật khẩu thành công"));
	}

	@Operation(operationId = "moKhoaDangNhapTaiKhoan", summary = "Mở khóa đăng nhập sớm",
			description = "Xóa khóa tạm do nhập sai mật khẩu nhiều lần và đặt bộ đếm về 0. Không đổi trạng thái "
					+ "tài khoản, không thu hồi phiên. Không đang khóa thì vẫn trả 200.")
	@ChoPhep({ QUAN_TRI_VIEN, BAN_QUAN_LY })
	@DeleteMapping("/{maNguoiDung}/login-lock")
	public ResponseEntity<ApiResponse<TaiKhoanChiTietResponse>> moKhoaDangNhap(Authentication authentication,
			@PathVariable String maNguoiDung) {
		return ApiResponse.ok(this.moKhoa.moKhoa(NguoiQuanLy.tu(authentication), maNguoiDung),
				"Mở khóa đăng nhập thành công");
	}

	/** Response có mật khẩu tạm không được lưu ở bất kỳ cache nào (AC-1, AC-12). */
	static <T> ResponseEntity<T> khongLuu(ResponseEntity<T> response) {
		return ResponseEntity.status(response.getStatusCode())
			.headers(response.getHeaders())
			.cacheControl(CacheControl.noStore())
			.body(response.getBody());
	}

}
