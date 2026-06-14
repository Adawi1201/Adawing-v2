package cc.adabyte.blog.system.auth.service;

import cc.adabyte.blog.system.auth.entity.McpKey;

import java.util.List;

public interface McpKeyService {
    String generateKey(String name, String description);
    boolean validateKey(String plainKey);
    void revokeKey(Long id);
    List<McpKey> listKeys();
}
