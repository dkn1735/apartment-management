package vn.edu.hust.chungcu.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật {@code @Scheduled} cho cả ứng dụng (spec 0005, job dọn phiên). Một instance backend
 * nên không cần khóa phân tán.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {

}
