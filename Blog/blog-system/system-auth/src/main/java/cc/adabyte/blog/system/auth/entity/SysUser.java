package cc.adabyte.blog.system.auth.entity;

import cc.adabyte.blog.system.auth.enums.UserRole;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private Long avatarResourceId;
    private UserStatus status;
    private UserRole role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
