package cc.adabyte.blog.system.review.service.impl;

import cc.adabyte.blog.common.constants.ReviewStatus;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.gateway.ReviewContentDeletionGateway;
import cc.adabyte.blog.common.gateway.ReviewContentResolver;
import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.strategy.ReviewStrategy;
import cc.adabyte.blog.system.review.dto.ReviewTaskVO;
import cc.adabyte.blog.system.review.entity.ReviewTask;
import cc.adabyte.blog.system.review.mapper.ReviewTaskMapper;
import cc.adabyte.blog.system.review.service.ReviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewTaskMapper reviewTaskMapper;
    private final List<ReviewStrategy> strategies;
    private final ReviewContentResolver contentResolver;
    private final ReviewContentDeletionGateway deletionGateway;

    @Override
    @Transactional
    public ReviewTask submit(SubmitReviewRequest request) {
        ReviewTask task = new ReviewTask();
        task.setContentType(request.getContentType());
        task.setContentId(request.getContentId());
        task.setStatus(ReviewStatus.PENDING);
        task.setSubmitterType(request.getSubmitterType());
        task.setSubmitterId(request.getSubmitterId());
        task.setSubmitTime(LocalDateTime.now());
        reviewTaskMapper.insert(task);
        return task;
    }

    @Override
    public PageResult<ReviewTaskVO> listTasks(Integer status, int page, int size) {
        Page<ReviewTask> mpPage = new Page<>(page, size);
        var result = reviewTaskMapper.selectByStatus(mpPage, status);
        List<ReviewTaskVO> enriched = result.getRecords().stream()
                .map(this::enrich)
                .toList();
        return PageResult.of(result.getTotal(), enriched, result.getCurrent(), result.getSize());
    }

    private ReviewTaskVO enrich(ReviewTask task) {
        ReviewTaskVO vo = new ReviewTaskVO();
        vo.setId(task.getId());
        vo.setContentType(task.getContentType());
        vo.setContentId(task.getContentId());
        vo.setStatus(task.getStatus());
        vo.setSubmitterType(task.getSubmitterType());
        vo.setSubmitterId(task.getSubmitterId());
        vo.setRejectReason(task.getRejectReason());
        vo.setReviewerNote(task.getReviewerNote());
        vo.setSubmitTime(task.getSubmitTime());
        vo.setReviewTime(task.getReviewTime());

        ReviewContentResolver.Result resolved = contentResolver.resolve(
                task.getContentType(), task.getContentId());
        if (resolved != null) {
            vo.setContentTitle(resolved.title());
            vo.setContentBody(resolved.content());
            vo.setSubmitterName(resolved.submitterName());
            vo.setSubmitterEmail(resolved.submitterEmail());
            vo.setAvatarResourceId(resolved.avatarResourceId());
            vo.setContentStatus(resolved.currentStatus());
        }
        return vo;
    }

    @Override
    public long countPending() {
        Long count = reviewTaskMapper.countPending();
        return count != null ? count : 0L;
    }

    @Override
    @Transactional
    public void approve(Long taskId, String reviewerNote, Long coverResourceId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) return;
        ReviewStrategy strategy = resolveStrategy(task.getContentType());
        task.setStatus(ReviewStatus.APPROVED);
        task.setReviewerNote(reviewerNote);
        task.setReviewTime(LocalDateTime.now());
        reviewTaskMapper.updateById(task);
        strategy.onApprove(task.getContentId(), reviewerNote, coverResourceId);
    }

    @Override
    @Transactional
    public void reject(Long taskId, String reason, String reviewerNote) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) return;
        ReviewStrategy strategy = resolveStrategy(task.getContentType());
        task.setStatus(ReviewStatus.REJECTED);
        task.setRejectReason(reason);
        task.setReviewerNote(reviewerNote);
        task.setReviewTime(LocalDateTime.now());
        reviewTaskMapper.updateById(task);
        strategy.onReject(task.getContentId(), reason, reviewerNote);
    }

    @Override
    @Transactional
    public void ignorePending(Long taskId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) return;
        if (task.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessException("仅待审核任务支持忽略删除");
        }
        reviewTaskMapper.deleteById(taskId);
        deletionGateway.delete(task.getContentType(), task.getContentId());
    }

    @Override
    public Optional<ReviewTask> findByContent(String contentType, Long contentId) {
        return Optional.ofNullable(reviewTaskMapper.selectByContent(contentType, contentId));
    }

    @Override
    @Transactional
    public void resolveAndApprove(String contentType, Long contentId, String reviewerNote) {
        findByContent(contentType, contentId).ifPresent(task -> {
            task.setStatus(ReviewStatus.APPROVED);
            task.setReviewerNote(reviewerNote);
            task.setReviewTime(LocalDateTime.now());
            reviewTaskMapper.updateById(task);
        });
    }

    @Override
    @Transactional
    public void resolveAndReject(String contentType, Long contentId, String reason, String reviewerNote) {
        findByContent(contentType, contentId).ifPresent(task -> {
            task.setStatus(ReviewStatus.REJECTED);
            task.setRejectReason(reason);
            task.setReviewerNote(reviewerNote);
            task.setReviewTime(LocalDateTime.now());
            reviewTaskMapper.updateById(task);
        });
    }

    @Override
    @Transactional
    public void deleteByContent(String contentType, Long contentId) {
        reviewTaskMapper.delete(new LambdaQueryWrapper<ReviewTask>()
                .eq(ReviewTask::getContentType, contentType)
                .eq(ReviewTask::getContentId, contentId));
    }

    private ReviewStrategy resolveStrategy(String contentType) {
        return strategies.stream()
                .filter(s -> s.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未知内容类型: " + contentType));
    }
}
