package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.util.JwtUtil;
import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.enums.UserRole;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 写作指南配置端点契约：GET/PUT /api/v2/config/writing-guide 仅管理员可访问，
 * PUT 写入的 Markdown 原样经 GET 读回。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("写作指南配置契约测试")
class WritingGuideConfigContractTest {

    private static final String URL = "/api/v2/config/writing-guide";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String adminToken() {
        String username = "writing-guide-admin";
        if (sysUserMapper.selectByUsername(username) == null) {
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setPasswordHash("unused");
            u.setStatus(UserStatus.ACTIVE);
            u.setRole(UserRole.ADMIN);
            sysUserMapper.insert(u);
        }
        return JwtUtil.generateToken(username, jwtSecret);
    }

    @Test
    @DisplayName("未登录 GET / PUT 返回 401")
    void anonymousRejected() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无效令牌 GET / PUT 返回 401")
    void invalidTokenRejected() throws Exception {
        String token = "Bearer not-a-valid-jwt";
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(URL)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员 PUT 后 GET 读回相同内容")
    void adminRoundTrip() throws Exception {
        String token = "Bearer " + adminToken();
        String guide = "# 写作指南\n\n- 先写结论\n- 不写空话";

        mockMvc.perform(put(URL)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(java.util.Map.of("content", guide))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value(guide));
    }
}
