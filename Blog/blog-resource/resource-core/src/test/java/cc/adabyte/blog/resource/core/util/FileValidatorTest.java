package cc.adabyte.blog.resource.core.util;

import cc.adabyte.blog.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    @Test
    void shouldRejectHtmlFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "xss.html", "text/html", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> FileValidator.validate(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void shouldRejectJsFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "evil.js", "application/javascript", "alert(1)".getBytes());

        assertThatThrownBy(() -> FileValidator.validate(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void shouldRejectExecutableFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[]{0x4D, 0x5A});

        assertThatThrownBy(() -> FileValidator.validate(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void shouldRejectMismatchedExtensionAndMime() {
        // PNG extension but declares JPEG MIME
        MultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/jpeg", pngBytes());

        assertThatThrownBy(() -> FileValidator.validate(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("后缀与 MIME 类型不一致");
    }

    @Test
    void shouldRejectFakePngWithWrongMagic() {
        MultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not a real png".getBytes());

        assertThatThrownBy(() -> FileValidator.validate(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件内容与实际类型不符");
    }

    @Test
    void shouldAcceptValidPng() {
        MultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", pngBytes());

        assertThatNoException().isThrownBy(() -> FileValidator.validate(file));
    }

    @Test
    void shouldAcceptValidJpeg() {
        byte[] jpeg = new byte[16];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        MultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpeg);

        assertThatNoException().isThrownBy(() -> FileValidator.validate(file));
    }

    @Test
    void shouldRejectOversizedFile() {
        byte[] large = new byte[(int) (10 * 1024 * 1024L) + 1];
        MultipartFile file = new MockMultipartFile(
                "file", "large.png", "image/png", large);

        assertThatThrownBy(() -> FileValidator.validate(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过 10MB");
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
    }
}
