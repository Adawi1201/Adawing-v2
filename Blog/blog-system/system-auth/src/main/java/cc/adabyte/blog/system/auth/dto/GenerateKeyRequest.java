package cc.adabyte.blog.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateKeyRequest {
    @NotBlank
    private String name;
    private String description;
}
