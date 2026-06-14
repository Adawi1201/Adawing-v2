package cc.adabyte.blog.zoom.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ContentStatus {
    DRAFT(0), PENDING_REVIEW(1), PUBLISHED(2), REJECTED(3), HIDDEN(4);

    @EnumValue
    @JsonValue
    private final int value;

    ContentStatus(int value) { this.value = value; }

    @JsonCreator
    public static ContentStatus fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (ContentStatus t : values()) if (t.value == i) return t;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (ContentStatus t : values()) if (t.value == i) return t; }
            catch (NumberFormatException ignored) {}
            try { return ContentStatus.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown ContentStatus: " + v);
    }
}
