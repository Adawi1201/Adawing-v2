package cc.adabyte.blog.system.review.mapper;

import cc.adabyte.blog.system.review.entity.ReviewTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {

    @Select("SELECT * FROM review_task WHERE #{status} IS NULL OR status = #{status} ORDER BY submit_time DESC")
    IPage<ReviewTask> selectByStatus(Page<ReviewTask> page, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM review_task WHERE status = 0")
    Long countPending();

    @Select("SELECT * FROM review_task WHERE content_type = #{contentType} AND content_id = #{contentId} LIMIT 1")
    ReviewTask selectByContent(@Param("contentType") String contentType, @Param("contentId") Long contentId);
}
