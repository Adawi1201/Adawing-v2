package cc.adabyte.blog.zoom.message.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageSubmitRequest {
    @NotBlank
    private String nickname;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String content;
    private Long refId;
    private String refTitle;
}
