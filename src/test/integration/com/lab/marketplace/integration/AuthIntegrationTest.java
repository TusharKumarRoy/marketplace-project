package com.lab.marketplace.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lab.marketplace.repository.UserRepository;

@SpringBootTest
class AuthIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UserRepository userRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void registerThenLoginWithUsernameOrEmailSucceeds() throws Exception {
		String username = "buyer_it";
		String email = "buyer_it@example.com";

		String registerJson = """
			{
			  \"username\": \"%s\",
			  \"email\": \"%s\",
			  \"password\": \"password123\",
			  \"fullName\": \"Integration Buyer\",
			  \"phoneNumber\": \"1234567890\",
			  \"role\": \"BUYER\"
			}
		""".formatted(username, email);

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerJson))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value(username))
			.andExpect(jsonPath("$.email").value(email));

		// Ensure user got persisted
		boolean exists = userRepository.existsByUsername(username);
		org.assertj.core.api.Assertions.assertThat(exists).isTrue();

		String loginJsonWithUsername = """
			{
			  \"usernameOrEmail\": \"%s\",
			  \"password\": \"password123\"
			}
		""".formatted(username);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJsonWithUsername))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(username));

		String loginJsonWithEmail = """
			{
			  \"usernameOrEmail\": \"%s\",
			  \"password\": \"password123\"
			}
		""".formatted(email);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJsonWithEmail))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(username));
	}
}
