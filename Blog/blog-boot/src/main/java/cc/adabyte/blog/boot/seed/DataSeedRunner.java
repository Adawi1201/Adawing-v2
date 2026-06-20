package cc.adabyte.blog.boot.seed;

import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.enums.UserRole;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeedRunner implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        ensureTestAdmin();
    }

    private void ensureTestAdmin() {
        String username = "admin";
        SysUser existing = sysUserMapper.selectByUsername(username);
        if (existing != null) {
            log.info("测试账号已存在: {}", username);
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setNickname("Test Admin");
        user.setEmail("admin@adawing.cc");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.ADMIN);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        log.info("已创建测试账号: {} / 密码: admin123", username);
    }
}
