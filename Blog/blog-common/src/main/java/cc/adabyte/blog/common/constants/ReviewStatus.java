package cc.adabyte.blog.common.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReviewStatus {
    PENDING(0), APPROVED(1), REJECTED(2);

    @EnumValue
    @JsonValue
    private final int value;

    ReviewStatus(int value) { this.value = value; }

    @JsonCreator
    public static ReviewStatus fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (ReviewStatus t : values()) if (t.value == i) return t;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (ReviewStatus t : values()) if (t.value == i) return t; }
            catch (NumberFormatException ignored) {}
            try { return ReviewStatus.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown ReviewStatus: " + v);
    }
}
