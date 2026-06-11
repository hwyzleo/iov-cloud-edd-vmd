package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度配置
 * <p>
 * 启用Spring调度支持，用于MDM Part定时同步任务。
 * </p>
 *
 * @author CR-024
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Spring Boot自动配置会扫描@Scheduled注解
}
