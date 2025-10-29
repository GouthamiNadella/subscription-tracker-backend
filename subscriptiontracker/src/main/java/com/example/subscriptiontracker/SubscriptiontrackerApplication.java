package com.example.subscriptiontracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubscriptiontrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubscriptiontrackerApplication.class, args);
	}

}
