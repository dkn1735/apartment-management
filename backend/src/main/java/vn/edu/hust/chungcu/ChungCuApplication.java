package vn.edu.hust.chungcu;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChungCuApplication {

	public static void main(String[] args) {
		// JVM chạy UTC: driver PostgreSQL gửi múi giờ mặc định của JVM khi kết nối, và
		// trên
		// Windows đó là "Asia/Saigon", tên mà PostgreSQL 18 không nhận. Quy tắc theo ngày
		// dùng app.timezone qua bean Clock, không dựa vào múi giờ mặc định này.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ChungCuApplication.class, args);
	}

}
