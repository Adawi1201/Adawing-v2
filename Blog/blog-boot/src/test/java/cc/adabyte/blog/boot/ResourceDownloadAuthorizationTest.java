package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.service.ResourceDownload;
import cc.adabyte.blog.resource.core.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ResourceDownloadAuthorizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private ResourceService resourceService;

    @Test
    void publicResourceShouldBeAccessibleWithoutAuthentication() {
        when(resourceService.download(anyLong())).thenReturn(
                new ResourceDownload(new ByteArrayInputStream(new byte[0]), "image/png", 0L, "public.png", true));

        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                "/api/v2/resource/1/content", byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void privateResourceShouldBeRejectedForAnonymous() {
        when(resourceService.download(anyLong())).thenReturn(
                new ResourceDownload(new ByteArrayInputStream(new byte[0]), "image/png", 0L, "private.png", false));

        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                "/api/v2/resource/2/content", byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void privateResourceShouldBeAccessibleWithAdminToken() {
        when(resourceService.download(anyLong())).thenReturn(
                new ResourceDownload(new ByteArrayInputStream(new byte[0]), "image/png", 0L, "private.png", false));

        String token = loginAsAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/v2/resource/3/content",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);

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
