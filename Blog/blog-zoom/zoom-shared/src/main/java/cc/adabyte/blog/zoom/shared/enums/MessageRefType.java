package cc.adabyte.blog.zoom.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MessageRefType {
    ARTICLE(0),
    NOTE(1);

    @EnumValue
    @JsonValue
    private final int value;

    MessageRefType(int value) { this.value = value; }

    @JsonCreator
    public static MessageRefType fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (MessageRefType t : values()) if (t.value == i) return t;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (MessageRefType t : values()) if (t.value == i) return t; }
            catch (NumberFormatException ignored) {}
            try { return MessageRefType.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown MessageRefType: " + v);
    }
}
