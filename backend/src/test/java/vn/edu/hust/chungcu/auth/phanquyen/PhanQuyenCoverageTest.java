package vn.edu.hust.chungcu.auth.phanquyen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import vn.edu.hust.chungcu.TestcontainersConfiguration;
import vn.edu.hust.chungcu.common.security.PublicPaths;

/**
 * Mọi handler thật của app khai quyền đúng một cách và khớp {@code PublicPaths.CONG_KHAI}
 * (spec 0006, AC-1). Quên khai trên một endpoint mới thì test này đỏ.
 */
@Import({ TestcontainersConfiguration.class, ChiCuDanController.class })
@SpringBootTest
@ActiveProfiles({ "dev", "test" })
class PhanQuyenCoverageTest {

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	RequestMappingHandlerMapping mapping;

	@Test
	void moiHandlerDeuKhaiQuyen() {
		assertThat(this.mapping.getHandlerMethods().values())
			.anyMatch(h -> h.getBeanType().equals(ChiCuDanController.class));
		assertThat(PhanQuyenCoverage.kiem(this.mapping.getHandlerMethods(), PublicPaths.CONG_KHAI)).isEmpty();
	}

}
