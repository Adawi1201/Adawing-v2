package cc.adabyte.blog.zoom.shared.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NoteType {
    PERSONAL(0),
    TECH(1);

    @EnumValue
    @JsonValue
    private final int value;

    NoteType(int value) { this.value = value; }

    @JsonCreator
    public static NoteType fromValue(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) {
            for (NoteType t : values()) if (t.value == i) return t;
        }
        if (v instanceof String s) {
            try { int i = Integer.parseInt(s); for (NoteType t : values()) if (t.value == i) return t; }
            catch (NumberFormatException ignored) {}
            try { return NoteType.valueOf(s); } catch (IllegalArgumentException ignored) {}
        }
        throw new IllegalArgumentException("Unknown NoteType: " + v);
    }
}
