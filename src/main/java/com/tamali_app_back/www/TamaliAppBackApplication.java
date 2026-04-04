package com.tamali_app_back.www;

import com.tamali_app_back.www.config.WebPushProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WebPushProperties.class)
public class TamaliAppBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(TamaliAppBackApplication.class, args);
	}

}
