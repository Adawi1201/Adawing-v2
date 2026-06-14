package cc.adabyte.blog.system.auth.service.impl;

import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.util.JwtUtil;
import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
import cc.adabyte.blog.system.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    @Value("${jwt.secret}")
    private String jwtSecret;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        SysUser user = verifyPassword(username, password, "用户名或密码错误");
        return JwtUtil.generateToken(user.getUsername(), jwtSecret);
    }

    @Override
    public void changePassword(String username, String oldPwd, String newPwd) {
        SysUser user = verifyPassword(username, oldPwd, "原密码错误");
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        sysUserMapper.updateById(user);
    }

    private SysUser verifyPassword(String username, String password, String errorMsg) {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(errorMsg);
        }
        return user;
    }
}
