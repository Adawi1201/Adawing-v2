package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.mapper.NoteMapper;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class NoteXssSanitizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NoteMapper noteMapper;

    @Test
    void noteContentShouldBeSanitizedBeforeReturningToVisitor() {
        Note note = new Note();
        note.setTitle("XSS Test");
        note.setContent("Safe text.\n\n<script>alert('xss')</script>\n\n<img src=x onerror=alert('xss')>");
        note.setType(NoteType.PERSONAL);
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        noteMapper.insert(note);

        ResponseEntity<Result<Note>> response = restTemplate.exchange(
                "/api/v2/notes/" + note.getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        String content = response.getBody().getData().getContent();
        assertThat(content).doesNotContain("<script>").doesNotContain("</script>");
        assertThat(content).doesNotContain("onerror=");
    }

    @Test
    void noteContentShouldPreserveSafeMarkdownAndResourceLinks() {
        Note note = new Note();
        note.setTitle("Safe Markdown");
        note.setContent("# Hello\n\nSafe paragraph with **bold** and [link](https://example.com).");
        note.setType(NoteType.TECH);
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        noteMapper.insert(note);

        ResponseEntity<Result<Note>> response = restTemplate.exchange(
                "/api/v2/notes/" + note.getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        String content = response.getBody().getData().getContent();
        assertThat(content).contains("# Hello");
        assertThat(content).contains("**bold**");
        assertThat(content).contains("[link](https://example.com)");
    }
}
