package com.tamali_app_back.www;

import com.tamali_app_back.www.config.WebPushProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

@SpringBootApplication
@EnableConfigurationProperties(WebPushProperties.class)
public class TamaliAppBackApplication {

	public static void main(String[] args) {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		SpringApplication.run(TamaliAppBackApplication.class, args);
	}

}
