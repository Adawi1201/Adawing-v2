package cc.adabyte.blog.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 公开资源列举端点契约测试。
 *
 * <p>访客留言板的表情包选择器需要匿名列举 EMOJI 池，此前只能调管理端
 * {@code /api/v2/admin/resources}，匿名 401 后被前端重定向到后台登录页（P0）。
 * 本测试锁定 {@code GET /api/v2/resource/public} 的契约：
 * 公开池（EMOJI/AVATAR）匿名可列举，私有池（ARTICLE/MISC）拒绝。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("公开资源列举端点契约测试")
class ResourcePublicListContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("匿名可列举 EMOJI 公开池，返回数组")
    void publicPoolAnonymous() throws Exception {
        mockMvc.perform(get("/api/v2/resource/public").param("pool", "EMOJI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("私有池 MISC 拒绝匿名列举")
    void privatePoolRejected() throws Exception {
        mockMvc.perform(get("/api/v2/resource/public").param("pool", "MISC"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }
}
