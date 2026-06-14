package cc.adabyte.blog.system.auth.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    DISABLED(0, "禁用"),
    ACTIVE(1, "启用");

    @EnumValue
    @JsonValue
    private final int value;
    private final String label;

    UserStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static UserStatus fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (UserStatus t : values()) if (t.value == i) return t;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (UserStatus t : values()) if (t.value == i) return t; }
            catch (NumberFormatException ignored) {}
            try { return UserStatus.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown UserStatus: " + v);
    }
}
