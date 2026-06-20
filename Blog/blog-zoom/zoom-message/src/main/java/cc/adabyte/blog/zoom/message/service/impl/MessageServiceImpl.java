package cc.adabyte.blog.zoom.message.service.impl;

import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.system.review.service.ReviewService;
import cc.adabyte.blog.zoom.message.dto.MessageVo;
import cc.adabyte.blog.zoom.message.entity.Message;
import cc.adabyte.blog.zoom.message.mapper.MessageMapper;
import cc.adabyte.blog.zoom.message.service.MailService;
import cc.adabyte.blog.zoom.message.service.MessageService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final ResourceAllocationFacade resourceFacade;
    private final MailService mailService;
    private final ReviewService reviewService;

    @Override
    public PageResult<MessageVo> list(int page, int size) {
        Page<Message> mpPage = new Page<>(page, size);
        var result = messageMapper.selectByStatus(mpPage, ContentStatus.PUBLISHED.getValue());
        var records = result.getRecords().stream()
                .peek(this::renderMessage)
                .map(this::toVo)
                .toList();
        return PageResult.of(result.getTotal(), records, result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<Message> listAdmin(Integer status, int page, int size) {
        Page<Message> mpPage = new Page<>(page, size);
        var result = messageMapper.selectAdmin(mpPage, status);
        result.getRecords().forEach(this::renderMessage);
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    private MessageVo toVo(Message message) {
        if (message == null) return null;
        MessageVo vo = new MessageVo();
        vo.setId(message.getId());
        vo.setNickname(message.getNickname());
        vo.setAvatarResourceId(message.getAvatarResourceId());
        vo.setContent(message.getContent());
        vo.setStatus(message.getStatus());
        vo.setReply(message.getReply());
        vo.setLikeCount(message.getLikeCount());
        vo.setRefType(message.getRefType());
        vo.setRefId(message.getRefId());
        vo.setRefTitle(message.getRefTitle());
        vo.setCreateTime(message.getCreateTime());
        vo.setUpdateTime(message.getUpdateTime());
        return vo;
    }

    private void renderMessage(Message message) {
        if (message != null && message.getContent() != null) {
            message.setContent(resourceFacade.renderMarkdown(message.getContent()));
        }
    }

    @Override
    @Transactional
    public void submit(Message message) {
        if (message.getStatus() == null) {
            message.setStatus(ContentStatus.PENDING_REVIEW);
        }
        if (message.getLikeCount() == null) {
            message.setLikeCount(0);
        }
        messageMapper.insert(message);

        SubmitReviewRequest reviewRequest = new SubmitReviewRequest();
        reviewRequest.setContentType("message");
        reviewRequest.setContentId(message.getId());
        reviewRequest.setSubmitterType("visitor");
        reviewRequest.setSubmitterId(message.getEmail());
        reviewService.submit(reviewRequest);
        log.info("[Message] 已创建审核任务: messageId={}, email={}", message.getId(), message.getEmail());
    }

    @Override
    @Transactional
    public void approve(Long id, String reply, Long avatarResourceId) {
        Message msg = messageMapper.selectById(id);
        if (msg == null) return;
        msg.setStatus(ContentStatus.PUBLISHED);

        if (avatarResourceId != null) {
            msg.setAvatarResourceId(avatarResourceId);
            resourceFacade.bindMessageAvatar(id, avatarResourceId);
        }
        if (reply != null) {
            msg.setReply(reply);
        }

        messageMapper.updateById(msg);

        reviewService.resolveAndApprove("message", id, reply);

        if (msg.getEmail() != null && !msg.getEmail().isBlank()) {
            mailService.sendApprovalNotification(msg.getEmail(), msg.getNickname());
        }
    }

    @Override
    @Transactional
    public void reject(Long id, String reason) {
        Message msg = messageMapper.selectById(id);
        if (msg == null) return;
        msg.setStatus(ContentStatus.REJECTED);
        msg.setRejectReason(reason);
        messageMapper.updateById(msg);

        reviewService.resolveAndReject("message", id, reason, null);

        if (msg.getEmail() != null && !msg.getEmail().isBlank()) {
            mailService.sendRejectionNotification(msg.getEmail(), msg.getNickname(), reason);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Message msg = messageMapper.selectById(id);
        if (msg == null) return;
        resourceFacade.unbindMessageAvatar(id);
        reviewService.deleteByContent("message", id);
        messageMapper.deleteById(id);
    }
}
