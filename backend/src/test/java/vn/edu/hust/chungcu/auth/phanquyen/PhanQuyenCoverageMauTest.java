package vn.edu.hust.chungcu.auth.phanquyen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import vn.edu.hust.chungcu.auth.phanquyen.mau.MauController;
import vn.edu.hust.chungcu.common.security.PublicPaths.DuongCongKhai;

/** {@link PhanQuyenCoverage} báo đúng từng kiểu khai sai (spec 0006, AC-1). */
class PhanQuyenCoverageMauTest {

	private static final String MAU = MauController.class.getName() + "#";

	@Test
	void baoTungLoiKhaiSai() throws Exception {
		Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
		them(handlers, "thieu", "/api/thieu", RequestMethod.GET);
		them(handlers, "haiAnnotation", "/api/hai", RequestMethod.GET);
		them(handlers, "choPhepRong", "/api/rong", RequestMethod.GET);
		them(handlers, "preAuthorize", "/api/pre", RequestMethod.GET);
		them(handlers, "congKhai", "/api/cong-khai", RequestMethod.POST);
		them(handlers, "dung", "/api/dung", RequestMethod.GET);

		List<String> loi = PhanQuyenCoverage.kiem(handlers,
				List.of(new DuongCongKhai(HttpMethod.POST, "/api/khong-co-handler"),
						new DuongCongKhai(null, "/actuator/health")));

		assertThat(loi).containsExactlyInAnyOrder(MAU + "thieu: thiếu @ChoPhep, @DaDangNhap hoặc @CongKhai",
				MAU + "haiAnnotation: mang nhiều hơn một annotation phân quyền",
				MAU + "choPhepRong: @ChoPhep không có vai trò nào", MAU + "preAuthorize: không dùng @PreAuthorize",
				"@CongKhai POST /api/cong-khai: thiếu trong PublicPaths.CONG_KHAI",
				"PublicPaths.CONG_KHAI POST /api/khong-co-handler: không có handler @CongKhai");
	}

	@Test
	void congKhaiKhopDanhSachThiKhongLoi() throws Exception {
		Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
		them(handlers, "congKhai", "/api/cong-khai", RequestMethod.POST);
		them(handlers, "dung", "/api/dung", RequestMethod.GET);

		assertThat(PhanQuyenCoverage.kiem(handlers, List.of(new DuongCongKhai(HttpMethod.POST, "/api/cong-khai"))))
			.isEmpty();
	}

	@Test
	void handlerNgoaiDuAnKhongBiTinh() throws Exception {
		Map<RequestMappingInfo, HandlerMethod> handlers = new LinkedHashMap<>();
		handlers.put(RequestMappingInfo.paths("/ngoai").build(),
				new HandlerMethod(new Object(), Object.class.getMethod("toString")));

		assertThat(PhanQuyenCoverage.kiem(handlers, List.of())).isEmpty();
	}

	private static void them(Map<RequestMappingInfo, HandlerMethod> handlers, String method, String path,
			RequestMethod requestMethod) throws NoSuchMethodException {
		handlers.put(RequestMappingInfo.paths(path).methods(requestMethod).build(),
				new HandlerMethod(new MauController(), MauController.class.getMethod(method)));
	}

}
