package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 配置项枚举值领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class ConfigItemOption implements DomainObj<ConfigItemOption> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置项编码
     */
    private String configItemCode;

    /**
     * 枚举值编码
     */
    private String code;

    /**
     * 枚举值名称
     */
    private String name;

}
