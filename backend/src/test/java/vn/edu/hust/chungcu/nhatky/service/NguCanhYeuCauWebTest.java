package vn.edu.hust.chungcu.nhatky.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.hust.chungcu.common.web.RequestIdFilter;

/** Request id và IP của request đang chạy (spec 0007, AC-1, AC-3). */
class NguCanhYeuCauWebTest {

	final NguCanhYeuCauWeb nguCanh = new NguCanhYeuCauWeb();

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
		MDC.remove(RequestIdFilter.MDC_KEY);
	}

	@Test
	void khongCoRequestThiTraNull() {
		RequestContextHolder.resetRequestAttributes();
		assertThat(this.nguCanh.requestId()).isNull();
		assertThat(this.nguCanh.diaChiIp()).isNull();
	}

	@Test
	void docRequestIdTuMdcVaIpTuRemoteAddr() {
		MDC.put(RequestIdFilter.MDC_KEY, "abc-123");
		datIp("203.0.113.9");
		assertThat(this.nguCanh.requestId()).isEqualTo("abc-123");
		assertThat(this.nguCanh.diaChiIp()).isEqualTo("203.0.113.9");
	}

	@Test
	void ipDaiBiCatIpRongThanhNull() {
		datIp("x".repeat(60));
		assertThat(this.nguCanh.diaChiIp()).hasSize(45);
		datIp("  ");
		assertThat(this.nguCanh.diaChiIp()).isNull();
	}

	private static void datIp(String ip) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(ip);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

}
