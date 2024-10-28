package com.md.sign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

@SpringBootApplication
public class SignApplication {
	static {
		// BouncyCastle provider at startup
		Security.addProvider(new BouncyCastleProvider());
	}

	public static void main(String[] args) {
		SpringApplication.run(SignApplication.class, args);
	}
}