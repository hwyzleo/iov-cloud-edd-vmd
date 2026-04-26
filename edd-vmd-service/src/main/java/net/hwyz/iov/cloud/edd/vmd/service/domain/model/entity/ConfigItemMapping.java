package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 配置项映射领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class ConfigItemMapping implements DomainObj<ConfigItemMapping> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置项代码
     */
    private String configItemCode;

    /**
     * 源系统
     */
    private String sourceSystem;

    /**
     * 源系统代码
     */
    private String sourceCode;

    /**
     * 源系统值
     */
    private String sourceValue;

    /**
     * 映射的枚举值编码
     */
    private String targetOptionCode;

    /**
     * 映射值
     */
    private String targetValue;

}
