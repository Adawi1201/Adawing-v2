package cc.adabyte.blog.common.event;

import java.util.Set;

public record SiteConfigSavedEvent(Set<Long> resourceIds) {}
