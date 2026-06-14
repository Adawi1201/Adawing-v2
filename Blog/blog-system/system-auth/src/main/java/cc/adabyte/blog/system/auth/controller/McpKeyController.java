package cc.adabyte.blog.system.auth.controller;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.system.auth.dto.GenerateKeyRequest;
import cc.adabyte.blog.system.auth.entity.McpKey;
import cc.adabyte.blog.system.auth.service.McpKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/mcp-keys")
@RequiredArgsConstructor
public class McpKeyController {

    private final McpKeyService mcpKeyService;

    @GetMapping
    public Result<List<McpKey>> list() {
        return Result.ok(mcpKeyService.listKeys());
    }

    @PostMapping
    public Result<String> generate(@Valid @RequestBody GenerateKeyRequest request) {
        String plainKey = mcpKeyService.generateKey(request.getName(), request.getDescription());
        return Result.ok(plainKey);
    }

    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        mcpKeyService.revokeKey(id);
        return Result.ok();
    }
}
