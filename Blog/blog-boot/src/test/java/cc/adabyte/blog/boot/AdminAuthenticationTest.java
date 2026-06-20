package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class AdminAuthenticationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void adminEndpointShouldRejectAnonymousRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v2/articles/admin", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminEndpointShouldAllowAuthenticatedAdmin() {
        String token = loginAsAdmin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v2/articles/admin",
                HttpMethod.GET,
                request,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void publicEndpointShouldRemainAccessibleWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v2/articles/published", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requestWithInvalidTokenShouldBeRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v2/articles/admin",
                HttpMethod.GET,
                request,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
