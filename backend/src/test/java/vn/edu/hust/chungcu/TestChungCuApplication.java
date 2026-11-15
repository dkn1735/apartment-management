package vn.edu.hust.chungcu;

import org.springframework.boot.SpringApplication;

public class TestChungCuApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChungCuApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
