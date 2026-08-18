package cc.adabyte.blog.boot;

import cc.adabyte.blog.system.config.dto.SiteConfigDto;
import cc.adabyte.blog.system.config.dto.ExternalLinkDto;
import cc.adabyte.blog.system.config.entity.SystemConfig;
import cc.adabyte.blog.system.config.mapper.SystemConfigMapper;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestExecutionListeners(
        listeners = {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class},
        mergeMode = MergeMode.REPLACE_DEFAULTS)
@Transactional
@DisplayName("站点配置 About 数据形状测试")
class SiteConfigShapeTest {

    @Autowired
    private SystemConfigMapper mapper;

    @Autowired
    private SystemConfigService service;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearConfig() {
        SystemConfig existing = mapper.selectByKey("site.config");
        if (existing != null) mapper.deleteById(existing.getId());
    }

    @Test
    @DisplayName("新 About 配置归一化为完整的可编辑结构")
    void newAboutIsNormalized() {
        insertConfig("""
                {
                  "about": {
                    "pin": {"ownerName": "Ada"},
                    "siteContent": {"intro": "记录技术与实践"}
                  }
                }
                """);

        SiteConfigDto result = service.getSiteConfig();

        assertNotNull(result.getAbout());
        assertEquals("Ada", result.getAbout().getPin().getOwnerName());
        assertEquals("记录技术与实践", result.getAbout().getSiteContent().getIntro());
        assertNotNull(result.getAbout().getAbility().getDevStack());
        assertNotNull(result.getAbout().getContact().getOtherSocialPlatform());
        assertNotNull(result.getAbout().getLinks().getItems());
    }

    @Test
    @DisplayName("保存新配置时写入 About")
    void saveAboutConfig() throws Exception {
        SiteConfigDto dto = service.getSiteConfig();
        dto.getAbout().getPin().setOwnerName("Ada");
        service.saveSiteConfig(dto);

        JsonNode saved = objectMapper.readTree(mapper.selectByKey("site.config").getConfigValue());
        assertEquals("Ada", saved.get("about").get("pin").get("ownerName").asText());
    }

    @Test
    @DisplayName("保存链接时保留自定义分组和可选字段")
    void saveGenericLinks() throws Exception {
        SiteConfigDto dto = service.getSiteConfig();
        ExternalLinkDto link = new ExternalLinkDto();
        link.setSection("研究项目");
        link.setName("Demo");
        link.setUrl("https://example.com");
        link.setDescription("说明");
        link.setIcon("12");
        dto.getAbout().getLinks().setItems(List.of(link));
        service.saveSiteConfig(dto);

        JsonNode saved = objectMapper.readTree(mapper.selectByKey("site.config").getConfigValue());
        JsonNode savedLink = saved.get("about").get("links").get("items").get(0);

        assertEquals("研究项目", savedLink.get("section").asText());
        assertEquals("12", savedLink.get("icon").asText());
    }

    private void insertConfig(String json) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("site.config");
        config.setConfigValue(json);
        config.setDescription("测试站点配置");
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        mapper.insert(config);
    }
}
