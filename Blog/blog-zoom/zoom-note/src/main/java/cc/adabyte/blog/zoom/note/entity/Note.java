package cc.adabyte.blog.zoom.note.entity;

import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note")
public class Note {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private NoteType type;
    private Long sourceId;
    private ContentStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
