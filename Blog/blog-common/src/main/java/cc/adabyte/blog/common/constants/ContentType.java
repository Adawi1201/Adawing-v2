package cc.adabyte.blog.common.constants;

public enum ContentType {
    ARTICLE("article"), NOTE("note"), MESSAGE("message");
    private final String value;
    ContentType(String value) { this.value = value; }
    public String getValue() { return value; }
}
