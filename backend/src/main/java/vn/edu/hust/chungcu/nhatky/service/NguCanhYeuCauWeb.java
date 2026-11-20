package vn.edu.hust.chungcu.nhatky.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.hust.chungcu.common.web.RequestIdFilter;

/**
 * Đọc request id từ MDC của {@link RequestIdFilter} và IP từ {@code getRemoteAddr()} (đã
 * qua {@code forward-headers-strategy}). IP theo cùng quy tắc với {@code ThongTinThietBi}
 * của {@code auth}: cắt về 45 ký tự, chuỗi rỗng thành {@code null}.
 */
@Component
class NguCanhYeuCauWeb implements NguCanhYeuCau {

	static final int IP_TOI_DA = 45;

	@Override
	public String requestId() {
		return MDC.get(RequestIdFilter.MDC_KEY);
	}

	@Override
	public String diaChiIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return cat(attributes.getRequest().getRemoteAddr());
		}
		return null;
	}

	static String cat(String ip) {
		if (ip == null || ip.isBlank()) {
			return null;
		}
		return (ip.length() > IP_TOI_DA) ? ip.substring(0, IP_TOI_DA) : ip;
	}

}
