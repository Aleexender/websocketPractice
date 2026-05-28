package org.example.websocketpractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling
public class WebsocketPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebsocketPracticeApplication.class, args);
	}

}
