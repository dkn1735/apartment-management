package vn.edu.hust.chungcu.nhatky;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;
import vn.edu.hust.chungcu.nhatky.service.TenKhoaNhayCam;

/**
 * Mọi chi tiết nhật ký là record và không có thành phần nào mang tên nhạy cảm (spec 0007,
 * AC-8 c). Quét cả record lồng và record không public, cả trong code test.
 */
class ChiTietNhatKyAnToanTest {

	@Test
	void khongChiTietNaoCoThanhPhanNhayCam() throws Exception {
		ClassPathScanningCandidateComponentProvider quet = new ClassPathScanningCandidateComponentProvider(false);
		quet.addIncludeFilter(new AssignableTypeFilter(ChiTietNhatKy.class));

		List<String> lop = new ArrayList<>();
		List<String> vanDe = new ArrayList<>();
		for (BeanDefinition bd : quet.findCandidateComponents("vn.edu.hust.chungcu")) {
			Class<?> type = Class.forName(bd.getBeanClassName());
			lop.add(type.getName());
			if (!type.isRecord()) {
				vanDe.add(type.getName() + " không phải record");
				continue;
			}
			for (RecordComponent tp : type.getRecordComponents()) {
				if (TenKhoaNhayCam.nhayCam(tp.getName())) {
					vanDe.add(type.getName() + "." + tp.getName());
				}
			}
		}
		// Quét phải thấy chi tiết của auth, nếu không thì test không kiểm gì
		assertThat(lop).contains("vn.edu.hust.chungcu.auth.service.ChiTietPhien",
				"vn.edu.hust.chungcu.auth.service.ChiTietDangNhapThatBai");
		assertThat(vanDe).as("Chi tiết nhật ký có thành phần nhạy cảm").isEmpty();
	}

}
