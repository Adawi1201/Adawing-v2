package cc.adabyte.blog.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 留言板公开性契约测试。
 *
 * <p>约定：{@code GET /api/v2/messages} 列表与 {@code POST /api/v2/messages} 提交
 * 均对访客匿名开放（POST 进入待审核链）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("留言板公开提交契约测试")
class MessageSubmitContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("匿名 GET 留言列表可用")
    void anonymousList() throws Exception {
        mockMvc.perform(get("/api/v2/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("匿名 POST 提交留言可用（进入待审核）")
    void anonymousSubmit() throws Exception {
        mockMvc.perform(post("/api/v2/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"契约测试访客\",\"email\":\"visitor@test.com\",\"content\":\"匿名提交契约测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
