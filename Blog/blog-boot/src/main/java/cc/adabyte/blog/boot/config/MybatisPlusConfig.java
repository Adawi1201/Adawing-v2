package cc.adabyte.blog.boot.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局插件配置。
 *
 * <p>此前全站未注册任何 {@link MybatisPlusInterceptor}，导致所有 Mapper 中的
 * {@code Page} 分页参数被静默忽略：查询既不追加 {@code LIMIT}，也不执行
 * {@code COUNT}，{@code getTotal()} 恒为 0，前端分页控件因 {@code total} 为 0
 * 而永不显示。注册分页内部拦截器后分页与计数才真正生效。
 *
 * <p>{@link PaginationInnerInterceptor} 不显式指定 DbType，让其按运行时数据库
 * 连接自动识别方言（生产 MySQL / 开发测试 H2），避免硬编码单一方言。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
