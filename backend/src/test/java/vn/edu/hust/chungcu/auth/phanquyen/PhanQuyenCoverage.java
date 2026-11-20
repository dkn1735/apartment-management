package vn.edu.hust.chungcu.auth.phanquyen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import vn.edu.hust.chungcu.common.security.PublicPaths;

/**
 * Kiểm mọi handler của dự án khai quyền đúng một cách và khớp với
 * {@link PublicPaths#CONG_KHAI} (spec 0006, AC-1). Lớp thuần, nhận danh sách handler để
 * test được bằng controller mẫu cố tình sai.
 */
final class PhanQuyenCoverage {

	static final String GOC = "vn.edu.hust.chungcu.";

	/** Có trong {@code CONG_KHAI} nhưng không có handler của dự án. */
	static final Set<String> KHONG_CO_HANDLER = Set.of("/actuator/health", "/actuator/health/**");

	private static final List<String> CAM = List.of("org.springframework.security.access.prepost.PreAuthorize",
			"org.springframework.security.access.prepost.PostAuthorize",
			"org.springframework.security.access.annotation.Secured", "jakarta.annotation.security.RolesAllowed");

	private PhanQuyenCoverage() {
	}

	/** Cặp (method, mẫu đường dẫn); method rỗng nghĩa là mọi method. */
	record Cap(String method, String mau) implements Comparable<Cap> {

		@Override
		public int compareTo(Cap o) {
			return toString().compareTo(o.toString());
		}

		@Override
		public String toString() {
			return (this.method.isEmpty() ? "*" : this.method) + " " + this.mau;
		}

	}

	/** Danh sách lỗi, rỗng khi mọi handler khai đúng. */
	static List<String> kiem(Map<RequestMappingInfo, HandlerMethod> handlers,
			List<PublicPaths.DuongCongKhai> congKhai) {
		List<String> loi = new ArrayList<>();
		Set<Cap> capHandler = new TreeSet<>();
		handlers.forEach((info, handler) -> {
			if (!handler.getBeanType().getName().startsWith(GOC)) {
				return;
			}
			String ten = handler.getBeanType().getName() + "#" + handler.getMethod().getName();
			Method method = handler.getMethod();
			ChoPhep choPhep = method.getAnnotation(ChoPhep.class);
			long soKhai = Stream
				.of(choPhep, method.getAnnotation(DaDangNhap.class), method.getAnnotation(CongKhai.class))
				.filter(a -> a != null)
				.count();
			if (soKhai == 0) {
				loi.add(ten + ": thiếu @ChoPhep, @DaDangNhap hoặc @CongKhai");
			}
			if (soKhai > 1) {
				loi.add(ten + ": mang nhiều hơn một annotation phân quyền");
			}
			if (choPhep != null && choPhep.value().length == 0) {
				loi.add(ten + ": @ChoPhep không có vai trò nào");
			}
			Stream.concat(Arrays.stream(method.getAnnotations()), Arrays.stream(handler.getBeanType().getAnnotations()))
				.map(Annotation::annotationType)
				.map(Class::getName)
				.filter(CAM::contains)
				.distinct()
				.forEach(cam -> loi.add(ten + ": không dùng @" + cam.substring(cam.lastIndexOf('.') + 1)));
			if (method.isAnnotationPresent(CongKhai.class)) {
				capHandler.addAll(cap(info));
			}
		});
		Set<Cap> capDanhSach = new TreeSet<>();
		congKhai.stream()
			.filter(d -> !KHONG_CO_HANDLER.contains(d.mau()))
			.forEach(d -> capDanhSach
				.add(new Cap((d.methodHoacNull() != null) ? d.methodHoacNull().name() : "", d.mau())));
		capHandler.stream()
			.filter(c -> !capDanhSach.contains(c))
			.forEach(c -> loi.add("@CongKhai " + c + ": thiếu trong PublicPaths.CONG_KHAI"));
		capDanhSach.stream()
			.filter(c -> !capHandler.contains(c))
			.forEach(c -> loi.add("PublicPaths.CONG_KHAI " + c + ": không có handler @CongKhai"));
		return loi;
	}

	private static List<Cap> cap(RequestMappingInfo info) {
		Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
		List<String> tenMethod = methods.isEmpty() ? List.of("") : methods.stream().map(RequestMethod::name).toList();
		List<Cap> result = new ArrayList<>();
		for (String mau : info.getPatternValues()) {
			for (String m : tenMethod) {
				result.add(new Cap(m, mau));
			}
		}
		return result;
	}

}
