package com.readplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readplan.common.api.ApiResponse;
import com.readplan.support.ApiResponseType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReadPlanAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsJwtAndAllowsFetchingUserInfo() throws Exception {
        String loginBody = """
            {
              "username": "reader",
              "password": "123456"
            }
            """;

        String loginJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode loginRoot = objectMapper.readTree(loginJson);
        String token = loginRoot.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        String userInfoJson = mockMvc.perform(get("/api/user/info")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        ApiResponseType<JsonNode> userInfo = toApiResponse(userInfoJson);
        assertThat(userInfo.code()).isEqualTo(200);
        assertThat(userInfo.data().path("username").asText()).isEqualTo("reader");
        assertThat(userInfo.data().path("roles").get(0).asText()).isEqualTo("USER");
    }

    @Test
    void publicBookEndpointIsAccessibleWithoutAuthentication() throws Exception {
        String responseJson = mockMvc.perform(get("/api/books?pageNum=1&pageSize=10"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        ApiResponseType<JsonNode> response = toApiResponse(responseJson);
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data().path("total").asLong()).isEqualTo(3);
    }

    private ApiResponseType<JsonNode> toApiResponse(String content) throws Exception {
        ApiResponse<JsonNode> response = objectMapper.readValue(content, new TypeReference<>() {
        });
        return ApiResponseType.from(response);
    }
}
