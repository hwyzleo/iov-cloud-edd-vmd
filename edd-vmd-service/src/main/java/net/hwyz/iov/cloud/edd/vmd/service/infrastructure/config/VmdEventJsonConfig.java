package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * VMD 事件专用 JSON codec 装配（VMD-DSN-CR-070 既有边界例外）。
 * <p>
 * 仅注册 {@link VmdEventJsonCodec}（非 ObjectMapper bean），避免触发
 * Boot {@code @ConditionalOnMissingBean(ObjectMapper.class)} 让位。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Configuration
public class VmdEventJsonConfig {

    @Bean
    public VmdEventJsonCodec vmdEventJsonCodec() {
        return new VmdEventJsonCodec();
    }
}
