package cc.adabyte.blog.zoom.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ArticleSource {
    ORIGINAL(0, "原创"),
    AI_GENERATED(1, "Agent生成");

    @EnumValue
    @JsonValue
    private final int value;
    private final String label;

    ArticleSource(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static ArticleSource fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (ArticleSource t : values()) if (t.value == i) return t;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (ArticleSource t : values()) if (t.value == i) return t; }
            catch (NumberFormatException ignored) {}
            try { return ArticleSource.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown ArticleSource: " + v);
    }
}
