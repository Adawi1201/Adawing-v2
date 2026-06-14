package cc.adabyte.blog.system.auth.service;

public interface AuthService {
    String login(String username, String password);
    void changePassword(String username, String oldPwd, String newPwd);
}
