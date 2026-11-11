package com.controltower.backend.areas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AreaControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser(username = "auditor", roles = "AUDITOR")
	void returnsSeededProductionAreasFromDatabase() throws Exception {
		mockMvc.perform(get("/api/v1/areas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].code").value("PROD_A"))
				.andExpect(jsonPath("$[0].name").value("Producción A"))
				.andExpect(jsonPath("$[0].warningAfterSeconds").value(60))
				.andExpect(jsonPath("$[0].timeoutAfterSeconds").value(180))
				.andExpect(jsonPath("$[1].code").value("PROD_B"))
				.andExpect(jsonPath("$[2].code").value("PROD_C"));
	}
}
