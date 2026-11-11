package com.controltower.backend.controller;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsApplicationHealth() throws Exception {
		String response = mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.application").value("DT Control Power"))
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.timestamp").isString())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String timestamp = response.replaceAll(".*\\\"timestamp\\\":\\\"([^\\\"]+)\\\".*", "$1");
		assertThatCode(() -> Instant.parse(timestamp)).doesNotThrowAnyException();
	}
}
