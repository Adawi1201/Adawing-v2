package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.event.SiteConfigSavedEvent;
import cc.adabyte.blog.system.config.dto.SiteConfigDto;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestExecutionListeners(
        listeners = {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class},
        mergeMode = MergeMode.REPLACE_DEFAULTS)
@Transactional
@DisplayName("About 资源绑定测试")
class SiteConfigResourceBindingTest {

    @Autowired
    private SystemConfigService service;

    @Autowired
    private SiteConfigEventCapture eventCapture;

    @BeforeEach
    void resetCapture() {
        eventCapture.event = null;
    }

    @Test
    @DisplayName("收集 About 各区块的头像和图标资源")
    void collectAboutResourceIds() {
        SiteConfigDto dto = new SiteConfigDto();
        dto.setLogo("1");
        dto.setFavicon("2");
        dto.setAbout(TestAboutFixtures.withResourceIds());

        service.saveSiteConfig(dto);

        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 7L, 8L, 9L), eventCapture.event.resourceIds());
    }

    @TestConfiguration
    static class EventCaptureConfiguration {
        @Bean
        SiteConfigEventCapture siteConfigEventCapture() {
            return new SiteConfigEventCapture();
        }
    }

    static class SiteConfigEventCapture {
        private SiteConfigSavedEvent event;

        @EventListener
        public void capture(SiteConfigSavedEvent event) {
            this.event = event;
        }
    }
}
