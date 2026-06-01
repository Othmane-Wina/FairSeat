package com.pfa.fairseatqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Required later to power our automated background batch release clocks
public class FairseatQueueApplication {
	public static void main(String[] args) {
		SpringApplication.run(FairseatQueueApplication.class, args);
	}
}