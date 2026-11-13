package cn.easykits.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * easy-crm 后端启动类。
 *
 * <p>包结构约定：{@code cn.easykits.crm.{module}.{controller|service|mapper|entity|dto|vo}}。
 * Mapper 接口统一加 {@code @Mapper} 注解，MyBatis-Plus 自动配置会扫描
 * 启动类所在包下所有带 {@code @Mapper} 的接口，无需集中声明 @MapperScan。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
