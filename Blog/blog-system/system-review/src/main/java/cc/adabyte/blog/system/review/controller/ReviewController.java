package cc.adabyte.blog.system.review.controller;

import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.system.review.dto.ReviewActionRequest;
import cc.adabyte.blog.system.review.dto.ReviewTaskVO;
import cc.adabyte.blog.system.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/tasks")
    public Result<PageResult<ReviewTaskVO>> listTasks(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.listTasks(status, page, size));
    }

    @GetMapping("/stats")
    public Result<Long> countPending() {
        return Result.ok(reviewService.countPending());
    }

    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody SubmitReviewRequest request) {
        reviewService.submit(request);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ReviewActionRequest request) {
        reviewService.approve(id, request.getReviewerNote(), request.getCoverResourceId());
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody ReviewActionRequest request) {
        reviewService.reject(id, request.getReason(), request.getReviewerNote());
        return Result.ok();
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Void> ignorePending(@PathVariable Long id) {
        reviewService.ignorePending(id);
        return Result.ok();
    }
}
