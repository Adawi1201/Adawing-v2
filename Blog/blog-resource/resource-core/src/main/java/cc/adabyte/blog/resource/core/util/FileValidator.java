package cc.adabyte.blog.resource.core.util;

import cc.adabyte.blog.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 上传文件校验器。
 *
 * <p>对文件名后缀、声明的 MIME 类型以及文件 Magic Number 进行白名单校验，
 * 阻止可执行文件、HTML/JS 等可能导致 XSS 或代码执行的文件上传。
 */
public final class FileValidator {

    private FileValidator() {
        // utility class
    }

    /** 允许上传的文件扩展名（小写，含点号）。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp",
            ".pdf", ".md", ".txt",
            ".mp4", ".webm",
            ".zip"
    );

    /** 允许上传的 MIME 类型。 */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml", "image/bmp",
            "application/pdf", "text/markdown", "text/plain",
            "video/mp4", "video/webm",
            "application/zip", "application/x-zip-compressed"
    );

    /** 扩展名与 MIME 类型的期望映射（用于一致性校验）。 */
    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".bmp", "image/bmp"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".md", "text/markdown"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm"),
            Map.entry(".zip", "application/zip")
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /**
     * 校验上传文件是否合法。
     *
     * @param file 上传文件
     * @throws BusinessException 校验失败时抛出
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("上传文件大小不能超过 10MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("上传文件名不能为空");
        }

        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件类型: " + extension);
        }

        String declaredMime = normalizeMime(file.getContentType());
        if (declaredMime == null || !ALLOWED_MIME_TYPES.contains(declaredMime)) {
            throw new BusinessException("不支持的文件 MIME 类型: " + declaredMime);
        }

        String expectedMime = EXTENSION_TO_MIME.get(extension);
        if (expectedMime != null && !expectedMime.equals(declaredMime)) {
            throw new BusinessException("文件后缀与 MIME 类型不一致");
        }

        byte[] magic = readMagicBytes(file);
        if (!matchesMagicNumber(extension, magic)) {
            throw new BusinessException("文件内容与实际类型不符");
        }
    }

    private static String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot).toLowerCase(Locale.ROOT);
    }

    private static String normalizeMime(String mime) {
        if (mime == null) {
            return null;
        }
        String lower = mime.trim().toLowerCase(Locale.ROOT);
        // Spring 对 .md 文件可能返回 text/plain 或 application/octet-stream
        if (lower.contains("markdown")) {
            return "text/markdown";
        }
        return lower;
    }

    private static byte[] readMagicBytes(MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            int len = Math.min(content.length, 16);
            byte[] magic = new byte[len];
            System.arraycopy(content, 0, magic, 0, len);
            return magic;
        } catch (IOException e) {
            throw new BusinessException("读取文件内容失败");
        }
    }

    private static boolean matchesMagicNumber(String extension, byte[] magic) {
        if (magic.length == 0) {
            return false;
        }
        return switch (extension) {
            case ".jpg", ".jpeg" -> startsWith(magic, 0xFF, 0xD8, 0xFF);
            case ".png" -> startsWith(magic, 0x89, 0x50, 0x4E, 0x47);
            case ".gif" -> startsWith(magic, 0x47, 0x49, 0x46, 0x38);
            case ".webp" -> startsWith(magic, 0x52, 0x49, 0x46, 0x46) && hasAt(magic, 8, 0x57, 0x45, 0x42, 0x50);
            case ".pdf" -> startsWith(magic, 0x25, 0x50, 0x44, 0x46);
            case ".zip" -> startsWith(magic, 0x50, 0x4B, 0x03, 0x04);
            case ".mp4" -> hasMp4Signature(magic);
            // SVG/WEBM/纯文本/BMP 等不做强 Magic 校验，依赖后缀与 MIME 一致性
            default -> true;
        };
    }

    private static boolean startsWith(byte[] data, int... bytes) {
        if (data.length < bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if ((data[i] & 0xFF) != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAt(byte[] data, int offset, int... bytes) {
        if (data.length < offset + bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if ((data[offset + i] & 0xFF) != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasMp4Signature(byte[] data) {
        // MP4 文件特征：文件头 4 字节为 box size，之后通常为 ftyp/msft/isom 等
        if (data.length < 12) {
            return false;
        }
        String brand = new String(data, 4, 4);
        return "ftyp".equals(brand);
    }
}
