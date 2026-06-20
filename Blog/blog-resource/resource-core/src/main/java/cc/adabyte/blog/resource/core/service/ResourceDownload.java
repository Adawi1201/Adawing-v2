package cc.adabyte.blog.resource.core.service;

import java.io.InputStream;

public record ResourceDownload(
        InputStream stream,
        String mimeType,
        Long size,
        String originalName,
        boolean publicAccess
) {
    public ResourceDownload(InputStream stream, String mimeType, Long size, String originalName) {
        this(stream, mimeType, size, originalName, false);
    }
}
