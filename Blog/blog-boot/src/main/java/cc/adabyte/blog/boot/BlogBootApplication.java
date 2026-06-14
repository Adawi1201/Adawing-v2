package cc.adabyte.blog.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("cc.adabyte.blog.**.mapper")
@SpringBootApplication(scanBasePackages = "cc.adabyte.blog")
public class BlogBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogBootApplication.class, args);
    }
}
