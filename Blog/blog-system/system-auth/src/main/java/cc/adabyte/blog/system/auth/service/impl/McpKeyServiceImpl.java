package cc.adabyte.blog.system.auth.service.impl;

import cc.adabyte.blog.common.gateway.McpKeyValidator;
import cc.adabyte.blog.common.util.DigestUtil;
import cc.adabyte.blog.system.auth.entity.McpKey;
import cc.adabyte.blog.system.auth.mapper.McpKeyMapper;
import cc.adabyte.blog.system.auth.service.McpKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpKeyServiceImpl implements McpKeyService, McpKeyValidator {

    private final McpKeyMapper mcpKeyMapper;

    @Override
    @Transactional
    public String generateKey(String name, String description) {
        String plainKey = "mcp_" + UUID.randomUUID().toString().replace("-", "");
        String hash = DigestUtil.sha256Base64(plainKey);

        McpKey entity = new McpKey();
        entity.setName(name);
        entity.setKeyHash(hash);
        entity.setEnabled(1);
        entity.setDescription(description);
        entity.setCreateTime(LocalDateTime.now());
        mcpKeyMapper.insert(entity);

        return plainKey;
    }

    @Override
    public boolean validateKey(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return false;
        String hash = DigestUtil.sha256Base64(plainKey);
        McpKey key = mcpKeyMapper.selectByKeyHash(hash);
        if (key != null) {
            key.setLastUsedAt(LocalDateTime.now());
            mcpKeyMapper.updateById(key);
        }
        return key != null;
    }

    @Override
    @Transactional
    public void revokeKey(Long id) {
        mcpKeyMapper.deleteById(id);
    }

    @Override
    public List<McpKey> listKeys() {
        return mcpKeyMapper.selectList(null);
    }

}
