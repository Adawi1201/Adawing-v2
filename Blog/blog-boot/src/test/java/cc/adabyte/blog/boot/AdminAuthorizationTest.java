package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.message.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class AdminAuthorizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private MessageService messageService;

    @Test
    void anonymousRequestsToMcpKeyEndpointAreRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v2/mcp-keys", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymousRequestsToArticlePublishAreRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v2/articles/1/publish", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymousRequestsToMessageApproveAreRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v2/messages/1/approve", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminCanAccessMcpKeyList() {
        String token = loginAsAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Result<?>> response = restTemplate.exchange(
                "/api/v2/mcp-keys",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminCanAccessArticlePublish() {
        String token = loginAsAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                "/api/v2/articles/1/publish",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String loginAsAdmin() {
        Map<String, String> body = Map.of("username", "admin", "password", "admin123");
        ResponseEntity<Result<String>> response = restTemplate.exchange(
                "/api/v2/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        return response.getBody().getData();
    }
}
