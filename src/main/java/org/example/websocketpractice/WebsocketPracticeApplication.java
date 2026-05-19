package org.example.websocketpractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class WebsocketPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebsocketPracticeApplication.class, args);
	}

}
