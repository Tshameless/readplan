package com.readplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ReadPlanFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void readerCanManagePlanNoteAndCommentFlow() throws Exception {
        String readerToken = login("reader", "123456");
        String adminToken = login("admin", "123456");

        long newPlanId = api(mockMvc.perform(post("/api/plans")
                .header("Authorization", bearer(readerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bookId": 2,
                      "status": 2
                    }
                    """))
            .andExpect(status().isOk()))
            .data()
            .path("id")
            .asLong();

        JsonNode updatedPlan = api(mockMvc.perform(put("/api/plans/" + newPlanId + "/status")
                .header("Authorization", bearer(readerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": 1
                    }
                    """))
            .andExpect(status().isOk()))
            .data();
        assertThat(updatedPlan.path("status").asInt()).isEqualTo(1);

        long newNoteId = api(mockMvc.perform(post("/api/notes")
                .header("Authorization", bearer(readerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bookId": 2,
                      "title": "测试笔记",
                      "content": "测试内容"
                    }
                    """))
            .andExpect(status().isOk()))
            .data()
            .path("id")
            .asLong();

        JsonNode updatedNote = api(mockMvc.perform(put("/api/notes/" + newNoteId)
                .header("Authorization", bearer(readerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bookId": 2,
                      "title": "测试笔记已更新",
                      "content": "测试内容已更新"
                    }
                    """))
            .andExpect(status().isOk()))
            .data();
        assertThat(updatedNote.path("title").asText()).isEqualTo("测试笔记已更新");

        long commentId = api(mockMvc.perform(post("/api/comments")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "noteId": %d,
                      "content": "管理员评论"
                    }
                    """.formatted(newNoteId)))
            .andExpect(status().isOk()))
            .data()
            .path("id")
            .asLong();

        JsonNode comments = api(mockMvc.perform(get("/api/notes/" + newNoteId + "/comments")
                .header("Authorization", bearer(readerToken)))
            .andExpect(status().isOk()))
            .data();
        assertThat(comments.isArray()).isTrue();
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).path("content").asText()).isEqualTo("管理员评论");

        mockMvc.perform(delete("/api/comments/" + commentId)
                .header("Authorization", bearer(readerToken)))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/notes/" + newNoteId)
                .header("Authorization", bearer(readerToken)))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/plans/" + newPlanId)
                .header("Authorization", bearer(readerToken)))
            .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        String responseJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(responseJson).path("data").path("token").asText();
    }

    private ApiResponseType<JsonNode> api(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        String content = actions.andReturn().getResponse().getContentAsString();
        ApiResponse<JsonNode> response = objectMapper.readValue(content, new TypeReference<>() {
        });
        return ApiResponseType.from(response);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
