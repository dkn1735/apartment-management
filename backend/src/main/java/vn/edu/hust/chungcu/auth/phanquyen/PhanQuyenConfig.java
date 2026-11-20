package vn.edu.hust.chungcu.auth.phanquyen;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Đăng ký {@link ChoPhepInterceptor} cho mọi API (spec 0006). */
@Configuration(proxyBeanMethods = false)
public class PhanQuyenConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ChoPhepInterceptor()).addPathPatterns("/api/**");
	}

}
