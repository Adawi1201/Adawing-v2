package cc.adabyte.blog.boot.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局插件配置。
 *
 * <p>注册 {@link PaginationInnerInterceptor}，使 Mapper 中的 {@code Page} 参数
 * 真正生效（追加 LIMIT、执行 COUNT），分页查询返回受控行数与真实 total。
 *
 * <p>不显式指定 DbType，由插件按运行时数据库连接自动识别方言
 * （生产 MySQL / 开发测试 H2），避免硬编码单一方言。
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
