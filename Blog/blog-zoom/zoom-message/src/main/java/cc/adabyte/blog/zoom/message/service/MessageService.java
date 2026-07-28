package cc.adabyte.blog.zoom.message.service;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.message.dto.MessageVo;
import cc.adabyte.blog.zoom.message.entity.Message;

public interface MessageService {
    PageResult<MessageVo> list(int page, int size);
    PageResult<Message> listAdmin(Integer status, int page, int size);
    void submit(Message message);
    void approve(Long id, String reply, Long avatarResourceId);
    void reject(Long id, String reason);
    void delete(Long id);

    /** 对已发布留言点赞 +1，返回最新点赞数（含内存未刷回增量）。 */
    long like(Long id);

    /** 将内存中的点赞增量批量刷回数据库。 */
    void syncLikeCountsToDb();
}
