package cc.adabyte.blog.zoom.message.service;

import cc.adabyte.blog.zoom.message.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
public class MailService {

    private final MailProperties mailProperties;

    public MailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public void sendApprovalNotification(String to, String nickname) {
        String subject = "您的评论已通过审核";
        String body = buildApprovalBody(nickname);
        send(to, subject, body);
    }

    public void sendRejectionNotification(String to, String nickname, String reason) {
        String subject = "您的评论审核结果通知";
        String body = buildRejectionBody(nickname, reason);
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            JavaMailSender sender = createSender();
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            sender.send(mimeMessage);
            log.info("[Mail] 已发送邮件: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[Mail] 邮件发送失败: to={}, subject={}", to, subject, e);
        }
    }

    private JavaMailSender createSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getHost());
        sender.setPort(mailProperties.getPort());
        sender.setUsername(mailProperties.getUsername());
        sender.setPassword(mailProperties.getPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }

    private String buildApprovalBody(String nickname) {
        return wrap(nickname, "approved") + footer();
    }

    private String buildRejectionBody(String nickname, String reason) {
        String reasonBlock = reason != null && !reason.isBlank()
                ? "<p style='margin:20px 0 0;padding:14px 16px;background:#fef2f2;"
                  + "border-left:3px solid #ef4444;font-size:14px;color:#991b1b;'>"
                  + "拒绝理由：" + reason + "</p>"
                : "";
        return wrap(nickname, "rejected") + reasonBlock + footer();
    }

    private String wrap(String nickname, String type) {
        String accent = "approved".equals(type) ? "#10b981" : "#64748b";
        String icon = "approved".equals(type) ? "&#10003;" : "&#10005;";
        String title = "approved".equals(type) ? "评论审核通过" : "评论审核结果";
        String message = "approved".equals(type)
                ? "您的评论已通过审核，现已公开发布。感谢您的分享。"
                : "您的评论未能通过审核。如有疑问，欢迎通过其他方式联系我们。";

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f5f1eb;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                <tr>
                <td align="center">
                <table width="580" cellpadding="0" cellspacing="0"
                       style="background:#ffffff;border-radius:12px;overflow:hidden;
                              box-shadow:0 2px 16px rgba(0,0,0,.06);font-family:'Segoe UI',system-ui,-apple-system,sans-serif;">
                <!-- header -->
                <tr>
                <td style="padding:32px 40px 24px;border-bottom:1px solid #ebe5de;">
                <table cellpadding="0" cellspacing="0"><tr>
                <td style="width:36px;height:36px;border-radius:50%%;background:%s;color:#fff;
                           font-size:18px;text-align:center;vertical-align:middle;">%s</td>
                <td style="padding-left:12px;font-size:18px;font-weight:700;color:#2c2416;">%s</td>
                </tr></table>
                </td>
                </tr>
                <!-- body -->
                <tr>
                <td style="padding:32px 40px;">
                <p style="margin:0 0 12px;font-size:15px;color:#5c5142;">Hi <strong style="color:#2c2416;">%s</strong>，</p>
                <p style="margin:0;font-size:15px;line-height:1.8;color:#5c5142;">%s</p>
                """.formatted(accent, icon, title, nickname, message);
    }

    private String footer() {
        return """
                </td>
                </tr>
                <!-- footer -->
                <tr>
                <td style="padding:24px 40px;background:#faf8f5;border-top:1px solid #ebe5de;">
                <p style="margin:0 0 6px;font-size:13px;font-weight:600;color:#9c8f7a;">AdaWing</p>
                <p style="margin:0;font-size:12px;color:#b8a997;">此邮件由系统自动发送，请勿回复。</p>
                </td>
                </tr>
                </table>
                </td>
                </tr>
                </table>
                </body>
                </html>
                """;
    }
}
