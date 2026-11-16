package com.cstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CstudyApplication {

	public static void main(String[] args) {
		SpringApplication.run(CstudyApplication.class, args);
	}

}
