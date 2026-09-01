package com.supportdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SupportdeskApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupportdeskApplication.class, args);
	}
}
