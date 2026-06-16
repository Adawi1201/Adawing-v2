package cc.adabyte.blog.zoom.message.controller;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.zoom.message.dto.MessageSubmitRequest;
import cc.adabyte.blog.zoom.message.entity.Message;
import cc.adabyte.blog.zoom.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<Message>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(messageService.list(page, size));
    }

    @GetMapping("/admin")
    public Result<PageResult<Message>> listAdmin(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(messageService.listAdmin(status, page, size));
    }

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody MessageSubmitRequest request) {
        Message message = new Message();
        message.setNickname(request.getNickname());
        message.setEmail(request.getEmail());
        message.setContent(request.getContent());
        message.setRefId(request.getRefId());
        message.setRefTitle(request.getRefTitle());
        messageService.submit(message);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestParam(required = false) Long avatarResourceId,
            @RequestBody(required = false) String reply) {
        messageService.approve(id, reply, avatarResourceId);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) String reason) {
        messageService.reject(id, reason);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(id);
        return Result.ok();
    }
}
