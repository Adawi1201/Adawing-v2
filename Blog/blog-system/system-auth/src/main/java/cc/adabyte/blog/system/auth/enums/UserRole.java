package cc.adabyte.blog.system.auth.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN(0, "管理员");

    @EnumValue
    @JsonValue
    private final int value;
    private final String label;

    UserRole(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static UserRole fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (UserRole r : values()) if (r.value == i) return r;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (UserRole r : values()) if (r.value == i) return r; }
            catch (NumberFormatException ignored) {}
            try { return UserRole.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown UserRole: " + v);
    }
}
