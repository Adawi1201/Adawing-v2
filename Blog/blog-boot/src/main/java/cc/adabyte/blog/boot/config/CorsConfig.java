package cc.adabyte.blog.boot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * 允许跨域的来源列表，逗号分隔。
     * 优先级：环境变量 CORS_ALLOWED_ORIGINS > 配置文件 app.cors.allowed-origins > 本地开发默认值。
     * 生产环境必须配置为实际域名，禁止使用 *。
     */
    @Value("${CORS_ALLOWED_ORIGINS:${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}}")
    private String allowedOrigins;

    /**
     * CORS 以最高优先级 Filter 注册：认证/授权 Filter 直接写出的 4xx/5xx 错误响应
     * 发生在 DispatcherServlet 之前，WebMvcConfigurer 方式覆盖不到，只有前置
     * CorsFilter 能让这类错误响应同样携带 CORS 头。
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
