package com.exercise.contactBook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ContactBookApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactBookApplication.class, args);
	}

}
