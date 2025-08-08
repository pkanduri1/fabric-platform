package com.truist.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories("com.truist.batch.repository")
@EnableTransactionManagement
public class InterfaceBatchApplication {
	public static void main(String[] args) {
		SpringApplication.run(InterfaceBatchApplication.class, args);
	}

}
