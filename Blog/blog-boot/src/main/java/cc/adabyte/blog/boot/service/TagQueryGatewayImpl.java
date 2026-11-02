package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.TagQueryGateway;
import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TagQueryGatewayImpl implements TagQueryGateway {

    private final TagService tagService;

    @Override
    public List<String> listNames() {
        return tagService.list().stream()
                .map(Tag::getName)
                .toList();
    }
}
