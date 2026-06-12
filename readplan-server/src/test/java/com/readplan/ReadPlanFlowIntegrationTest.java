package com.readplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readplan.common.api.ApiResponse;
import com.readplan.support.ApiResponseType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

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

    @Test
    void adminCanUploadBooksFromJsonFileWithTags() throws Exception {
        String adminToken = login("admin", "123456");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "books.json",
            MediaType.APPLICATION_JSON_VALUE,
            """
                [
                  {
                    "title": "领域驱动设计精粹",
                    "author": "Vaughn Vernon",
                    "publishYear": 2017,
                    "isbn": "9787111544937",
                    "description": "测试上传",
                    "tags": ["架构", "DDD"]
                  }
                ]
                """.getBytes(StandardCharsets.UTF_8)
        );

        JsonNode imported = api(mockMvc.perform(multipart("/api/admin/books/upload")
                .file(file)
                .param("tags", "上传导入,后端")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk()))
            .data();

        assertThat(imported.isArray()).isTrue();
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).path("title").asText()).isEqualTo("领域驱动设计精粹");
        assertThat(imported.get(0).path("tags").toString()).contains("上传导入");
        assertThat(imported.get(0).path("tags").toString()).contains("DDD");
    }

    @Test
    void adminCanImportCandidateWithStoredMetadata() throws Exception {
        String adminToken = login("admin", "123456");

        JsonNode imported = api(mockMvc.perform(post("/api/admin/books/import")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "olIds": ["OL45883W"]
                    }
                    """))
            .andExpect(status().isOk()))
            .data();

        assertThat(imported.isArray()).isTrue();
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).path("title").asText()).isEqualTo("Domain-Driven Design");
        assertThat(imported.get(0).path("isbn").asText()).isEqualTo("9780321125217");
        assertThat(imported.get(0).path("description").asText()).contains("候选数据");
        assertThat(imported.get(0).path("tags").toString()).contains("DDD");
    }

    @Test
    void adminCanUploadPdfBookFile() throws Exception {
        String adminToken = login("admin", "123456");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "Clean Architecture.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8)
        );

        JsonNode imported = api(mockMvc.perform(multipart("/api/admin/books/upload")
                .file(file)
                .param("tags", "上传导入,电子书")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk()))
            .data();

        assertThat(imported.isArray()).isTrue();
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).path("title").asText()).isEqualTo("Clean Architecture");
        assertThat(imported.get(0).path("fileType").asText()).isEqualTo("PDF");
        assertThat(imported.get(0).path("fileUrl").asText()).contains("/files/books/");
        assertThat(imported.get(0).path("tags").toString()).contains("电子书");
        assertThat(imported.get(0).path("description").asText()).contains("本地 PDF");
    }

    @Test
    void adminCanSearchAndImportLegalPublicResources() throws Exception {
        String adminToken = login("admin", "123456");

        JsonNode resources = api(mockMvc.perform(get("/api/admin/books/legal-resources")
                .header("Authorization", bearer(adminToken))
                .param("keyword", "alice"))
            .andExpect(status().isOk()))
            .data();

        assertThat(resources.isArray()).isTrue();
        assertThat(resources.size()).isGreaterThan(0);

        JsonNode first = resources.get(0);
        JsonNode imported = api(mockMvc.perform(post("/api/admin/books/legal-resources/import")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tags": ["公版资源", "测试导入"],
                      "resources": [
                        {
                          "sourceId": "%s",
                          "title": "%s",
                          "author": "%s",
                          "publishYear": %d,
                          "cover": "%s",
                          "resourceUrl": "%s",
                          "resourceType": "%s",
                          "sourceName": "%s",
                          "description": "%s",
                          "selected": true
                        }
                      ]
                    }
                    """.formatted(
                    first.path("sourceId").asText(),
                    escapeJson(first.path("title").asText()),
                    escapeJson(first.path("author").asText()),
                    first.path("publishYear").asInt(),
                    escapeJson(first.path("cover").asText()),
                    escapeJson(first.path("resourceUrl").asText()),
                    escapeJson(first.path("resourceType").asText()),
                    escapeJson(first.path("sourceName").asText()),
                    escapeJson(first.path("description").asText())
                )))
            .andExpect(status().isOk()))
            .data();

        assertThat(imported.isArray()).isTrue();
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).path("fileUrl").asText()).isNotBlank();
        assertThat(imported.get(0).path("tags").toString()).contains("公版资源");
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

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
