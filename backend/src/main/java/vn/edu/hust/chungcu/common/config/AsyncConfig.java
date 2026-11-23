package vn.edu.hust.chungcu.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor riêng có giới hạn cho email (spec 0010, AC-3): 2 luồng, hàng đợi 100. Không
 * dùng executor mặc định vì nó không giới hạn, một đợt yêu cầu dồn dập không được làm cạn
 * bộ nhớ. Hàng đợi đầy thì {@code execute} ném
 * {@link org.springframework.core.task.TaskRejectedException}; nơi gửi bắt lỗi đó, bỏ
 * email và ghi log ERROR kèm mã của mình (ví dụ mã yêu cầu), điều mà một
 * {@code RejectedExecutionHandler} chung không biết.
 */
@Configuration(proxyBeanMethods = false)
public class AsyncConfig {

	public static final String EMAIL_EXECUTOR = "emailExecutor";

	static final int SO_LUONG = 2;

	static final int HANG_DOI = 100;

	@Bean(EMAIL_EXECUTOR)
	ThreadPoolTaskExecutor emailExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(SO_LUONG);
		executor.setMaxPoolSize(SO_LUONG);
		executor.setQueueCapacity(HANG_DOI);
		executor.setThreadNamePrefix("email-");
		// Dừng backend thì chờ email đang gửi một lúc, không cắt ngang
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(10);
		return executor;
	}

}
