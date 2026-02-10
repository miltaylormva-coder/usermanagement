package com.project.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Class for User Order Management System
 */
@SpringBootApplication
public class UserOrderManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserOrderManagementApplication.class, args);
		System.out.println("\n" +
				"===============================================\n" +
				"  User Order Management API is Running! 🚀\n" +
				"  Port: 8080\n" +
				"  Access: http://localhost:8080\n" +
				"===============================================\n");
	}
}