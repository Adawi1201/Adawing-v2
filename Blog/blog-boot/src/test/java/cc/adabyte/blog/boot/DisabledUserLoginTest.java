package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DisabledUserLoginTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private SysUserMapper sysUserMapper;

    @Test
    void disabledUserShouldNotObtainToken() {
        SysUser disabled = new SysUser();
        disabled.setUsername("disabled");
        disabled.setPasswordHash(new BCryptPasswordEncoder().encode("pass"));
        disabled.setStatus(UserStatus.DISABLED);
        when(sysUserMapper.selectByUsername("disabled")).thenReturn(disabled);

        Map<String, String> body = Map.of("username", "disabled", "password", "pass");
        ResponseEntity<Result<String>> response = restTemplate.exchange(
                "/api/v2/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(500);
        assertThat(response.getBody().getData()).isNull();
    }
}
