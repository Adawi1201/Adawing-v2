package cc.adabyte.blog.system.auth.controller;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.system.auth.dto.ChangePasswordRequest;
import cc.adabyte.blog.system.auth.dto.LoginRequest;
import cc.adabyte.blog.system.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return Result.ok(token);
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }
}
