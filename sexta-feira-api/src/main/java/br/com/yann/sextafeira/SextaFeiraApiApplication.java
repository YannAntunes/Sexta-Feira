package br.com.yann.sextafeira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SextaFeiraApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SextaFeiraApiApplication.class, args);
	}

}
