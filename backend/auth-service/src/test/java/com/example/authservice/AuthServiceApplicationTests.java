package com.example.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "modu.internal-api.token=test-internal-token")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
