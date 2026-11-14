package com.example.ResourceServer;

import com.example.ResourceServer.dao.ProfilesDao;
import com.example.ResourceServer.dao.UploadsDao;
import com.example.ResourceServer.domains.Profile;
import com.example.ResourceServer.exceptions.UserNotFoundException;
import com.example.ResourceServer.security.AuthManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class ResourceServerApplicationTests {

	@Autowired
	ProfilesDao profilesDao;

	@Autowired
	UploadsDao uploadsDao;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry){
		registry.add("spring.datasource.url", PostgresTest.postgres::getJdbcUrl);
		registry.add("spring.datasource.username", PostgresTest.postgres::getUsername);
		registry.add("spring.datasource.password", PostgresTest.postgres::getPassword);
	}
	@Test
	void contextLoads() {
	}



}
