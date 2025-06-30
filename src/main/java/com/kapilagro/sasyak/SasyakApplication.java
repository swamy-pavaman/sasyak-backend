package com.kapilagro.sasyak;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
public class SasyakApplication {

	public static void main(String[] args) {
		SpringApplication.run(SasyakApplication.class, args);
//		String accessSecret = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
//		String refreshSecret = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());

//		System.out.println("Access Token Secret:\n" + accessSecret); // KA92Ab8OphARt/lQwY6u5Zn+LkwISP6m9ABjI3JQfVo=
//		System.out.println("Refresh Token Secret:\n" + refreshSecret); // HyaFfpChC8IekjRGc5loPYid4/uHekm0dBmlJaYnvq0=
	}

}
