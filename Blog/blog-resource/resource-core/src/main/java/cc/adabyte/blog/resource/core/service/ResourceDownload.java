package cc.adabyte.blog.resource.core.service;

public record ResourceDownload(
        byte[] content,
        String mimeType,
        Long size,
        String originalName,
        boolean publicAccess
) {
    public ResourceDownload(byte[] content, String mimeType, Long size, String originalName) {
        this(content, mimeType, size, originalName, false);
    }
}
