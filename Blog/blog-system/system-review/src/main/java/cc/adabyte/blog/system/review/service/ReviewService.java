package cc.adabyte.blog.system.review.service;

import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.system.review.dto.ReviewTaskVO;
import cc.adabyte.blog.system.review.entity.ReviewTask;

import java.util.Optional;

public interface ReviewService {
    ReviewTask submit(SubmitReviewRequest request);
    void approve(Long taskId, String reviewerNote, Long coverResourceId, Long avatarResourceId);
    void reject(Long taskId, String reason, String reviewerNote);
    void ignorePending(Long taskId);
    PageResult<ReviewTaskVO> listTasks(Integer status, int page, int size);
    long countPending();
    Optional<ReviewTask> findByContent(String contentType, Long contentId);
    void resolveAndApprove(String contentType, Long contentId, String reviewerNote);
    void resolveAndReject(String contentType, Long contentId, String reason, String reviewerNote);
    void deleteByContent(String contentType, Long contentId);
}
