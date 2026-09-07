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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 全局错误契约测试。
 *
 * <p>约定：/api/v2 下所有错误响应（Controller 层与 Filter 层）返回真实 HTTP 状态码，
 * body 为 Result JSON 且 code 与状态码一致；跨域 Origin 下错误响应同样携带 CORS 头。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("全局错误契约测试")
class ErrorContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String adminToken() {
        String username = "error-contract-admin";
        SysUser existing = sysUserMapper.selectByUsername(username);
        if (existing == null) {
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
    @DisplayName("@Valid 校验失败返回 400 + 字段错误信息")
    void validationFailure() throws Exception {
        mockMvc.perform(post("/api/v2/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }

    @Test
    @DisplayName("登录凭证错误返回 401 + Result JSON")
    void loginFailure() throws Exception {
        mockMvc.perform(post("/api/v2/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("无令牌访问管理端点返回 401 + Result JSON + CORS 头")
    void filterUnauthorizedWithCors() throws Exception {
        mockMvc.perform(get("/api/v2/review/tasks")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("不存在的已发布文章返回 404")
    void articleNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/articles/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("文章不存在或未发布"));
    }

    @Test
    @DisplayName("未映射路径返回 404 而非兜底 500")
    void unmappedPathNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/definitely-not-mapped")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET-only 端点收到 POST 返回 405")
    void methodNotSupported() throws Exception {
        mockMvc.perform(post("/api/v2/articles/archive")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    @DisplayName("上传超过 10MB 返回 413")
    void uploadTooLarge() throws Exception {
        byte[] content = new byte[10 * 1024 * 1024 + 1];
        // PNG magic number，保证命中的是大小校验而非内容校验
        byte[] pngMagic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(pngMagic, 0, content, 0, pngMagic.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", content);
        mockMvc.perform(multipart("/api/v2/admin/resources/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(413));
    }
}
